package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.GroundedEntity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.JCasUtil;

import java.util.*;

/**
 * Created by hunte on 5/17/2017.
 */
public class CoverageReporter extends AbstractLoggingAnnotator {
    public static final String PARAM_REPORT_NAME = "ReportName";

    @ConfigurationParameter(name = PARAM_REPORT_NAME)
    private String reportname;

    Class<Annotation>[] reportingClasses;

    Map<String, TObjectIntMap<String>> corpusPosCoveredByClass;
    TObjectIntMap<String> corpusTokenCoveredByClass;

    TObjectIntMap<String> corpusPosCoveredOverall;
    double corpusTokenCoveredOverall;

    TObjectIntMap<String> posCounts;

    int numTokens;
    int numDocs;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        reportingClasses = new Class[]{EventMention.class, EntityMention.class, GroundedEntity.class};

        corpusPosCoveredByClass = new HashMap<>();
        corpusTokenCoveredByClass = new TObjectIntHashMap<>();

        for (Class<Annotation> reportingClass : reportingClasses) {
            corpusPosCoveredByClass.put(reportingClass.getSimpleName(), new TObjectIntHashMap<>());
            corpusTokenCoveredByClass.put(reportingClass.getSimpleName(), 0);
        }

        corpusPosCoveredOverall = new TObjectIntHashMap<>();

        corpusTokenCoveredOverall = 0;

        posCounts = new TObjectIntHashMap<>();

        numDocs = 0;
        numTokens = 0;
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        Collection<StanfordCorenlpToken> allTokens = JCasUtil.select(jCas, StanfordCorenlpToken.class);
        countTokens(allTokens, posCounts);
        numTokens += allTokens.size();

        Set<StanfordCorenlpToken> combinedCoveredTokens = new HashSet<>();

        for (Class clazz : reportingClasses) {
            Set<StanfordCorenlpToken> coveredTokens = checkClassCoverage(jCas, clazz);
            combinedCoveredTokens.addAll(coveredTokens);
            countTokens(coveredTokens, corpusPosCoveredByClass.get(clazz.getSimpleName()));
            corpusTokenCoveredByClass.adjustValue(clazz.getSimpleName(), coveredTokens.size());
        }

        corpusTokenCoveredOverall += combinedCoveredTokens.size();

        countTokens(combinedCoveredTokens, corpusPosCoveredOverall);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        TObjectDoubleMap<String> posCoveragesOverall = calculatePosCoverage(posCounts, corpusPosCoveredOverall);
        double tokenCoverageOverall = 1.0 * corpusTokenCoveredOverall / numTokens;

        logger.info(String.format("==================Report %s==================", reportname));
        logger.info(String.format("Overall token coverage is %.2f.", tokenCoverageOverall));
        posCoveragesOverall.forEachEntry((pos, v) -> {
            if (v > 0) {
                logger.info(String.format("POS %s coverage is %.2f.", pos, v));
            }
            return true;
        });

        for (Class clazz : reportingClasses) {
            logger.info(String.format("=================Class %s===================", clazz));
            TObjectDoubleMap<String> posCoverageForClass = calculatePosCoverage(posCounts,
                    corpusPosCoveredByClass.get(clazz.getSimpleName()));
            double tokenCoverageForClass = 1.0 * corpusTokenCoveredByClass.get(clazz.getSimpleName());
            logger.info(String.format("Overall token coverage for class %s is %.2f.", clazz.getSimpleName(),
                    tokenCoverageForClass));
            posCoverageForClass.forEachEntry((pos, v) -> {
                if (v > 0) {
                    logger.info(String.format("POS %s coverage for class %s is %.2f.", pos, clazz.getSimpleName(), v));
                }
                return true;
            });
        }
        logger.info("=========================================");

    }

    private TObjectDoubleMap<String> calculatePosCoverage(TObjectIntMap<String> posCounts,
                                                          TObjectIntMap<String> coveredPosCounts) {
        TObjectDoubleMap<String> posCoverage = new TObjectDoubleHashMap<>();
        posCounts.forEachEntry((pos, count) -> {
            int numCovered = coveredPosCounts.get(pos);
            posCoverage.put(pos, 1.0 * numCovered / count);
            return true;
        });

        return posCoverage;
    }

    private void countTokens(Collection<StanfordCorenlpToken> tokens, TObjectIntMap<String> counter) {
        for (StanfordCorenlpToken token : tokens) {
            counter.adjustOrPutValue(getPos(token), 1, 1);
        }
    }

    private String getPos(StanfordCorenlpToken token) {
        String pos = token.getPos();
        return pos.length() > 2 ? token.getPos().substring(0, 2) : pos;
    }

    private Set<StanfordCorenlpToken> checkClassCoverage(JCas aJCas, Class<Annotation> clazz) {
        int totalTokens = JCasUtil.select(aJCas, StanfordCorenlpToken.class).size();

        Set<StanfordCorenlpToken> coveredTokens = new HashSet<>();
        for (Annotation anno : JCasUtil.select(aJCas, clazz)) {
            coveredTokens.addAll(JCasUtil.selectCovered(StanfordCorenlpToken.class, anno));
        }

        return coveredTokens;
    }
}
