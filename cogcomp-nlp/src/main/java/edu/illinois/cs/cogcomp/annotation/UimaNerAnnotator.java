package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 5:54 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaNerAnnotator extends Annotator {

    public UimaNerAnnotator() {
        super(ViewNames.NER_CONLL, new String[0], false);
    }

    @Override
    public void initialize(ResourceManager rm) {

    }

    @Override
    protected void addView(TextAnnotation ta) throws AnnotatorException {
        SpanLabelView nerView = new SpanLabelView(getViewName(), ta);

        String docid = ta.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);

        int tokenIndex = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            token.setIndex(tokenIndex++);
        }

        for (EntityMention mention : JCasUtil.select(aJCas, EntityMention.class)) {
            List<StanfordCorenlpToken> mentionTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention);
            int s = mentionTokens.get(0).getIndex();
            int e = mentionTokens.get(mentionTokens.size() - 1).getIndex();
            nerView.addSpanLabel(s, e, mention.getEntityType(), 1d);
        }
        ta.addView(viewName, nerView);
    }
}
