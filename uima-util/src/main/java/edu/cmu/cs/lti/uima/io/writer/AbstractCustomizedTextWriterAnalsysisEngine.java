package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide an funciton to implemented, which allow customizing what text to be printed out
 *
 * @author Zhengzhong Liu, Hector
 */
public abstract class AbstractCustomizedTextWriterAnalsysisEngine extends AbstractLoggingAnnotator {

    public static final String PARAM_PARENT_OUTPUT_DIR = "ParentOutputDirectory";

    public static final String PARAM_BASE_OUTPUT_DIR_NAME = "BaseOutputDirectoryName";

    public static final String PARAM_STEP_NUMBER = "StepNumber";

    public static final String PARAM_OUTPUT_FILE_SUFFIX = "OutputFileSuffix";

    public static final String PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME = "sourceDocumentViewName";

    @ConfigurationParameter(name = PARAM_PARENT_OUTPUT_DIR, mandatory = true)
    private String parentOutputDir;

    @ConfigurationParameter(name = PARAM_BASE_OUTPUT_DIR_NAME, mandatory = true)
    private String baseOutputDirName;

    @ConfigurationParameter(name = PARAM_STEP_NUMBER, mandatory = true)
    private Integer stepNumber;

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE_SUFFIX, mandatory = false)
    private String outputFileSuffix;

    @ConfigurationParameter(name = PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME, mandatory = false, description = "The view name that contains source document information")
    private String sourceDocumentViewName;

    private File outputDir;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        try {
            super.initialize(context);
        } catch (ResourceInitializationException e) {
            throw new ResourceInitializationException(e);
        }

        List<Object> partOfDirNames = new ArrayList<Object>();
        if (stepNumber != null) {
            String stepNumberStr = Integer.toString(stepNumber);
            partOfDirNames.add(StringUtils.leftPad(stepNumberStr, 2, '0'));
        }
        partOfDirNames.add(baseOutputDirName);

        outputDir = new File(parentOutputDir + File.separator + StringUtils.join(partOfDirNames, "_"));
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        subinitialize(context);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas sourceDocumentView = null;
        if (sourceDocumentViewName != null) {
            try {

                sourceDocumentView = aJCas.getView(sourceDocumentViewName);
            } catch (CASException e) {
                throw new AnalysisEngineProcessException(e);
            }
        } else {
            sourceDocumentView = aJCas;
        }

        String text = getTextToPrint(aJCas);

        if (text != null) {
            // Retrieve the filename of the input file from the CAS.
            File outFile = null;

            SourceDocumentInformation fileLoc = JCasUtil.selectSingle(sourceDocumentView,
                    SourceDocumentInformation.class);
            File inFile;
            try {
                inFile = new File(new URL(fileLoc.getUri()).getPath());
                String outFileName = inFile.getName();
                if (fileLoc.getOffsetInSource() > 0) {
                    outFileName += ("_" + fileLoc.getOffsetInSource());
                }
                if (outputFileSuffix != null && outputFileSuffix.length() > 0) {
                    outFileName += outputFileSuffix;
                } else {
                    String defaultOutputFileSuffix = ".txt";
                    if (!outFileName.endsWith(defaultOutputFileSuffix)) {
                        outFileName += defaultOutputFileSuffix;
                    }
                }
                outFile = new File(outputDir, outFileName);

                FileUtils.write(outFile, text);
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }

    public abstract String getTextToPrint(JCas aJCas);

    /**
     * Subclass can implement this to get more things to done
     *
     * @param context
     */
    public void subinitialize(UimaContext context) {

    }

}