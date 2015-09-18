package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/13/15
 * Time: 11:36 PM
 *
 * @author Zhengzhong Liu
 */
public class WordNetBasedEntityAnnotator extends AbstractLoggingAnnotator {

    public static final String PARAM_JOB_TITLE_LIST = "JobTitleList";

    public static final String PARAM_WN_PATH = "WordNetPath";

    @ConfigurationParameter(name = PARAM_JOB_TITLE_LIST)
    private File jobTitleFile;

    @ConfigurationParameter(name = PARAM_WN_PATH)
    private String wnDictPath;

    private WordNetSearcher wns;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            wns = new WordNetSearcher(wnDictPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCasUtil.select(aJCas, StanfordCorenlpToken.class).stream().forEach(token -> {
            if (token.getPos().startsWith("N")) {
                if (isOfType(token.getLemma(), "worker", "leader")) {
                    JobTitle jobTitle = new JobTitle(aJCas);
                    UimaAnnotationUtils.finishAnnotation(jobTitle, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                } else if (isOfType(token.getLemma(), "body_part")) {
                    BodyPart bodyPart = new BodyPart(aJCas);
                    UimaAnnotationUtils.finishAnnotation(bodyPart, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                } else if (isOfType(token.getLemma(), "monetary_system")) {
                    Monetary monetary = new Monetary(aJCas);
                    UimaAnnotationUtils.finishAnnotation(monetary, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                } else if (isOfType(token.getLemma(), "possession")) {
                    Possession possession = new Possession(aJCas);
                    UimaAnnotationUtils.finishAnnotation(possession, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                } else if (isOfType(token.getLemma(), "government")) {
                    Government government = new Government(aJCas);
                    UimaAnnotationUtils.finishAnnotation(government, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                }
            }
        });
    }

    private boolean isOfType(String lemma, String... targetTypes) {
        for (Set<String> hypernyms : wns.getAllNounHypernymsForAllSense(lemma.toLowerCase())) {
            for (String type : targetTypes) {
                if (hypernyms.contains(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}