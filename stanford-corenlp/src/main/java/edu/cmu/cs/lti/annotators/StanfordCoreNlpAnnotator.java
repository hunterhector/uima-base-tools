package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.ChineseCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.AbstractCollinsHeadFinder;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.international.pennchinese.ChineseSemanticHeadFinder;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

//import edu.stanford.nlp.pipeline.HybridCorefAnnotator;

/**
 * This class uses the Stanford Corenlp Pipeline to annotate POS, NER, Parsing
 * and entity coreference
 * <p>
 * The current issue is that it does not split on DATETIME and TITLE correctly.
 * <p>
 * We recently added Chinese support.
 *
 * @author Zhengzhong Liu, Hector
 * @author Jun Araki
 * @author Da Teng
 */
public class StanfordCoreNlpAnnotator extends AbstractLoggingAnnotator {
    public final static String PARAM_USE_SUTIME = "UseSUTime";
    @ConfigurationParameter(name = PARAM_USE_SUTIME, defaultValue = "true")
    private boolean useSUTime;

    public static final String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, defaultValue = "en")
    private String language;

    public static final String PARAM_SPLIT_ONLY = "splitOnly";
    @ConfigurationParameter(name = PARAM_SPLIT_ONLY, defaultValue = "false")
    private boolean splitOnly;

    public static final String PARAM_WHITESPACE_TOKENIZE = "whiteSpaceTokenize";
    @ConfigurationParameter(name = PARAM_WHITESPACE_TOKENIZE, defaultValue = "false")
    private boolean whiteSpaceTokenize;

    public static final String PARAM_PARSER_MAXLEN = "parserMaxLength";
    @ConfigurationParameter(name = PARAM_PARSER_MAXLEN, mandatory = false)
    private Integer parserMaxLen;

    public static final String PARAM_PARSER_MAX_COREF_DIST = "corefMaxLookback";
    @ConfigurationParameter(name = PARAM_PARSER_MAX_COREF_DIST, defaultValue = "-1")
    private Integer corefMaxLookback;

    public final static String PARAM_NUMERIC_CLASSIFIER = "numericClassifier";
    @ConfigurationParameter(name = PARAM_NUMERIC_CLASSIFIER, defaultValue = "true")
    private boolean useNumericClassifier;

    public final static String PARAM_SHIFT_REDUCE = "shiftReduceParse";
    @ConfigurationParameter(name = PARAM_SHIFT_REDUCE, defaultValue = "false")
    private boolean shiftReduceParser;

    public static final String PARAM_NUM_THREADS = "numThreads";
    @ConfigurationParameter(name = PARAM_NUM_THREADS, defaultValue = "1")
    private int numThreads;


    private StanfordCoreNLP pipeline;

    private final static String PARSE_TREE_ROOT_NODE_LABEL = "ROOT";

    private AbstractCollinsHeadFinder hf;

    private boolean explicitCorefCall = false;

    private Object hybridCorefAnnotator = null;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        logger.info("Language is " + language);

        if (language.equals("en")) {
            Properties props = new Properties();
            if (splitOnly) {
                props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
            } else {
                props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
                props.setProperty("dcoref.postprocessing", "true");
            }

            if (useSUTime) {
                props.setProperty("ner.useSUTime", "true");
            } else {
                props.setProperty("ner.useSUTime", "false");
            }

            if (useNumericClassifier) {
                props.setProperty("ner.applyNumericClassifiers", "true");
            } else {
                props.setProperty("ner.applyNumericClassifiers", "false");
            }

            if (whiteSpaceTokenize) {
                props.setProperty("tokenize.whitespace", "true");
            }

            if (shiftReduceParser) {
                props.setProperty("-parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
            }

            if (parserMaxLen != null) {
                logger.info("Parser will have a max length of " + parserMaxLen);
                props.setProperty("parse.maxlen", String.valueOf(parserMaxLen));
            }

            if (numThreads > 1) {
                logger.info("Setting multiple threads for StanfordCoreNLP: " + numThreads);
                props.setProperty("threads", String.valueOf(numThreads));
            }

            props.setProperty("dcoref.maxdist", String.valueOf(corefMaxLookback));

            hf = new SemanticHeadFinder();

            pipeline = new StanfordCoreNLP(props);
        } else if (language.equals("zh")) {

            String prop36Path = "edu/stanford/nlp/hcoref/properties/zh-coref-default.properties";
            String prop35Path = "edu/stanford/nlp/hcoref/properties/zh-dcoref-default.properties";

            URL zhProp36 = this.getClass().getClassLoader().getResource(prop36Path);
            URL zhProp35 = this.getClass().getClassLoader().getResource(prop35Path);

            String[] args;

            if (zhProp36 != null) {
                args = new String[]{"-props", prop36Path};
                logger.info("Using 3.6.0 style Chinese coreference.");
            } else if (zhProp35 != null) {
                args = new String[]{"-props", prop35Path};
                logger.info("Using 3.5.2 style Chinese coreference.");
                // If we are using Stanford model 3.5.2, the coreference call is not automatically included in
                // pipeline, we need to explicitly call HybridCorefAnnotator.
                explicitCorefCall = true;
            } else {
                throw new ResourceInitializationException(new IOException(
                        String.format("Cannot find 3.5.2 style path [%s] nor 3.6.0 style path [%s]",
                                prop35Path, prop36Path)
                ));
            }

            Properties props = StringUtils.argsToProperties(args);
            if (splitOnly) {
                props.setProperty("annotators", "segment, ssplit, pos, lemma");
            }

            props.setProperty("segment.verbose", "false");
            props.setProperty("coref.verbose", "false");

            if (numThreads > 1) {
                logger.info("Setting multiple threads for StanfordCoreNLP: " + numThreads);
                props.setProperty("threads", String.valueOf(numThreads));
            }

            pipeline = new StanfordCoreNLP(props);

            if (!splitOnly && explicitCorefCall) {
                // This means we are under Stanford 3.5.2, since this HybridCorefAnnotator is not available in 3.6.0, to
                // call it explicitly, we call it by name.
                logger.info("Explicitly loading HybridCorefAnnotator.");
                String corefClassName = "edu.stanford.nlp.pipeline.HybridCorefAnnotator";
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(corefClassName);
                    Constructor<?> ctor = clazz.getConstructor(Properties.class);
                    hybridCorefAnnotator = ctor.newInstance(props);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                        InvocationTargetException | InstantiationException e) {
                    logger.error(String.format("Cannot initialize %s from name, maybe the Stanford CoreNLP version is" +
                            " not supported.", corefClassName));
                    logger.error("Hybrid coreference annotator is not initialized.");
                    throw new ResourceInitializationException(e);
                }
            }

            hf = new ChineseSemanticHeadFinder();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (language.equals("en")) {
            annotateEnglish(aJCas, 0);
            for (JCas view : getAdditionalViews(aJCas)) {
                annotateEnglish(view, 0);
            }
        } else if (language.equals("zh")) {
            annotateChinese(aJCas, 0);
            for (JCas view : getAdditionalViews(aJCas)) {
                annotateChinese(view, 0);
            }
        }
    }

    private void annotateChinese(JCas aJCas, int textOffset) {
        Annotation document = new Annotation(aJCas.getDocumentText());
        logger.info(String.format("Annotate %s with Chinese CoreNLP.", UimaConvenience.getDocumentName(aJCas)));
        pipeline.annotate(document);
        logger.info("Annotation done, applying to JCas.");

        // Adding token level annotations.
        Map<Span, StanfordEntityMention> spanMentionMap = new HashMap<Span, StanfordEntityMention>();
        List<EntityMention> allMentions = new ArrayList<>();

        addCharacterAnnotation(aJCas, document, textOffset);
        addTokenLevelAnnotation(aJCas, document, textOffset, spanMentionMap, allMentions);

        // Adding sentence level annotations.
        addSentenceLevelAnnotation(aJCas, document, textOffset);

        if (!splitOnly) {
            // Adding coreference level annotations.
            if (explicitCorefCall) {
                logger.info("Explicitly annotation hybrid coreference.");
                try {
                    if (hybridCorefAnnotator != null) {
                        Method method = hybridCorefAnnotator.getClass().getMethod("annotate", Annotation.class);
                        method.invoke(hybridCorefAnnotator, document);
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    logger.error("We cannot annotate through hybrid coref annotator here.");
                    e.printStackTrace();
                }
            }
            addCorefAnnotation(aJCas, document, spanMentionMap, allMentions);
            createEntities(aJCas, allMentions);
        }
    }

    private void annotateEnglish(JCas aJCas, int textOffset) {
        Annotation document = new Annotation(aJCas.getDocumentText());
        logger.info(String.format("Annotate %s with English CoreNLP.", UimaConvenience.getDocumentName(aJCas)));
        pipeline.annotate(document);
        logger.info("Annotation done, applying to JCas.");

        // Adding token level annotations.
        Map<Span, StanfordEntityMention> spanMentionMap = new HashMap<Span, StanfordEntityMention>();
        List<EntityMention> allMentions = new ArrayList<>();
        addTokenLevelAnnotation(aJCas, document, textOffset, spanMentionMap, allMentions);

        // Adding sentence level annotations.
        addSentenceLevelAnnotation(aJCas, document, textOffset);

        // Adding coreference level annotations.
//        addDCoreferenceAnnotation(aJCas, document, spanMentionMap, allMentions);

//        addHCorefAnnotation(aJCas, document, spanMentionMap, allMentions);

        if (!splitOnly) {
            addCorefAnnotation(aJCas, document, spanMentionMap, allMentions);
            createEntities(aJCas, allMentions);
        }
    }

    private void createEntities(JCas aJCas, List<EntityMention> allMentions) {
        //Sort and assign id to mentions.
        Collections.sort(allMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention m1, EntityMention m2) {
                return m1.getBegin() - m2.getBegin();
            }
        });

        int mentionIdx = 0;
        for (EntityMention mention : allMentions) {
            UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionIdx++, aJCas);
            if (mention.getReferingEntity() == null) {
                // Add singleton entities.
                Entity entity = new Entity(aJCas);
                entity.setEntityMentions(new FSArray(aJCas, 1));
                entity.setEntityMentions(0, mention);
                mention.setReferingEntity(entity);
                entity.setRepresentativeMention(mention);
            }

            if (mention.getHead() == null) {
                mention.setHead(UimaNlpUtils.findHeadFromStanfordAnnotation(mention));
            }
        }
    }

    private void addCorefAnnotation(JCas aJCas, Annotation document, Map<Span, StanfordEntityMention>
            spanMentionMap, List<EntityMention> allMentions) {

        boolean hcorefFound = false;
        if (document.get(edu.stanford.nlp.hcoref.CorefCoreAnnotations.CorefChainAnnotation.class) != null) {
            hcorefFound = true;
        }

        boolean dcorefFound = false;

        if (document.get(CorefCoreAnnotations.CorefChainAnnotation.class) != null) {
            dcorefFound = true;
        }

        if (hcorefFound) {
            addHCorefAnnotation(aJCas, document, spanMentionMap, allMentions);
        } else if (dcorefFound) {
            addDCoreferenceAnnotation(aJCas, document, spanMentionMap, allMentions);
        } else {
            logger.error("No coreference annotation chain found, what key is used for Stanford CoreNLP?");
        }
    }

    private void addHCorefAnnotation(JCas aJCas, Annotation document, Map<Span, StanfordEntityMention>
            spanMentionMap, List<EntityMention> allMentions) {
        Map<Integer, edu.stanford.nlp.hcoref.data.CorefChain> graph = document.get(edu.stanford.nlp.hcoref
                .CorefCoreAnnotations.CorefChainAnnotation.class);

        List<List<StanfordCorenlpToken>> sentTokens = new ArrayList<List<StanfordCorenlpToken>>();

        for (StanfordCorenlpSentence sSent : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentTokens.add(JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, sSent));
        }

        for (Map.Entry<Integer, edu.stanford.nlp.hcoref.data.CorefChain> entry : graph.entrySet()) {
            edu.stanford.nlp.hcoref.data.CorefChain refChain = entry.getValue();
            Entity entity = new Entity(aJCas);
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, 0, aJCas);

            List<StanfordEntityMention> stanfordEntityMentions = new ArrayList<StanfordEntityMention>();

            edu.stanford.nlp.hcoref.data.CorefChain.CorefMention representativeMention = refChain
                    .getRepresentativeMention();

            for (edu.stanford.nlp.hcoref.data.CorefChain.CorefMention mention : refChain.getMentionsInTextualOrder()) {
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

                    em.setReferingEntity(entity);
                    em.setHead(mentionHead);
                    stanfordEntityMentions.add(em);

                    if (representativeMention.equals(mention)) {
                        entity.setRepresentativeMention(em);
                    }
                } catch (Exception e) {
                    logger.error("Cannot find the correct range for mention " + mention.mentionSpan + " " +
                            mention.sentNum + " " + mention.startIndex + " " + mention.endIndex);
                }
            }

            // Convert the list to CAS entity mention FSList type.
            FSArray mentionFSList = UimaConvenience.makeFsArray(stanfordEntityMentions, aJCas);
            // Put that in the cluster type.
            entity.setEntityMentions(mentionFSList);
        }
    }

    /**
     * @param aJCas
     * @param document
     * @param spanMentionMap
     * @param allMentions
     * @deprecated DCoref.CorefChainAnnotations is not used in new stanford version, use addHCorefAnnotation() instead.
     */
    @Deprecated
    private void addDCoreferenceAnnotation(JCas aJCas, Annotation document, Map<Span, StanfordEntityMention>
            spanMentionMap, List<EntityMention> allMentions) {
        // The following set the coreference chain to CAS annotation.
        Map<Integer, CorefChain> graph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        List<List<StanfordCorenlpToken>> sentTokens = new ArrayList<List<StanfordCorenlpToken>>();

        for (StanfordCorenlpSentence sSent : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            sentTokens.add(JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, sSent));
        }

        for (Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain refChain = entry.getValue();
            Entity entity = new Entity(aJCas);
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, 0, aJCas);

            List<StanfordEntityMention> stanfordEntityMentions = new ArrayList<StanfordEntityMention>();

            CorefChain.CorefMention representativeMention = refChain.getRepresentativeMention();

            for (CorefChain.CorefMention mention : refChain.getMentionsInTextualOrder()) {
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

                    em.setReferingEntity(entity);
                    em.setHead(mentionHead);
                    stanfordEntityMentions.add(em);

                    if (representativeMention.equals(mention)) {
                        entity.setRepresentativeMention(em);
                    }
                } catch (Exception e) {
                    logger.error("Cannot find the correct range for mention " + mention.mentionSpan + " " +
                            mention.sentNum + " " + mention.startIndex + " " + mention.endIndex);
                }
            }

            // Convert the list to CAS entity mention FSList type.
            FSArray mentionFSList = UimaConvenience.makeFsArray(stanfordEntityMentions, aJCas);
            // Put that in the cluster type.
            entity.setEntityMentions(mentionFSList);
        }
    }

    private void addSentenceLevelAnnotation(JCas aJCas, Annotation document, int textOffset) {
        List<CoreMap> sentAnnos = document.get(SentencesAnnotation.class);
        int sentenceId = 0;

        for (CoreMap sentAnno : sentAnnos) {
            // The following add Stanford sentence to CAS.
            int begin = sentAnno.get(CharacterOffsetBeginAnnotation.class);
            int end = sentAnno.get(CharacterOffsetEndAnnotation.class);
            StanfordCorenlpSentence sSent = new StanfordCorenlpSentence(aJCas, begin + textOffset, end + textOffset);
            UimaAnnotationUtils.finishAnnotation(sSent, COMPONENT_ID, sentenceId, aJCas);
            sentenceId++;

            if (!splitOnly) {
                // The following deals with tree annotation.
                Tree tree = sentAnno.get(TreeAnnotation.class);
                StanfordTreeAnnotation uimaTree = addPennTreeAnnotation(aJCas, tree, null, null, textOffset);

                // The following add the collapsed cc processed dependencies of each sentence into CAS annotation.
                SemanticGraph depends = sentAnno.get(CollapsedCCProcessedDependenciesAnnotation.class);

                List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, sSent);
                Map<IndexedWord, StanfordCorenlpToken> stanford2UimaMap = new HashMap<>();
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

                ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> headRelationMap = ArrayListMultimap
                        .create();
                ArrayListMultimap<StanfordCorenlpToken, StanfordDependencyRelation> childRelationMap = ArrayListMultimap
                        .create();

                for (SemanticGraphEdge stanfordEdge : depends.edgeIterable()) {
                    String edgeType = stanfordEdge.getRelation().toString();
                    double edgeWeight = stanfordEdge.getWeight(); // weight is usually infinity
                    StanfordCorenlpToken head = stanford2UimaMap.get(stanfordEdge.getGovernor());
                    StanfordCorenlpToken child = stanford2UimaMap.get(stanfordEdge.getDependent());

                    StanfordDependencyRelation sr = new StanfordDependencyRelation(aJCas);
                    sr.setHead(head);
                    sr.setChild(child);
                    sr.setWeight(edgeWeight);
                    sr.setDependencyType(edgeType);
                    UimaAnnotationUtils.finishTop(sr, COMPONENT_ID, 0, aJCas);

                    headRelationMap.put(child, sr);
                    childRelationMap.put(head, sr);
                }

                // associate the edges to the nodes
                for (StanfordCorenlpToken sNode : stanford2UimaMap.values()) {
                    if (headRelationMap.containsKey(sNode)) {
                        sNode.setHeadDependencyRelations(FSCollectionFactory.createFSList(aJCas, headRelationMap.get
                                (sNode)));
                    }
                    if (childRelationMap.containsKey(sNode)) {
                        sNode.setChildDependencyRelations(FSCollectionFactory.createFSList(aJCas, childRelationMap.get
                                (sNode)));
                    }
                }
            }
        }
    }

    private void addCharacterAnnotation(JCas aJCas, Annotation document, int textOffset) {
        int characterId = 0;
        for (CoreLabel character : document.get(ChineseCoreAnnotations.CharactersAnnotation.class)) {
            int beginIndex = character.get(CharacterOffsetBeginAnnotation.class) + textOffset;
            int endIndex = character.get(CharacterOffsetEndAnnotation.class) + textOffset;

            CharacterAnnotation sChar = new CharacterAnnotation(aJCas, beginIndex, endIndex);

            UimaAnnotationUtils.finishAnnotation(sChar, COMPONENT_ID, characterId++, aJCas);
        }
    }

    private void addTokenLevelAnnotation(JCas aJCas, Annotation document, int textOffset, Map<Span,
            StanfordEntityMention> spanMentionMap, List<EntityMention> allMentions) {
        // The following put token annotation to CAS
        int tokenId = 0;
        String preNe = "";
        int neBegin = 0;
        int neEnd = 0;

        for (CoreLabel token : document.get(TokensAnnotation.class)) {
            int beginIndex = token.get(CharacterOffsetBeginAnnotation.class) + textOffset;
            int endIndex = token.get(CharacterOffsetEndAnnotation.class) + textOffset;

            // add token annotation
            StanfordCorenlpToken sToken = new StanfordCorenlpToken(aJCas, beginIndex, endIndex);
            sToken.setPos(token.get(PartOfSpeechAnnotation.class));
            sToken.setLemma(token.get(LemmaAnnotation.class));
            UimaAnnotationUtils.finishAnnotation(sToken, COMPONENT_ID, tokenId++, aJCas);

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
                        sne.setBegin(neBegin);
                        sne.setEnd(neEnd);
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
    }

    private StanfordTreeAnnotation addPennTreeAnnotation(JCas aJCas, Tree currentNode, Tree parentNode,
                                                         StanfordTreeAnnotation parent, int textOffset) {
        StanfordTreeAnnotation treeAnno = new StanfordTreeAnnotation(aJCas);

        if (!currentNode.isLeaf()) {
            Tree headTree = hf.determineHead(currentNode);

            int thisBegin = 0;
            int thisEnd = 0;
            int count = 0;
            int numChild = currentNode.children().length;
            String currentNodeLabel = currentNode.value();

            List<StanfordTreeAnnotation> childrenList = new ArrayList<StanfordTreeAnnotation>();

            StanfordTreeAnnotation uimaHeadTree = null;

            for (Tree child : currentNode.children()) {
                StanfordTreeAnnotation childTree = addPennTreeAnnotation(aJCas, child, currentNode,
                        treeAnno, textOffset);
//                stanford2UimaTree.put(child, childTree);

                if (child.equals(headTree)) {
                    uimaHeadTree = childTree;
                }

                childrenList.add(childTree);
                if (count == 0) {
                    thisBegin = childTree.getBegin();
                }
                if (count == numChild - 1) {
                    thisEnd = childTree.getEnd();
                }
                count++;
            }

            treeAnno.setHeadTree(uimaHeadTree);
            treeAnno.setHead(uimaHeadTree.getHead());

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

            UimaAnnotationUtils.finishAnnotation(treeAnno, COMPONENT_ID, 0, aJCas);
            return treeAnno;
        } else {
            ArrayList<edu.stanford.nlp.ling.Word> words = currentNode.yieldWords();
//            StanfordTreeAnnotation leafTree = new StanfordTreeAnnotation(aJCas);

            int leafBegin = words.get(0).beginPosition() + textOffset;
            int leafEnd = words.get(words.size() - 1).endPosition() + textOffset;

            treeAnno.setBegin(leafBegin);
            treeAnno.setEnd(leafEnd);
            treeAnno.setPennTreeLabel(currentNode.value());
            treeAnno.setIsLeaf(true);

            List<StanfordCorenlpToken> leafTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, treeAnno);

            if (leafTokens.size() != 1) {
                logger.warn(String.format("Incorrect leave span [%d:%d], it contains %d words.", leafBegin, leafEnd,
                        leafTokens.size()));
            }

            if (leafTokens.size() > 0) {
                treeAnno.setHead(leafTokens.get(0));
            }

            UimaAnnotationUtils.finishAnnotation(treeAnno, COMPONENT_ID, 0, aJCas);
            return treeAnno;
        }
    }

}