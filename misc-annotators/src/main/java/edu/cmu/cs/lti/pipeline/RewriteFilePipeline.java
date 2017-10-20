package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.CoreferenceJSONWriter;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/12/17
 * Time: 2:27 PM
 *
 * @author Zhengzhong Liu
 */
public class RewriteFilePipeline {
    public static void writeJson(TypeSystemDescription typeSystemDescription, String parentDir,
                                 String inputBase, String spottedSource, String outputPath)
            throws UIMAException, IOException, CpeDescriptorException, SAXException {
        System.out.println("Writing JSON");
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createRecursiveXmiReader(
                parentDir, inputBase);

        String jsonOut = FileUtils.joinPaths(outputPath);
        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                CoreferenceJSONWriter.class, typeSystemDescription,
                CoreferenceJSONWriter.PARAM_OUTPUT_PATH, jsonOut,
                CoreferenceJSONWriter.PARAM_SOURCE_TAGGED_TEXT_FOLDER, spottedSource
        );

        new BasicPipeline(reader, 8, jsonWriter).run();

//        SimplePipeline.runPipeline(reader, jsonWriter);
    }

    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        String parentDir = argv[0];
        String inputBase = argv[1];
        String spottedSourceDir = argv[2];

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String jsonOut = FileUtils.joinPaths(parentDir, "all.json");

//        writeXmi(typeSystemDescription, parentDir, inputBase, outputDir);
        writeJson(typeSystemDescription, parentDir, inputBase, spottedSourceDir, jsonOut);
    }
}
