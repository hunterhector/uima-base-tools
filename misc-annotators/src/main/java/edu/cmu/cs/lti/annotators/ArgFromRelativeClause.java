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
 * Created with IntelliJ IDEA. This runs after VerbBasedEventDetector to find subjects that are from relative clauses.
 * Date: 2019-06-23
 * Time: 15:38
 *
 * @author Zhengzhong Liu
 */
public class ArgFromRelativeClause extends AbstractLoggingAnnotator {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        Map<Word, EntityMention> h2Entities = UimaNlpUtils.indexEntityMentions(jCas);

        EntityMentionManager manager = new EntityMentionManager(jCas);

        for (EventMention eventMention : JCasUtil.select(jCas, EventMention.class)) {
            if (eventMention.getHeadWord() != null && eventMention.getHeadWord().getPos().startsWith("V")) {
                addRelativeClauseHead(jCas, eventMention, manager);
            }
        }
    }

    private void addRelativeClauseHead(JCas jCas, EventMention eventMention, EntityMentionManager manager) {
        StanfordCorenlpToken token = (StanfordCorenlpToken) eventMention.getHeadWord();
        Map<EntityMention, EventMentionArgumentLink> existingArgs = UimaNlpUtils.indexArgs(eventMention);

        List<EventMentionArgumentLink> argumentLinks = new ArrayList<>(existingArgs.values());

        FSList depChildrenFS = token.getChildDependencyRelations();
        FSList depParentFS = token.getHeadDependencyRelations();

        if (depChildrenFS != null && depParentFS != null) {
            Word aclHead = null;
            for (StanfordDependencyRelation headDep : FSCollectionFactory.create(depParentFS,
                    StanfordDependencyRelation.class)) {
                if (headDep.getDependencyType().equals("acl:relcl")) {
                    aclHead = headDep.getHead();
                }
            }

            if (aclHead == null) return;

            for (StanfordDependencyRelation childDep : FSCollectionFactory.create(depChildrenFS,
                    StanfordDependencyRelation.class)) {
                Word child = childDep.getChild();
                if (child.getPos().startsWith("W")) {
                    String inferredRole = depAsRole(childDep.getDependencyType());

                    if (inferredRole != null) {
                        // We use the wh-word's dependency and the relative head to create a new arg.
                        EventMentionArgumentLink argumentLink = UimaNlpUtils.addEventArgument(
                                jCas, eventMention, manager, existingArgs, argumentLinks,
                                aclHead, COMPONENT_ID);
                        argumentLink.setDependency("acl:relcl");
                        argumentLink.setPropbankRoleName(inferredRole);
                        argumentLinks.add(argumentLink);
                    }
                }
            }

            eventMention.setArguments(FSCollectionFactory.createFSList(jCas, argumentLinks));
        }
    }

    private String depAsRole(String depType) {
        if (depType.equals("nsubj") || depType.contains("agent")) {
            return "ARG0";
        } else if (depType.equals("dobj") || depType.equals("nsubjpass")) {
            return "ARG1";
        } else if (depType.equals("iobj")) {
            return "ARG2";
        } else if (depType.startsWith("prep_")) {
            return "ARG2";
        } else if (depType.startsWith("nmod:")) {
            if (depType.equals("nmod:tmod") || depType.equals("nmod:poss")) {
                return null;
            } else {
                return "ARG2";
            }
        }
        return null;
    }
}
