package edu.cmu.cs.lti.script.annotators;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.cmu.cs.lti.ark.pipeline.SemaforFullPipeline;
import edu.cmu.cs.lti.ark.pipeline.parsing.ParsingException;
import edu.cmu.cs.lti.script.model.SemaforConstants;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.maltparser.core.exception.MaltChainedException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Required Stanford Corenlp ssplit, tokenize, pos, lemma
 * <p>
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/23/15
 * Time: 11:01 PM
 */
public class SemaforAnnotator extends AbstractLoggingAnnotator {
    public static final String SEMAFOR_MODEL_PATH = "modelPath";

    public static final String COMPONENT_ID = SemaforAnnotator.class.getSimpleName();

    private SemaforFullPipeline semafor;

    public static final String PARAM_JSON_OUTPUT_REDIRECT = "jsonOutputRedirect";
    @ConfigurationParameter(name = PARAM_JSON_OUTPUT_REDIRECT, mandatory = false)
    private String jsonOutputRidirectDir;

    private boolean redirectJsonOutput;

    // A shared gson object;
    private Gson gson = new Gson();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        File semaforModelDir = new File((String) aContext.getConfigParameterValue(SEMAFOR_MODEL_PATH));

        try {
            logger.info(String.format("Initializing from model : %s", semaforModelDir.getCanonicalPath()));
            semafor = new SemaforFullPipeline(semaforModelDir);
        } catch (IOException | URISyntaxException | ClassNotFoundException | MaltChainedException e) {
            e.printStackTrace();
        }

        if (jsonOutputRidirectDir != null) {
            if (!FileUtils.ensureDirectory(jsonOutputRidirectDir)) {
                throw new ResourceInitializationException(new IOException("Cannot ensure output directory : " +
                        jsonOutputRidirectDir));
            }
            redirectJsonOutput = true;
        } else {
            redirectJsonOutput = false;
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(String.format("Processing %s with Semafor.", UimaConvenience.getDocumentName(aJCas)));
        try {
            if (!skipMainView) {
                annotateSemafor(aJCas);
            }
            for (JCas view : getAdditionalViews(aJCas)) {
                annotateSemafor(view);
            }
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void annotateSemafor(JCas aJCas) throws IOException {
        String docName = UimaConvenience.getDocumentName(aJCas);
        BufferedWriter jsonRedirectOutput = null;

        if (redirectJsonOutput) {
            jsonRedirectOutput = new BufferedWriter(new FileWriter(
                    new File(jsonOutputRidirectDir, docName + ".json"))
            );
        }

        for (Sentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            List<Token> semaforTokens = new ArrayList<>();

            List<StanfordCorenlpToken> words = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            for (int i = 0; i < words.size(); i++) {
                Word word = words.get(i);

                String surface = word.getCoveredText();
                String lemma = word.getLemma();

                // "|" symbol is used by semafor for special purpose, we replace all "|" occurred in text before
                // passed in.
                surface = surface.replaceAll("\\|", "-");
                lemma = lemma.replaceAll("\\|", "-");

                semaforTokens.add(new Token(i + 1, surface, lemma, word.getCoarsePos(),
                        word.getPos(), null, null, null, null, null));
            }

            try {
                SemaforParseResult result = semafor.parse(semaforTokens);
                if (redirectJsonOutput) {
                    String resultJson = result.toJson();
                    JsonObject semaforJson = gson.fromJson(resultJson, JsonObject.class);
                    JsonArray tokenOffsetJson = getTokenOffsetJson(words);
                    semaforJson.add("offsets", tokenOffsetJson);
                    String combinedResult = semaforJson.toString();
                    jsonRedirectOutput.write(combinedResult);
                    jsonRedirectOutput.write("\n");
                }
                annotateSemaforSentence(aJCas, sentence, result);
            } catch (ParsingException | IOException e) {
                e.printStackTrace();
            }
        }

        if (redirectJsonOutput) {
            jsonRedirectOutput.close();
        }
    }

    class JsonToken {
        String surface;
        List<Integer> offset = new ArrayList<>();
    }

    private <T extends Word> JsonArray getTokenOffsetJson(List<T> tokens) {
        List<JsonToken> allTokens = new ArrayList<>();

        for (Word token : tokens) {
            JsonToken t = new JsonToken();
            t.offset.add(token.getBegin());
            t.offset.add(token.getEnd());
            t.surface = token.getCoveredText();
            allTokens.add(t);
        }

        return gson.fromJson(gson.toJson(allTokens), JsonArray.class);
    }

    private void annotateSemaforSentence(JCas aJCas, Sentence sentence, SemaforParseResult result) {
        List<StanfordCorenlpToken> words = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
        int frameId = 0;

        for (SemaforParseResult.Frame frame : result.frames) {
            SemaforAnnotationSet annotationSet = new SemaforAnnotationSet(aJCas, sentence.getBegin(),
                    sentence.getEnd());
            UimaAnnotationUtils.finishAnnotation(annotationSet, COMPONENT_ID, frameId, aJCas);
            frameId++;

            SemaforParseResult.Frame.NamedSpanSet target = frame.target;
            annotationSet.setFrameName(target.name);
            List<SemaforLayer> layers = new ArrayList<>();

            SemaforLabel targetLabel = namedSpan2Label(aJCas, words, target, SemaforConstants.TARGET_LAYER_NAME);
            SemaforLayer targetLayer = new SemaforLayer(aJCas, targetLabel.getBegin(), targetLabel.getEnd());
            targetLayer.setName(SemaforConstants.TARGET_LAYER_NAME);
            FSArray targetLabelArray = new FSArray(aJCas, 1);
            targetLabelArray.set(0, targetLabel);
            targetLayer.setLabels(targetLabelArray);
            layers.add(targetLayer);

            int roleId = 0;
            for (SemaforParseResult.Frame.ScoredRoleAssignment roleAssignment : frame.annotationSets) {
                SemaforLayer layer = new SemaforLayer(aJCas);
                UimaAnnotationUtils.finishAnnotation(layer, COMPONENT_ID, roleId, aJCas);
                roleId++;

                int rank = roleAssignment.rank;
                double score = roleAssignment.score;

                layer.setName(SemaforConstants.FRAME_ELEMENT_LAYER_NAME);
                layer.setRank(rank);
                layer.setScore(score);
                layers.add(layer);

                List<SemaforLabel> labels = new ArrayList<>();

                for (SemaforParseResult.Frame.NamedSpanSet frameElement : roleAssignment.frameElements) {
                    labels.add(namedSpan2Label(aJCas, words, frameElement, null));
                }
                layer.setLabels(FSCollectionFactory.createFSArray(aJCas, labels));
            }
            annotationSet.setLayers(FSCollectionFactory.createFSArray(aJCas, layers));
        }
    }

    private SemaforLabel namedSpan2Label(JCas aJCas, List<StanfordCorenlpToken> words,
                                         SemaforParseResult.Frame.NamedSpanSet namedSpanSet, String name) {
        // Assume only continous span is predicted, so return only one label.
        int first = -1;
        int last = -1;
        for (SemaforParseResult.Frame.Span span : namedSpanSet.spans) {
            if (first == -1 || span.start < first) {
                first = span.start;
            }
            if (last == -1 || span.end > last) {
                last = span.end - 1;
            }
        }

        SemaforLabel label = new SemaforLabel(aJCas, words.get(first).getBegin(), words.get(last).getEnd());
        if (name == null) {
            label.setName(namedSpanSet.name);
        } else {
            label.setName(name);
        }
        UimaAnnotationUtils.finishAnnotation(label, COMPONENT_ID, 0, aJCas);
        return label;
    }
}
