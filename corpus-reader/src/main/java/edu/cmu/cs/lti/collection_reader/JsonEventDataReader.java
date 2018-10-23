package edu.cmu.cs.lti.collection_reader;

import com.google.gson.Gson;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/22/18
 * Time: 12:07 PM
 *
 * @author Zhengzhong Liu
 */
public class JsonEventDataReader extends AbstractLoggingAnnotator {
    public static final String PARAM_JSON_ANNO_DIR = "inputDir";
    @ConfigurationParameter(name = PARAM_JSON_ANNO_DIR)
    private File annoDir;

    private Gson gson;

    class AnnoDoc {
        String text;
        List<JEvent> events;
        List<JEntity> entities;
    }

    class JEvent {
        String id;
        String annotation;
        List<JEventMention> mentions;
    }

    class JEventMention {
        String id;
        String annotation;
        String text;
        List<Span> spans;
        List<JArgument> arguments;
        String type;
    }

    class JArgument {
        String arg;
        String role;
        ArgMeta meta;
    }

    class ArgMeta {
        boolean incorporated;
        boolean succeeding;
    }

    class JEntity {
        String id;
        String annotation;
        List<JEntityMention> mentions;
        String type;
    }

    class JEntityMention {
        String id;
        String annotation;
        String text;
        String type;
        List<Span> spans;
    }

    class Span {
        int begin;
        int end;
    }

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        gson = new Gson();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String docid = UimaConvenience.getArticleName(aJCas);

        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, true);
        if (goldView.getDocumentText() == null) {
            goldView.setDocumentText(aJCas.getDocumentText());
        }

        File annotationFile = new File(annoDir, docid + ".json");

        if (annotationFile.exists()) {
            try {
                AnnoDoc annoDoc = gson.fromJson(FileUtils.readFileToString(annotationFile), AnnoDoc.class);
                addAnnotations(goldView, annoDoc);
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        } else {
            logger.warn(String.format("Cannot find [%s].", annotationFile.getPath()));
        }
    }

    private void annotateSpan(JCas aJCas, DiscontinuousComponentAnnotation anno, List<Span> spans) {
        anno.setRegions(new FSArray(aJCas, spans.size()));

        int earliestBegin = Integer.MAX_VALUE;
        int latestEnd = 0;

        for (int spanIndex = 0; spanIndex < spans.size(); spanIndex++) {
            Span span = spans.get(spanIndex);
            if (span.begin < earliestBegin) {
                earliestBegin = span.begin;
            }
            if (span.end > latestEnd) {
                latestEnd = span.end;
            }
            Annotation region = new Annotation(aJCas, span.begin, span.end);
            anno.setRegions(spanIndex, region);
        }
        anno.setBegin(earliestBegin);
        anno.setEnd(latestEnd);
    }


    private void addAnnotations(JCas aJCas, AnnoDoc annoDoc) {
        Map<String, EntityMention> id2Ent = new HashMap<>();

        for (JEntity jEntity : annoDoc.entities) {
            Entity entity = new Entity(aJCas);

            List<EntityMention> mentions = new ArrayList<>();
            for (JEntityMention jMention : jEntity.mentions) {
                EntityMention ent = new EntityMention(aJCas);
                ent.setEntityType(jMention.type);
                annotateSpan(aJCas, ent, jMention.spans);
                mentions.add(ent);

                id2Ent.put(jMention.id, ent);
            }
            entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, jEntity.id, aJCas);
        }

        for (JEvent jEvent : annoDoc.events) {
            Event event = new Event(aJCas);
            List<EventMention> mentions = new ArrayList<>();
            for (JEventMention jMention : jEvent.mentions) {
                EventMention evm = new EventMention(aJCas);
                evm.setEventType(jMention.type);
                annotateSpan(aJCas, evm, jMention.spans);
                UimaAnnotationUtils.finishAnnotation(evm, COMPONENT_ID, jMention.id, aJCas);
                mentions.add(evm);

                for (JArgument argument : jMention.arguments) {
                    EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
                    argumentLink.setEventMention(evm);
                    argumentLink.setArgument(id2Ent.get(argument.arg));
                    argumentLink.setArgumentRole(argument.role);


                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "incorporated",
                            Boolean.toString(argument.meta.incorporated));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "succeeding",
                            Boolean.toString(argument.meta.succeeding));

                    UimaAnnotationUtils.finishTop(argumentLink, COMPONENT_ID, 0, aJCas);
                }
            }
            event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, mentions));
            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, jEvent.id, aJCas);
        }
    }

    public static void main(String[] args) throws UIMAException, IOException {
        String sourceTextDir = args[0];
        String annotateDir = args[1];
        String outputDir = args[2];

        TypeSystemDescription des = TypeSystemDescriptionFactory.createTypeSystemDescription("TypeSystem");

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, ".txt");

        AnalysisEngineDescription engine = AnalysisEngineFactory.createEngineDescription(
                JsonEventDataReader.class, des,
                JsonEventDataReader.PARAM_JSON_ANNO_DIR, annotateDir
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                outputDir, "gold", null, null
        );

        SimplePipeline.runPipeline(reader, engine, writer);
    }
}
