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

import java.util.List;

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

    private void removeAllEventRelated(JCas view) {
        List<EventMentionRelation> relations = UimaConvenience.getAnnotationList(view, EventMentionRelation.class);
        for (EventMentionRelation relation : relations) {
            relation.removeFromIndexes();
        }

        List<EventMentionSpanRelation> spanRelations = UimaConvenience.getAnnotationList(view,
                EventMentionSpanRelation.class);
        for (EventMentionSpanRelation relation : spanRelations) {
            relation.removeFromIndexes();
        }

        List<Event> events = UimaConvenience.getAnnotationList(view, Event.class);
        for (Event event : events) {
            event.removeFromIndexes();
        }

        List<EventMention> mentions = UimaConvenience.getAnnotationList(view, EventMention.class);
        for (EventMention mention : mentions) {
            mention.removeFromIndexes();
        }

        List<EventMentionSpan> spans = UimaConvenience.getAnnotationList(view, EventMentionSpan.class);
        for (EventMentionSpan span : spans) {
            span.removeFromIndexes();
        }
    }
}