package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.graph.GraphUtils;
import edu.cmu.cs.lti.model.*;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.Comparators;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.util.BratFormat;
import edu.cmu.cs.lti.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/11/15
 * Time: 3:25 PM
 *
 * @author Zhengzhong Liu
 */
public class BratEventGoldStandardAnnotator extends AbstractAnnotator {
    public static final String PARAM_ANNOTATION_DIR = "annotationDir";
    public static final String PARAM_TOKENIZATION_MAP_DIR = "tokenDir";

    public static final String PARAM_ANNOTATION_FILE_NAME_SUFFIX = "annotationFileNameSuffix";
    public static final String PARAM_TOKEN_OFFSET_SUFFIX = "tokenOffsetSuffix";
    public static final String PARAM_TEXT_FILE_SUFFIX = "textFileSuffix";
    public static final String PARAM_TOKEN_OFFSET_BEGIN_FIELD_NUM = "tokenOffsetBeginFieldNum";
    public static final String PARAM_TOKEN_OFFSET_END_FIELD_NUM = "tokenOffsetEndFieldNum";
    public static final String PARAM_PREFER_COREF_LINK = "preferCorefLink";

    @ConfigurationParameter(name = PARAM_ANNOTATION_DIR)
    private File annotationDir;

    @ConfigurationParameter(name = PARAM_TOKENIZATION_MAP_DIR, mandatory = false)
    private File tokenizationDir;

    @ConfigurationParameter(name = PARAM_ANNOTATION_FILE_NAME_SUFFIX, defaultValue = ".tkn.ann")
    private String annotationFileNameSuffix;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_SUFFIX, defaultValue = ".txt.tab")
    private String tokenOffsetSuffix;

    @ConfigurationParameter(name = PARAM_TEXT_FILE_SUFFIX, defaultValue = ".tkn.txt")
    private String textFileNameSuffix;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_BEGIN_FIELD_NUM, defaultValue = "2")
    private Integer tokenOffsetBeginFieldNumber;

    @ConfigurationParameter(name = PARAM_TOKEN_OFFSET_END_FIELD_NUM, defaultValue = "3")
    private Integer tokenOffsetEndFieldNumber;

    @ConfigurationParameter(name = PARAM_PREFER_COREF_LINK, defaultValue = "false")
    private boolean preferCorefLink;

    private static final String realisTypeName = "Realis";
    private static final String coreferenceLinkName = "Coreference";
    private static final String afterLinkName = "After";
    private static final String subeventLinkName = "Subevent";

    private static final Logger logger = LoggerFactory.getLogger(BratEventGoldStandardAnnotator.class);

    public static final String COMPONENT_ID = BratEventGoldStandardAnnotator.class.getSimpleName();

    private Map<String, File> annotationsByName;
    private Map<String, File> offsetsByName;

    private boolean usePredefinedTokens;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        if (!annotationDir.isDirectory()) {
            throw new IllegalArgumentException("Cannot find annotation directory " + annotationDir.getAbsolutePath());
        }

        if (tokenizationDir == null) {
            logger.info("Will use character based span.");
            usePredefinedTokens = false;
        } else {
            logger.info("Will use token based span.");
            usePredefinedTokens = true;
            File[] offsetDocuments = edu.cmu.cs.lti.utils.FileUtils.getFilesWithSuffix(tokenizationDir,
                    tokenOffsetSuffix);
            offsetsByName = trimAsDocId(offsetDocuments, tokenOffsetSuffix);
        }

        File[] annotationDocuments = edu.cmu.cs.lti.utils.FileUtils.getFilesWithSuffix(annotationDir,
                annotationFileNameSuffix);

        annotationsByName = trimAsDocId(annotationDocuments, annotationFileNameSuffix);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, true);
        if (goldView.getDocumentText() == null) {
            goldView.setDocumentText(aJCas.getDocumentText());
        }

        String plainDocId = StringUtils.removeEnd(UimaConvenience.getDocId(aJCas), textFileNameSuffix);

        File annotationDocument = annotationsByName.get(plainDocId);

        List<String> rawAnnotations;
        try {
            rawAnnotations = FileUtils.readLines(annotationDocument, encoding);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        BratAnnotations annotations = BratFormat.parseBratAnnotations(rawAnnotations);

        try {
            Map<String, MultiSpan> textBoundId2Spans;
            if (usePredefinedTokens) {
                File tokenDocument = offsetsByName.get(plainDocId);
                textBoundId2Spans = getTokenBasedSpans(tokenDocument, annotations);
            } else {
                textBoundId2Spans = getSpans(annotations);
            }
            annotateGoldStandard(goldView, annotations, textBoundId2Spans);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private Map<String, MultiSpan> getSpans(BratAnnotations annotations) {
        Map<String, MultiSpan> textBoundId2Spans = new HashMap<>();

        for (Map.Entry<String, BratAnnotations.TextBound> textBoundById : annotations.getTid2TextBound().entrySet()) {
            String annoId = textBoundById.getKey();
            textBoundId2Spans.put(annoId, textBoundById.getValue().spans);
        }
        return textBoundId2Spans;
    }

    private Map<String, MultiSpan> getTokenBasedSpans(File tokenDocument, BratAnnotations annotations)
            throws IOException {
        List<Span> tokenOffsets = new ArrayList<>();
        for (String line : Iterables.skip(FileUtils.readLines(tokenDocument, encoding), 1)) {
            String[] parts = line.trim().split("\t");
            int tokenBegin = Integer.parseInt(parts[tokenOffsetBeginFieldNumber]);
            int tokenEnd = Integer.parseInt(parts[tokenOffsetEndFieldNumber]);
            tokenOffsets.add(Span.of(tokenBegin, tokenEnd + 1));
        }

        Map<String, MultiSpan> textBoundId2Spans = new HashMap<>();

        for (Span tokenSpan : tokenOffsets) {
            for (Map.Entry<String, BratAnnotations.TextBound> textBoundById : annotations
                    .getTid2TextBound().entrySet()) {
                String annoId = textBoundById.getKey();

                List<Span> convertedSpans = new ArrayList<>();

                for (Span span : textBoundById.getValue().spans) {
                    if (span.covers(tokenSpan) || tokenSpan.covers(span)) {
                        convertedSpans.add(tokenSpan);
                    }
                }

                textBoundId2Spans.put(annoId, new MultiSpan(convertedSpans));
            }
        }

        return textBoundId2Spans;
    }

    private Map<String, File> trimAsDocId(File[] annotationDocuments, String suffix) {
        Map<String, File> annotationDocByName = new HashMap<>();
        for (File annotationDocument : annotationDocuments) {
            String annotationDocName = StringUtils.removeEnd(annotationDocument.getName(), suffix);
            annotationDocByName.put(annotationDocName, annotationDocument);
        }
        return annotationDocByName;
    }

    private Map<String, EventMention> annotateMention(JCas aJCas, BratAnnotations annotations,
                                                      Map<String, MultiSpan> textBoundId2Spans) {
        Map<String, EventMention> id2Mentions = new HashMap<>();
        for (int i = 0; i < annotations.getEventIds().size(); i++) {
            String eventId = annotations.getEventIds().get(i);
            String eventTextBoundId = annotations.getEventTextBoundIds().get(i);
            BratAnnotations.TextBound eventInfo = annotations.getTid2TextBound().get(eventTextBoundId);
            EventMention eventMention = new EventMention(aJCas);
            eventMention.setEventType(eventInfo.type);
            MultiSpan spans = textBoundId2Spans.get(eventTextBoundId);

            eventMention.setRegions(new FSArray(aJCas, spans.size()));

            int earliestBegin = Integer.MAX_VALUE;
            int latestEnd = 0;

            for (int spanIndex = 0; spanIndex < spans.size(); spanIndex++) {
                Span span = spans.get(spanIndex);
                if (span.getBegin() < earliestBegin) {
                    earliestBegin = span.getBegin();
                }
                if (span.getEnd() > latestEnd) {
                    latestEnd = span.getEnd();
                }
                Annotation region = new Annotation(aJCas, span.getBegin(), span.getEnd());
                eventMention.setRegions(spanIndex, region);
            }
            eventMention.setBegin(earliestBegin);
            eventMention.setEnd(latestEnd);

            UimaAnnotationUtils.finishAnnotation(eventMention, COMPONENT_ID, eventId, aJCas);

//            if (eventMention.getCoveredText().trim().length() == 0) {
//                logger.info(String.format("Found empty mention %s in document %s.", eventId,
//                        UimaConvenience.getDocId(aJCas)));
//            }

            id2Mentions.put(eventId, eventMention);
            for (BratAttribute attribute : annotations.getId2Attribute().get(eventId)) {
                switch (attribute.attributeName) {
                    case realisTypeName:
                        eventMention.setRealisType(attribute.attributeValue);
                        break;
                    default:
                        logger.warn("Attribute Name not recognized : " + attribute.attributeName);
                }
            }
        }
        return id2Mentions;
    }

    private List<Set<EventMention>> getCorefClusters(List<BratRelation> relations,
                                                     Map<String, EventMention> id2Mentions) {
        List<EventMention> allMentions = new ArrayList<>(id2Mentions.values());
        List<Pair<EventMention, EventMention>> corefLinks = new ArrayList<>();

        for (BratRelation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            EventMention mention1 = id2Mentions.get(e1);
            EventMention mention2 = id2Mentions.get(e2);
            String relationName = relation.relationName;

            if (relationName.equals(coreferenceLinkName)) {
                corefLinks.add(Pair.of(mention1, mention2));
            }
        }

        return GraphUtils.createCluster(allMentions, corefLinks);
    }


    private Table<EventMention, EventMention, String> getSpanRelations(List<BratRelation> relations,
                                                                       Map<String, EventMention> id2Mentions) {
        Table<EventMention, EventMention, String> spanRelations = HashBasedTable.create();

        for (BratRelation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            EventMention mention1 = id2Mentions.get(e1);
            EventMention mention2 = id2Mentions.get(e2);

            String relationName = relation.relationName;

            if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
                spanRelations.put(mention1, mention2, relationName);
            }
        }

        return spanRelations;
    }


    private void annotateSpanRelations(JCas aJCas, Table<EventMention, EventMention, String> spanRelations) {
        for (Table.Cell<EventMention, EventMention, String> spanRelation : spanRelations.cellSet()) {
            String relationName = spanRelation.getValue();
            EventMention mention1 = spanRelation.getRowKey();
            EventMention mention2 = spanRelation.getColumnKey();

            EventMentionRelation eventMentionRelation = new EventMentionRelation(aJCas);
            eventMentionRelation.setRelationType(relationName);
            eventMentionRelation.setHead(mention1);
            eventMentionRelation.setChild(mention2);
            mention1.setChildEventRelations(UimaConvenience.appendFSList(aJCas, mention1.getChildEventRelations()
                    , eventMentionRelation, EventMentionRelation.class));
            mention2.setHeadEventRelations(UimaConvenience.appendFSList(aJCas, mention2.getChildEventRelations(),
                    eventMentionRelation, EventMentionRelation.class));
            UimaAnnotationUtils.finishTop(eventMentionRelation, COMPONENT_ID, 0, aJCas);
        }
    }

    private void annotateCoref(JCas aJCas, List<Set<EventMention>> clusters,
                               ArrayList<EventMention> discoursedSortedEventMentions) {
        int eventIndex = 0;
        for (EventMention mention : discoursedSortedEventMentions) {
            mention.setIndex(eventIndex);
            eventIndex++;
        }

        List<Event> allEvents = new ArrayList<>();
        Set<EventMention> mappedMentions = new HashSet<>();

        clusters.forEach(cluster -> {
            Event event = new Event(aJCas);
            List<EventMention> sortedCluster = cluster.stream().sorted(new Comparators.AnnotationSpanComparator<>())
                    .collect(Collectors.toList());
            event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, sortedCluster));
            cluster.forEach(mention -> {
                mention.setReferringEvent(event);
                mappedMentions.add(mention);
            });
            allEvents.add(event);
        });

        discoursedSortedEventMentions.stream().filter(
                mention -> !mappedMentions.contains(mention)
        ).forEach(
                sortedEventMention -> {
                    Event event = new Event(aJCas);
                    event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, Arrays.asList(sortedEventMention)));
                    sortedEventMention.setReferringEvent(event);
                    allEvents.add(event);
                }
        );

        int[] eventId = new int[1];
        int numNonSingleton = allEvents.stream().sorted((e1, e2) -> new Comparators.AnnotationSpanComparator<>()
                .compare(e1.getEventMentions(0), e2.getEventMentions(0))).mapToInt(
                event -> {
                    UimaAnnotationUtils.finishTop(event, COMPONENT_ID, eventId[0], aJCas);
                    event.setIndex(eventId[0]);
                    eventId[0]++;
                    return event.getEventMentions().size() > 1 ? 1 : 0;
                }
        ).sum();
    }

    private void annotateGoldStandard(JCas aJCas, BratAnnotations annotations,
                                      Map<String, MultiSpan> textBoundId2Spans) {
        Map<String, EventMention> id2Mentions = annotateMention(aJCas, annotations, textBoundId2Spans);
        ArrayList<EventMention> discoursedSortedEventMentions = new ArrayList<>(id2Mentions.values());
        discoursedSortedEventMentions.sort(new Comparators.AnnotationSpanComparator<>());

        Table<EventMention, EventMention, String> spanRelations = getSpanRelations(annotations.getRelations(),
                id2Mentions);
        List<Set<EventMention>> clusters = getCorefClusters(annotations.getRelations(), id2Mentions);

        Set<Pair<EventMention, EventMention>> typesToRemove = new HashSet<>();

        if (preferCorefLink) {
            for (Table.Cell<EventMention, EventMention, String> spanRelation : spanRelations.cellSet()) {
                EventMention mention1 = spanRelation.getRowKey();
                EventMention mention2 = spanRelation.getColumnKey();

                for (Set<EventMention> cluster : clusters) {
                    if (cluster.contains(mention1) && cluster.contains(mention2)) {
                        logger.info(String.format(
                                "Mention %s:%s and %s:%s have relation, but also coref, keeping coreference.",
                                mention1.getId(), mention1.getCoveredText(), mention2.getId(), mention2.getCoveredText()
                        ));
                        typesToRemove.add(Pair.of(mention1, mention2));
                    }
                }
            }
        }

        for (Pair<EventMention, EventMention> mentionPair : typesToRemove) {
            spanRelations.remove(mentionPair.getKey(), mentionPair.getValue());
        }

        annotateSpanRelations(aJCas, spanRelations);
        annotateCoref(aJCas, clusters, discoursedSortedEventMentions);
    }

    public static void main(String[] args) throws UIMAException, IOException {
        logger.info(COMPONENT_ID + " started");
        String parentDir = "data/brat_event";
        String sourceTextDir = parentDir + "/LDC2014E121/source";
        String tokenOffsetDir = parentDir + "/LDC2014E121/token_offset";
        String annotationDir = parentDir + "/LDC2014E121/annotation";
        String baseDir = "gold_annotated";

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription
                (paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, ".tkn.txt");

        AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
                BratEventGoldStandardAnnotator.class, typeSystemDescription,
                BratEventGoldStandardAnnotator.PARAM_ANNOTATION_DIR, annotationDir,
                BratEventGoldStandardAnnotator.PARAM_TOKENIZATION_MAP_DIR, tokenOffsetDir
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentDir, baseDir, null, null
        );

        SimplePipeline.runPipeline(reader, engine, writer);
        System.out.println(COMPONENT_ID + " successfully completed.");
    }
}
