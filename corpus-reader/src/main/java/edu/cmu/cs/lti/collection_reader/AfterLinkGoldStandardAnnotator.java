package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.BratAnnotations;
import edu.cmu.cs.lti.model.BratRelation;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.util.BratFormat;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use to add gold standard annotations to existing processed CASes.
 *
 * @author Zhengzhong Liu
 */
public class AfterLinkGoldStandardAnnotator extends AbstractAnnotator {
    public static final String PARAM_ANNOTATION_FILE_NAME_SUFFIX = "annFileNameSuffix";
    @ConfigurationParameter(name = PARAM_ANNOTATION_FILE_NAME_SUFFIX, defaultValue = ".ann")
    private String annFileNameSuffix;

    public static final String PARAM_ANNOTATION_DIR = "annotationDirectory";

    @ConfigurationParameter(name = PARAM_ANNOTATION_DIR)
    private File annotationDir;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);

        String articleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        File annotationFile = new File(annotationDir, articleName + "annFileNameSuffix");

        List<String> rawAnnotations = null;
        try {
            rawAnnotations = FileUtils.readLines(annotationFile, encoding);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        BratAnnotations annotations = BratFormat.parseBratAnnotations(rawAnnotations);
        ArrayListMultimap<String, Span> textBoundId2Spans = getRegularSpans(annotations);

        Map<String, EventMentionSpan> id2MentionSpans = getIdToEventMention(aJCas, annotations, textBoundId2Spans);

        annotateSpanRelations(goldView, annotations.getRelations(), id2MentionSpans);
    }

    private Map<String, EventMentionSpan> getIdToEventMention(JCas aJCas, BratAnnotations annotations,
                                                              ArrayListMultimap<String, Span> textBoundId2Spans) {
        Map<String, EventMentionSpan> id2Mentions = new HashMap<>();

        // Have a unique ID for each span, containing multiple event mentions.
        ArrayListMultimap<Integer, EventMention> spanId2Mentions = ArrayListMultimap.create();
        Map<Span, Integer> spanToId = new HashMap<>();

        Map<String, Span> mention2Span = new HashMap<>();

        int spanId = 0;
        for (int i = 0; i < annotations.getEventIds().size(); i++) {
            String eventId = annotations.getEventIds().get(i);
            String eventTextBoundId = annotations.getEventTextBounds().get(i);

            List<Span> mentionSpan = annotations.getTextBoundId2SpanAndType().get(eventTextBoundId).getValue0();
            Span range = getRange(mentionSpan);

            List<EventMention> mentions = JCasUtil.selectCovered(aJCas, EventMention.class,
                    range.getBegin(), range.getEnd());

            if (!spanToId.containsKey(range)) {
                spanToId.put(range, spanId);
                spanId2Mentions.putAll(spanId, mentions);
                spanId++;
            }
        }


        return id2Mentions;

    }

    private Span getRange(List<Span> mentionSpan) {
        int earliest = Integer.MAX_VALUE;
        int latest = 0;
        for (Span span : mentionSpan) {
            if (span.getBegin() < earliest) {
                earliest = span.getBegin();
            }
            if (span.getEnd() > latest) {
                latest = span.getEnd();
            }
        }

        return Span.of(earliest, latest);
    }


    private void annotateSpanRelations(JCas aJCas, List<BratRelation> relations,
                                       Map<String, EventMentionSpan> id2Mentions) {
        for (BratRelation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            EventMentionSpan mention1 = id2Mentions.get(e1);
            EventMentionSpan mention2 = id2Mentions.get(e2);

            String relationName = relation.relationName;

//            if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
//                EventMentionRelation eventMentionRelation = new EventMentionRelation(aJCas);
//                eventMentionRelation.setRelationType(relationName);
//                eventMentionRelation.setHead(mention1);
//                eventMentionRelation.setChild(mention2);
//                mention1.setChildEventRelations(UimaConvenience.appendFSList(aJCas, mention1.getChildEventRelations()
//                        , eventMentionRelation, EventMentionRelation.class));
//                mention2.setHeadEventRelations(UimaConvenience.appendFSList(aJCas, mention2.getChildEventRelations(),
//                        eventMentionRelation, EventMentionRelation.class));
//                UimaAnnotationUtils.finishTop(eventMentionRelation, COMPONENT_ID, 0, aJCas);
//            }
        }
    }

    private ArrayListMultimap<String, Span> getRegularSpans(BratAnnotations annotations) {
        ArrayListMultimap<String, Span> textBoundId2Spans = ArrayListMultimap.create();

        for (Map.Entry<String, Pair<List<Span>, String>> textBoundById : annotations
                .getTextBoundId2SpanAndType().entrySet()) {
            String annoId = textBoundById.getKey();
            textBoundId2Spans.putAll(annoId, textBoundById.getValue().getValue0());
        }
        return textBoundId2Spans;
    }


}
