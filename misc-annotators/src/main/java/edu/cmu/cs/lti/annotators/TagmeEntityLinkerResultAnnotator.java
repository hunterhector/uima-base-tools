package edu.cmu.cs.lti.annotators;

import com.google.gson.*;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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

        private int getBegin(){
            return begin + offset;
        }

        private int getEnd(){
            return end + offset;
        }
    }

    private List<EntityAnnotation> loadResults(File linkerResultFile, boolean useToken) throws IOException {
        List<EntityAnnotation> annotations = new ArrayList<>();

        String jsonStr = FileUtils.readFileToString(linkerResultFile);

        Gson gson = new Gson();

        gson.toJson(jsonStr);
        JsonParser parser = new JsonParser();

        JsonObject document = parser.parse(jsonStr).getAsJsonObject();

        String docno = document.get("docno").getAsString();

//        String bodyText = document.get("bodyText").getAsString();
        String title = document.get("title").getAsString();

        JsonObject spot = document.get("spot").getAsJsonObject();
        JsonArray bodySpots = spot.get("bodyText").getAsJsonArray();
        JsonArray titleSpots = spot.get("title").getAsJsonArray();

        int titleOffset = useToken ?  title.split(" ").length : title.length() + 1;

        for (JsonElement bodySpot : bodySpots) {
            annotations.add(addSpot(bodySpot, titleOffset));
        }

        for (JsonElement titleSpot : titleSpots) {
            annotations.add(addSpot(titleSpot, 0));
        }

        return annotations;
    }

    private EntityAnnotation addSpot(JsonElement bodySpot, int offset){
        JsonObject spotObj = bodySpot.getAsJsonObject();
        JsonArray locs = spotObj.get("loc").getAsJsonArray();
        int begin = locs.get(0).getAsInt();
        int end = locs.get(1).getAsInt();
        String entityId = spotObj.get("entities").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
        EntityAnnotation entityAnnotation = new EntityAnnotation(entityId, begin, end, offset);
        return entityAnnotation;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String baseName = UimaConvenience.getArticleName(aJCas);
        File tagmeResultFile = new File(linkerResultFolder, baseName);

        ArrayList<StanfordCorenlpToken> tokens = new ArrayList<>(JCasUtil.select(aJCas, StanfordCorenlpToken.class));

        try {
            if (tagmeResultFile.exists()) {
                List<EntityAnnotation> annotations = loadResults(tagmeResultFile, useToken);
                for (EntityAnnotation entityAnnotation : annotations) {
                    if (entityAnnotation.getEnd() -1 < tokens.size()) {
                        int begin = useToken ? tokens.get(entityAnnotation.getBegin()).getBegin() : entityAnnotation.getBegin();
                        int end = useToken ? tokens.get(entityAnnotation.getEnd() - 1).getEnd() : entityAnnotation.getEnd();
                        GroundedEntity groundedEntity = new GroundedEntity(aJCas, begin, end);
                        groundedEntity.setKnowledgeBaseId(entityAnnotation.entityId);
                        UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
