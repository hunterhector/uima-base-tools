package edu.cmu.cs.lti.uima.io.reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class GzippedXmiCollectionReader extends AbstractDirReader {

    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    public static final String PARAM_RECURSIVE = "recursive";

    private static final String DEFAULT_FILE_SUFFIX = ".xmi.gz";

    private String inputViewName;

    private List<File> xmiFiles;

    private int currentDocIndex;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = DEFAULT_FILE_SUFFIX;
        }

        Boolean recursive = (Boolean) getConfigParameterValue(PARAM_RECURSIVE);
        if (recursive == null) {
            recursive = false;
        }

        // Get a list of XMI files in the specified directory
        String[] exts = new String[1];
        exts[0] = inputFileSuffix;

        xmiFiles = new ArrayList<>(FileUtils.listFiles(inputDir, exts, recursive));

//        File[] files = inputDir.listFiles();
//        for (int i = 0; i < files.length; i++) {
//            if (!files[i].isDirectory() && files[i].getName().endsWith(inputFileSuffix)) {
//                xmiFiles.add(files[i]);
//            }
//        }

        if (xmiFiles.size() == 0) {
            logger.warn("The directory " + inputDir.getAbsolutePath()
                    + " does not have any compressed files ending with " + inputFileSuffix);
        }

        currentDocIndex = 0;
    }

    @Override
    public void subInitialize() {
    }

    @Override
    protected String getDefaultFileSuffix() {
        return DEFAULT_FILE_SUFFIX;
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    public boolean hasNext() {
        return currentDocIndex < xmiFiles.size();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     */
    public void getNext(CAS aCAS) throws IOException, CollectionException {
        try {
            if (!StringUtils.isEmpty(inputViewName)) {
                aCAS = aCAS.getView(inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        File currentFile = xmiFiles.get(currentDocIndex);
        currentDocIndex++;

        GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(currentFile));
        try {
            XmiCasDeserializer.deserialize(gzipIn, aCAS, !failOnUnknownType);
            gzipIn.close();
        } catch (SAXException e) {
            throw new CollectionException(e);
        }
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() throws IOException {
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, xmiFiles.size(), Progress.ENTITIES)};
    }

}