package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/7/17
 * Time: 11:00 AM
 *
 * @author Zhengzhong Liu
 */
public class LinkedEntityMerger extends AbstractLoggingAnnotator {
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        // Several merging to be done:
        // 1.a Multiple grounded entities in one entity mention.
        // 1.b Multiple named entities in one grounded entity.
        // 2.a Entity mention contained in a grounded entity.
        // 2.b Grounded entity contained in a entity mention.
        // 3   Entity mentions not annotated as grounded entity.

        // For case 1, we merge the multiple ones into larger.
        // For case 2, we choose the larger entity.
        // For case 3, we add it as a grounded entity if we can find a coref entity mention.

        Map<StanfordEntityMention, GroundedEntity> entityGroundedMap = new HashMap<>();

        for (StanfordEntityMention namedEntity : JCasUtil.select(aJCas, StanfordEntityMention.class)) {
            List<GroundedEntity> grounds = JCasUtil.selectCovered(GroundedEntity.class, namedEntity);
            GroundedEntity merged = mergeFragmentedGrounds(aJCas, grounds, namedEntity.getHead());
            // Removing the sub entities.

            for (GroundedEntity groundedEntity : JCasUtil.selectCovered(GroundedEntity.class, namedEntity)) {
                entityGroundedMap.put(namedEntity, groundedEntity);
            }
        }
    }

    private GroundedEntity mergeFragmentedGrounds(JCas aJCas, List<GroundedEntity> grounds, Word headword) {
        GroundedEntity leadEntity = null;
        for (GroundedEntity ground : grounds) {
            if (covers(ground, headword)) {
                leadEntity = ground;
            }
        }
        GroundedEntity mergedEntity = new GroundedEntity(aJCas);
        mergedEntity.setConfidence(leadEntity.getConfidence());
        mergedEntity.setEntityType(leadEntity.getEntityType());
        mergedEntity.setKnowledgeBaseId(leadEntity.getKnowledgeBaseId());

        mergedEntity.setKnowledgeBaseValues(new StringArray(aJCas, leadEntity.getKnowledgeBaseValues().size()));
        mergedEntity.setKnowledgeBaseNames(new StringArray(aJCas, leadEntity.getKnowledgeBaseNames().size()));


        return mergedEntity;
    }

    private boolean covers(ComponentAnnotation covering, ComponentAnnotation coverred) {
        if (covering.getBegin() <= coverred.getBegin() && covering.getEnd() >= coverred.getEnd()) {
            return true;
        }
        return false;
    }

}
