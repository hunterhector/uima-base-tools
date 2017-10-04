package edu.illinois.cs.cogcomp.annotation;

import edu.cmu.cs.lti.annotators.SRLAnnotator;
import edu.cmu.cs.lti.script.type.ParseTreeAnnotation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TreeView;
import edu.illinois.cs.cogcomp.core.datastructures.trees.Tree;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/1/17
 * Time: 5:57 PM
 *
 * @author Zhengzhong Liu
 */
public class UimaStanfordDepAnnotator extends UimaSentenceAnnotator {
    public UimaStanfordDepAnnotator() {
        super(ViewNames.PARSE_STANFORD);
    }

    private Tree<String> convertFromStanfordTree(ParseTreeAnnotation uimaTree) {
        Tree<String> tree = new Tree<>(uimaTree.getPennTreeLabel());

        for (int i = 0; i < uimaTree.getChildren().size(); i++) {
            ParseTreeAnnotation uimaSubTree = uimaTree.getChildren(i);

            if (uimaSubTree.getIsLeaf()) {
                tree.addLeaf(uimaSubTree.getCoveredText());
            } else {
                Tree<String> subTree = convertFromStanfordTree(uimaSubTree);
                tree.addSubtree(subTree);
            }
        }
        return tree;
    }

    private StanfordTreeAnnotation getTree(StanfordCorenlpSentence sentence) {
        List<StanfordTreeAnnotation> trees = JCasUtil.selectCovered(StanfordTreeAnnotation.class, sentence);
        for (StanfordTreeAnnotation tree : trees) {
            if (tree.getBegin() == sentence.getBegin() && tree.getEnd() == sentence.getEnd()) {
                if (tree.getPennTreeLabel().equals("ROOT")) {
                    return tree;
                }
            }
        }
        return null;
    }

    @Override
    protected void addView(TextAnnotation textAnnotation) throws AnnotatorException {
        TreeView treeView = new TreeView(ViewNames.PARSE_STANFORD, "UimaStanfordDepAnnotator", textAnnotation, 1d);
        String docid = textAnnotation.getId();
        JCas aJCas = SRLAnnotator.docCas.get(docid);

        int sentenceId = getNextSentenceId(docid);
        ArrayList<StanfordCorenlpSentence> sentences = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpSentence.class));
        StanfordCorenlpSentence sentence = sentences.get(sentenceId);

        StanfordTreeAnnotation parseTree = getTree(sentence);
        Tree<String> tree = convertFromStanfordTree(parseTree);
        treeView.setParseTree(0, tree);

        textAnnotation.addView(viewName, treeView);
    }
}
