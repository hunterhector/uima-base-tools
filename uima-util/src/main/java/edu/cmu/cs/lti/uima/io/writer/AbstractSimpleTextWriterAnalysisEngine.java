package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Provide an function to implemented, which allow customizing what text to be printed out
 *
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractSimpleTextWriterAnalysisEngine extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_PATH = "outputPath";

    @ConfigurationParameter(name = PARAM_OUTPUT_PATH, mandatory = true)
    private File baseOutputFile;

    public static final String PARAM_NEW_FILE_AFTER_N = "newFileAfterN";

    @ConfigurationParameter(name = PARAM_NEW_FILE_AFTER_N, defaultValue = "-1")
    private int newFileAfterN;

    public static final String PARAM_TARGET_VIEW_NAME = "targetViewName";

    @ConfigurationParameter(name = PARAM_TARGET_VIEW_NAME, defaultValue = CAS.NAME_DEFAULT_SOFA)
    private String targetViewName;

    private int count = 0;

    private File currentOutputFile;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        File parentDir = baseOutputFile.getAbsoluteFile().getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        updateCurrentOutputFile();

        try {
            FileUtils.write(currentOutputFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentOutputFile() {
        if (newFileAfterN > 0) {
            currentOutputFile = new File(baseOutputFile.getAbsolutePath() + "_" + (count / newFileAfterN));
        } else {
            currentOutputFile = baseOutputFile;
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        updateCurrentOutputFile();

        JCas targetView = JCasUtil.getView(aJCas, targetViewName, aJCas);

        String text = getTextToPrint(targetView);
        if (text != null) {
            try {
                FileUtils.write(currentOutputFile, text, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract String getTextToPrint(JCas aJCas);

    protected void incrementCount() {
        count++;
    }

    protected int getCount() {
        return count;
    }

    protected String getOutputPath() {
        return baseOutputFile.getAbsolutePath();
    }
}