package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
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

    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            loadResults(aJCas);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void loadResults(JCas aJCas) throws IOException {
        loadNuggets(aJCas);
        loadArguments(aJCas);
        loadLinks(aJCas);
    }

    private void loadLinks(JCas aJCas) throws IOException {
        for (File linkingFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "linking"), null, false)) {
            for (String line : FileUtils.readLines(linkingFile)) {

            }
        }
    }

    private void loadArguments(JCas aJCas) throws IOException {
        for (File argumentFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "arguments"), null, false)) {
            for (String line : FileUtils.readLines(argumentFile)) {
                String[] fields = line.split("\t");
                if (fields.length >= 11) {
                    Span span = asSpan(fields[5], "-");
                    String role = fields[3];
                    EntityMention argumentMention = new EntityMention(aJCas, span.getBegin(), span.getEnd());
                    argumentMention.addToIndexes();
                }
            }
        }
    }

    private void loadNuggets(JCas aJCas) throws IOException {
        for (File nuggetFile : FileUtils.listFiles(new File(kbpArgumentResultFolder, "nuggets"), null, false)) {
            for (String line : FileUtils.readLines(nuggetFile)) {
                String[] fields = line.split("\t");
                if (fields.length >= 7) {
                    Span span = asSpan(fields[3], ",");
                    String t = fields[5];
                    String realis = fields[6];
                    EventMention mention = new EventMention(aJCas, span.getBegin(), span.getEnd());
                    mention.setEventType(t);
                    mention.setRealisType(realis);
                    mention.addToIndexes(aJCas);
                }
            }
        }
    }

    private Span asSpan(String spanStr, String splitter) {
        String[] parts = spanStr.split(splitter);
        return new Span(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
