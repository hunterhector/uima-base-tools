package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/15/17
 * Time: 4:56 PM
 *
 * @author Zhengzhong Liu
 */
public class EntityLinkerResultAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_ENTITY_LINKER_RESULTS = "entityLinkerResults";
    @ConfigurationParameter(name = PARAM_ENTITY_LINKER_RESULTS)
    private File linkerResultFile;

    private Multimap<String, EntityAnnotation> annoMap;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            loadResults();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    class EntityAnnotation {
        String entityId;
        String entityType;
        int begin;
        int end;

        public EntityAnnotation(String entityId, String entityType, int begin, int end) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.begin = begin;
            this.end = end;
        }
    }

    private void loadResults() throws IOException {
        annoMap = ArrayListMultimap.create();

        for (String line : FileUtils.readLines(linkerResultFile)) {
            String[] parts = line.split("\t");
            if (parts.length == 8) {
                String location = parts[3];
                String entity = parts[4];
                String type = parts[5];

                String[] locParts = location.split(":");
                String docId = locParts[0];
                String[] spanParts = locParts[1].split("-");
                int begin = Integer.parseInt(spanParts[0]);
                int end = Integer.parseInt(spanParts[1]) + 1;

                annoMap.put(docId, new EntityAnnotation(entity, type, begin, end));
            }
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String baseName = UimaConvenience.getShortDocumentName(aJCas);

        for (EntityAnnotation entityAnnotation : annoMap.get(baseName)) {
            GroundedEntity groundedEntity = new GroundedEntity(aJCas, entityAnnotation.begin, entityAnnotation.end);
            groundedEntity.setEntityType(entityAnnotation.entityType);
            groundedEntity.setKnowledgeBaseId(entityAnnotation.entityId);
            UimaAnnotationUtils.finishAnnotation(groundedEntity, COMPONENT_ID, 0, aJCas);
        }
    }
}
