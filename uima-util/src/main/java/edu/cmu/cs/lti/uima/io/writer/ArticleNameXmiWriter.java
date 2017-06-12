package edu.cmu.cs.lti.uima.io.writer;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * This analysis engine outputs CAS in the XMI format, however, it name the files using the name in Article
 * annotation. That being said, it is necessary to have Article annotation in the JCas for this to work.
 *
 * @author Zhengzhong Liu
 */
public class ArticleNameXmiWriter extends AbstractStepBasedDirWriter {
    private static final String DEFAULT_FILE_SUFFIX = ".xmi";

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String articleName = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        if (StringUtils.isEmpty(outputFileSuffix)) {
            outputFileSuffix = DEFAULT_FILE_SUFFIX;
        }

        String outputFileName = articleName + outputFileSuffix;

        File outputFile = new File(outputDir, outputFileName);

        // Serialize XCAS and write to output file.
        try {
            CasSerialization.writeAsXmi(aJCas.getCas(), outputFile);
        } catch (IOException | SAXException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
