package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/21/17
 * Time: 4:27 PM
 *
 * @author Zhengzhong Liu
 */
public class EntityOverlapAnalyzer extends AbstractLoggingAnnotator {
    int totalNonSingletonEntities = 0;
    int totalMentions = 0;
    int entitySizeSum = 0;

    int numSpotsInCluster = 0;
    int numExactSpotsInCluster = 0;
    int numHeadSpotsInCluster = 0;

    int numExactMatchSpots = 0;
    int numHeadSpots = 0;
    int numCoveredSpots = 0;

    int totalNumberSpots = 0;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Collection<Entity> entities = JCasUtil.select(aJCas, Entity.class);

        for (Entity entity : entities) {
            Collection<EntityMention> nonSingletonMentions = FSCollectionFactory.create(
                    entity.getEntityMentions(), EntityMention.class);

            for (EntityMention nonSingletonMention : nonSingletonMentions) {
                List<GroundedEntity> coveredSpots = JCasUtil.selectCovered(GroundedEntity.class, nonSingletonMention);
                numSpotsInCluster += coveredSpots.size();

                StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(nonSingletonMention);

                for (GroundedEntity coveredSpot : coveredSpots) {
                    if (match(coveredSpot, nonSingletonMention)) {
                        numExactSpotsInCluster++;
                    }

                    if (match(coveredSpot, head)) {
                        numHeadSpotsInCluster++;
                    }
                }

            }

            int size = nonSingletonMentions.size();
            if (size > 1) {
                totalNonSingletonEntities += 1;
                entitySizeSum += size;
            }
        }

        Collection<StanfordEntityMention> mentions = JCasUtil.select(aJCas, StanfordEntityMention.class);
        totalMentions += mentions.size();

        totalNumberSpots += JCasUtil.select(aJCas, GroundedEntity.class).size();

        for (StanfordEntityMention entityMention : mentions) {
            List<GroundedEntity> coveredSpots = JCasUtil.selectCovered(GroundedEntity.class, entityMention);
            for (GroundedEntity coveredSpot : coveredSpots) {
                if (match(entityMention, coveredSpot)) {
                    numExactMatchSpots++;
                }

                StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(entityMention);

                if (match(head, coveredSpot)) {
                    numHeadSpots++;
                }

            }

            numCoveredSpots += coveredSpots.size();
        }
    }

    private boolean match(ComponentAnnotation anno1, ComponentAnnotation anno2) {
        return (anno1.getBegin() == anno2.getBegin() && anno1.getEnd() == anno2.getEnd());
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info("=====================================");
        logger.info(String.format("Number of non singleton entities: %d", totalNonSingletonEntities));
        logger.info(String.format("Number of mentions: %d", totalMentions));
        logger.info(String.format("Number of mentions in clusters: %d", entitySizeSum));
        logger.info(String.format("Average cluster size: %.4f", 1.0 * entitySizeSum / totalNonSingletonEntities));

        logger.info("=====================================");
        logger.info(String.format("Total number of spots: %d", totalNumberSpots));

        logger.info("=====================================");
        logger.info(String.format("Number of spots (grounded entities) in clusters: %d (%.3f)", numSpotsInCluster, numSpotsInCluster * 1.0 / totalNumberSpots));
        logger.info(String.format("Number of spots match exactly with a mention in clusters: %d (%.3f)", numExactSpotsInCluster, numExactSpotsInCluster * 1.0 / totalNumberSpots));
        logger.info(String.format("Number of spots match the head of a mention in clusters: %d (%.3f)", numHeadSpotsInCluster, numHeadSpotsInCluster * 1.0 / totalNumberSpots));

        logger.info("=====================================");
        logger.info(String.format("Number of spots that are covered by some mentions: %d (%.3f)", numCoveredSpots, numCoveredSpots * 1.0 / totalNumberSpots));
        logger.info(String.format("Number of exact matched spots and mentions: %d (%.3f)", numExactMatchSpots, numExactMatchSpots * 1.0 / totalNumberSpots));
        logger.info(String.format("Number of spots that matches head of a mention: %d (%.3f)", numHeadSpots, numHeadSpots * 1.0 / totalNumberSpots));
    }
}
