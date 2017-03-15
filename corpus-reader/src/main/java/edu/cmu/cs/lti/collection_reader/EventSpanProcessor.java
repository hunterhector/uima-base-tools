package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.DiscontinuousComponentAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;
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

    private void addToView(JCas aJCas) {
        for (EventMentionSpan eventMentionSpan : UimaConvenience.getAnnotationList(aJCas, EventMentionSpan.class)) {
            eventMentionSpan.removeFromIndexes();
        }

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
                copyRegions(aJCas, eventMention, ems);
                ems.setRealisType(eventMention.getRealisType());
                types.add(eventMention.getEventType());
            }

            ems.setEventType(MentionTypeUtils.joinMultipleTypes(types));
            UimaAnnotationUtils.finishAnnotation(ems, COMPONENT_ID, 0, aJCas);
        }
    }

    private void copyRegions(JCas toView, DiscontinuousComponentAnnotation from, DiscontinuousComponentAnnotation to) {
        if (from.getRegions() != null) {
            to.setRegions(new FSArray(toView, from.getRegions().size()));
            for (int i = 0; i < from.getRegions().size(); i++) {
                to.setRegions(i, from.getRegions(i));
            }
        }
    }

    public static void main(String[] args) throws UIMAException, IOException {

        if (args.length < 2) {
            System.out.println("Please provide parent, base input directory");
            System.exit(1);
        }

        String parentInput = args[0]; //"data";

        // Parameters for the writer
        String baseInput = args[1]; //"01_event_tuples"

        String paramBaseOutputDirName = "span_added";

        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription addSpan = AnalysisEngineFactory.createEngineDescription(
                EventSpanProcessor.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName);

        SimplePipeline.runPipeline(reader, addSpan, writer);

    }

}
