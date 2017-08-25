package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/25/17
 * Time: 4:19 PM
 *
 * @author Zhengzhong Liu
 */
public class AnnotationRemover extends AbstractLoggingAnnotator {
    public static final String PARAM_TARGET_VIEWS = "targetViewNames";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS, mandatory = false)
    private String[] targetViewNames;

    public static final String PARAM_TARGET_ANNOTATIONS = "targetAnnotations";

//    @ConfigurationParameter(name = PARAM_TARGET_ANNOTATIONS)
//    private String[] targetAnnotations;

    @ConfigurationParameter(name = PARAM_TARGET_ANNOTATIONS)
    private Class<TOP>[] targetAnnotations;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        if (targetViewNames == null) {
            targetViewNames = new String[]{CAS.NAME_DEFAULT_SOFA};
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (String targetViewName : targetViewNames) {
            JCas targetView = JCasUtil.getView(aJCas, targetViewName, false);
            for (Class<TOP> targetAnnotation : targetAnnotations) {
                List<TOP> annotations = UimaConvenience.getAnnotationList(targetView, targetAnnotation);
                for (TOP annotation : annotations) {
                    annotation.removeFromIndexes();
                }
//                logger.info(String.format("Removed %d %s, remaining %s.", annotations.size(),
//                        targetAnnotation.getName(),
//                        UimaConvenience.getAnnotationList(targetView, targetAnnotation).size()));
            }
        }
    }
}
