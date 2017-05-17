package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/17/17
 * Time: 5:33 PM
 *
 * @author Zhengzhong Liu
 */
public class KBPArgumentOutputAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_KBP_ARGUMENT_RESULTS = "kbpArgumentResults";
    @ConfigurationParameter(name = PARAM_KBP_ARGUMENT_RESULTS)
    private String kbpArgumentResultFolder;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            loadResults();
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

    }

    private void loadResults() throws IOException {
        for (File nuggetFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "nuggets"), null, false)) {
            for (String line : FileUtils.readLines(nuggetFile)) {

            }
        }

        for (File linkingFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "linking"), null, false)) {
            for (String line : FileUtils.readLines(linkingFile)) {

            }
        }

        for (File argumentFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "arguments"), null, false)) {
            for (String line : FileUtils.readLines(argumentFile)) {

            }
        }
    }
}
