package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/7/14
 * Time: 1:45 PM
 */
public abstract class AbstractLoggingAnnotator extends JCasAnnotator_ImplBase {
    public static final String PARAM_KEEP_QUIET = "keepQuiet";

    public static final String PARAM_LOGGING_OUT_FILE = "loggingOutFile";

    public static final String PARAM_ADDITIONAL_VIEWS = "targetViewNames";

    private String className = this.getClass().getName();

    protected Logger logger = Logger.getLogger(className);

    @ConfigurationParameter(name = PARAM_KEEP_QUIET, mandatory = false)
    private Boolean keepQuiet;

    @ConfigurationParameter(name = PARAM_LOGGING_OUT_FILE, mandatory = false)
    private String loggingFileName;

    @ConfigurationParameter(name = PARAM_ADDITIONAL_VIEWS, mandatory = false)
    private String[] targetViews;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

//        keepQuiet = (Boolean) aContext.getConfigParameterValue(PARAM_KEEP_QUIET);
//
//        loggingFileName = (String) aContext.getConfigParameterValue(PARAM_LOGGING_OUT_FILE);

        //default should not be quiet
        keepQuiet = keepQuiet == null ? false : keepQuiet;

        if (keepQuiet) {
            logger.setLevel(Level.SEVERE);
        } else {
            logger.setLevel(Level.INFO);
        }

        if (loggingFileName != null) {
            try {
                logger.addHandler(new FileHandler(loggingFileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected JCas[] getAdditionalViews(JCas mainView) {
        if (targetViews == null) {
            return new JCas[0];
        }

        JCas[] additionalViews = new JCas[targetViews.length];
        for (String viewName : targetViews) {
            UimaConvenience.getView(mainView, viewName);
        }
        return additionalViews;
    }

    protected String progressInfo(JCas aJCas) {
        return "Processing " + UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    }
}