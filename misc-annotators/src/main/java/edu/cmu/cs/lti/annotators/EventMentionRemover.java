package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/15
 * Time: 2:45 PM
 * <p>
 * Annotate event mentions based on Gold Standard, while this is useful
 * for training, it is can also be used in some evaluation case when
 * gold standard mentions are given
 *
 * @author Zhengzhong Liu
 */
public class EventMentionRemover extends AbstractLoggingAnnotator {
    public static final String COMPONENT_ID = EventMentionRemover.class.getSimpleName();
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_TARGET_VIEWS = "targetViewNames";

    @ConfigurationParameter(name = PARAM_TARGET_VIEWS, mandatory = false)
    private String[] targetViewNames;


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
            removeAllEventRelated(targetView);
        }
    }

    private void removeAllEventRelated(JCas toView) {
        for (EventMentionRelation relation : UimaConvenience.getAnnotationList(toView, EventMentionRelation.class)) {
            relation.removeFromIndexes();
        }

        for (EventMentionSpanRelation relation : UimaConvenience.getAnnotationList(toView,
                EventMentionSpanRelation.class)) {
            relation.removeFromIndexes();
        }

        for (Event event : UimaConvenience.getAnnotationList(toView, Event.class)) {
            event.removeFromIndexes();
        }

        for (EventMention mention : UimaConvenience.getAnnotationList(toView, EventMention.class)) {
            mention.removeFromIndexes();
        }

        for (EventMentionSpan span : UimaConvenience.getAnnotationList(toView, EventMentionSpan.class)) {
            span.removeFromIndexes();
        }
    }
}