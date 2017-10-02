package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.lemmatizer.IllinoisLemmatizer;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 5:52 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaLemmaAnnotator extends Annotator {
    private static final String NAME = IllinoisLemmatizer.class.getCanonicalName();

    public UimaLemmaAnnotator() {
        super(ViewNames.LEMMA, new String[0], false);
    }

    @Override
    public void initialize(ResourceManager rm) {

    }

    @Override
    protected void addView(TextAnnotation textAnnotation) throws AnnotatorException {
        String docid = textAnnotation.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);
        Collection<StanfordCorenlpToken> uimaTokens = JCasUtil.select(aJCas, StanfordCorenlpToken.class);
        TokenLabelView lemmaView = new TokenLabelView(ViewNames.LEMMA, NAME, textAnnotation, 1.0);
        int tid = 0;
        for (StanfordCorenlpToken uimaToken : uimaTokens) {
            Constituent lemmaConstituent = new Constituent(uimaToken.getLemma(), ViewNames.LEMMA,
                    textAnnotation, tid, tid + 1);
            lemmaView.addConstituent(lemmaConstituent);
            tid++;
        }
        textAnnotation.addView(viewName, lemmaView);
    }
}
