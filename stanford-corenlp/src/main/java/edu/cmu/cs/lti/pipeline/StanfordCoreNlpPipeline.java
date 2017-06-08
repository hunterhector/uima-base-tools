package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.JSONReader;
import edu.cmu.cs.lti.annotators.JSONWriter;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;

/**
 * This pipeline runs Stanford Corenlp Annotator.
 */
public class StanfordCoreNlpPipeline {
    private static String className = StanfordCoreNlpPipeline.class.getSimpleName();

    private static CollectionReaderDescription getReader(TypeSystemDescription typeSystemDescription, String path)
            throws ResourceInitializationException {
//        return CollectionReaderFactory.createReaderDescription(
//                PlainTextCollectionReader.class, typeSystemDescription,
//                PlainTextCollectionReader.PARAM_INPUTDIR, "data/test/input"
//        );

        return CollectionReaderFactory.createReaderDescription(
                JSONReader.class, typeSystemDescription,
                JSONReader.PARAM_INPUT_JSON, path
        );
    }

//    public static void main(String[] args) throws UIMAException {
//        System.out.println(className + " started...");
//
//        // Parameters for the writer
////        String paramParentOutputDir = "data/test/process";
//        String outputDir = args[0];
//
//        String paramBaseOutputDirName = "stanford";
//
//        String paramTypeSystemDescriptor = "TypeSystem";
//
//        // Instantiate the analysis engine.
//        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
//                .createTypeSystemDescription(paramTypeSystemDescriptor);
//
//        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
//                outputDir, paramBaseOutputDirName);
//
//        String jsonOut = FileUtils.joinPaths(outputDir, "json");
//
//        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
//                JSONWriter.class, typeSystemDescription,
//                JSONWriter.PARAM_OUTPUT_PATH, jsonOut
//        );
//
//        // Run the pipeline.
//        try {
//            SimplePipeline.runPipeline(reader, jsonWriter);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//
//        System.out.println(className + " successfully completed.");
//    }

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        // Parameters for the writer
//        String paramParentOutputDir = "data/test/process";
        String input = args[0];
        String outputDir = args[1];

        String paramBaseOutputDirName = "stanford";

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = getReader(typeSystemDescription, input);

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

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                outputDir, paramBaseOutputDirName, null,
                null);

        String jsonOut = FileUtils.joinPaths(outputDir, "json");
        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                JSONWriter.class, typeSystemDescription,
                JSONWriter.PARAM_OUTPUT_PATH, jsonOut
        );

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, stanfordAnalyzer, writer, jsonWriter);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }

}
