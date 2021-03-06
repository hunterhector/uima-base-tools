package edu.cmu.cs.lti.uima.io.writer;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.type.UimaMeta;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * An abstract writer to generate output in a directory whose name is based on the current date and
 * specified step number for convenience
 *
 * @author Zhengzhong Liu, Hector
 * @author Jun Araki
 */
public abstract class AbstractStepBasedDirWriter extends AbstractLoggingAnnotator {

    public static final String PARAM_PARENT_OUTPUT_DIR_PATH = "ParentOutputDirPath";

    public static final String PARAM_BASE_OUTPUT_DIR_NAME = "BaseOutputDirectoryName";

    public static final String PARAM_OUTPUT_STEP_NUMBER = "OutputStepNumber";

    public static final String PARAM_OUTPUT_FILE_SUFFIX = "OutputFileSuffix";

    public static final String PARAM_SRC_DOC_INFO_VIEW_NAME = "SourceDocumentInfoViewName";

    public static final String PARAM_SKIP_INDICATED_DOCUMENTS = "skipIndicatedDocuments";

    @ConfigurationParameter(name = PARAM_PARENT_OUTPUT_DIR_PATH, mandatory = true)
    private String parentOutputDirPath;

    @ConfigurationParameter(name = PARAM_BASE_OUTPUT_DIR_NAME, mandatory = true)
    private String baseOutputDirName;

    @ConfigurationParameter(name = PARAM_OUTPUT_STEP_NUMBER, mandatory = false)
    private Integer outputStepNumber;

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE_SUFFIX, mandatory = false)
    protected String outputFileSuffix;

    @ConfigurationParameter(name = PARAM_SRC_DOC_INFO_VIEW_NAME, mandatory = false)
    /** The view where you extract source document information */
    protected String srcDocInfoViewName;

    @ConfigurationParameter(name = PARAM_SKIP_INDICATED_DOCUMENTS, mandatory = false)
    protected boolean skipIndicatedDocuments;

    public static Function<String, String> dirSegFunction;

    protected File outputDir;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        List<String> dirNameSegments = new ArrayList<String>();
        if (outputStepNumber != null) {
            dirNameSegments.add(String.format("%02d", outputStepNumber));
        }
        dirNameSegments.add(baseOutputDirName);

        String dirName = Joiner.on("_").join(dirNameSegments);
        outputDir = new File(parentOutputDirPath, dirName);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            logger.info("Writing documents to " + outputDir.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getAdditionalDirPath(String filename) {
        if (dirSegFunction != null) {
            return dirSegFunction.apply(filename);
        } else {
            throw new IllegalArgumentException("Directory segment function is not provided.");
        }
    }

    protected boolean checkSkipIndicator(JCas aJCas) {
        try {
            UimaMeta meta = JCasUtil.selectSingle(aJCas, UimaMeta.class);
            if (meta.getSkipOutput()) {
                return true;
            } else {
                return false;
            }
        } catch (IllegalArgumentException e) {
            // If the meta is not set, we won't skip anything.
            return false;
        }
    }

}
