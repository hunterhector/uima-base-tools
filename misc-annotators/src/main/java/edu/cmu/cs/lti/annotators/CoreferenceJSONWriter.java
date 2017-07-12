package edu.cmu.cs.lti.annotators;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.FileUtils;
import edu.cmu.cs.lti.utils.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Write out entity and entity coreference as JSON format.
 * 1. Title and body are indexed separately.
 * 2. Indices are token base.
 * Date: 6/7/17
 * Time: 2:49 PM
 *
 * @author Zhengzhong Liu
 */
public class CoreferenceJSONWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_SOURCE_TAGGED_TEXT_FOLDER = "sourceTaggedText";

    public static final String PARAM_OUTPUT_PATH = "outputPath";

    @ConfigurationParameter(name = PARAM_OUTPUT_PATH)
    private File outputFile;

    @ConfigurationParameter(name = PARAM_SOURCE_TAGGED_TEXT_FOLDER, mandatory = false)
    private File sourceTaggedDir;

    // Use source text to correct the spans.
    private boolean spanCorrection = false;

    private BufferedWriter writer;

    private AtomicInteger numDocuments;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        File dir = outputFile.getParentFile();
        FileUtils.ensureDirectory(dir);

        if (sourceTaggedDir != null) {
            spanCorrection = true;
        }

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        numDocuments = new AtomicInteger(0);
    }

    // Represent entities used by GSon package.
    class JsonEntityMention {
        String surface;
        List<Integer> loc = new ArrayList<>();
        int head;
        String source;
    }

    class JsonEntity {
        List<JsonEntityMention> mentions = new ArrayList<>();
    }

    class Document {
        String docno;
        List<JsonEntity> coreferences = new ArrayList<>();
    }

    private int getNumTokensInTitle(String mergedText) {
        String[] parts = mergedText.split("\\r\\n|\\n|\\r", 2);
        String title = parts[0];
        return title.split(" ").length;
    }

    private int getNumTokensInBody(String mergedText) {
        String[] parts = mergedText.split("\\r\\n|\\n|\\r", 2);
        String body = parts[1];
        return body.split(" ").length;
    }


    /**
     * Load the text from a JSON file. Merge title and body together, since this is how the UIMA deal with the titles.
     *
     * @param linkerResultFile
     * @return
     * @throws IOException
     */
    private JsonObject loadDocument(File linkerResultFile) throws IOException {
        String jsonStr = org.apache.commons.io.FileUtils.readFileToString(linkerResultFile);
        Gson gson = new Gson();
        gson.toJson(jsonStr);
        JsonParser parser = new JsonParser();
        JsonObject document = parser.parse(jsonStr).getAsJsonObject();
        return document;
    }

    /**
     * Match the words we annotated are matching with the tokenized word string (tokenized word string can simply be
     * got by a space)
     *
     * @param words
     * @param tokenizedText
     * @param <T>
     */
    private <T extends Word> void matchTokens(List<T> words, String tokenizedText) {
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        for (T word : words) {
            sb.append(delimiter);
            sb.append(word.getCoveredText());
            delimiter = " ";
        }

        String ourTokenizedText = sb.toString();

        int[] offsets = StringUtils.translateOffset(ourTokenizedText, tokenizedText);

        int baseTokenIndex = 0;

        Set<Integer> alteredTokenIndices = new HashSet<>();

        for (int baseIndex = 0; baseIndex < offsets.length; baseIndex++) {
            char baseChar = tokenizedText.charAt(baseIndex);
            if (baseIndex == offsets.length - 1 || baseChar == ' ') {
                for (Integer alteredTokenIndex : alteredTokenIndices) {
                    words.get(alteredTokenIndex).setIndex(baseTokenIndex);
                }
                alteredTokenIndices = new HashSet<>();
                baseTokenIndex++;
            } else {
                int[] ourTokenMap = getChar2Token(ourTokenizedText);
                int alteredIndex = offsets[baseIndex];
                int alteredTokenIndex = ourTokenMap[alteredIndex];
                if (alteredTokenIndex != -1) {
                    alteredTokenIndices.add(alteredTokenIndex);
                }
            }
        }
    }

    private int[] getChar2Token(String text) {
        char[] ourTokenizedChars = text.toCharArray();
        int[] char2Token = new int[ourTokenizedChars.length];
        int tokenIndex = 0;
        for (int index = 0; index < ourTokenizedChars.length; index++) {
            if (ourTokenizedChars[index] == ' ') {
                char2Token[index] = -1;
                tokenIndex++;
            } else {
                char2Token[index] = tokenIndex;
            }
        }
        return char2Token;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String articleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();
        List<StanfordCorenlpToken> words = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));
        int numTokenInTitle = 0;

        boolean willDo = true;

        if (spanCorrection) {
            File tagmeResultFile = new File(sourceTaggedDir, articleName);
            if (tagmeResultFile.exists()) {
                String title = null;
                String bodyText = null;
                try {
                    JsonObject jsonDocument = loadDocument(tagmeResultFile);
                    title = jsonDocument.get("title").getAsString();
                    bodyText = jsonDocument.get("bodyText").getAsString();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                numTokenInTitle = title.split(" ").length;
                matchTokens(words, title + " " + bodyText);
            } else {
                // Will not do this document if we didn't find a source spot file.
                willDo = false;
            }
        } else {
            int i = 0;
            for (StanfordCorenlpToken token : words) {
                token.setIndex(i);
                i++;
            }
            numTokenInTitle = getNumTokensInTitle(aJCas.getDocumentText());
        }

        if (!willDo) {
            return;
        }

        System.out.print(String.format("[%s] Processed %d documents.\r", LocalDateTime.now(),
                numDocuments.incrementAndGet()));

        Document document = new Document();
        document.docno = articleName;

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            Collection<StanfordEntityMention> mentions = FSCollectionFactory.create(entity.getEntityMentions(),
                    StanfordEntityMention.class);
            if (mentions.size() > 1) {
                JsonEntity jsonEntity = new JsonEntity();
                for (StanfordEntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(),
                        StanfordEntityMention.class)) {

                    Span tokenSpan = getTokenSpan(mention);
                    StanfordCorenlpToken headToken = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    int headTokenIndex = headToken.getIndex();

                    // The title sometimes got glued with the content. Let's split them.
                    if (tokenSpan.getBegin() < numTokenInTitle) {
                        // The beginning token is within the title.
                        if (tokenSpan.getEnd() > numTokenInTitle) {
                            // The end token is not within the title, we need to split this into two.
                            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(
                                    StanfordCorenlpToken.class, mention);

                            int titlePortionBegin = Integer.MAX_VALUE;
                            int titlePortionEnd = 0;

                            int bodyPortionBegin = Integer.MAX_VALUE;
                            int bodyPortionEnd = 0;

                            for (StanfordCorenlpToken token : tokens) {
                                if (token.getIndex() < numTokenInTitle) {
                                    if (token.getBegin() < titlePortionBegin) {
                                        titlePortionBegin = token.getBegin();
                                    }
                                    if (token.getEnd() > titlePortionEnd) {
                                        titlePortionEnd = token.getEnd();
                                    }
                                } else {
                                    if (token.getBegin() < bodyPortionBegin) {
                                        bodyPortionBegin = token.getBegin();
                                    }
                                    if (token.getEnd() > bodyPortionEnd) {
                                        bodyPortionEnd = token.getEnd();
                                    }
                                }
                            }

                            StanfordEntityMention titleMention = new StanfordEntityMention(aJCas, titlePortionBegin,
                                    titlePortionEnd);
                            UimaAnnotationUtils.finishAnnotation(titleMention, COMPONENT_ID, 0, aJCas);
                            Span titleTokenSpan = getTokenSpan(titleMention);

                            StanfordEntityMention bodyMention = new StanfordEntityMention(aJCas, bodyPortionBegin,
                                    bodyPortionEnd);
                            UimaAnnotationUtils.finishAnnotation(bodyMention, COMPONENT_ID, 0, aJCas);
                            Span bodyTokenSpan = getTokenSpan(bodyMention);

                            boolean hasNounOverlap = checkNounOverlap(titleMention, bodyMention);

                            boolean addTitlePortion;
                            boolean addBodyPortion;

                            if (headTokenIndex < numTokenInTitle) {
                                addTitlePortion = true;
                                addBodyPortion = hasNounOverlap;
                            } else {
                                addBodyPortion = true;
                                addTitlePortion = hasNounOverlap;
                            }

                            if (addTitlePortion) {
                                // Head token belongs to title.
                                jsonEntity.mentions.add(createEntityMention(titleMention.getCoveredText(),
                                        titleTokenSpan.getBegin(), titleTokenSpan.getEnd(),
                                        UimaNlpUtils.findHeadFromStanfordAnnotation(titleMention).getIndex(), "title"));
                            }

                            if (addBodyPortion) {
                                jsonEntity.mentions.add(createEntityMention(bodyMention.getCoveredText(),
                                        bodyTokenSpan.getBegin() - numTokenInTitle,
                                        bodyTokenSpan.getEnd() - numTokenInTitle,
                                        UimaNlpUtils.findHeadFromStanfordAnnotation(bodyMention)
                                                .getIndex() - numTokenInTitle,
                                        "body"
                                ));
                            }
                        } else {
                            // The whole mention is in title, let's add a title entry.
                            jsonEntity.mentions.add(createEntityMention(
                                    mention.getCoveredText(), tokenSpan.getBegin(), tokenSpan.getEnd(),
                                    headTokenIndex, "title"));
                        }
                    } else {
                        // The whole mention is in body, let's add a body entry.
                        jsonEntity.mentions.add(createEntityMention(
                                mention.getCoveredText(), tokenSpan.getBegin() - numTokenInTitle,
                                tokenSpan.getEnd() - numTokenInTitle,
                                headTokenIndex - numTokenInTitle, "body"));
                    }
                }
                document.coreferences.add(jsonEntity);
            }
        }

        Gson gson = new Gson();
        String jsonStr = gson.toJson(document);

        try {
            writer.write(jsonStr);
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JsonEntityMention createEntityMention(String text, int begin, int end, int head, String source) {
        JsonEntityMention jsonEntityMention = new JsonEntityMention();
        jsonEntityMention.surface = text;
        jsonEntityMention.loc.add(begin);
        jsonEntityMention.loc.add(end);
        jsonEntityMention.head = head;
        jsonEntityMention.source = source;
        return jsonEntityMention;
    }

    private boolean checkNounOverlap(StanfordEntityMention mention1, StanfordEntityMention mention2) {
        List<StanfordCorenlpToken> mention1Tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention1);
        List<StanfordCorenlpToken> mention2Tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention2);

        Set<String> words = new HashSet<>();
        for (StanfordCorenlpToken mention1Token : mention1Tokens) {
            if (mention1Token.getPos().startsWith("N")) {
                words.add(mention1Token.getLemma().toLowerCase());
            }
        }

        for (StanfordCorenlpToken mention2Token : mention2Tokens) {
            String tokenStr = mention2Token.getLemma().toLowerCase();
            if (words.contains(tokenStr)) {
                return true;
            }
        }
        return false;
    }

    private Span getTokenSpan(StanfordEntityMention mention) {
        int beginToken = -1;
        int endToken = -1;

        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
            if (beginToken == -1) {
                beginToken = token.getIndex();
            }

            if (endToken == -1) {
                endToken = token.getIndex();
            }

            if (token.getIndex() < beginToken) {
                beginToken = token.getIndex();
            }

            if (token.getIndex() > endToken) {
                endToken = token.getIndex();
            }
        }
        return Span.of(beginToken, endToken + 1);
    }


    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        System.out.println();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
