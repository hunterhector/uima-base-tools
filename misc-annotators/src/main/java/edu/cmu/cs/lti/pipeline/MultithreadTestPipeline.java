package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.QuoteAnnotator;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/17/17
 * Time: 5:53 PM
 *
 * @author Zhengzhong Liu
 */
public class MultithreadTestPipeline {
    public static void main(String[] args) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputDir = args[0];
        String outputDir = args[1];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputDir,
                LDCXmlCollectionReader.PARAM_LANGUAGE, "en"
        );


        AnalysisEngineDescription stanford = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                AbstractAnnotator.MULTI_THREAD, true
        );


        final String semaforModelDirectory = "../models/semafor_malt_model_20121129";

        AnalysisEngineDescription semafor = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory,
                SemaforAnnotator.PARAM_JSON_OUTPUT_REDIRECT,
                FileUtils.joinPaths(outputDir, "semafor_json"),
                AbstractAnnotator.MULTI_THREAD, true
        );

        AnalysisEngineDescription quote = AnalysisEngineFactory.createEngineDescription(
                QuoteAnnotator.class, typeSystemDescription,
                QuoteAnnotator.MULTI_THREAD, true
        );

        new BasicPipeline(reader, true, outputDir, "xmi", stanford, semafor, quote).run();
    }
}
