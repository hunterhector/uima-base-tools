package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Test the semafor pipeline and uima statistics.
 */
public class SemaforAnnotatorPipeline {

    public static void main(String[] args) throws UIMAException, CpeDescriptorException, SAXException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputDir = args[0];
        String outputDir = args[1];

        String semaforModelDirectory = "../models/semafor_malt_model_20121129";

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputDir,
                LDCXmlCollectionReader.PARAM_LANGUAGE, "en"
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_WHITESPACE_TOKENIZE, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAXLEN, 70,
                StanfordCoreNlpAnnotator.PARAM_NUMERIC_CLASSIFIER, false,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, false,
                StanfordCoreNlpAnnotator.PARAM_SPLIT_ONLY, false,
                StanfordCoreNlpAnnotator.PARAM_SHIFT_REDUCE, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAX_COREF_DIST, 5
        );


        AnalysisEngineDescription semaforAnalyzer = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory,
                SemaforAnnotator.PARAM_JSON_OUTPUT_REDIRECT, FileUtils.joinPaths(outputDir, "json")
        );

        new BasicPipeline(new ProcessorWrapper() {
            @Override
            public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                return new AnalysisEngineDescription[]{stanfordAnalyzer, semaforAnalyzer};
//                return new AnalysisEngineDescription[]{quote};
            }
        }, true, outputDir, "xmi").runWithOutput();
    }
}
