package edu.cmu.cs.lti.collection_reader.test;

import edu.cmu.cs.lti.collection_reader.AgigaCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * This pipeline runs FanseAnnotator.
 */
public class AgigaReaderTestPipeline {

    private static String className = AgigaReaderTestPipeline.class.getSimpleName();

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        String paramInputDir = "/Users/zhengzhongliu/Documents/data/agiga_sample";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "xmi";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AgigaCollectionReader.class, typeSystemDescription,
                AgigaCollectionReader.PARAM_INPUTDIR, paramInputDir);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                paramOutputFileSuffix);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }

}
