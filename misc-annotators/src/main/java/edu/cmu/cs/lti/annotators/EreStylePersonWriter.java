package edu.cmu.cs.lti.annotators;

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

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<StanfordEntityMention> entityMentions = JCasUtil.select(aJCas, StanfordEntityMention
                .class);

        int index = 0;
        for (StanfordEntityMention entityMention : entityMentions) {
            entityMention.setId("m-" + index);
            index++;
        }

        Collection<Entity> entities = JCasUtil.select(aJCas, Entity.class);

        Set<EntityMention> nonSingletons = new HashSet<>();
        Map<String, Collection<EntityMention>> personalEntities = new HashMap<>();
        int eid = 0;
        for (Entity entity : entities) {
            if (isPersonalEntity(entity)) {
                Collection<EntityMention> mentions = FSCollectionFactory.create(entity.getEntityMentions(),
                        EntityMention.class);
                personalEntities.put("ent-" + eid, mentions);
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
                personalEntities.put("ent-" + eid, Collections.singletonList(entityMention));
                eid++;
            }
        }

        String docid = UimaConvenience.getDocId(aJCas);

        String normalizedDocId = FilenameUtils.getBaseName(docid);

        Document personalEntityDoc = createXml(personalEntities, normalizedDocId);

        XMLOutputter outter = new XMLOutputter();
        outter.setFormat(Format.getPrettyFormat());

        try {
            outter.output(personalEntityDoc, new FileWriter(new File(outputDir, docid + ".rich_ere.xml")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getEntityMentionString(EntityMention mention) {
        int numTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention).size();
        if (numTokens > 5) {
            StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
            return head.getCoveredText().replaceAll("\n", " ");
        } else {
            return mention.getCoveredText().replaceAll("\n", " ");
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

    private Document createXml(Map<String, Collection<EntityMention>> uimaEntities, String docid) {
        Document doc = new Document();
        Element root = new Element("deft_ere");
        root.setAttribute("doc_id", docid);
        doc.setRootElement(root);

        Element entities = new Element("entities");

        for (Map.Entry<String, Collection<EntityMention>> entityEntry : uimaEntities.entrySet()) {
            Element entity = new Element("entity");
            entity.setAttribute("id", entityEntry.getKey());
            entity.setAttribute("type", "PER");
            entity.setAttribute("specificity", "specific");

            for (EntityMention uimaMention : entityEntry.getValue()) {
                ComponentAnnotation anno = getEntityAnnotation(uimaMention);

                Element entityMention = new Element("entity_mention");
                entityMention.setAttribute("id", uimaMention.getId());
                entityMention.setAttribute("source", docid);
                entityMention.setAttribute("offset", String.valueOf(anno.getBegin()));
                entityMention.setAttribute("length", String.valueOf(anno.getEnd() - anno.getBegin()));
                Element mentionText = new Element("mention_text");
                entity.addContent(entityMention.addContent(mentionText.addContent(
                        anno.getCoveredText().replaceAll("\n", " "))));
            }
            entities.addContent(entity);
        }

        root.addContent(entities);

        return doc;
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
                    "her", "herself", "we", "us", "ourselves", "yourselves", "they", "them", "their", "themselves"};
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
        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                EreStylePersonWriter.class, typeSystemDescription,
                EreStylePersonWriter.PARAM_OUTPUT_PATH, outputDir,
                EreStylePersonWriter.PARAM_NOUN_WORDS_FILE, nounList,
                EreStylePersonWriter.PARAM_LANGUAGE, language
        );
        SimplePipeline.runPipeline(reader, writer);
    }
}
