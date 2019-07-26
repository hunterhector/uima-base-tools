package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.EntityMentionManager;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The support subject may not always help though.
 * <p>
 * Created with IntelliJ IDEA.
 * Date: 2019-06-24
 * Time: 01:14
 *
 * @author Zhengzhong Liu
 */
public class SupportArgumentFinder extends AbstractLoggingAnnotator {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        EntityMentionManager manager = new EntityMentionManager(jCas);

        for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
            Word supportHead = null;
            if (eventMention.getHeadWord() != null) {
                if (eventMention.getHeadWord().getPos().startsWith("V")) {
                    supportHead = findXcompArgs(eventMention.getHeadWord());
                } else if (eventMention.getHeadWord().getPos().startsWith("N")) {
                    supportHead = findNounSupport(eventMention.getHeadWord());
                }
            }

            if (supportHead != null) {
                addSupportHeadSubjs(jCas, eventMention, manager, supportHead);
            }
        }
    }

    private void addSupportHeadSubjs(JCas aJCas, EventMention eventMention, EntityMentionManager manager,
                                     Word supportHead) {
        FSList depChildrenFS = supportHead.getChildDependencyRelations();
        Map<EntityMention, EventMentionArgumentLink> existingArgs = UimaNlpUtils.indexArgs(eventMention);

        List<EventMentionArgumentLink> argumentLinks = new ArrayList<>(existingArgs.values());

        if (depChildrenFS != null) {
            for (StanfordDependencyRelation childDep : FSCollectionFactory.create(depChildrenFS,
                    StanfordDependencyRelation.class)) {
                String depType = childDep.getDependencyType();
                if (depType.equals("nsubj") || depType.contains("agent")) {
                    String inferredRole = "ARG0";
                    Word inferredSubj = childDep.getChild();

                    EventMentionArgumentLink argumentLink = UimaNlpUtils.addEventArgument(
                            aJCas, eventMention, manager, existingArgs, argumentLinks,
                            inferredSubj, COMPONENT_ID);
                    argumentLink.setPropbankRoleName(inferredRole);
                    logger.info(String.format("Adding arg0 for event %s from support %s: %s",
                            eventMention.getCoveredText(), supportHead.getCoveredText(), inferredSubj.getCoveredText()));
                }
            }
        }

        eventMention.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
    }

    private Word findXcompArgs(Word mentionHead) {
        FSList headDepsFS = mentionHead.getHeadDependencyRelations();
        if (headDepsFS != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(headDepsFS, StanfordDependencyRelation.class)) {
                if (dep.getDependencyType().equals("xcomp")) {
                    return dep.getHead();
                }
            }
        }
        return null;
    }

    private Word findNounSupport(Word mentionHead) {
        FSList headDepsFS = mentionHead.getHeadDependencyRelations();
        if (headDepsFS != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(headDepsFS, StanfordDependencyRelation.class)) {
                if (dep.getDependencyType().endsWith("obj")) {
                    return dep.getHead();
                }
            }
        }
        return null;
    }
}
