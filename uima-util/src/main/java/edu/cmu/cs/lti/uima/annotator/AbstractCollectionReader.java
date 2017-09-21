package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.model.UimaConst;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/11/15
 * Time: 5:19 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractCollectionReader extends JCasCollectionReader_ImplBase {
    public final static String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public final static String PARAM_ENCODING = "encoding";

    public static final String PARAM_INPUT_VIEW_NAME = "inputViewName";

    @ConfigurationParameter(mandatory = false, description = "The view name for the golden standard view", name =
            PARAM_GOLD_STANDARD_VIEW_NAME)
    protected String goldStandardViewName;

    @ConfigurationParameter(name = PARAM_INPUT_VIEW_NAME, defaultValue = UimaConst.inputViewName)
    protected String inputViewName;

    @ConfigurationParameter(mandatory = false, description = "Specify the encoding of the input", name = PARAM_ENCODING)
    protected String encoding;

    public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, defaultValue = "en")
    protected String language;

    public static final String PARAM_RECURSIVE = "recursive";
    @ConfigurationParameter(name = PARAM_RECURSIVE, defaultValue = "false")
    protected boolean recursive;

    public static final String PARAM_BASE_NAME_FILE_FILTER = "BaseNameFileFilter";
    @ConfigurationParameter(name = PARAM_BASE_NAME_FILE_FILTER, mandatory = false)
    protected File baseNameFileFilter;

    public static final String PARAM_BASE_NAME_IGNORES = "BaseNameIgnores";
    @ConfigurationParameter(name = PARAM_BASE_NAME_IGNORES, mandatory = false)
    protected File baseNameIgnores;

    public static final String PARAM_FULL_PATH_IGNORES = "FullPathIgnores";
    @ConfigurationParameter(name = PARAM_FULL_PATH_IGNORES, mandatory = false)
    protected File fullPathIgnores;

    public static final String PARAM_DATA_PATH = "dataPath";
    @ConfigurationParameter(name = PARAM_DATA_PATH, mandatory = false)
    protected String dataPath;

    public static final String PARAM_PARENT_INPUT_DIR_PATH = "ParentInputDirPath";
    @ConfigurationParameter(name = PARAM_PARENT_INPUT_DIR_PATH, mandatory = false)
    private String parentInputDirPath;

    public static final String PARAM_BASE_INPUT_DIR_NAME = "BaseInputDirectoryName";
    @ConfigurationParameter(name = PARAM_BASE_INPUT_DIR_NAME, mandatory = false)
    private String baseInputDirName;

    public static Comparator<File> inputComparator;

    public static final String PARAM_EXTENSION = "extensionFilter";
    @ConfigurationParameter(name = PARAM_EXTENSION, mandatory = false,
            description = "If set, only files matching the extension will be read. " +
                    "Base names will be obtained by removing the extension.")
    protected String extension;

//    public static final String PARAM_INPUT_FILE_SUFFIX = "InputFileSuffix";
//    @ConfigurationParameter(name = PARAM_INPUT_FILE_SUFFIX, mandatory = false)
//    protected String inputFileSuffix;

    public static final String PARAM_FAIL_UNKNOWN = "FailOnUnknownType";
    @ConfigurationParameter(name = PARAM_FAIL_UNKNOWN, defaultValue = "false")
    protected Boolean failOnUnknownType;

    protected List<File> files;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String DEFAULT_GOLD_STANDARD_NAME = "GoldStandard";

    public final String COMPONENT_ID = this.getClass().getSimpleName();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected int fileIndex;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        if (goldStandardViewName == null) {
            goldStandardViewName = DEFAULT_GOLD_STANDARD_NAME;
        }

        if (dataPath == null) {
            if (parentInputDirPath == null || baseInputDirName == null) {
                logger.error("Both data path, and parent/base pair path are null.");
                throw new ResourceInitializationException(
                        new IllegalArgumentException("One of the two directory parameter options must be true.")
                );
            }
            dataPath = new File(parentInputDirPath, baseInputDirName).getPath();
        }

        logger.info("Reading from: " + dataPath);

        // Setup ignores.
        Set<String> ignoringBaseNames = new HashSet<>();
        boolean useBasenameBlackList = false;
        if (baseNameIgnores != null) {
            if (baseNameIgnores.exists()) {
                logger.info("Base name ignore file is: " + baseNameIgnores);
                useBasenameBlackList = true;
                try {
                    for (String s : FileUtils.readLines(baseNameIgnores)) {
                        ignoringBaseNames.add(s.trim());
                    }
                } catch (IOException e) {
                    throw new ResourceInitializationException(e);
                }
            } else {
                logger.warn(String.format("Base name black list file [%s] cannot be found, will not use blacklist.",
                        baseNameIgnores));
            }
        }
        logger.info(String.format("Number of black listed base name: %d.", ignoringBaseNames.size()));


        Set<String> ignoringFullpaths = new HashSet<>();
        boolean useFullpathBlackList = false;
        if (fullPathIgnores != null) {
            if (fullPathIgnores.exists()) {
                logger.info("Full path ignore file is: " + fullPathIgnores);
                useFullpathBlackList = true;
                try {
                    for (String s : FileUtils.readLines(fullPathIgnores)) {
                        ignoringFullpaths.add(s.trim());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.warn(String.format("Full path black list file [%s] cannot be found, will not use it",
                        fullPathIgnores));
            }
        }
        logger.info(String.format("Number of ignored full path: %d.", ignoringFullpaths.size()));

        // Setup accepts
        Set<String> acceptableBasenames = new HashSet<>();
        boolean useWhiteList = false;
        if (baseNameFileFilter != null) {
            if (baseNameFileFilter.exists()) {
                logger.info("Reading with base name white list from : " + dataPath);
                useWhiteList = true;
                try {
                    for (String line : FileUtils.readLines(baseNameFileFilter)) {
                        acceptableBasenames.add(line.trim());
                    }
                } catch (IOException e) {
                    throw new ResourceInitializationException(e);
                }
                logger.info(String.format("Number of white listed files: %d.", acceptableBasenames.size()));
            } else {
                logger.warn(String.format("Base name whitelist file [%s] cannot be found, will not use whitelist.",
                        baseNameFileFilter));
            }
        } else {
            logger.info("No white listed file found.");
        }

        IOFileFilter fileFilter;

        AtomicInteger numFilesToRead = new AtomicInteger();
        AtomicInteger numFilesIgnored = new AtomicInteger();

        boolean finalUseBlackList = useBasenameBlackList;
        boolean finalUseWhiteList = useWhiteList;
        boolean finalUseFullpathBlackList = useFullpathBlackList;
        fileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                String baseName = getBaseName(file.getName());
                String fullpathName = null;
                try {
                    fullpathName = file.getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                boolean trueExtension = extension == null || file.getName().endsWith(extension);
                boolean basenameBlackList = finalUseBlackList && ignoringBaseNames.contains(baseName);
                boolean fullPathBlackList = finalUseFullpathBlackList && ignoringFullpaths.contains(fullpathName);
                boolean passWhiteList = !finalUseWhiteList || acceptableBasenames.contains(baseName);

                if (basenameBlackList || fullPathBlackList) {
                    numFilesIgnored.incrementAndGet();
                }

                boolean accept = passWhiteList && !basenameBlackList && !fullPathBlackList && trueExtension;

                if (accept) {
                    if (file.isFile()) {
                        numFilesToRead.incrementAndGet();
                    }
                }
                return accept;
            }
        };

        IOFileFilter dirFilter = recursive ? TrueFileFilter.INSTANCE : null;

        this.files = new ArrayList<>(FileUtils.listFiles(new File(dataPath), fileFilter, dirFilter));
        fileIndex = 0;

        if (inputComparator != null) {
            this.files.sort(inputComparator);
            logger.info("Sorted input files before reading.");
        }

        logger.info(String.format("%d files ignored, %d files will be read.",
                numFilesIgnored.get(), numFilesToRead.get()));
    }

    private String getBaseName(String filename) {
        if (extension != null) {
            return filename.replaceAll(extension + "$", "");
        } else {
            return FilenameUtils.getBaseName(filename);
        }
    }
}
