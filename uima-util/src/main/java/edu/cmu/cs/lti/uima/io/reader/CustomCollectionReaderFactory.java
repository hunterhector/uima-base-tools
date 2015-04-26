package edu.cmu.cs.lti.uima.io.reader;

import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

public class CustomCollectionReaderFactory {

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(String parentInputDirName,
                                                              String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
                StepBasedDirXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Creates a simple XMI reader with the specified type system, assuming the directory naming
     * convention.
     *
     * @param typeSystemDescription
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(
            TypeSystemDescription typeSystemDescription, String inputDirName,
            Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription,
                XmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                XmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    public static CollectionReaderDescription createTimeSortedXmiReader(
            TypeSystemDescription typeSystemDescription, String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                TimeSortedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedXmiCollectionReader.PARAM_INPUTDIR, inputDirName,
                TimeSortedXmiCollectionReader.PARAM_FAILUNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(TypeSystemDescription typeSystemDescription, String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRecursiveGzippedXmiReader(TypeSystemDescription typeSystemDescription, String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_INPUT_DIR, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true);
    }


    /**
     * Create a gzipped XMI reader, with specified type system. Assuming the directory naming
     * convention.
     *
     * @param typeSystemDescription
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createStepBasedGzippedXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDirName,
            String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirGzippedXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }


    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param stepNumber
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createStepBasedGzippedXmiReader(String parentInputDirName,
                                                                              String baseInputDirName, Integer stepNumber, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirGzippedXmiCollectionReader.class,
                StepBasedDirGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_INPUT_STEP_NUMBER, stepNumber,
                StepBasedDirGzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    public static CollectionReaderDescription createTimeSortedGzipXmiReader(
            TypeSystemDescription typeSystemDescription, String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                TimeSortedGzippedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedGzippedXmiCollectionReader.PARAM_INPUTDIR, inputDirName,
                TimeSortedGzippedXmiCollectionReader.PARAM_FAILUNKNOWN, failOnUnkown);
    }


    /**
     * Creates a simple plain text reader for the text under the specified directory.
     *
     * @param inputDir
     * @param encoding
     * @param textSuffix
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createPlainTextReader(String inputViewName, String inputDir, String encoding, String[] textSuffix)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get plain text as input.
        return CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME, inputViewName,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_ENCODING, encoding,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    }

    @Deprecated
    public static CollectionReaderDescription createPlainTextReader(String inputViewName, String[] srcDocInfoViewNames, String inputDir, String encoding,
                                                                    String[] textSuffix) throws ResourceInitializationException {
        // Instantiate a collection reader to get plain text as input.
        return CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUT_VIEW_NAME, inputViewName,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_ENCODING, encoding,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    }

    /**
     * Creates a simple plain text reader for the text under the specified directory.
     *
     * @param inputDir
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createPlainTextReader(String inputDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get plain text as input.
        String[] textSuffix = {""};
        return CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    }

    /**
     * Creates a simple plain text reader for the text under the specified directory.
     *
     * @param inputDir
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createPlainTextReader(String inputDir, TypeSystemDescription typeSystemDescription)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get plain text as input.
        String[] textSuffix = {""};
        return CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, textSuffix);
    }

}
