package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.pipeline.SimplePipeline;

/**
 * This pipeline runs FanseAnnotator.
 */
public class StanfordCoreNlpPipeline {

    private static String className = StanfordCoreNlpPipeline.class.getSimpleName();

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        // Parameters for the writer
        String paramParentOutputDir = "data/test/process";
        String paramBaseOutputDirName = "xmi";

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createPlainTextReader("data/test/input");

        AnalysisEngineDescription stanfordAnalyzer = CustomAnalysisEngineFactory.createAnalysisEngine(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                null);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, stanfordAnalyzer, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }

}
