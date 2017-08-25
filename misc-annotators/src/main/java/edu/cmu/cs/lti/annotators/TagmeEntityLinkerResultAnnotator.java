package edu.cmu.cs.lti.annotators;

import com.google.common.collect.Lists;
import com.google.gson.*;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/15/17
 * Time: 4:56 PM
 *
 * @author Zhengzhong Liu
 */
public class TagmeEntityLinkerResultAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_ENTITY_RESULT_FOLDER = "entityResultFolder";
    @ConfigurationParameter(name = PARAM_ENTITY_RESULT_FOLDER)
    private File linkerResultFolder;

    public static final String PARAM_USE_TOKEN = "useToken";
    @ConfigurationParameter(name = PARAM_USE_TOKEN)
    private boolean useToken;

    public static final String PARAM_ADDITIONAL_VIEW = "additionalViews";
    @ConfigurationParameter(name = PARAM_ADDITIONAL_VIEW)
    private List<String> additionalViews;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    class EntityAnnotation {
        String entityId;
        int begin;
        int end;
        int offset;

        public EntityAnnotation(String entityId, int begin, int end, int offset) {
            this.entityId = entityId;
            this.begin = begin;
            this.end = end;
            this.offset = offset;
        }

        private int getBegin() {
            return begin + offset;
        }

        private int getEnd() {
            return end + offset;
        }
    }

    private JsonObject parseResultFile(File linkerResultFile) throws IOException {
        String jsonStr = FileUtils.readFileToString(linkerResultFile);
        Gson gson = new Gson();
        gson.toJson(jsonStr);
        JsonParser parser = new JsonParser();
        JsonObject document = parser.parse(jsonStr).getAsJsonObject();
        return document;
    }

    private int countTokensInJson(JsonObject document,
                                  List<String> fields) {
        int count = 0;
        for (String field : fields) {
            String text = document.get(field).getAsString();
            if (text.equals("N/A")) {
                return 0;
            }
            count += text.split(" ").length;
        }
        return count;
    }

    private List<EntityAnnotation> loadResults(JsonObject document,
                                               List<String> fields,
                                               boolean useToken) throws IOException {
        List<EntityAnnotation> annotations = new ArrayList<>();
        JsonObject allSpots = document.get("spot").getAsJsonObject();

        int offset = 0;
        for (String field : fields) {
            JsonArray spots = allSpots.get(field).getAsJsonArray();
            for (JsonElement spot : spots) {
                annotations.add(addSpot(spot, offset));
            }
            String text = document.get(field).getAsString();
            offset += useToken ? text.split(" ").length : text.length() + 1;
        }

        return annotations;
    }

    private EntityAnnotation addSpot(JsonElement spot, int offset) {
        JsonObject spotObj = spot.getAsJsonObject();
//        logger.info("Found spot : " + spot.toString());
        JsonArray locs = spotObj.get("loc").getAsJsonArray();
        int begin = locs.get(0).getAsInt();
        int end = locs.get(1).getAsInt();
        String entityId = spotObj.get("entities").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
        EntityAnnotation entityAnnotation = new EntityAnnotation(entityId, begin, end, offset);
        return entityAnnotation;
    }

    private int setAnnotationsFromToken(JCas aJCas, List<EntityAnnotation> annotations,
                                        List<StanfordCorenlpToken> tokens) {
        int numAdded = 0;
        for (EntityAnnotation entityAnnotation : annotations) {
            if (entityAnnotation.getEnd() - 1 < tokens.size()) {
                int begin = tokens.get(entityAnnotation.getBegin()).getBegin();
                int end = tokens.get(entityAnnotation.getEnd() - 1).getEnd();
                GroundedEntity groundedEntity = new GroundedEntity(aJCas, begin, end);
                groundedEntity.setKnowledgeBaseId(entityAnnotation.entityId);
                UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);

//                logger.info(String.format("Adding annotation %s [%d:%d].", groundedEntity.getCoveredText(), begin,
//                        end));
                numAdded++;
            }
        }
        return numAdded;
    }

    private void setAnnotations(JCas aJCas, List<EntityAnnotation> annotations) {
        for (EntityAnnotation entityAnnotation : annotations) {
            if (entityAnnotation.getEnd() < aJCas.getDocumentText().length()) {
                int begin = entityAnnotation.getBegin();
                int end = entityAnnotation.getEnd();
                GroundedEntity groundedEntity = new GroundedEntity(aJCas, begin, end);
                groundedEntity.setKnowledgeBaseId(entityAnnotation.entityId);
                UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);
            }
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String baseName = UimaConvenience.getArticleName(aJCas);
        File tagmeResultFile = new File(linkerResultFolder, baseName);

        ArrayList<StanfordCorenlpToken> defaultTokens = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpToken.class));

        List<String> defaultFields = new ArrayList<>();
        defaultFields.add("title");
        defaultFields.add("bodyText");

        try {
            if (tagmeResultFile.exists()) {
                JsonObject jsonDoc = parseResultFile(tagmeResultFile);

                List<EntityAnnotation> annotations = loadResults(jsonDoc, defaultFields, useToken);
                logger.info(String.format("Found %d annotations from default view.", annotations.size()));

                if (useToken) {
                    int numberAdded = setAnnotationsFromToken(aJCas, annotations, defaultTokens);
                    int defaultJsonTokenCount = countTokensInJson(jsonDoc, defaultFields);
                    if (defaultTokens.size() != defaultJsonTokenCount) {
                        logger.warn(String.format("Unmatched tokens in default view: JSON (%d) and XMI (%d).",
                                defaultJsonTokenCount, defaultTokens.size()));
                    }
                    if (numberAdded != annotations.size()) {
                        logger.warn(String.format("Number annotations added %d is not the same as annotations " +
                                "read %d in default view.", numberAdded, annotations.size()));
                    }
                } else {
                    setAnnotations(aJCas, annotations);
                }

                for (String additionalViewName : additionalViews) {
                    List<String> additionalFields = Lists.newArrayList(additionalViewName);
                    List<EntityAnnotation> viewAnnotations = loadResults(jsonDoc, additionalFields, useToken);
                    JCas view = JCasUtil.getView(aJCas, additionalViewName, false);

                    ArrayList<StanfordCorenlpToken> viewTokens = new ArrayList<>(
                            JCasUtil.select(view, StanfordCorenlpToken.class));

                    if (useToken) {
                        int numberAdded = setAnnotationsFromToken(view, viewAnnotations, viewTokens);
                        int viewJsonTokenCount = countTokensInJson(jsonDoc, additionalFields);
                        if (viewTokens.size() != viewJsonTokenCount) {
                            logger.warn(String.format("Unmatched tokens in view [%s]: JSON (%d) and XMI (%d).",
                                    additionalViewName, viewJsonTokenCount, viewTokens.size()));
                        }
                        if (numberAdded != viewAnnotations.size()) {
                            logger.warn(String.format("Number annotations added %d is not the same as annotations " +
                                    "read %d in view %s.", numberAdded, viewAnnotations.size(), additionalViewName));
                        }
                    } else {
                        setAnnotations(view, viewAnnotations);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        DebugUtils.pause();
    }
}
