package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.EntityLinkerResultAnnotator;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/15/17
 * Time: 4:54 PM
 *
 * @author Zhengzhong Liu
 */
public class CmuResultAggregator extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    public static void main(String[] argv) throws IOException, UIMAException {
        String typeSystemName = "TaskEventMentionDetectionTypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        String workingDir = "~/projects/data/project_data/cold_start_results/uima";

        CollectionReaderDescription preprocessed = preprocess(typeSystemDescription, workingDir);
        runAggregator(typeSystemDescription, preprocessed, workingDir);
    }

    private static CollectionReaderDescription runAggregator(TypeSystemDescription typeSystemDescription,
                                                             CollectionReaderDescription reader, String workingDir)
            throws UIMAException, IOException {
        String entityResults = "~/projects/data/project_data/cold_start_results/submission_post.txt";

        AnalysisEngineDescription entityLinker = AnalysisEngineFactory.createEngineDescription(
                EntityLinkerResultAnnotator.class, typeSystemDescription,
                EntityLinkerResultAnnotator.PARAM_ENTITY_LINKER_RESULTS, entityResults
        );


        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                workingDir, "aggregated", 0,
                null);

        SimplePipeline.runPipeline(reader, entityLinker, writer);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, "aggregated");
    }

    private static CollectionReaderDescription preprocess(TypeSystemDescription typeSystemDescription,
                                                          String workingDir)
            throws UIMAException, IOException {
        String inputPath = "~/projects/data/project_data/LDC/LDC2016E63_Cold_Start_Selected_Eng/";
        String fileFilter = "~/projects/data/project_data/cold_start_results/common_doc.lst";

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath,
                LDCXmlCollectionReader.PARAM_BASE_NAME_FILE_FILTER, fileFilter,
                LDCXmlCollectionReader.PARAM_LANGUAGE, "zh"
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                workingDir, "preprocessed", 0,
                null);

        SimplePipeline.runPipeline(reader, stanfordAnalyzer, writer);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                workingDir, "preprocessed");
    }
}
