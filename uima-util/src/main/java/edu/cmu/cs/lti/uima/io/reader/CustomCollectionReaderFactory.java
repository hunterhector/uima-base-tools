package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.CrossValidationReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;

public class CustomCollectionReaderFactory {
    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(String parentInputDirName,
                                                              String baseInputDirName,
                                                              Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Creates a simple XMI reader with the specified type system.
     * convention.
     *
     * @param typeSystemDescription
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription,
                                                              String inputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription,
                XmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                XmiCollectionReader.PARAM_FAIL_UNKNOWN, false);
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
                XmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                XmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }


    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(String parentInputDirName, String baseInputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRecursiveXmiReader(String parentInputDirName,
                                                                       String baseInputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription, String
            parentInputDirName, String baseInputDirName) throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription, String
            parentInputDirName, String baseInputDirName, File blackList) throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_NAME_IGNORES, blackList
        );
    }

    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createXmiReader(TypeSystemDescription typeSystemDescription, String
            parentInputDirName, String baseInputDirName, File blackList, File whiteList)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                StepBasedDirXmiCollectionReader.class, typeSystemDescription,
                StepBasedDirXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                StepBasedDirXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_NAME_IGNORES, blackList,
                StepBasedDirGzippedXmiCollectionReader.PARAM_BASE_NAME_FILE_FILTER, whiteList
        );
    }


    /**
     * Creates a simple XMI reader assuming the directory naming convention
     *
     * @param parentInputDirName
     * @param baseInputDirName
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRandomizedXmiReader(TypeSystemDescription typeSystemDescription,
                                                                        String parentInputDirName,
                                                                        String baseInputDirName,
                                                                        int randomSeed)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                RandomizedXmiCollectionReader.class, typeSystemDescription,
                RandomizedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                RandomizedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                RandomizedXmiCollectionReader.PARAM_SEED, randomSeed
        );
    }

    /**
     * Create a cross validation reader
     *
     * @param typeSystemDescription The type system of the cas.
     * @param parentInputDirName    The main working directory.
     * @param baseInputDirName      The directory under parentInput that really contains the data.
     * @param takeEvalSplit         Whether to take the evaluation split (small slice).
     * @param randomSeed            The random seed.
     * @param slice                 Choose which slice as eval (others as training)
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createCrossValidationReader(TypeSystemDescription typeSystemDescription,
                                                                          String parentInputDirName,
                                                                          String baseInputDirName,
                                                                          boolean takeEvalSplit,
                                                                          int randomSeed,
                                                                          int split,
                                                                          int slice)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                CrossValidationReader.class, typeSystemDescription,
                CrossValidationReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                CrossValidationReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                CrossValidationReader.PARAM_MODE_EVAL, takeEvalSplit,
                CrossValidationReader.PARAM_SEED, randomSeed,
                CrossValidationReader.PARAM_SLICE, slice,
                CrossValidationReader.PARAM_SPLITS, split
        );
    }

    /**
     * Create a cross validation reader
     *
     * @param typeSystemDescription The type system of the cas.
     * @param parentInputDirName    The main working directory.
     * @param baseInputDirName      The directory under parentInput that really contains the data.
     * @param extension             The file extension to read (no ".")
     * @param takeEvalSplit         Whether to take the evaluation split (small slice).
     * @param randomSeed            The random seed.
     * @param slice                 Choose which slice as eval (others as training)
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createCrossValidationReader(TypeSystemDescription typeSystemDescription,
                                                                          String parentInputDirName,
                                                                          String baseInputDirName,
                                                                          String extension,
                                                                          boolean takeEvalSplit,
                                                                          int randomSeed,
                                                                          int slice)
            throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                CrossValidationReader.class, typeSystemDescription,
                CrossValidationReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDirName,
                CrossValidationReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDirName,
                CrossValidationReader.PARAM_MODE_EVAL, takeEvalSplit,
                CrossValidationReader.PARAM_SEED, randomSeed,
                CrossValidationReader.PARAM_SLICE, slice,
                CrossValidationReader.PARAM_EXTENSION, extension
        );
    }


    /**
     * Create a xmi reader that read the documents according to the timestamp (Gigaword docs)
     *
     * @param typeSystemDescription
     * @param parentInputDir
     * @param baseInputDir
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createTimeSortedXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDir, String baseInputDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                TimeSortedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDir,
                TimeSortedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDir
        );
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
                GzippedXmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(TypeSystemDescription typeSystemDescription,
                                                                     String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createGzippedXmiReader(TypeSystemDescription typeSystemDescription,
                                                                     String inputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, false);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRecursiveGzippedXmiReader(TypeSystemDescription
                                                                                      typeSystemDescription,
                                                                              String inputDirName)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, false,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true);
    }

    /**
     * Create a gzipped XMI reader assuming the directory naming convention.
     *
     * @param failOnUnkown
     * @return
     * @throws ResourceInitializationException
     */
    public static CollectionReaderDescription createRecursiveGzippedXmiReader(TypeSystemDescription
                                                                                      typeSystemDescription,
                                                                              String inputDirName, Boolean failOnUnkown)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_DATA_PATH, inputDirName,
                GzippedXmiCollectionReader.PARAM_FAIL_UNKNOWN, failOnUnkown,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );
    }

    public static CollectionReaderDescription createRecursiveGzippedXmiReader(
            TypeSystemDescription typeSystemDescription, String parentDir, String baseDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.
        return CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseDir,
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );
    }


    public static CollectionReaderDescription createTimeSortedGzipXmiReader(
            TypeSystemDescription typeSystemDescription, String parentInputDir, String baseInputDir)
            throws ResourceInitializationException {
        // Instantiate a collection reader to get XMI as input.

        return CollectionReaderFactory.createReaderDescription(
                TimeSortedGzippedXmiCollectionReader.class, typeSystemDescription,
                TimeSortedGzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInputDir,
                TimeSortedGzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );
    }
}
