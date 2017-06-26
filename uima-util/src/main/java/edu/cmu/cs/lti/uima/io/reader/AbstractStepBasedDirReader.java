package edu.cmu.cs.lti.uima.io.reader;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract reader to consume input in a directory whose name is based on the
 * step number for convenience.
 * <p>
 * This is based on the UIMA Fit components.
 *
 * @author Jun Araki
 * @author Zhengzhong Liu
 */
public abstract class AbstractStepBasedDirReader extends JCasCollectionReader_ImplBase {
    public static final String PARAM_PARENT_INPUT_DIR_PATH = "ParentInputDirPath";

    public static final String PARAM_BASE_INPUT_DIR_NAME = "BaseInputDirectoryName";

    public static final String PARAM_INPUT_STEP_NUMBER = "InputStepNumber";

    public static final String PARAM_INPUT_FILE_SUFFIX = "InputFileSuffix";

    public static final String PARAM_FAIL_UNKNOWN = "FailOnUnknownType";

    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    public static final String PARAM_RECURSIVE = "recursive";

    @ConfigurationParameter(name = PARAM_PARENT_INPUT_DIR_PATH)
    private String parentInputDirPath;

    @ConfigurationParameter(name = PARAM_BASE_INPUT_DIR_NAME)
    private String baseInputDirName;

    @ConfigurationParameter(name = PARAM_INPUT_STEP_NUMBER, mandatory = false)
    private Integer inputStepNumber;

    @ConfigurationParameter(name = PARAM_INPUT_FILE_SUFFIX, mandatory = false)
    protected String inputFileSuffix;

    @ConfigurationParameter(name = PARAM_INPUT_VIEW_NAME, mandatory = false)
    protected String inputViewName;

    @ConfigurationParameter(name = PARAM_FAIL_UNKNOWN, defaultValue = "false")
    protected Boolean failOnUnknownType;

    @ConfigurationParameter(name = PARAM_RECURSIVE, defaultValue = "false")
    protected Boolean recursive;

    protected File inputDir;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ArrayList<File> files;

    protected abstract String defaultFileSuffix();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        List<String> dirNameSegments = new ArrayList<String>();

        if (inputStepNumber != null) {
            dirNameSegments.add(String.format("%02d", inputStepNumber));
        }

        dirNameSegments.add(baseInputDirName);

        String dirName = Joiner.on("_").join(dirNameSegments);

        inputDir = new File(parentInputDirPath, dirName);
        if (!inputDir.exists()) {
            throw new IllegalArgumentException(String.format(
                    "Cannot find the directory [%s] specified, please check parameters",
                    inputDir.getAbsolutePath()));
        }

        logger.info(String.format("Reading from [%s]", inputDir.getAbsolutePath()));

        if (recursive) {
            logger.info("Reading the directory recursively.");
        }

        if (StringUtils.isEmpty(inputFileSuffix)) {
            inputFileSuffix = defaultFileSuffix();
        }

        files = new ArrayList<>(FileUtils.listFiles(inputDir, new String[]{inputFileSuffix}, recursive));

        if (this.files.size() == 0) {
            logger.warn("The directory " + inputDir.getAbsolutePath()
                    + " does not have any files ending with " + inputFileSuffix);
        } else {
            logger.info("Number of files read : " + this.files.size());
        }
    }

}
