package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
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

        String workingDir = args[0];
        String baseInput = args[1];
        String baseOutput = args[2];

        String fileFilter = null;
        if (args.length > 3) {
            fileFilter = args[3];
        }


        String semaforModelDirectory = "../models/semafor_malt_model_20121129";

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInput,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_BASE_NAME_FILE_FILTER, fileFilter,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );


        AnalysisEngineDescription semaforAnalyzer = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, typeSystemDescription,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory
//                SemaforAnnotator.PARAM_JSON_OUTPUT_REDIRECT, FileUtils.joinPaths(workingDir, baseOutput, "json")
        );

        new BasicPipeline(reader, true, true, 15, workingDir, baseOutput + "/xmi", true, semaforAnalyzer).run();
    }
}
