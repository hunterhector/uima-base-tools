package edu.cmu.cs.lti.collection_reader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.model.CSR;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assumed each CSR file is about one document only.
 * Date: 4/5/18
 * Time: 2:08 PM
 *
 * @author Zhengzhong Liu
 */
public class AidaCsrReader extends AbstractCollectionReader {
    public static final String PARAM_SOURCE_DIR = "sourceDir";
    @ConfigurationParameter(name = PARAM_SOURCE_DIR)
    private File sourceDir;

    public static final String PARAM_SOURCE_EXT = "SourceExtension";
    @ConfigurationParameter(name = PARAM_SOURCE_EXT, defaultValue = "txt")
    String sourceExt;

    Map<String, File> name2File;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        name2File = new HashMap<>();
        for (File sourceFile : FileUtils.listFiles(sourceDir, new String[]{sourceExt}, false)) {
            name2File.put(FilenameUtils.getBaseName(sourceFile.getName()), sourceFile);
        }

    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        File csrFile = files.get(fileIndex++);
        String content = FileUtils.readFileToString(csrFile);
        Gson gson = new Gson();

        JsonParser parser = new JsonParser();
        JsonObject topObject = parser.parse(content).getAsJsonObject();

        CSR.Meta meta = gson.fromJson(topObject.get("meta"), CSR.Meta.class);

        String docId = meta.documentId;
        String plainDocName = docId.substring(docId.indexOf(':') + 1, docId.lastIndexOf('-'));

        File sourceFile = name2File.get(plainDocName);

        String documentText = FileUtils.readFileToString(sourceFile);

        jCas.setDocumentText(documentText);

        UimaAnnotationUtils.setSourceDocumentInformation(jCas, csrFile.toURI().toURL().toString(),
                documentText.length(), 0, true);
        Article article = new Article(jCas, 0, documentText.length());
        article.setArticleName(plainDocName);
        UimaAnnotationUtils.finishAnnotation(article, COMPONENT_ID, 0, jCas);

        Map<String, Integer> sentStarts = new HashMap<>();
        List<CSR.EventMentionFrame> events = new ArrayList<>();
        List<CSR.EntityMentionFrame> entities = new ArrayList<>();

        Map<String, EntityMention> id2EntityMentions = new HashMap<>();

        for (JsonElement frameElement : topObject.get("frames").getAsJsonArray()) {
            JsonObject frame = frameElement.getAsJsonObject();
            String objectType = frame.get("@type").getAsString();

            switch (objectType) {
                case "document":
                    CSR.DocFrame doc = gson.fromJson(frame, CSR.DocFrame.class);
                    break;
                case "sentence":
                    CSR.SentenceFrame sentence = gson.fromJson(frame, CSR.SentenceFrame.class);
                    addSentence(jCas, sentStarts, sentence);
                    break;
                case "event_mention":
                    CSR.EventMentionFrame evm = gson.fromJson(frame, CSR.EventMentionFrame.class);
                    events.add(evm);
                    break;
                case "entity_mention":
                    CSR.EntityMentionFrame en = gson.fromJson(frame, CSR.EntityMentionFrame.class);
                    entities.add(en);
                default:
                    break;
            }
        }

        for (CSR.EntityMentionFrame entity : entities) {
            addEntityMention(jCas, entity, sentStarts, id2EntityMentions);
        }

        for (CSR.EventMentionFrame event : events) {
            addEventMention(jCas, event, sentStarts, id2EntityMentions);
        }
    }

    private void addSentence(JCas aJCas, Map<String, Integer> sentStarts, CSR.SentenceFrame frame) {
        sentStarts.put(frame.id, frame.extent.start);
        Sentence sentence = new Sentence(aJCas, frame.extent.start, frame.extent.start + frame.extent.length);
        UimaAnnotationUtils.finishAnnotation(sentence, COMPONENT_ID, frame.id, aJCas);
    }

    private void addEventMention(JCas aJCas, CSR.EventMentionFrame frame, Map<String, Integer> sentStarts,
                                 Map<String, EntityMention> id2EntityMentions) {
        int sentStart = sentStarts.get(frame.trigger.reference);
        int begin = sentStart + frame.trigger.start;
        int end = sentStart + frame.trigger.start + frame.trigger.length;
        EventMention evm = new EventMention(aJCas, begin, end);

        List<EventMentionArgumentLink> argumentLinks = new ArrayList<>();
        for (CSR.EventMentionArg arg : frame.interp.args) {
            String argRole = arg.type;
            String argEnt = arg.arg;
            EntityMention entityMention = id2EntityMentions.get(argEnt);
            EventMentionArgumentLink argLink = new EventMentionArgumentLink(aJCas);
            argLink.setArgumentRole(argRole);
            argLink.setEventMention(evm);
            argLink.setArgument(entityMention);
            argumentLinks.add(argLink);
        }
        evm.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
        evm.setEventType(frame.interp.type);
        UimaAnnotationUtils.finishAnnotation(evm, COMPONENT_ID, frame.id, aJCas);
    }

    private void addEntityMention(JCas aJCas, CSR.EntityMentionFrame frame, Map<String, Integer> sentStarts,
                                  Map<String, EntityMention> id2EntityMentions) {
        int sentStart = sentStarts.get(frame.reference);
        int begin = sentStart + frame.start;
        int end = sentStart + frame.start + frame.length;
        EntityMention ent = new EntityMention(aJCas, begin, end);
        id2EntityMentions.put(frame.id, ent);
        ent.setEntityType(frame.interp.type);
        UimaAnnotationUtils.finishAnnotation(ent, COMPONENT_ID, frame.id, aJCas);
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return fileIndex < files.size();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    public static void main(String[] args) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        String csrDir = args[0];
        String sourceDir = args[1];

        String outputDir = args[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AidaCsrReader.class, typeSystemDescription,
                AidaCsrReader.PARAM_DATA_PATH, csrDir,
                AidaCsrReader.PARAM_SOURCE_DIR, sourceDir,
                AidaCsrReader.PARAM_EXTENSION, "json"
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(outputDir, "xmi");
        SimplePipeline.runPipeline(reader, writer);
    }
}
