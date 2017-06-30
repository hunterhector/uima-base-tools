package edu.cmu.cs.lti.annotators;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.utils.StringUtils;
import edu.hit.ir.ltp4j.*;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This annotator runs the LTP platform's Java interface. This requires JNI dynamic library to be accessible through
 * LD_LIBRARY_PATH environment variable.
 *
 * @author Zhengzhong Liu
 */
public class LtpAnnotator extends AbstractLoggingAnnotator {

    public static final String PARAM_CWS_MODEL = "cwsModel";
    @ConfigurationParameter(name = PARAM_CWS_MODEL)
    private String cwsModel;

    public static final String PARAM_POS_MODEL = "posModel";
    @ConfigurationParameter(name = PARAM_POS_MODEL)
    private String posModel;

    public static final String PARAM_NER_MODEL = "nerModel";
    @ConfigurationParameter(name = PARAM_NER_MODEL)
    private String nerModel;

    public static final String PARAM_DEPENDENCY_MODEL = "depModel";
    @ConfigurationParameter(name = PARAM_DEPENDENCY_MODEL)
    private String depModel;

    public static final String PARAM_SRL_MODEL = "srlModel";
    @ConfigurationParameter(name = PARAM_SRL_MODEL)
    private String srlModel;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        logger.info("Loading CWS model...");
        if (Segmentor.create(cwsModel) < 0) {
            throw new ResourceInitializationException(new Throwable(String.format("Fail to load chinese segmentor " +
                    "model from [%s].", cwsModel)));
        }
        logger.info("Done.");

        logger.info("Loading POS model...");
        if (Postagger.create(posModel) < 0) {
            throw new ResourceInitializationException(new Throwable(String.format("Fail to load chinese pos model " +
                    "model from [%s].", posModel)));
        }

        logger.info("Loading NER model...");
        if (NER.create(nerModel) < 0) {
            throw new ResourceInitializationException(new Throwable(String.format("Fail to load chinese ner model " +
                    "model from [%s].", nerModel)));
        }
        logger.info("Done.");

        logger.info("Loading dependency model...");
        if (Parser.create(depModel) < 0) {
            throw new ResourceInitializationException(new Throwable(String.format("Fail to load chinese dep model " +
                    "model from [%s].", depModel)));
        }
        logger.info("Done.");

        logger.info("Loading SRL model...");
        if (SRL.create(srlModel) < 0) {
            throw new ResourceInitializationException(new Throwable(String.format("Fail to load chinese srl model " +
                    "model from [%s].", srlModel)));
        }
        logger.info("Done.");
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        UimaConvenience.printProcessLog(aJCas);
        segment(aJCas);

        for (LtpSentence sentence : JCasUtil.select(aJCas, LtpSentence.class)) {
            List<String> words = new ArrayList<>();
            List<String> posTags = new ArrayList<>();

            List<LtpToken> tokens = JCasUtil.selectCovered(LtpToken.class, sentence);

            for (LtpToken ltpToken : tokens) {
                words.add(ltpToken.getLemma());
                posTags.add(ltpToken.getPos());
            }

            List<String> ners = annotateNer(aJCas, tokens, words, posTags);
            List<Integer> heads = new ArrayList<>();
            List<String> depRels = new ArrayList<>();
            annotateDep(aJCas, tokens, heads, depRels);

            annotateSrl(aJCas, tokens, words, posTags, ners, heads, depRels);
        }
    }

    private void segment(JCas aJCas) {
        String text = aJCas.getDocumentText();

        List<String> sentences = new ArrayList<>();
        List<String> allWords = new ArrayList<>();
        List<String> allPos = new ArrayList<>();

        SplitSentence.splitSentence(text, sentences);

        List<LtpSentence> ltpSentences = new ArrayList<>();
        List<Integer> firstWords = new ArrayList<>();
        List<Integer> lastWords = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sent = sentences.get(i);

            if (!sent.trim().isEmpty()) {
                List<String> words = new ArrayList<>();
                List<String> pos = new ArrayList<>();

                Segmentor.segment(sent, words);

                if (words.size() > 0) {
                    LtpSentence sentence = new LtpSentence(aJCas);
                    ltpSentences.add(sentence);
                    Postagger.postag(words, pos);

                    firstWords.add(allWords.size());

                    allWords.addAll(words);
                    allPos.addAll(pos);

                    lastWords.add(allWords.size() - 1);
                }
            }
        }

        String wordStr = Joiner.on("").join(allWords);

        int[] offsets = StringUtils.matchOffset(text, wordStr);

        List<LtpToken> tokens = new ArrayList<>();

        int currentOffset = 0;
        for (int i = 0; i < allWords.size(); i++) {
            String word = allWords.get(i);

            int begin_char = currentOffset;
            int end_char = currentOffset + word.length() - 1;

            int begin_base_char = offsets[begin_char];
            int end_base_char = offsets[end_char];


            LtpToken ltpToken = new LtpToken(aJCas, begin_base_char, end_base_char + 1);
            ltpToken.setLemma(word);
            ltpToken.setPos(allPos.get(i));
            UimaAnnotationUtils.finishAnnotation(ltpToken, COMPONENT_ID, 0, aJCas);

            tokens.add(ltpToken);

            currentOffset += word.length();
        }

        for (int i = 0; i < ltpSentences.size(); i++) {
            LtpSentence sent = ltpSentences.get(i);
            int begin = tokens.get(firstWords.get(i)).getBegin();
            int end = tokens.get(lastWords.get(i)).getEnd();
            UimaAnnotationUtils.finishAnnotation(sent, begin, end, COMPONENT_ID, 0, aJCas);
        }
    }

    private void annotateSrl(JCas aJCas, List<LtpToken> tokens, List<String> words, List<String> posttags,
                             List<String> ners, List<Integer> heads, List<String> deprels) {
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<>();

        List<Integer> headsForSrl = heads.stream().map(head -> head - 1).collect(Collectors.toList());
        SRL.srl(words, posttags, ners, headsForSrl, deprels, srls);

        for (int i = 0; i < srls.size(); ++i) {
            int head = srls.get(i).first;
            LtpToken headToken = tokens.get(head);

//            logger.info("Predicate is " + headToken.getCoveredText());

            List<LtpSemanticRelation> semanticRelations = new ArrayList<>();
            for (int j = 0; j < srls.get(i).second.size(); ++j) {
                String type = srls.get(i).second.get(j).first;
                int beginTokenIndex = srls.get(i).second.get(j).second.first;
                int endTokenIndex = srls.get(i).second.get(j).second.second;

                LtpArgument argument = new LtpArgument(aJCas, tokens.get(beginTokenIndex).getBegin(),
                        tokens.get(endTokenIndex).getEnd());
                UimaAnnotationUtils.finishAnnotation(argument, COMPONENT_ID, 0, aJCas);
                LtpSemanticRelation semanticRelation = new LtpSemanticRelation(aJCas);
                semanticRelation.setHead(headToken);
                semanticRelation.setChild(argument);
                semanticRelation.setPropbankRoleName(type);
                semanticRelations.add(semanticRelation);
                UimaAnnotationUtils.finishTop(semanticRelation, COMPONENT_ID, 0, aJCas);
//                logger.info(type + " " + argument.getCoveredText());
            }

            headToken.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, semanticRelations));
        }
    }

    private void annotateDep(JCas aJCas, List<LtpToken> tokens, List<Integer> heads, List<String> depRels) {
        List<String> words = new ArrayList<>();
        List<String> posTags = new ArrayList<>();

        for (LtpToken token : tokens) {
            words.add(token.getCoveredText());
            posTags.add(token.getPos());
        }

        int size = Parser.parse(words, posTags, heads, depRels);

        ArrayListMultimap<LtpToken, LtpDependency> headDependencies = ArrayListMultimap.create();
        ArrayListMultimap<LtpToken, LtpDependency> childDependencies = ArrayListMultimap.create();

        for (int i = 0; i < size; i++) {
            int headIndex = heads.get(i) - 1;
            LtpToken childToken = tokens.get(i);
            if (headIndex > 0) {
                LtpToken headToken = tokens.get(headIndex);
                String depRel = depRels.get(i);

                LtpDependency dep = new LtpDependency(aJCas);
                dep.setHead(headToken);
                dep.setChild(childToken);
                dep.setDependencyType(depRel);
                UimaAnnotationUtils.finishTop(dep, COMPONENT_ID, 0, aJCas);

                headDependencies.put(childToken, dep);
                childDependencies.put(headToken, dep);
            } else {
                childToken.setIsDependencyRoot(true);
            }
        }

        for (Map.Entry<LtpToken, Collection<LtpDependency>> token2Heads : headDependencies.asMap().entrySet()) {
            LtpToken token = token2Heads.getKey();
            token.setHeadDependencyRelations(FSCollectionFactory.createFSList(aJCas, token2Heads.getValue()));
        }

        for (Map.Entry<LtpToken, Collection<LtpDependency>> token2Children : childDependencies.asMap().entrySet()) {
            LtpToken token = token2Children.getKey();
            token.setChildDependencyRelations(FSCollectionFactory.createFSList(aJCas, token2Children.getValue()));
        }
    }

    private List<String> annotateNer(JCas aJCas, List<LtpToken> tokens, List<String> words, List<String> posTags) {
        List<String> ners = new ArrayList<>();

        NER.recognize(words, posTags, ners);

        int neBegin = -1;

        for (int i = 0; i < words.size(); i++) {
            String tokenNerTag = ners.get(i);
            tokens.get(i).setNerTag(tokenNerTag);

            if (tokenNerTag.startsWith("B-")) {
                neBegin = i;
            }

            if (tokenNerTag.startsWith("E-")) {
                LtpEntityMention mention = new LtpEntityMention(aJCas, tokens.get(neBegin).getBegin(),
                        tokens.get(i).getEnd());
                mention.setEntityType(tokenNerTag.substring(2));

                UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, 0, aJCas);
            }

            if (tokenNerTag.startsWith("S-")) {
                LtpEntityMention mention = new LtpEntityMention(aJCas, tokens.get(i).getBegin(),
                        tokens.get(i).getEnd());
                mention.setEntityType(tokenNerTag.substring(2));

                UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, 0, aJCas);
            }
        }

        return ners;
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        Segmentor.release();
        Postagger.release();
        NER.release();
        Parser.release();
        SRL.release();
        logger.info("LTP resources successfully released.");
    }


}
