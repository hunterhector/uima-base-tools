package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TokenLabelView;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.pos.POSAnnotator;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 4:16 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaPOSAnnotator extends Annotator {
    private final Logger logger = LoggerFactory.getLogger(UimaPOSAnnotator.class);
    private static final String NAME = POSAnnotator.class.getCanonicalName();

    public UimaPOSAnnotator() {
        super(ViewNames.POS, new String[0], false);
    }

    @Override
    public void initialize(ResourceManager rm) {
    }

    @Override
    protected void addView(TextAnnotation record) throws AnnotatorException {
        String docid = record.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);

        List<Constituent> tokens = record.getView(ViewNames.TOKENS).getConstituents();
        TokenLabelView posView = new TokenLabelView(ViewNames.POS, NAME, record, 1.0);

        int tcounter = 0;
        for (StanfordCorenlpToken uimaToken : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            Constituent token = tokens.get(tcounter);
            Constituent label = new Constituent(uimaToken.getPos(), ViewNames.POS, record,
                    token.getStartSpan(), token.getEndSpan());
            posView.addConstituent(label);
            tcounter++;
        }
        record.addView(viewName, posView);
    }
}
