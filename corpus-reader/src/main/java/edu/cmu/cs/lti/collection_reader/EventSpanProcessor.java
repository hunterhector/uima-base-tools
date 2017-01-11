package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/12/16
 * Time: 10:48 PM
 *
 * @author Zhengzhong Liu
 */
public class EventSpanProcessor extends AbstractLoggingAnnotator {
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Adding event mention span process.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);
        addToView(goldView);
    }

    private void addToView(JCas aJCas){
        ArrayListMultimap<Span, EventMention> allSpan2Mentions = ArrayListMultimap.create();

        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            allSpan2Mentions.put(Span.of(eventMention.getBegin(), eventMention.getEnd()), eventMention);
        }

        for (Map.Entry<Span, Collection<EventMention>> span2Mentions : allSpan2Mentions.asMap().entrySet()) {
            Span span = span2Mentions.getKey();
            EventMentionSpan ems = new EventMentionSpan(aJCas, span.getBegin(), span.getEnd());
            ems.setEventMentions(FSCollectionFactory.createFSList(aJCas, span2Mentions.getValue()));

            List<String> types = new ArrayList<>();
            for (EventMention eventMention : span2Mentions.getValue()) {
                ems.setHeadWord(eventMention.getHeadWord());
                ems.setRegions(eventMention.getRegions());
                ems.setRealisType(eventMention.getRealisType());
                types.add(eventMention.getEventType());
            }

            ems.setEventType(MentionTypeUtils.joinMultipleTypes(types));
            UimaAnnotationUtils.finishAnnotation(ems, COMPONENT_ID, 0, aJCas);
        }
    }
}
