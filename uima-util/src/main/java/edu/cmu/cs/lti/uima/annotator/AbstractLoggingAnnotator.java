package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/7/14
 * Time: 1:45 PM
 */
public abstract class AbstractLoggingAnnotator extends JCasAnnotator_ImplBase{
    public static final String PARAM_KEEP_QUIET = "kee_quiet";

    private String className = this.getClass().getName();

    protected Logger logger = Logger.getLogger(className);

    @ConfigurationParameter(name = PARAM_KEEP_QUIET, mandatory = false)
    private Boolean keepQuiet;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        keepQuiet = (Boolean) aContext.getConfigParameterValue(PARAM_KEEP_QUIET);
        //default should not be quiet
        keepQuiet = keepQuiet == null ? false : keepQuiet;

        if (keepQuiet) {
            logger.setLevel(Level.SEVERE);
        }else{
            logger.setLevel(Level.INFO);
        }
    }

    protected String progressInfo(JCas aJCas){
        return "Processing " + UimaConvenience.getShortDocumentNameWithOffset(aJCas);
    }
}