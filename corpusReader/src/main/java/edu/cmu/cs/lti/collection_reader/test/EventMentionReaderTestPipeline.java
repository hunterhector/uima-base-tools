package edu.cmu.cs.lti.collection_reader.test;

import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
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
public class EventMentionReaderTestPipeline {

    private static String className = EventMentionReaderTestPipeline.class.getSimpleName();

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        String paramInputDir =
                "/Users/zhengzhongliu/Documents/projects" +
                        "/cmu-script/event-mention-detection" +
                        "/data/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/";

        String goldStandardFilePath = paramInputDir + "converted.tbf";
        String sourceDataPath = paramInputDir + "source";
        String tokenDataPath = paramInputDir + "token_offset";

        // Parameters for the writer
        String paramParentOutputDir = "data/event_mention_detection";
        String paramBaseOutputDirName = "plain";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                EventMentionDetectionDataReader.class, typeSystemDescription,
                EventMentionDetectionDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                EventMentionDetectionDataReader.PARAM_SOURCE_EXT, ".tkn.txt",
                EventMentionDetectionDataReader.PARAM_SOURCE_TEXT_DIRECTORY, sourceDataPath,
                EventMentionDetectionDataReader.PARAM_TOKEN_DIRECTORY, tokenDataPath,
                EventMentionDetectionDataReader.PARAM_TOKEN_EXT, ".txt.tab"
        );


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
