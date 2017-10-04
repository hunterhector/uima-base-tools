package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.nlp.lemmatizer.IllinoisLemmatizer;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 5:52 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaLemmaAnnotator extends UimaSentenceAnnotator {
    private static final String NAME = IllinoisLemmatizer.class.getCanonicalName();

    public UimaLemmaAnnotator() {
        super(ViewNames.LEMMA);
    }

    @Override
    protected void addView(TextAnnotation textAnnotation) throws AnnotatorException {
        String docid = textAnnotation.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);

        int sentenceId = getNextSentenceId(docid);
        ArrayList<StanfordCorenlpSentence> sentences = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpSentence.class));
        StanfordCorenlpSentence sentence = sentences.get(sentenceId);

        TokenLabelView lemmaView = new TokenLabelView(ViewNames.LEMMA, NAME, textAnnotation, 1.0);
        int tid = 0;
        for (StanfordCorenlpToken uimaToken : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
            Constituent lemmaConstituent = new Constituent(uimaToken.getLemma(), ViewNames.LEMMA,
                    textAnnotation, tid, tid + 1);
//            System.out.println(String.format("lemma is %s, uima token is %s.",
//                    lemmaConstituent.getTokenizedSurfaceForm(), uimaToken.getCoveredText()));
            lemmaView.addConstituent(lemmaConstituent);
            tid++;
        }
//        DebugUtils.pause();
        textAnnotation.addView(viewName, lemmaView);
    }
}
