package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class UimaNlpUtils {
    public static String getLemmatizedAnnotation(Annotation a) {
        StringBuilder builder = new StringBuilder();
        String spliter = "";
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, a)) {
            builder.append(spliter);
            builder.append(token.getLemma());
            spliter = " ";
        }
        return builder.toString();
    }

    public static EntityMention createEntityMention(JCas jcas, int begin, int end, String componentId) {
        EntityMention mention = new EntityMention(jcas, begin, end);
        UimaAnnotationUtils.finishAnnotation(mention, componentId, null, jcas);
        mention.setHead(findHeadFromTreeAnnotation(jcas, mention));
        return mention;
    }

    public static StanfordCorenlpToken findFirstToken(Annotation anno) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, anno)) {
            return token;
        }
        return null;
    }

    public static StanfordCorenlpToken findHeadFromTreeAnnotation(JCas aJCas, Annotation anno) {
        StanfordTreeAnnotation largestContainingTree = null;

        for (StanfordTreeAnnotation tree : JCasUtil.selectCovered(aJCas, StanfordTreeAnnotation.class,
                anno)) {
            if (largestContainingTree == null) {
                largestContainingTree = tree;
            } else if (largestContainingTree.getEnd() - largestContainingTree.getBegin() < tree.getEnd()
                    - tree.getBegin()) {
                largestContainingTree = tree;
            }
        }

        if (largestContainingTree != null) {
            if (largestContainingTree.getIsLeaf()) {
                return findFirstToken(largestContainingTree);
            } else {
                return largestContainingTree.getHead();
            }
        } else {
            return null;
        }
    }
}