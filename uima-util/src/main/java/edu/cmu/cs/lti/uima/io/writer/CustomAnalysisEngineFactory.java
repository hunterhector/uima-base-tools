package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.Set;

public class CustomAnalysisEngineFactory {
    /**
     * Creates an XMI writer assuming the directory naming convention
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath, String baseOutputDirName)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirXmiWriter.class,
                StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates an XMI writer assuming the directory naming convention
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath,
                                                            String baseOutputDirName, Integer stepNumber, String
                                                                    outputFileSuffix)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirXmiWriter.class,
                StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates an XMI writer assuming the directory naming convention
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath,
                                                            String baseOutputDirName, Integer stepNumber)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirXmiWriter.class,
                StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }


    /**
     * Creates an XMI writer assuming the directory naming convention
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @param srcDocInfoViewName  the view that contains the source document info
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createXmiWriter(String parentOutputDirPath,
                                                            String baseOutputDirName, Integer stepNumber, String
                                                                    outputFileSuffix,
                                                            String srcDocInfoViewName) throws
            ResourceInitializationException {
        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                StepBasedDirXmiWriter.class, StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH,
                parentOutputDirPath, StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME,
                baseOutputDirName, StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
        return writer;
    }

    /**
     * Creates an XMI writer assuming the directory naming convention but compress into gzip
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @param srcDocInfoViewName
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createGzipWriter(String parentOutputDirPath,
                                                             String baseOutputDirName, Integer stepNumber,
                                                             String outputFileSuffix,
                                                             String srcDocInfoViewName) throws
            ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates an XMI writer assuming the directory naming convention but compress into gzip
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @param srcDocInfoViewName
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createSelectiveGzipWriter(String parentOutputDirPath,
                                                                      String baseOutputDirName, Integer stepNumber,
                                                                      String outputFileSuffix,
                                                                      String srcDocInfoViewName, Set<Integer>
                                                                              outputDocumentNumbers)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName,
                StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates an XMI writer assuming the directory naming convention and provides an array of indices
     * to select documents to output
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix    the view that contains the source document info
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createSelectiveXmiWriter(String parentOutputDirPath,
                                                                     String baseOutputDirName, Integer stepNumber,
                                                                     String outputFileSuffix,
                                                                     Set<Integer> outputDocumentNumbers) throws
            ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirXmiWriter.class, StepBasedDirXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH,
                parentOutputDirPath, StepBasedDirXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME,
                baseOutputDirName, StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates a gzipped XMI writer without specifying a particular output view.
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createGzippedXmiWriter(String parentOutputDirPath,
                                                                   String baseOutputDirName)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates a gzipped XMI writer without specifying a particular output view.
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createGzippedXmiWriter(String parentOutputDirPath,
                                                                   String baseOutputDirName, Integer stepNumber,
                                                                   String outputFileSuffix)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    /**
     * Creates a gzipped XMI writer while specifying an output view.
     *
     * @param parentOutputDirPath
     * @param baseOutputDirName
     * @param stepNumber
     * @param outputFileSuffix
     * @param srcDocInfoViewName
     * @return
     * @throws ResourceInitializationException
     */
    public static AnalysisEngineDescription createGzippedXmiWriter(String parentOutputDirPath,
                                                                   String baseOutputDirName, Integer stepNumber,
                                                                   String outputFileSuffix,
                                                                   String srcDocInfoViewName) throws
            ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiWriter.PARAM_SRC_DOC_INFO_VIEW_NAME, srcDocInfoViewName,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }

    public static AnalysisEngineDescription createSelectiveGzippedXmiWriter(
            String parentOutputDirPath, String baseOutputDirName, Integer stepNumber,
            String outputFileSuffix, Set<Integer> outputDocumentNumbers)
            throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, parentOutputDirPath,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, baseOutputDirName,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_SUFFIX, outputFileSuffix,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiWriter.PARAM_OUTPUT_FILE_NUMBERS, outputDocumentNumbers,
                AbstractLoggingAnnotator.MULTI_THREAD, true
        );
    }
}
