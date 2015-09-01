package edu.cmu.cs.lti.uima.annotator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 10:27 AM
 *
 * @author Zhengzhong Liu
 */
public class CrossValidationGenerator extends AbstractLoggingAnnotator {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String PARAM_CORPUS_NAME = "corpusName";

    public static final String PARAM_PARENT_DIR = "parentDir";

    @ConfigurationParameter(name = PARAM_PARENT_DIR)
    private String parentDir;

    @ConfigurationParameter(name = PARAM_CORPUS_NAME)
    private String corpusName;

    private File store;
    private int docId;

    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException {
        super.initialize(context);

        store = new File(parentDir, "cv_" + corpusName);
        docId = 0;

        if (store.exists()) {
            throw new RuntimeException("corpus exists at " + store.getPath());
        } else {
            logger.info("creating Cross-Validation data in {}", store.getPath());
            store.mkdirs();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }
}
