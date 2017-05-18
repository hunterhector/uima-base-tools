package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    Map<String, Map<String, String>> outputsByTypes = new HashMap<>();
    String[] outputTypes;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        outputTypes = new String[]{"linking", "arguments", "nuggets"};

        for (String outputType : outputTypes) {
            outputsByTypes.put(outputType, findFiles(outputType));
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String baseName = UimaConvenience.getDocId(aJCas).replace(".xml", "");

        try {
            loadNuggets(aJCas, baseName);
            loadArguments(aJCas, baseName);
            loadLinks(aJCas, baseName);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private Map<String, String> findFiles(String subfolder) {
        Map<String, String> files = new HashMap<>();
        for (File f : FileUtils.listFiles(new File(kbpArgumentResultFolder, subfolder), null, false)) {
            files.put(f.getName(), f.getAbsolutePath());
        }
        return files;
    }

    private void loadLinks(JCas aJCas, String baseName) throws IOException {

    }

    private void loadArguments(JCas aJCas, String basename) throws IOException {
        String fileName = outputsByTypes.get("arguments").get(basename);

        if (fileName == null){
            logger.warn(String.format("Argument %s file not found.", basename));
            return;
        }

        File argumentFile = new File(fileName);
        for (String line : FileUtils.readLines(argumentFile)) {
            String[] fields = line.split("\t");
            if (fields.length >= 11) {
                Span span = asSpan(fields[5], "-");
                String role = fields[3];
                EntityMention argumentMention = new EntityMention(aJCas, span.getBegin(), span.getEnd() + 1);
                argumentMention.addToIndexes();
            }
        }
    }

    private void loadNuggets(JCas aJCas, String basename) throws IOException {
        String fileName = outputsByTypes.get("nuggets").get(basename);

        if (fileName == null){
            logger.warn(String.format("Nugget %s file not found.", basename));
            return;
        }

        File nuggetFile = new File(fileName);
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

    private Span asSpan(String spanStr, String splitter) {
        String[] parts = spanStr.split(splitter);
        return new Span(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
