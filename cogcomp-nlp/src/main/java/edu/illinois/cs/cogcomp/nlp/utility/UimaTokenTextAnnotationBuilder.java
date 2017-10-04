package edu.illinois.cs.cogcomp.nlp.utility;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.tokenizer.Tokenizer;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 4:50 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaTokenTextAnnotationBuilder implements TextAnnotationBuilder {
    private final Logger logger = LoggerFactory.getLogger(UimaTokenTextAnnotationBuilder.class);

    private static final String NAME = UimaTokenTextAnnotationBuilder.class.getSimpleName();
    private static final String DEFAULT_TEXT_ID = "dummyTextId";
    private static final String DEFAULT_CORPUS_ID = "dummyCorpusId";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public TextAnnotation createTextAnnotation(String text) throws IllegalArgumentException {
        return createTextAnnotation(DEFAULT_CORPUS_ID, DEFAULT_TEXT_ID, text);
    }

    @Override
    public TextAnnotation createTextAnnotation(String corpusId, String textId, String text) throws
            IllegalArgumentException {
        JCas aJCas = SRLAnnotator.docCas.get(textId);

        List<IntPair> characterOffsets = new LinkedList<>();
        List<String> tokens = new LinkedList<>();

        Collection<StanfordCorenlpSentence> uimaSentences = JCasUtil.select(aJCas, StanfordCorenlpSentence.class);

        int sentenceEndTokenIndex = 0;
        int sentIndex = 0;
        int[] sentenceEndTokenOffsets = new int[uimaSentences.size()];
        for (StanfordCorenlpSentence sentence : uimaSentences) {
            List<StanfordCorenlpToken> uimaTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            for (StanfordCorenlpToken token : uimaTokens) {
                IntPair wordOffsets = new IntPair(token.getBegin(), token.getEnd());
                characterOffsets.add(wordOffsets);
                tokens.add(token.getCoveredText());
            }
            sentenceEndTokenIndex += uimaTokens.size();

            sentenceEndTokenOffsets[sentIndex++] = sentenceEndTokenIndex;
        }

        String[] tokenArray = tokens.toArray(new String[tokens.size()]);
        IntPair[] charOffsetArray = characterOffsets.toArray(new IntPair[characterOffsets.size()]);

        TextAnnotation ta = new TextAnnotation(corpusId, textId, text, charOffsetArray,
                tokenArray, sentenceEndTokenOffsets);
        SpanLabelView view =
                new SpanLabelView(ViewNames.SENTENCE, NAME, ta, 1.0);

        int start = 0;
        for (int s : sentenceEndTokenOffsets) {
            view.addSpanLabel(start, s, ViewNames.SENTENCE, 1d);
            start = s;
        }
        ta.addView(ViewNames.SENTENCE, view);
        return ta;
    }

    @Override
    public TextAnnotation createTextAnnotation(String corpusId, String textId, String text, Tokenizer.Tokenization
            tokenization) throws IllegalArgumentException {
        throw new IllegalArgumentException("Uima annotator did not implement this method.");
    }
}
