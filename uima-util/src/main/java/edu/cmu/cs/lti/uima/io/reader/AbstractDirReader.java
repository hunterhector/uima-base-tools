package edu.cmu.cs.lti.uima.io.reader;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * An abstract reader to consume input in a directory whose name is based on the
 * step number for convenience
 *
 * @author Jun Araki
 */
public abstract class AbstractDirReader extends CollectionReader_ImplBase {

    public static final String PARAM_INPUTDIR = "InputDirectory";

    public static final String PARAM_FILE_SUFFIX = "InputFileSuffix";

    public static final String PARAM_FAIL_UNKNOWN = "FailOnUnknownType";

    protected Boolean failOnUnknownType;

    protected File inputDir;

    protected String inputFileSuffix;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize() throws ResourceInitializationException {
        super.initialize();

        failOnUnknownType = (Boolean) getConfigParameterValue(PARAM_FAIL_UNKNOWN);
        if (null == failOnUnknownType) {
            failOnUnknownType = true; // default to true if not specified
        }

        inputFileSuffix = (String) getConfigParameterValue(PARAM_FILE_SUFFIX);
        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = getDefaultFileSuffix();
        }

        inputDir = new File(((String) getConfigParameterValue(PARAM_INPUTDIR)).trim());

        // if input directory does not exist or is not a directory, throw exception
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
                    new Object[]{PARAM_INPUTDIR, this.getMetaData().getName(), inputDir.getPath()});
        }

        subInitialize();
    }

    /**
     * A subclass can do its own initialization in this method
     *
     * @throws Exception
     */
    public abstract void subInitialize();

    protected abstract String getDefaultFileSuffix();

}
