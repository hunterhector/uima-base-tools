package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Predicate;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.corpus.Word;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/5/16
 * Time: 10:06 AM
 *
 * @author Zhengzhong Liu
 */
public class MateChineseSrlAnnotator extends AbstractLoggingAnnotator {

    public final static String PARAM_MODEL_FILE = "modelFile";
    @ConfigurationParameter(name = PARAM_MODEL_FILE)
    private File modelFile;

    private SemanticRoleLabeler srl;

    public final static String COMPONENT_ID = MateChineseSrlAnnotator.class.getSimpleName();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            logger.info(String.format("Loading SRL model from %s.", modelFile.getCanonicalPath()));
            srl = Pipeline.fromZipFile(new ZipFile(modelFile));
            Language.setLanguage(Language.L.chi);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (edu.cmu.cs.lti.script.type.Sentence sentence : JCasUtil.select(aJCas, edu.cmu.cs.lti.script.type
                .Sentence.class)) {
            Sentence srlSentence = createSentence(sentence);
            srl.parseSentence(srlSentence);
            annotateParse(aJCas, sentence, srlSentence);
        }
    }

    private void annotateParse(JCas aJCas, edu.cmu.cs.lti.script.type.Sentence sentence, Sentence srlSentence) {
        List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
        MateToken[] mateTokens = new MateToken[tokens.size()];

        for (int i = 0; i < tokens.size(); i++) {
            StanfordCorenlpToken token = tokens.get(i);
            MateToken mateToken = new MateToken(aJCas, token.getBegin(), token.getEnd());
            mateToken.setIndex(token.getIndex());
            mateToken.setInSentenceIndex(i);
            mateTokens[i] = mateToken;
        }

        List<Predicate> predicates = srlSentence.getPredicates();

        ArrayListMultimap<MateToken, SemanticRelation> semanticChildRelations = ArrayListMultimap.create();

        for (Predicate predicate : predicates) {
            String sense = predicate.getSense();
            MateToken headToken = mateTokens[predicate.getIdx() - 1];
            headToken.setLexicalSense(sense);


            List<SemanticRelation> childRelations = new ArrayList<>();
            for (Map.Entry<Word, String> argument : predicate.getArgMap().entrySet()) {
                MateToken argumentHead = mateTokens[argument.getKey().getIdx() - 1];
                MateArgument arg = new MateArgument(aJCas, argumentHead.getBegin(), argumentHead.getEnd());
                arg.setHead(argumentHead);
                UimaAnnotationUtils.finishAnnotation(arg, COMPONENT_ID, 0, aJCas);

                String argumentType = argument.getValue();

                SemanticRelation relation = new SemanticRelation(aJCas);
                relation.setHead(headToken);
                relation.setChild(arg);
                relation.setPropbankRoleName(argumentType);
                UimaAnnotationUtils.finishTop(relation, COMPONENT_ID, 0, aJCas);

                childRelations.add(relation);
            }

            headToken.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, childRelations));
        }

        for (MateToken mateToken : mateTokens) {
            UimaAnnotationUtils.finishAnnotation(mateToken, COMPONENT_ID, String.valueOf(mateToken.getIndex()), aJCas);
        }
    }

    private Sentence createSentence(edu.cmu.cs.lti.script.type.Sentence sentence) {
        List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

        // The SRL Sentence need an artificial root node.
        int numWords = tokens.size() + 1;
        String[] wordForms = new String[numWords];
        String[] lemmas = new String[numWords];
        String[] morphs = new String[numWords];
        String[] tags = new String[numWords];

        wordForms[0] = "ROOT";
        lemmas[0] = "ROOT";
        morphs[0] = "ROOT";
        tags[0] = "ROOT";

        for (int i = 0; i < tokens.size(); i++) {
            StanfordCorenlpToken token = tokens.get(i);
            int wordIndex = i + 1;
            wordForms[wordIndex] = token.getCoveredText();
            lemmas[wordIndex] = token.getLemma();
            morphs[wordIndex] = token.getMorpha();
            tags[wordIndex] = token.getPos();
        }

        Sentence srlSentence = new Sentence(wordForms, lemmas, tags, morphs);

        addDependencies(tokens, srlSentence);

        return srlSentence;
    }

    private void addDependencies(List<StanfordCorenlpToken> sentenceTokens, Sentence srlSentence) {
        int[] heads = new int[sentenceTokens.size()];
        String[] deprels = new String[sentenceTokens.size()];

        for (int i = 0; i < sentenceTokens.size(); i++) {
            StanfordCorenlpToken token = sentenceTokens.get(i);
            token.setInSentenceIndex(i);
        }

        for (int i = 0; i < sentenceTokens.size(); i++) {
            StanfordCorenlpToken token = sentenceTokens.get(i);
            FSList headDepFs = token.getHeadDependencyRelations();
            if (headDepFs != null) {
                Dependency headDep = FSCollectionFactory.create(headDepFs, Dependency.class).iterator().next();
                heads[i] = headDep.getHead().getInSentenceIndex() + 1;
                deprels[i] = headDep.getDependencyType();
            } else {
                heads[i] = 0;
                deprels[i] = "ROOT";
            }

//            System.out.println(String.format("%d\t%s\t%s\t%s\t%s\t%s\t-\t-\t%d\t%d\t%s\t%s", i + 1, token
//                    .getCoveredText(), token.getCoveredText(), token.getCoveredText(), token.getPos(), token.getPos()
//                    , heads[i], heads[i], deprels[i], deprels[i]));
        }
        srlSentence.setHeadsAndDeprels(heads, deprels);

        srlSentence.buildDependencyTree();
    }
}
