package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import edu.cmu.cs.lti.model.BratAnnotations;
import edu.cmu.cs.lti.model.BratRelation;
import edu.cmu.cs.lti.model.MultiSpan;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.MentionTypeUtils;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.util.BratFormat;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

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

    private int numTotalMentions = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("Annotating AFTER links.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
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
        Map<String, EventMention> id2MentionSpans = indexMentionsById(goldView, annotations);

        annotateMentionRelations(goldView, annotations.getRelations(), id2MentionSpans);
    }

    private Map<String, EventMention> indexMentionsById(JCas aJCas, BratAnnotations annotations) {
        Map<String, EventMention> id2Mention = new HashMap<>();

        Multimap<Span, String> range2EventIds = ArrayListMultimap.create();
        Map<String, String> eid2Type = new HashMap<>();

        for (Map.Entry<String, BratAnnotations.TextBound> textBoundById : annotations
                .getTid2TextBound().entrySet()) {
            BratAnnotations.TextBound textBound = textBoundById.getValue();
            MultiSpan textSpan = textBound.spans;

            Span range = textSpan.getRange();

            List<String> eventIds = annotations.getEventIds(textBoundById.getKey());

            String type = textBound.type;

            numTotalMentions += 1;

            if (checkOmittedAnnotation(aJCas, textBound)) {
                numOmittedMentions += 1;
            } else {
                for (String eventId : eventIds) {
                    range2EventIds.put(range, eventId);
                    eid2Type.put(eventId, type);
//                    logger.info(String.format("Event id %s have type %s.", eventId, type));
                }
            }
        }

        for (Map.Entry<Span, Collection<String>> range2Ids : range2EventIds.asMap().entrySet()) {
            Span range = range2Ids.getKey();

            List<String> eids = new ArrayList<>(range2Ids.getValue());
            Set<String> usedIds = new HashSet<>();

            for (EventMention mention : JCasUtil.selectCovered(aJCas, EventMention.class,
                    range.getBegin(), range.getEnd())) {
                String mentionType = MentionTypeUtils.canonicalize(mention.getEventType());

                for (String eid : eids) {
                    String type = MentionTypeUtils.canonicalize(eid2Type.get(eid));

                    if (mentionType.equals(type) && !usedIds.contains(eid)) {
//                        logger.info(String.format("Mention %s is mapped to %s.", mention.getCoveredText(), eid));
                        id2Mention.put(eid, mention);
                        usedIds.add(eid);
                    }
                }
            }

            if (usedIds.size() != eids.size()) {
                logger.error("Document is " + UimaConvenience.getShortDocumentName(aJCas));
                logger.error(String.format("The range is %s", range));
                logger.error("Used Ids are: " + usedIds);
                logger.error("Annotated Ids are: " + eids);

                throw new RuntimeException("Not all ids in this span is mapped to the annotated mentions.");
            }
        }

        return id2Mention;
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

    private void annotateMentionRelations(JCas aJCas, List<BratRelation> relations,
                                          Map<String, EventMention> id2Mentions) {
        for (EventMentionRelation relation : UimaConvenience.getAnnotationList(aJCas, EventMentionRelation.class)) {
            relation.removeFromIndexes();
        }

        Map<String, String> annotatedRelation = new HashMap<>();

        for (BratRelation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            if (!id2Mentions.containsKey(e1) || !id2Mentions.containsKey(e2)) {
                continue;
            }

            EventMention mention1 = id2Mentions.get(e1);
            EventMention mention2 = id2Mentions.get(e2);

            String relationName = relation.relationName;

            String relationPair = e1 + "_" + e2;
            if (annotatedRelation.containsKey(relationPair)) {
                String oldType = annotatedRelation.get(relationPair);
                if (oldType.equals(afterLinkName) && relationName.equals(subeventLinkName)) {
                    // We don't allow subevent links to overwrite after links.
                    continue;
                } else if (oldType.equals(relationName)) {
                    // If the relation is totally same, we don't annotate one more time.
                    continue;
                }
            }

            annotatedRelation.put(relationPair, relationName);

            ArrayListMultimap<EventMention, EventMentionRelation> headRelations = ArrayListMultimap.create();
            ArrayListMultimap<EventMention, EventMentionRelation> childRelations = ArrayListMultimap.create();

            if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
                EventMentionRelation eventMentionRelation = new EventMentionRelation(aJCas);
                eventMentionRelation.setRelationType(relationName);
                eventMentionRelation.setHead(mention1);
                eventMentionRelation.setChild(mention2);
                UimaAnnotationUtils.finishTop(eventMentionRelation, COMPONENT_ID, 0, aJCas);

                headRelations.put(mention2, eventMentionRelation);
                childRelations.put(mention1, eventMentionRelation);
            }

            for (Map.Entry<EventMention, Collection<EventMentionRelation>> headRelation :
                    headRelations.asMap().entrySet()) {
                headRelation.getKey().setHeadEventRelations(FSCollectionFactory.createFSList(aJCas,
                        headRelation.getValue()));
            }

            for (Map.Entry<EventMention, Collection<EventMentionRelation>> childRelation :
                    childRelations.asMap().entrySet()) {
                childRelation.getKey().setChildEventRelations(FSCollectionFactory.createFSList(aJCas,
                        childRelation.getValue()));
            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info(String.format("Number of omitted mentions : %d (out of %d total)", numOmittedMentions, numTotalMentions));
    }
}
