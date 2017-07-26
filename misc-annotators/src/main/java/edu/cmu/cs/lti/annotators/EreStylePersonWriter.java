package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 7/19/17
 * Time: 3:59 PM
 *
 * @author Zhengzhong Liu
 */
public class EreStylePersonWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_PATH = "outputPath";
    @ConfigurationParameter(name = PARAM_OUTPUT_PATH)
    private File outputDir;

    public static final String PARAM_NOUN_WORDS_FILE = "nounWordsFile";
    @ConfigurationParameter(name = PARAM_NOUN_WORDS_FILE)
    private File nounWordsFile;

    public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE)
    private String language;

    private Set<String> personalNouns;

    private Pattern nwAuthorPattern = Pattern.compile("<AUTHOR>(.*?)</AUTHOR>");

    private Pattern dfAuthorPattern = Pattern.compile("<post\\sauthor=\"(.*?)\"\\s");

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        if (!FileUtils.ensureDirectory(outputDir)) {
            logger.info("Cannot found or create directory: " + outputDir + " , please check the path.");
        }
        try {
            loadPersonalWords();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    class EreEntity {
        List<EreMention> mentions;
        String id;
        String type = "PER";
        String specificity = "specific";

        public EreEntity(String id) {
            this.mentions = new ArrayList<>();
            this.id = id;
        }

        public EreEntity(List<EreMention> mentions, String id) {
            this.mentions = new ArrayList<>(mentions);
            this.id = id;
        }

        public String toString() {
            return String.format("ID:%s, type: %s", id, type);
        }
    }

    class EreMention {
        String id;
        int offset;
        int length;
        int end;
        String noun_type;
        String text;

        public EreMention(EntityMention mention) {
            this.id = mention.getId();
            ComponentAnnotation anno = getEntityAnnotation(mention);
            this.length = anno.getEnd() - anno.getBegin();
            this.offset = anno.getBegin();
            this.end = anno.getEnd();
            this.noun_type = detectNounType(mention);
            this.text = anno.getCoveredText().replace("\n", " ");
        }

        public EreMention(Span mentionSpan, String documentText, String mentionId) {
            this.id = mentionId;
            this.length = mentionSpan.getEnd() - mentionSpan.getBegin();
            this.end = mentionSpan.getEnd();
            this.offset = mentionSpan.getBegin();
            this.noun_type = "NAM";
            this.text = documentText.substring(mentionSpan.getBegin(), mentionSpan.getEnd()).replaceAll("\n", " ");
        }

        public String toString() {
            return String.format("%s: %s [%d:%d], type: %s", id, text, offset, end, noun_type);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<EntityMention> entityMentions = JCasUtil.select(aJCas, EntityMention.class);
        String docid = FilenameUtils.getBaseName(UimaConvenience.getDocId(aJCas));

        int mentionIndex = 0;
        for (EntityMention entityMention : entityMentions) {
            entityMention.setId("m-" + mentionIndex);
            mentionIndex++;
        }

        Collection<Entity> entities = JCasUtil.select(aJCas, Entity.class);

        List<EreEntity> ereEntities = new ArrayList<>();

        Map<Span, EreEntity> entityLookup = new HashMap<>();

        Set<EntityMention> nonSingletons = new HashSet<>();

        int eid = 0;
        for (Entity entity : entities) {
            if (isPersonalEntity(entity)) {
                Collection<EntityMention> mentions = FSCollectionFactory.create(entity.getEntityMentions(),
                        EntityMention.class);

                nonSingletons.addAll(mentions);

                List<EreMention> ereMentions = mentions.stream().map(EreMention::new).collect(Collectors.toList());

                EreEntity ereEntity = new EreEntity(ereMentions, "ent-" + eid);

                for (EreMention ereMention : ereMentions) {
                    entityLookup.put(Span.of(ereMention.offset, ereMention.end), ereEntity);
                }

                ereEntities.add(ereEntity);

                eid++;
            }
        }

        for (EntityMention entityMention : entityMentions) {
            if (nonSingletons.contains(entityMention)) {
                // only check for singleton here.
                continue;
            }
            String type = getEntityMentionType(entityMention);
            if (type != null && type.equals("PERSON")) {
                EreMention ereMention = new EreMention(entityMention);
                EreEntity ereEntity = new EreEntity(Collections.singletonList(ereMention), "ent-" + eid);
                ereEntities.add(ereEntity);
                entityLookup.put(Span.of(ereMention.offset, ereMention.end), ereEntity);
                eid++;
            }
        }

        ArrayListMultimap<String, Span> authorEntities = findAuthorsWithRegex(aJCas);
        String documentText = aJCas.getDocumentText();
        for (String authorName : authorEntities.keySet()) {
            List<Span> authors = authorEntities.get(authorName);

            EreEntity ereEntity = null;

            for (Span authorSpan : authors) {
                if (entityLookup.containsKey(authorSpan)) {
                    ereEntity = entityLookup.get(authorSpan);
                }
            }

            if (ereEntity == null) {
                ereEntity = new EreEntity("ent-" + eid);
                eid++;
            }

            for (Span authorSpan : authors) {
                if (entityLookup.containsKey(authorSpan)) {
                    continue;
                }

                EreMention authorMention = new EreMention(authorSpan, documentText, "m-" + mentionIndex);
                ereEntity.mentions.add(authorMention);
                mentionIndex++;
            }

            ereEntities.add(ereEntity);
        }

        Document personalEntityDoc = createXml(ereEntities, docid);

        XMLOutputter outter = new XMLOutputter();
        outter.setFormat(Format.getPrettyFormat());

        try {
            outter.output(personalEntityDoc, new FileWriter(new File(outputDir, docid + ".rich_ere.xml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private ComponentAnnotation getEntityAnnotation(EntityMention mention) {
        int numTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention).size();
        if (numTokens > 5) {
            return UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
        } else {
            return mention;
        }
    }

    private Document createXml(List<EreEntity> ereEntities, String docid) {
        Document doc = new Document();
        Element root = new Element("deft_ere");
        root.setAttribute("doc_id", docid);
        doc.setRootElement(root);

        Element entities = new Element("entities");

        for (EreEntity ereEntity : ereEntities) {
            Element entity = new Element("entity");
            entity.setAttribute("id", ereEntity.id);
            entity.setAttribute("type", ereEntity.type);
            entity.setAttribute("specificity", ereEntity.specificity);

            for (EreMention ereMention : ereEntity.mentions) {
                Element entityMention = new Element("entity_mention");
                entityMention.setAttribute("id", ereMention.id);
                entityMention.setAttribute("source", docid);
                entityMention.setAttribute("offset", String.valueOf(ereMention.offset));
                entityMention.setAttribute("length", String.valueOf(ereMention.end - ereMention.offset));
                entityMention.setAttribute("noun_type", ereMention.noun_type);
                Element mentionText = new Element("mention_text");
                entity.addContent(entityMention.addContent(mentionText.addContent(ereMention.text)));
            }
            entities.addContent(entity);
        }
        root.addContent(entities);

        return doc;
    }

    private ArrayListMultimap<String, Span> findAuthorsWithRegex(JCas aJCas) {
        JCas originalView = JCasUtil.getView(aJCas, UimaConst.inputViewName, aJCas);

        String text = originalView.getDocumentText();

        Matcher nwMatcher = nwAuthorPattern.matcher(text);
        ArrayListMultimap<String, Span> results = ArrayListMultimap.create();

        while (nwMatcher.find()) {
//            logger.info(nwMatcher.group(1) + " " + nwMatcher.start(1) + " " + nwMatcher.end(1));
            results.put(nwMatcher.group(1), Span.of(nwMatcher.start(1), nwMatcher.end(1)));
        }

//        logger.info(text);
        Matcher dfMatcher = dfAuthorPattern.matcher(text);
        while (dfMatcher.find()) {
//            logger.info(dfMatcher.group(1) + " " + dfMatcher.start(1) + " " + dfMatcher.end(1));
            results.put(dfMatcher.group(1), Span.of(dfMatcher.start(1), dfMatcher.end(1)));
        }

        return results;
    }

    private String detectNounType(EntityMention mention) {
        StanfordCorenlpToken mentionHead = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
        if (mentionHead.getPos().startsWith("PR") || mentionHead.getPos().startsWith("W")) {
            return "PRO";
        } else {
            if (mention.getEntityType() != null) {
                return "NAM";
            } else {
                return "NOM";
            }
        }
    }

    private void loadPersonalWords() throws IOException {
        personalNouns = new HashSet<>();
        for (String line : org.apache.commons.io.FileUtils.readLines(nounWordsFile)) {
            String[] parts = line.trim().split(" ");
            if (parts.length == 1) {
                personalNouns.add(parts[0]);
            }
        }

        String[] additionalPersonalWords;
        if (language.equals("zh")) {
            // Add Chinese pronouns.
            additionalPersonalWords = new String[]{
                    "我", "你", "我", "他", "它", "她", "俺", "自己", "你们", "我们", "此人", "阁下", "足下",
                    "咱", "咱们", "她们", "他们", "它们", "俺们", "大家", "本人", "人家", "某", "尔等",
                    "鄙人", "敝人", "吾", "小妹", "洒家", "小弟", "愚兄", "愚姊", "愚姐", "小生", "晚生", "后学", "不才",
                    "我等", "老子", "老娘", "朕", "孤家", "不穀", "寡人", "哀家", "本王", "本座", "臣", "微臣",
                    "下臣", "下官", "卑职", "本官", "本将", "末将", "在下", "草民", "这货", "老奴", "妾",
                    "贫僧", "贫尼", "老衲", "小僧", "贫道", "小道", "草民", "区区", "小人", "小可", "小的"
            };
        } else {
            // Treat the rest as english.
            additionalPersonalWords = new String[]{"me", "myself", "you", "yourself", "he", "him", "himself", "she",
                    "her", "herself", "we", "us", "ourselves", "yourselves", "they", "them", "their", "themselves",
                    "who", "whose"};
        }

        for (String englishPronoun : additionalPersonalWords) {
            personalNouns.add(englishPronoun);
        }
    }

    private boolean isPersonalEntity(Entity entity) {
        Map<String, Integer> typeCount = new HashMap<>();
        for (int i = 0; i < entity.getEntityMentions().size(); i++) {
            EntityMention mention = entity.getEntityMentions(i);
            String entityType = getEntityMentionType(mention);

            if (entityType != null) {
                increment(typeCount, entityType);
            }
        }

        List<Map.Entry<String, Integer>> counts = typeCount.entrySet().stream().sorted(Map.Entry.comparingByValue
                (Collections.reverseOrder())).collect(Collectors.toList());

        if (counts.size() > 0) {
            if (counts.get(0).getKey().equals("PERSON")) {
                return true;
            }
        }
        return false;
    }

    private String getEntityMentionType(EntityMention mention) {
        String entityType = mention.getEntityType();
        if (entityType == null) {
            StanfordCorenlpToken headword = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
            if (headword != null) {
                String lemma = headword.getLemma();
                if (personalNouns.contains(lemma.toLowerCase())) {
                    entityType = "PERSON";
//                        logger.info("Mention is " + mention.getCoveredText());
//                        logger.info("Head lemma is " + lemma);
                } else if (lemma.equals("I")) {
                    // The only capital comparison.
                    entityType = "PERSON";
                }
            }

            if (language.equals("zh")) {
                CharacterAnnotation headCharacter = UimaNlpUtils.findHeadCharacterFromZparAnnotation(mention);
                if (headCharacter != null) {
                    String head = headCharacter.getCoveredText();
//                        logger.info("The head character is " + head);
                    if (personalNouns.contains(head)) {
//                            logger.info("Mention is " + mention.getCoveredText());
//                            logger.info("Head character is " + head);
                        entityType = "PERSON";
                    }
                }
            }
        }

        return entityType;
    }

    private void increment(Map<String, Integer> counts, String key) {
        if (counts.containsKey(key)) {
            counts.put(key, counts.get(key) + 1);
        } else {
            counts.put(key, 1);
        }
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputDir = argv[0];
        String outputDir = argv[1];
        String nounList = argv[2];
        String language = argv[3];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                inputDir);

        AnalysisEngineDescription whEntities = AnalysisEngineFactory.createEngineDescription(WhRcModResoluter.class,
                typeSystemDescription);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                EreStylePersonWriter.class, typeSystemDescription,
                EreStylePersonWriter.PARAM_OUTPUT_PATH, outputDir,
                EreStylePersonWriter.PARAM_NOUN_WORDS_FILE, nounList,
                EreStylePersonWriter.PARAM_LANGUAGE, language
        );
        if (language.equals("en")) {
            SimplePipeline.runPipeline(reader, whEntities, writer);
        } else {
            SimplePipeline.runPipeline(reader, writer);
        }
    }
}
