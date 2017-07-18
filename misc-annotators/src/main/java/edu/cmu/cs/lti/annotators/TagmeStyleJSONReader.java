package edu.cmu.cs.lti.annotators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;

import java.io.*;

import static edu.cmu.cs.lti.uima.util.CasSerialization.cleanText;

public class TagmeStyleJSONReader extends AbstractCollectionReader {
    public static final String PARAM_INPUT_JSON = "inputJson";

    @ConfigurationParameter(name = PARAM_INPUT_JSON)
    private File inputJSon;

    private BufferedReader reader;

    private String nextLine;

    private int lineNumber;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputJSon)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        JsonObject jsonObj = new JsonParser().parse(nextLine).getAsJsonObject();
        String title = jsonObj.get("title").getAsString();
        String text = jsonObj.get("bodyText").getAsString();
        String docid = jsonObj.get("docno").getAsString();

        JsonObject allSpots = jsonObj.get("spot").getAsJsonObject();

        JsonArray bodySpots = allSpots.get("bodyText").getAsJsonArray();
        JsonArray titleSpots = allSpots.get("title").getAsJsonArray();

        String documentText = cleanText(title + "\n" + text);

        jCas.setDocumentText(documentText);

        // Spots not implemented yet.
        addSpots(jCas, titleSpots, 0);
        addSpots(jCas, bodySpots, title.length() + 1);

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, jCas);
        article.setArticleName(FilenameUtils.getBaseName(docid));
        article.setLanguage(language);

        // Also store location of source document in CAS. This information is critical
        // if CAS Consumers will need to know where the original document contents are located.
        // For example, the Semantic Search CAS Indexer writes this information into the
        // search index that it creates, which allows applications that use the search index to
        // locate the documents that satisfy their semantic queries.
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jCas);
        srcDocInfo.setUri(inputJSon.toURI().toURL().toString());
        srcDocInfo.setOffsetInSource(lineNumber);
        srcDocInfo.setDocumentSize((int) inputJSon.length());
        srcDocInfo.setLastSegment(false);
        srcDocInfo.addToIndexes();

        lineNumber++;
    }

    private void addSpots(JCas aJCas, JsonArray spots, int offset){

    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return (nextLine = reader.readLine()) != null;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    @Override
    public void close() throws IOException {
        super.close();
        reader.close();
    }
}