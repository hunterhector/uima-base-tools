package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.JSONReader;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
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

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        // Parameters for the writer
//        String paramParentOutputDir = "data/test/process";
        String outptuDir = args[0];
        String paramBaseOutputDirName = "stanford";

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = getReader(typeSystemDescription, "/media/hdd/hdd0/data/joint_semantics/doc_spot.json");

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                outptuDir, paramBaseOutputDirName, 0,
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
