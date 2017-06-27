package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.CoverageReporter;
import edu.cmu.cs.lti.annotators.KbpEntityLinkerResultAnnotator;
import edu.cmu.cs.lti.annotators.KBPArgumentOutputAnnotator;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.LDCXmlCollectionReader;
import edu.cmu.cs.lti.model.UimaConst;
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
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/15/17
 * Time: 4:54 PM
 *
 * @author Zhengzhong Liu
 */
public class CmuResultAggregator extends AbstractLoggingAnnotator {
    static String workingDir = "../data/project_data/cold_start_results/uima";
    static TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
            .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    public static void main(String[] argv) throws IOException, UIMAException {
//        CollectionReaderDescription preprocessed = preprocess(typeSystemDescription, workingDir);

        CollectionReaderDescription preprocessed = CustomCollectionReaderFactory.createXmiReader
                (typeSystemDescription, workingDir, "preprocessed");

        runAggregator(typeSystemDescription, preprocessed, workingDir);
    }

    private static CollectionReaderDescription runAggregator(TypeSystemDescription typeSystemDescription,
                                                             CollectionReaderDescription reader, String workingDir)
            throws UIMAException, IOException {
        String entityResults = "../data/project_data/cold_start_results/submission_post.txt";

        List<AnalysisEngineDescription> annotators = new ArrayList<>();

        AnalysisEngineDescription entityLinker = AnalysisEngineFactory.createEngineDescription(
                KbpEntityLinkerResultAnnotator.class, typeSystemDescription,
                KbpEntityLinkerResultAnnotator.PARAM_ENTITY_LINKER_RESULTS, entityResults
        );


        annotators.add(entityLinker);
        annotators.add(getReporter("entity_report"));
        annotators.add(getWriter("entity"));

        String[] argumentOutputs = {
                "../data/project_data/cold_start_results/arguments_jun",
                "../data/project_data/cold_start_results/arguments_hector",
                "../data/project_data/cold_start_results/arguments_andrew",
        };

        for (String argumentOutput : argumentOutputs) {
            AnalysisEngineDescription eventAdder = AnalysisEngineFactory.createEngineDescription(
                    KBPArgumentOutputAnnotator.class, typeSystemDescription,
                    KBPArgumentOutputAnnotator.PARAM_KBP_ARGUMENT_RESULTS, argumentOutput
            );
            String name = new File(argumentOutput).getName();
            annotators.add(eventAdder);
            annotators.add(getReporter("event_" + name + "_report"));
            annotators.add(getWriter("event_" + name));
        }

        annotators.add(getWriter("overall"));
        annotators.add(getReporter("overall_report"));

        SimplePipeline.runPipeline(reader, annotators.toArray(new AnalysisEngineDescription[annotators.size()]));

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, workingDir, "aggregated");
    }

    private static AnalysisEngineDescription getReporter(String reportName) throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                CoverageReporter.class, typeSystemDescription,
                CoverageReporter.PARAM_REPORT_NAME, reportName
        );
    }

    private static AnalysisEngineDescription getWriter(String baseOutput) throws ResourceInitializationException {
        return CustomAnalysisEngineFactory.createXmiWriter(workingDir, baseOutput, null);

    }

    private static CollectionReaderDescription preprocess(TypeSystemDescription typeSystemDescription,
                                                          String workingDir)
            throws UIMAException, IOException {
        String inputPath = "../data/project_data/LDC/LDC2016E63_Cold_Start_Selected_Eng/";
        String fileFilter = "../data/project_data/cold_start_results/common_doc.lst";

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                LDCXmlCollectionReader.class, typeSystemDescription,
                LDCXmlCollectionReader.PARAM_DATA_PATH, inputPath,
                LDCXmlCollectionReader.PARAM_BASE_NAME_FILE_FILTER, fileFilter,
                LDCXmlCollectionReader.PARAM_LANGUAGE, "en"
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_SPLIT_ONLY, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, new String[]{UimaConst.inputViewName}
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                workingDir, "preprocessed", null);

        SimplePipeline.runPipeline(reader, stanfordAnalyzer, writer);

        return CustomCollectionReaderFactory.createXmiReader(typeSystemDescription,
                workingDir, "preprocessed");
    }
}
