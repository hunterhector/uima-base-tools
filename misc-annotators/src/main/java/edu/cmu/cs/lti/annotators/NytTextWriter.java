package edu.cmu.cs.lti.annotators;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.ArticleComponent;
import edu.cmu.cs.lti.script.type.Body;
import edu.cmu.cs.lti.script.type.Headline;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 7/19/17
 * Time: 11:45 PM
 *
 * @author Zhengzhong Liu
 */
public class NytTextWriter extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Headline headline = JCasUtil.selectSingle(aJCas, Headline.class);
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        String docid = UimaConvenience.getDocId(aJCas);

        JsonObject root = new JsonObject();

        String tokenizedHeadline = asTokenized(headline);

        if (tokenizedHeadline.trim().isEmpty()) {
            root.addProperty("title", "N/A");
        } else {
            root.addProperty("title", asTokenized(headline));
        }
        root.addProperty("bodyText", asTokenized(body));

        Gson gson = new Gson();
        String jsonString = gson.toJson(root);

        try {
            FileUtils.write(outputFile, jsonString + "\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String asTokenized(ArticleComponent component) {
        List<String> words = new ArrayList<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, component)) {
            words.add(token.getCoveredText());
        }
        return Joiner.on(" ").join(words);
    }

    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String paramInputDir = argv[0];
        String outputFile = argv[1];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(paramInputDir, "tokenized");

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                NytTextWriter.class, typeSystemDescription,
                NytTextWriter.PARAM_OUTPUT_FILE, outputFile
        );

        new BasicPipeline(reader, true, true, 5, writer).run();
    }
}
