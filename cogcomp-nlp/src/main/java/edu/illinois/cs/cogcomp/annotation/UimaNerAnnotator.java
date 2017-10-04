package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 5:54 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaNerAnnotator extends UimaSentenceAnnotator {

    public UimaNerAnnotator() {
        super(ViewNames.NER_CONLL);
    }

    @Override
    protected void addView(TextAnnotation ta) throws AnnotatorException {
        SpanLabelView nerView = new SpanLabelView(getViewName(), ta);
        String docid = ta.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);

        int sentenceId = getNextSentenceId(docid);
        ArrayList<StanfordCorenlpSentence> sentences = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpSentence.class));
        StanfordCorenlpSentence sentence = sentences.get(sentenceId);

        int tokenIndex = 0;
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
            token.setIndex(tokenIndex++);
        }

        for (EntityMention mention : JCasUtil.selectCovered(EntityMention.class, sentence)) {
            logger.info("Adding mention " + mention.getCoveredText());

            List<StanfordCorenlpToken> mentionTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention);
            int s = mentionTokens.get(0).getIndex();
            int e = mentionTokens.get(mentionTokens.size() - 1).getIndex();
            logger.info("Start is " + s + " end is " + e);
            nerView.addSpanLabel(s, e, mention.getEntityType(), 1d);
        }
        ta.addView(viewName, nerView);
    }
}
