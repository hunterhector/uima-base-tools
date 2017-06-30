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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @ConfigurationParameter(name = PARAM_RECURSIVE, description = "false")
    protected boolean recursive;

    public static final String PARAM_BASE_NAME_FILE_FILTER = "BaseNameFileFilter";
    @ConfigurationParameter(name = PARAM_BASE_NAME_FILE_FILTER, mandatory = false)
    protected File baseNameFileFilter;

    public static final String PARAM_BASE_NAME_IGNORES = "BaseNameIgnores";
    @ConfigurationParameter(name = PARAM_BASE_NAME_IGNORES, mandatory = false)
    protected File baseNameIgnores;

    public static final String PARAM_DATA_PATH = "dataPath";
    @ConfigurationParameter(name = PARAM_DATA_PATH)
    protected String dataPath;

    public static final String PARAM_FILE_EXTENSION = "extension";
    @ConfigurationParameter(name = PARAM_FILE_EXTENSION, mandatory = false)
    protected String extension;

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

        logger.info("Reading from: " + dataPath);

        // Setup ignores.
        Set<String> ignoringBaseNames = new HashSet<>();
        try {
            for (String s : FileUtils.readLines(baseNameIgnores)) {
                ignoringBaseNames.add(s.trim());
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        logger.info(String.format("Number of black listed base name: %d.", ignoringBaseNames.size()));

        // Setup accepts
        Set<String> acceptableBasenames = new HashSet<>();
        if (baseNameFileFilter != null) {
            logger.info("Reading with base name filter from : " + dataPath);
            try {
                for (String line : FileUtils.readLines(baseNameFileFilter)) {
                    acceptableBasenames.add(line);
                }
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
            logger.info(String.format("Number of white listed files: %d.", acceptableBasenames.size()));
        } else {
            logger.info("No white listed file found.");
        }

        IOFileFilter fileFilter;

        AtomicInteger numFilesToRead = new AtomicInteger();
        AtomicInteger numFilesIgnored = new AtomicInteger();

        fileFilter = new AbstractFileFilter() {
            @Override
            public boolean accept(File file) {
                String baseName = FilenameUtils.getBaseName(file.getName());
                boolean trueExtension = extension == null || file.getName().endsWith(extension);
                boolean blackList = ignoringBaseNames.contains(baseName);
                boolean inWhilteList = baseNameFileFilter == null || acceptableBasenames.contains(baseName);

                if (blackList) {
                    numFilesIgnored.incrementAndGet();
                }

                boolean accept = inWhilteList && !blackList && trueExtension;

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

        logger.info(String.format("%d files ignored, %d files will be read.",
                numFilesIgnored.get(), numFilesToRead.get()));
    }
}
