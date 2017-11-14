package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.uima.util.CasSerialization;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This analysis engine outputs gzipped CAS in the XMI format.
 *
 * @author Zhengzhong Liu
 */
public class StepBasedDirGzippedXmiWriter extends AbstractStepBasedDirWriter {
    public static final String PARAM_OUTPUT_FILE_NUMBERS = "OutputFileNumbers";

    private static final String DEFAULT_FILE_SUFFIX = ".xmi.gz";

    /**
     * This is a list of documents that you want to generate XMI output. If it is
     * null or empty, the writer works against all input.
     */
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE_NUMBERS, mandatory = false)
    private List<String> outputDocumentNumberList;

    private AtomicInteger docCounter;

    private AtomicInteger skippedDocument;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        docCounter = new AtomicInteger(0);
        skippedDocument = new AtomicInteger(0);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (!CollectionUtils.isEmpty(outputDocumentNumberList)) {
            if (!outputDocumentNumberList.contains(Integer.toString(docCounter.get()))) {
                return;
            }
        }

        if (StringUtils.isEmpty(outputFileSuffix)) {
            outputFileSuffix = DEFAULT_FILE_SUFFIX;
        }

        String outputFileName = CasSerialization.getOutputFileNameFromSource(aJCas, outputFileSuffix);

        File outputFile;
        if (outputFileName == null) {
            outputFile = new File(outputDir, "doc" + (docCounter.get()) + DEFAULT_FILE_SUFFIX);
        } else {
            if (dirSegFunction != null) {
                String articleName = UimaConvenience.getArticleName(aJCas);
                if (articleName != null) {
                    outputFileName = getAdditionalDirPath(articleName) + outputFileName;
                }
            }
            outputFile = new File(outputDir, outputFileName);
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
        }

        docCounter.incrementAndGet();

        // serialize XCAS and write to output file
        try {
            if (skipIndicatedDocuments && checkSkipIndicator(aJCas)) {
                // Do not write skipped documents.
                skippedDocument.incrementAndGet();
                return;
            }
            CasSerialization.writeAsGzip(aJCas.getCas(), outputFile);
        } catch (IOException | SAXException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info("Number of documents skipped: " + skippedDocument.get());
    }
}
