package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 4:16 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaPOSAnnotator extends UimaSentenceAnnotator {
    private static final String NAME = POSAnnotator.class.getCanonicalName();

    public UimaPOSAnnotator() {
        super(ViewNames.POS);
    }

    @Override
    protected void addView(TextAnnotation record) throws AnnotatorException {
        String docid = record.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);
        List<Constituent> tokens = record.getView(ViewNames.TOKENS).getConstituents();
        TokenLabelView posView = new TokenLabelView(ViewNames.POS, NAME, record, 1.0);

        int sentenceId = getNextSentenceId(docid);
//        logger.info(String.format("Adding %s view for doc %s, sentence %d.", viewName, docid, sentenceId));

        ArrayList<StanfordCorenlpSentence> sentences = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpSentence.class));

        StanfordCorenlpSentence sentence = sentences.get(sentenceId);

        int tcounter = 0;
        for (StanfordCorenlpToken uimaToken : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
            Constituent token = tokens.get(tcounter);
            Constituent label = new Constituent(uimaToken.getPos(), ViewNames.POS, record,
                    token.getStartSpan(), token.getEndSpan());
            posView.addConstituent(label);
            tcounter++;
        }
        record.addView(viewName, posView);

//        logger.info(viewName + " view is found : " + record.hasView(viewName));

        if (sentenceId == sentences.size() - 1) {
            removeDoc(docid);
        }
    }
}
