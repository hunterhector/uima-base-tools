package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
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
 * Created with IntelliJ IDEA.
 * Date: 9/20/16
 * Time: 10:57 AM
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
        UimaConvenience.printProcessLog(aJCas, logger);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            List<String> words = new ArrayList<>();
            List<String> posTags = new ArrayList<>();

            boolean validSegmentation = segmentSentence(sentence, words);
            Postagger.postag(words, posTags);
            List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, sentence);

            if (validSegmentation) {
//            logger.info("Annotating tokens.");
                List<LtpToken> tokens = annotateTokens(aJCas, characters, words, posTags);

//            logger.info("Annotating NER.");
                List<String> ners = annotateNer(aJCas, tokens, words, posTags);

//            logger.info("Parsing.");
                List<Integer> heads = new ArrayList<>();
                List<String> depRels = new ArrayList<>();
                annotateDep(aJCas, tokens, heads, depRels);

//            logger.info("Annotating srl");
                annotateSrl(aJCas, tokens, words, posTags, ners, heads, depRels);
//            DebugUtils.pause();

            }else{
                logger.warn("Skipping adding annotation to this sentence.");
            }
        }
    }

    private boolean segmentSentence(StanfordCorenlpSentence sentence, List<String> words) {
        String sourceSent = sentence.getCoveredText().replaceAll("\\s", "").replaceAll("\\n", "").replaceAll("\\r", "");
//        logger.info("Annotating " + sourceSent);
        List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, sentence);

        // Do segmentation first.
        int size = Segmentor.segment(sourceSent, words);

        int totalLength = 0;
        for (String word : words) {
            totalLength += word.length();
        }

        if (totalLength != characters.size()) {
            logger.warn(String.format("Segmented words' total character length : %d is not the same as the " +
                    "original character length : %d", totalLength, characters.size()));
            return false;
        }
        return true;
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

//        logger.info(Joiner.on(" ").join(words));
//        logger.info("Number of words is " + words.size());
//        logger.info("Number of dep relations is " + size);

        ArrayListMultimap<LtpToken, LtpDependency> headDependencies = ArrayListMultimap.create();
        ArrayListMultimap<LtpToken, LtpDependency> childDependencies = ArrayListMultimap.create();

//        for (int i = 0; i < size; i++) {
//            System.out.print(heads.get(i) + ":" + depRels.get(i));
//            if (i == size - 1) {
//                System.out.println();
//            } else {
//                System.out.print("        ");
//            }
//        }

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

    private List<LtpToken> annotateTokens(JCas aJCas, List<CharacterAnnotation> characters, List<String> words,
                                          List<String> posTags) {
        List<LtpToken> tokens = new ArrayList<>();

//        String sourceSent = sentence.getCoveredText().replaceAll("\\s", "").replaceAll("\\n", "").replaceAll("\\r",
// "");
//        int size = Segmentor.segment(sourceSent, words);
////        logger.info("Annotating " + sourceSent);
//
//        Postagger.postag(words, posTags);
//
//        List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, sentence);
//
//        int totalLength = 0;
//        for (String word : words) {
//            totalLength += word.length();
//        }
//
//        if (totalLength != characters.size()) {
//            logger.error(String.format("Segmented words' total character length : %d is not the same as the " +
//                    "original character length : %d", totalLength, characters.size()));
//        }

        int currentLength = 0;
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            int wordEnd = currentLength + word.length() - 1;
            int begin = characters.get(currentLength).getBegin();
            int end = characters.get(wordEnd).getEnd();
            LtpToken ltpToken = new LtpToken(aJCas, begin, end);
            ltpToken.setLemma(word);
            ltpToken.setPos(posTags.get(i));
            UimaAnnotationUtils.finishAnnotation(ltpToken, begin, end, COMPONENT_ID, 0, aJCas);
            currentLength += word.length();
            tokens.add(ltpToken);
        }

        return tokens;
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
