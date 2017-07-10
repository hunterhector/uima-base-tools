package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.LtpAnnotator;
import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.DocumentTextWriter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/26/17
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class LtpPipeline {
    public static void main(String[] args) throws UIMAException, CpeDescriptorException, SAXException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputDir = args[0];
        String outputDir = args[1];

        String generalModelDir = "../models/";

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputDir,
                LDCXmlCollectionReader.PARAM_LANGUAGE, "zh"
        );

        AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(
                LtpAnnotator.class, typeSystemDescription,
                LtpAnnotator.PARAM_CWS_MODEL,
                new File(generalModelDir, "ltp_models/ltp_data/cws.model"),
                LtpAnnotator.PARAM_POS_MODEL,
                new File(generalModelDir, "ltp_models/ltp_data/pos.model"),
                LtpAnnotator.PARAM_NER_MODEL,
                new File(generalModelDir, "ltp_models/ltp_data/ner.model"),
                LtpAnnotator.PARAM_DEPENDENCY_MODEL,
                new File(generalModelDir, "ltp_models/ltp_data/parser.model"),
                LtpAnnotator.PARAM_SRL_MODEL,
                new File(generalModelDir, "ltp_models/ltp_data/srl")
        );

        AnalysisEngineDescription textWriter = AnalysisEngineFactory.createEngineDescription(
                DocumentTextWriter.class, typeSystemDescription,
                DocumentTextWriter.PARAM_PARENT_OUTPUT_DIR_PATH, outputDir,
                DocumentTextWriter.PARAM_BASE_OUTPUT_DIR_NAME, "text"
        );

        new BasicPipeline(reader, false, true, 4, outputDir, "xmi", annotator).run();
    }
}
