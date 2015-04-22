package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.BratConstants;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.Comparators;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/11/15
 * Time: 3:25 PM
 *
 * @author Zhengzhong Liu
 */
public class BratEventGoldStandardAnnotator extends AbstractAnnotator {
    public static final String PARAM_SOURCE_TEXT_DIR = "sourceTextDir";
    public static final String PARAM_ANNOTATION_DIR = "annDir";

    public static final String PARAM_TEXT_FILE_NAME_SUFFIX = "textFileNameSuffix";
    public static final String PARAM_ANNOTATION_FILE_NAME_SUFFIX = "annotationFileNameSuffix";

    @ConfigurationParameter(name = PARAM_ANNOTATION_DIR)
    private File annotationDir;

    @ConfigurationParameter(name = PARAM_TEXT_FILE_NAME_SUFFIX, mandatory = false)
    private String textFileNameSuffix;

    public static final String defaultTextFileNameSuffix = ".tkn.txt";


    @ConfigurationParameter(name = PARAM_ANNOTATION_FILE_NAME_SUFFIX, mandatory = false)
    private String annotationFileNameSuffix;

    public static final String defaultAnnotationFileNameSuffix = ".tkn.ann";

    private static final String realisTypeName = "Realis";
    private static final String coreferenceLinkName = "Coreference";
    private static final String afterLinkName = "After";
    private static final String subeventLinkName = "Subevent";

    private static final Logger logger = LoggerFactory.getLogger(BratEventGoldStandardAnnotator.class);

    public static final String COMPONENT_ID = BratEventGoldStandardAnnotator.class.getSimpleName();

    private Map<String, File> annotationsByName;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        if (textFileNameSuffix == null) {
            textFileNameSuffix = defaultTextFileNameSuffix;
        }
        if (annotationFileNameSuffix == null) {
            annotationFileNameSuffix = defaultAnnotationFileNameSuffix;
        }

        if (!annotationDir.isDirectory()) {
            throw new IllegalArgumentException("Cannot find annotation directory " + annotationDir.getAbsolutePath());
        }

        File[] annotationDocuments = edu.cmu.cs.lti.utils.FileUtils.getFilesWithSuffix(annotationDir, annotationFileNameSuffix);

        annotationsByName = findAnnotationFiles(annotationDocuments);
    }

    private Map<String, File> findAnnotationFiles(File[] annotationDocuments) {
        Map<String, File> annotationDocByName = new HashMap<>();
        for (File annotationDocument : annotationDocuments) {
            String annotationDocName = StringUtils.removeEnd(annotationDocument.getName(), annotationFileNameSuffix);
            annotationDocByName.put(annotationDocName, annotationDocument);
        }
        return annotationDocByName;
    }

    public void annotateGoldStandard(JCas aJCas, List<String> bratAnnotations) {
        Map<String, Pair<List<Span>, String>> allTextBounds = new HashMap<>();

        List<String> eventIds = new ArrayList<>();
        List<String> eventTextBounds = new ArrayList<>();

        ArrayListMultimap<String, Attribute> id2Attribute = ArrayListMultimap.create();
        List<Relation> relations = new ArrayList<>();

        for (String line : bratAnnotations) {
            String[] parts = line.trim().split("\t");

            String annoId = parts[0];

            if (annoId.startsWith(BratConstants.textBoundPrefix)) {
                allTextBounds.put(annoId, str2Span(parts[1]));
            } else if (annoId.startsWith(BratConstants.eventPrefix)) {
                eventIds.add(annoId);
                eventTextBounds.add(parts[1].split(":")[1]);
            } else if (annoId.startsWith(BratConstants.attributePrefix)) {
                Attribute attribute = new Attribute(line);
                id2Attribute.put(attribute.attributeHost, attribute);
            } else if (annoId.startsWith(BratConstants.relationPrefix)) {
                Relation relation = new Relation(line);
                relations.add(relation);
            }
        }

        Map<String, EventMention> id2Mentions = new HashMap<>();
        for (int i = 0; i < eventIds.size(); i++) {
            String eventId = eventIds.get(i);
            String eventTextBoundId = eventTextBounds.get(i);
            Pair<List<Span>, String> eventInfo = allTextBounds.get(eventTextBoundId);
            EventMention eventMention = new EventMention(aJCas);
            eventMention.setEventType(eventInfo.getValue1());

            List<Span> eventMentionSpans = eventInfo.getValue0();

            eventMention.setRegions(new FSArray(aJCas, eventMentionSpans.size()));

            int earliestBegin = Integer.MAX_VALUE;
            int latestEnd = 0;
            for (int spanIndex = 0; spanIndex < eventMentionSpans.size(); spanIndex++) {
                Span span = eventMentionSpans.get(spanIndex);
                if (span.getBegin() < earliestBegin) {
                    earliestBegin = span.getBegin();
                }
                if (span.getEnd() > latestEnd) {
                    latestEnd = span.getEnd();
                }
                Annotation region = new Annotation(aJCas, span.getBegin(), span.getEnd());
                eventMention.setRegions(spanIndex, region);
                eventMention.setBegin(earliestBegin);
                eventMention.setEnd(latestEnd);
            }

            id2Mentions.put(eventId, eventMention);
            for (Attribute attribute : id2Attribute.get(eventId)) {
                switch (attribute.attributeName) {
                    case realisTypeName:
                        eventMention.setRealisType(attribute.attributeValue);
                        break;
                    default:
                        logger.warn("Attribute Name not recognized : " + attribute.attributeName);
                }
            }
        }

        List<Set<EventMention>> clusters = new ArrayList<>();

        for (Relation relation : relations) {
            String e1 = relation.arg1Id;
            String e2 = relation.arg2Id;

            EventMention mention1 = id2Mentions.get(e1);
            EventMention mention2 = id2Mentions.get(e2);

            String relationName = relation.relationName;
            if (relationName.equals(coreferenceLinkName)) {
                boolean inCluster = false;
                for (Set<EventMention> cluster : clusters) {
                    if (cluster.contains(mention1)) {
                        cluster.add(mention2);
                        inCluster = true;
                        break;
                    } else if (cluster.contains(mention2)) {
                        cluster.add(mention1);
                        inCluster = true;
                        break;
                    }
                }
                if (!inCluster) {
                    Set<EventMention> newCluster = new HashSet<>();
                    newCluster.add(mention1);
                    newCluster.add(mention2);
                    clusters.add(newCluster);
                }
            } else if (relationName.equals(subeventLinkName) || relationName.equals(afterLinkName)) {
                EventRelation eventRelation = new EventRelation(aJCas);
                eventRelation.setRelationType(subeventLinkName);
                eventRelation.setHead(mention1);
                eventRelation.setChild(mention2);
                mention1.setChildEventRelations(UimaConvenience.appendFSList(aJCas, mention1.getChildEventRelations(), eventRelation, EventRelation.class));
                mention2.setHeadEventRelations(UimaConvenience.appendFSList(aJCas, mention2.getChildEventRelations(), eventRelation, EventRelation.class));
                UimaAnnotationUtils.finishTop(eventRelation, COMPONENT_ID, 0, aJCas);
            }

            for (Set<EventMention> cluster : clusters) {
                Event event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, cluster));
                for (EventMention mention : cluster) {
                    mention.setReferringEvent(event);
                }
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, aJCas);
            }

            ArrayList<EventMention> discoursedSortedEventMentions = new ArrayList<EventMention>(id2Mentions.values());
            Collections.sort(discoursedSortedEventMentions, new Comparators.AnnotationBeginComparator<EventMention>());

            int evmId = 0;
            for (EventMention sortedEventMention : discoursedSortedEventMentions) {
                UimaAnnotationUtils.finishAnnotation(sortedEventMention, COMPONENT_ID, evmId++, aJCas);
            }
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = null;
        try {
            goldView = aJCas.createView(goldStandardViewName);
        } catch (CASException e) {
            throw new RuntimeException(e);
        }
        goldView.setDocumentText(aJCas.getDocumentText());
        
        File annotationDocument = annotationsByName.get(StringUtils.removeEnd(UimaConvenience.getDocId(aJCas), textFileNameSuffix));

        try {
            annotateGoldStandard(goldView, FileUtils.readLines(annotationDocument, encoding));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public class Attribute {
        String attributeId;
        String attributeName;
        String attributeValue;
        String attributeHost;

        public Attribute(String attributeLine) {
            parseAttribute(attributeLine);
        }

        private void parseAttribute(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            attributeId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            attributeName = attributeFields[0];
            attributeHost = attributeFields[1];
            attributeValue = attributeFields[2];
        }
    }

    public class Relation {
        String relationId;
        String relationName;
        String arg1Name;
        String arg1Id;
        String arg2Name;
        String arg2Id;

        public Relation(String attributeLine) {
            parseRelation(attributeLine);
        }

        private void parseRelation(String attributeLine) {
            String[] parts = attributeLine.split("\t");
            relationId = parts[0];
            String[] attributeFields = parts[1].split(" ");
            relationName = attributeFields[0];
            String[] arg1 = attributeFields[1].split(":");
            String[] arg2 = attributeFields[2].split(":");

            arg1Name = arg1[0];
            arg1Id = arg1[1];

            arg2Name = arg2[0];
            arg2Id = arg2[1];
        }

    }

    private Pair<List<Span>, String> str2Span(String spanText) {
        String[] typeAndSpan = spanText.split(" ", 2);
        String type = typeAndSpan[0];
        String[] spanStrs = typeAndSpan[1].split(";");

        List<Span> spans = new ArrayList<>();
        for (String spanStr : spanStrs) {
            String[] spanTexts = spanStr.split(" ");
            spans.add(Span.of(Integer.parseInt(spanTexts[0]), Integer.parseInt(spanTexts[1])));
        }
        return Pair.with(spans, type);
    }


    public static void main(String[] args) throws UIMAException, IOException {
        logger.info(COMPONENT_ID + " started");
        String parentDir = "data/brat_event";
        String inputDir = parentDir + "/" + "LDC2014E121";
        String baseDir = "gold_annotated";

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, ".txt");

        AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
                BratEventGoldStandardAnnotator.class, typeSystemDescription,
                BratEventGoldStandardAnnotator.PARAM_ANNOTATION_DIR, inputDir,
                BratEventGoldStandardAnnotator.PARAM_SOURCE_TEXT_DIR, inputDir
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentDir, baseDir, null, null
        );

        SimplePipeline.runPipeline(reader, engine, writer);
        System.out.println(COMPONENT_ID + " successfully completed.");
    }
}