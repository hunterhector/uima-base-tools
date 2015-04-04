package edu.cmu.cs.lti.annotator;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.FSCollectionFactory;
import org.uimafit.util.JCasUtil;

import java.util.*;
import java.util.Map.Entry;

//import edu.cmu.lti.util.uima.FSCollectionFactory;

/**
 * This class uses the Stanford Corenlp Pipeline to annotate POS, NER, Parsing
 * and entity coreference
 * <p/>
 * The current issue is that it does not split on DATETIME and TITLE correctly.
 *
 * @author Zhengzhong Liu, Hector
 * @author Jun Araki
 * @author Da Teng
 */
public class StanfordCoreNlpAnnotator extends AbstractLoggingAnnotator {
    public final static String PARAM_USE_SUTIME = "UseSUTime";

    private final static String ANNOTATOR_COMPONENT_ID = "System-stanford-corenlp";

    private StanfordCoreNLP pipeline;

    private final static String PARSE_TREE_ROOT_NODE_LABEL = "ROOT";

    SemanticHeadFinder hf = new SemanticHeadFinder();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        Boolean useSUTime = (Boolean) aContext.getConfigParameterValue(PARAM_USE_SUTIME);

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("dcoref.postprocessing", "true");
        if (useSUTime) {
            props.setProperty("ner.useSUTime", "true");
        } else {
            props.setProperty("ner.useSUTime", "false");
        }

        pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        annotateText(aJCas.getDocumentText(), aJCas, 0);

        for (JCas view : getAdditionalViews(aJCas)) {
            annotateText(view.getDocumentText(), view, 0);
        }
    }

    private void annotateText(String text, JCas aJCas, int textOffset) {
        UimaConvenience.printProcessLog(aJCas, logger);
        Annotation document = new Annotation(text);
        logger.info("Parsing ...");
        pipeline.annotate(document);
        logger.info("Parsing done");

        // The following put token annotation to CAS
        int tokenId = 0;
        String preNe = "";
        int neBegin = 0;
        int neEnd = 0;

        Map<Span, StanfordEntityMention> spanMentionMap = new HashMap<Span, StanfordEntityMention>();

        List<EntityMention> allMentions = new ArrayList<>();
        for (CoreLabel token : document.get(TokensAnnotation.class)) {
            int beginIndex = token.get(CharacterOffsetBeginAnnotation.class);
            int endIndex = token.get(CharacterOffsetEndAnnotation.class);

            // add token annotation
            StanfordCorenlpToken sToken = new StanfordCorenlpToken(aJCas, beginIndex + textOffset, endIndex + textOffset);
            sToken.setPos(token.get(PartOfSpeechAnnotation.class));
            sToken.setLemma(token.get(LemmaAnnotation.class));
            UimaAnnotationUtils.finishAnnotation(sToken, ANNOTATOR_COMPONENT_ID, tokenId++, aJCas);

            // Add NER annotation
            String ne = token.get(NamedEntityTagAnnotation.class);
            if (ne != null) {
                // System.out.println("[" + token.originalText() + "] :" + ne);
                if (ne.equals(preNe) && !preNe.equals("")) {
                } else if (preNe.equals("")) {
                    // if the previous is start of sentence(no label).
                    neBegin = beginIndex;
                    preNe = ne;
                } else {
                    if (!preNe.equals("O")) {// "O" represent no label (other)
                        StanfordEntityMention sne = new StanfordEntityMention(aJCas);
                        sne.setBegin(neBegin + textOffset);
                        sne.setEnd(neEnd + textOffset);
                        sne.setEntityType(preNe);
                        allMentions.add(sne);
                        spanMentionMap.put(new Span(sne.getBegin(), sne.getEnd()), sne);
                    }
                    // set the next span of NE
                    neBegin = beginIndex;
                    preNe = ne;
                }
                neEnd = endIndex;
            }
        }

        List<CoreMap> sentAnnos = document.get(SentencesAnnotation.class);
        int sentenceId = 0;

        for (CoreMap sentAnno : sentAnnos) {
            // The following add Stanford sentence to CAS
            int begin = sentAnno.get(CharacterOffsetBeginAnnotation.class);
            int end = sentAnno.get(CharacterOffsetEndAnnotation.class);
            StanfordCorenlpSentence sSent = new StanfordCorenlpSentence(aJCas, begin + textOffset, end + textOffset);
            UimaAnnotationUtils.finishAnnotation(sSent, ANNOTATOR_COMPONENT_ID, sentenceId, aJCas);
            sentenceId++;

            // The following deals with tree annotation
            Tree tree = sentAnno.get(TreeAnnotation.class);

            addPennTreeAnnotation(aJCas, tree, null, null, textOffset, hf);

            // the following add the collapsed cc processed dependencies of each
            // sentence into CAS
            // annotation
            SemanticGraph depends = sentAnno.get(CollapsedCCProcessedDependenciesAnnotation.class);
            // SemanticGraph basicDepends =
            // sentAnno.get(BasicDependenciesAnnotation.class);

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, sSent);
            Map<IndexedWord, StanfordCorenlpToken> stanford2UimaMap = new HashMap<IndexedWord, StanfordCorenlpToken>();
            for (IndexedWord stanfordNode : depends.vertexSet()) {
                int indexBegin = stanfordNode.get(BeginIndexAnnotation.class);
                int indexEnd = stanfordNode.get(EndIndexAnnotation.class);
                if (indexBegin + 1 != indexEnd) {
                    System.err.print("Dependency node is not exactly one token here!");
                }

                StanfordCorenlpToken sToken = tokens.get(indexBegin);
                if (depends.getRoots().contains(stanfordNode)) {
                    sToken.setIsDependencyRoot(true);
                }
                stanford2UimaMap.put(stanfordNode, sToken);
            }

            ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> headRelationMap = ArrayListMultimap.create();
            ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> childRelationMap = ArrayListMultimap.create();

            for (SemanticGraphEdge stanfordEdge : depends.edgeIterable()) {
                String edgeType = stanfordEdge.getRelation().toString();
                double edgeWeight = stanfordEdge.getWeight(); // weight is
                // usually
                // infinity
                StanfordCorenlpToken head = stanford2UimaMap.get(stanfordEdge.getGovernor());
                StanfordCorenlpToken child = stanford2UimaMap.get(stanfordEdge.getDependent());

                StanfordDependencyRelation sr = new StanfordDependencyRelation(aJCas);
                sr.setHead(head);
                sr.setChild(child);
                sr.setWeight(edgeWeight);
                sr.setDependencyType(edgeType);
                UimaAnnotationUtils.finishTop(sr, ANNOTATOR_COMPONENT_ID, 0, aJCas);

                headRelationMap.put(child, sr);
                childRelationMap.put(head, sr);
            }

            // associate the edges to the nodes
            for (StanfordCorenlpToken sNode : stanford2UimaMap.values()) {
                if (headRelationMap.containsKey(sNode)) {
                    sNode.setHeadDependencyRelations(FSCollectionFactory.createFSList(aJCas, headRelationMap.get(sNode)));
                }
                if (childRelationMap.containsKey(sNode)) {
                    sNode.setChildDependencyRelations(FSCollectionFactory.createFSList(aJCas, childRelationMap.get(sNode)));
                }
            }
        }

        // the following set the coreference chain to CAS annotation
        Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);

        List<List<StanfordCorenlpToken>> sentTokens = new ArrayList<List<StanfordCorenlpToken>>();

        for (StanfordCorenlpSentence sSent : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentTokens.add(JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, sSent));
        }

        for (Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain refChain = entry.getValue();
            Entity entity = new Entity(aJCas);
            UimaAnnotationUtils.finishTop(entity, ANNOTATOR_COMPONENT_ID, 0, aJCas);

            List<StanfordEntityMention> stanfordEntityMentions = new ArrayList<StanfordEntityMention>();

            CorefMention representativeMention = refChain.getRepresentativeMention();

            for (CorefMention mention : refChain.getMentionsInTextualOrder()) {
                try {
                    List<StanfordCorenlpToken> sTokens = sentTokens.get(mention.sentNum - 1);

                    int begin = sTokens.get(mention.startIndex - 1).getBegin();
                    int end = sTokens.get(mention.endIndex - 2).getEnd();

                    StanfordEntityMention em;
                    if (spanMentionMap.containsKey(new Span(begin, end))) {
//                        System.out.println("This mention is contained ");
                        em = spanMentionMap.get(new Span(begin, end));
                    } else {
                        em = new StanfordEntityMention(aJCas, begin, end);
                        allMentions.add(em);
                    }

                    StanfordCorenlpToken mentionHead = sTokens.get(mention.headIndex - 1);

//                    System.out.println("\t(" + mention.sentNum + "," +
//                            mention.headIndex + ",[" +
//                            mention.startIndex + "," +
//                            mention.endIndex + "]) " + em.getCoveredText());

                    em.setReferingEntity(entity);
                    em.setHead(mentionHead);
                    stanfordEntityMentions.add(em);

                    if (representativeMention.equals(mention)) {
                        entity.setRepresentativeMention(em);
                    }
                } catch (Exception e) {
                    System.err.println("Cannot find the correct range for mention " + mention.mentionSpan + " " + mention.sentNum + " "
                            + mention.startIndex + " " + mention.endIndex);
                }
            }

            // convert the list to CAS entity mention FSList type
            FSArray mentionFSList = UimaConvenience.makeFsArray(stanfordEntityMentions, aJCas);
            // Put that in the cluster type
            entity.setEntityMentions(mentionFSList);
        }

        //sort and assign id to mentions
        Collections.sort(allMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention m1, EntityMention m2) {
                return m1.getBegin() - m2.getBegin();
            }
        });

        int mentionIdx = 0;
//        System.out.println("Number of mentions "+allMentions.size());
        for (EntityMention mention : allMentions) {
            UimaAnnotationUtils.finishAnnotation(mention, ANNOTATOR_COMPONENT_ID, mentionIdx++, aJCas);
            if (mention.getReferingEntity() == null) {
                //add singleton entities
                Entity entity = new Entity(aJCas);
                entity.setEntityMentions(new FSArray(aJCas, 1));
                entity.setEntityMentions(0, mention);
                mention.setReferingEntity(entity);
                entity.setRepresentativeMention(mention);
            }

            if (mention.getHead() == null) {
                mention.setHead(UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, mention));
            }
        }
    }

    public static StanfordTreeAnnotation addPennTreeAnnotation(JCas aJCas, Tree currentNode, Tree parentNode,
                                                               StanfordTreeAnnotation parent, int textOffset, HeadFinder hf) {
        StanfordTreeAnnotation treeAnno = new StanfordTreeAnnotation(aJCas);
        Tree headLeaf = currentNode.headTerminal(hf, parentNode);

        if (headLeaf != null) {
            List<edu.stanford.nlp.ling.Word> words = headLeaf.yieldWords();
            int leafBegin = words.get(0).beginPosition() + textOffset;
            int leafEnd = words.get(words.size() - 1).endPosition() + textOffset;

            List<StanfordCorenlpToken> leafNodes = org.apache.uima.fit.util.JCasUtil.selectCovered(aJCas,
                    StanfordCorenlpToken.class, leafBegin, leafEnd);

            if (leafNodes.size() != 1) {
                System.out.println("Incorrect leave span " + leafBegin + " " + leafEnd);
            } else {
                treeAnno.setHead(leafNodes.get(0));
            }

        }

        if (!currentNode.isLeaf()) {
            int thisBegin = 0;
            int thisEnd = 0;
            int count = 0;
            int numChild = currentNode.children().length;
            String currentNodeLabel = currentNode.value();

            List<StanfordTreeAnnotation> childrenList = new ArrayList<StanfordTreeAnnotation>();
            for (Tree child : currentNode.children()) {
                StanfordTreeAnnotation childTree = addPennTreeAnnotation(aJCas, child, currentNode,
                        treeAnno, textOffset, hf);
                childrenList.add(childTree);
                if (count == 0) {
                    thisBegin = childTree.getBegin();
                }
                if (count == numChild - 1) {
                    thisEnd = childTree.getEnd();
                }
                count++;
            }

            if (PARSE_TREE_ROOT_NODE_LABEL.equals(currentNodeLabel)) {
                treeAnno.setIsRoot(true);
            } else {
                treeAnno.setIsRoot(false);
            }

            treeAnno.setBegin(thisBegin + textOffset);
            treeAnno.setEnd(thisEnd + textOffset);
            treeAnno.setPennTreeLabel(currentNodeLabel);
            treeAnno.setParent(parent);
            treeAnno.setChildren(UimaConvenience.makeFsArray(childrenList, aJCas));
            treeAnno.setIsLeaf(false);
            UimaAnnotationUtils.finishAnnotation(treeAnno, ANNOTATOR_COMPONENT_ID, 0, aJCas);
            return treeAnno;
        } else {
            ArrayList<edu.stanford.nlp.ling.Word> words = currentNode.yieldWords();
            StanfordTreeAnnotation leafTree = new StanfordTreeAnnotation(aJCas);
            leafTree.setBegin(words.get(0).beginPosition() + textOffset);
            leafTree.setEnd(words.get(words.size() - 1).endPosition() + textOffset);
            leafTree.setPennTreeLabel(currentNode.value());
            leafTree.setIsLeaf(true);
            UimaAnnotationUtils.finishAnnotation(leafTree, ANNOTATOR_COMPONENT_ID, 0, aJCas);
            return leafTree;
        }
    }

}