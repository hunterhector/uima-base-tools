package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.util.NewsNameComparators;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class OffsetSortedGzippedXmiCollectionReader extends CollectionReader_ImplBase {
    public static final String PARAM_INPUTDIR = "InputDirectory";

    public static final String PARAM_FAILUNKNOWN = "failOnUnkown";

    public static final String PARAM_FILE_SUFFIX = "fileSuffix";

//    public static final String PARAM_INPUT_VIEW_NAME = "viewName";

    private static final String DEFAULT_FILE_SUFFIX = ".xmi.gz";

    private String inputViewName;

    private List<File> xmiFiles;

    private int currentDocIndex;

    private Boolean failOnUnknownType;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        failOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAILUNKNOWN);
        if (null == failOnUnknownType) {
            failOnUnknownType = true; // default to true if not specified
        }

        String inputFileSuffix = (String) getConfigParameterValue(PARAM_FILE_SUFFIX);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = DEFAULT_FILE_SUFFIX;
        }

        File inputDir = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());

        // if input directory does not exist or is not a directory, throw exception
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                    new Object[]{PARAM_INPUTDIR, this.getMetaData().getName(), inputDir.getPath()});
        }

        // Get a list of XMI files in the specified directory
        xmiFiles = new ArrayList<File>();
        File[] files = inputDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && files[i].getName().endsWith(inputFileSuffix)) {
                xmiFiles.add(files[i]);
            }
        }

        if (xmiFiles.size() == 0) {
            logger.warn("The directory " + inputDir.getAbsolutePath()
                    + " does not have any compressed files ending with " + inputFileSuffix);
        }
        Collections.sort(xmiFiles, NewsNameComparators.getGigawordDateComparator(inputFileSuffix, "yyyymm"));

        currentDocIndex = 0;
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