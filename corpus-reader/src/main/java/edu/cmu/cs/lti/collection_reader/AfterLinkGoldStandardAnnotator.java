package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.BratAnnotations;
import edu.cmu.cs.lti.model.BratRelation;
import edu.cmu.cs.lti.model.MultiSpan;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMentionSpan;
import edu.cmu.cs.lti.script.type.EventMentionSpanRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.util.BratFormat;
import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Use to add gold standard annotations to existing processed CASes.
 *
 * @author Zhengzhong Liu
 */
public class AfterLinkGoldStandardAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_ANNOTATION_FILE_NAME_SUFFIX = "annFileNameSuffix";
    @ConfigurationParameter(name = PARAM_ANNOTATION_FILE_NAME_SUFFIX, defaultValue = ".ann")
    private String annFileNameSuffix;

    public static final String PARAM_ANNOTATION_DIR = "annotationDirectory";

    @ConfigurationParameter(name = PARAM_ANNOTATION_DIR)
    private File annotationDir;

    private String subeventLinkName = "Subevent";

    private String afterLinkName = "After";

    private int numOmittedMentions = 0;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas, logger);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);

        String articleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        File annotationFile = new File(annotationDir, articleName + annFileNameSuffix);

        List<String> rawAnnotations = null;
        try {
            rawAnnotations = FileUtils.readLines(annotationFile, encoding);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        // Read the raw Brat annotations.
        BratAnnotations annotations = BratFormat.parseBratAnnotations(rawAnnotations);

        // Here we get the event id to EventMentionSpan mapping. Note that multiple id can map to the same
        // EventMentionSpan.
        Set<String> omittedMentions = new HashSet<>();
        Map<String, EventMentionSpan> id2MentionSpans = getEmsById(goldView, annotations, omittedMentions);

        annotateSpanRelations(goldView, annotations.getRelations(), id2MentionSpans);
    }

    private Map<String, EventMentionSpan> getEmsById(JCas aJCas, BratAnnotations annotations,
                                                     Set<String> omittedMentions) {
        Map<String, EventMentionSpan> id2MentionSpans = new HashMap<>();
        Map<Span, EventMentionSpan> emsBySpan = new HashMap<>();

        for (EventMentionSpan eventMentionSpan : JCasUtil.select(aJCas, EventMentionSpan.class)) {
            Span spanKey = Span.of(eventMentionSpan.getBegin(), eventMentionSpan.getEnd());
            emsBySpan.put(spanKey, eventMentionSpan);
        }

        for (Map.Entry<String, BratAnnotations.TextBound> textBoundById : annotations
                .getTid2TextBound().entrySet()) {
            BratAnnotations.TextBound textBound = textBoundById.getValue();
            MultiSpan textSpan = textBound.spans;

            Span range = textSpan.getRange();
            EventMentionSpan eventMentionSpan = emsBySpan.get(range);
            String textBoundId = textBoundById.getKey();

            List<String> eventIds = annotations.getEventIds(textBoundId);

            if (eventMentionSpan == null) {
                boolean omitOnPurpose = checkOmittedAnnotation(aJCas, textBound);
                if (!omitOnPurpose) {
                    logger.warn(String.format("The event mention is not omitted on purpose: [Doc]: %s, [Mention " +
                            "text]: %s, [Range]: %s.", UimaConvenience.getDocId(aJCas), textBound.text, textBound
                            .spans));
                }

                omittedMentions.addAll(eventIds);

                numOmittedMentions++;
                continue;
            }

            for (String eventId : eventIds) {
                id2MentionSpans.put(eventId, eventMentionSpan);
            }
        }

        return id2MentionSpans;
    }

    private boolean checkOmittedAnnotation(JCas aJCas, BratAnnotations.TextBound textBound) {
        String text = textBound.text;
        String fullText = aJCas.getDocumentText();
        StringBuilder sb = new StringBuilder();

        String sep = "";
        for (Span span : textBound.spans) {
            sb.append(sep);
            sb.append(fullText.substring(span.getBegin(), span.getEnd()));
            sep = " ";
        }

        return !sb.toString().equals(text);
    }

    private void annotateSpanRelations(JCas aJCas, List<BratRelation> relations,
                                       Map<String, EventMentionSpan> id2Mentions) {
        Set<Pair<EventMentionSpan, EventMentionSpan>> recordedRelations = new HashSet<>();

        for (BratRelation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            if (!(id2Mentions.containsKey(e1) && id2Mentions.containsKey(e2))) {
                continue;
            }

            EventMentionSpan mentionSpan1 = id2Mentions.get(e1);
            EventMentionSpan mentionSpan2 = id2Mentions.get(e2);

            String relationName = relation.relationName;

            Pair<EventMentionSpan, EventMentionSpan> mentionPair = Pair.with(mentionSpan1, mentionSpan2);
            if (recordedRelations.contains(mentionPair)) {
                continue;
            } else {
                recordedRelations.add(mentionPair);
            }

            ArrayListMultimap<EventMentionSpan, EventMentionSpanRelation> headRelations = ArrayListMultimap.create();
            ArrayListMultimap<EventMentionSpan, EventMentionSpanRelation> childRelations = ArrayListMultimap.create();

            if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
                EventMentionSpanRelation eventMentionRelation = new EventMentionSpanRelation(aJCas);
                eventMentionRelation.setRelationType(relationName);
                eventMentionRelation.setHead(mentionSpan1);
                eventMentionRelation.setChild(mentionSpan2);
                UimaAnnotationUtils.finishTop(eventMentionRelation, COMPONENT_ID, 0, aJCas);

                headRelations.put(mentionSpan2, eventMentionRelation);
                childRelations.put(mentionSpan1, eventMentionRelation);
            }

            for (Map.Entry<EventMentionSpan, Collection<EventMentionSpanRelation>> headRelation :
                    headRelations.asMap().entrySet()) {
//                logger.info(headRelation.getKey().getCoveredText());
//                logger.info(String.valueOf(headRelation.getValue()));
                headRelation.getKey().setHeadEventRelations(FSCollectionFactory.createFSList(aJCas,
                        headRelation.getValue()));
            }

            for (Map.Entry<EventMentionSpan, Collection<EventMentionSpanRelation>> childRelation :
                    childRelations.asMap().entrySet()) {
                childRelation.getKey().setChildEventRelations(FSCollectionFactory.createFSList(aJCas,
                        childRelation.getValue()));
            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info(String.format("Number of omitted mentions : %d.", numOmittedMentions));
    }
}
