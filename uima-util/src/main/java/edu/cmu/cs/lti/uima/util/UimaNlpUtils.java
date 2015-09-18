package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.cmu.cs.lti.script.type.Word;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.List;

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
        mention.setHead(findHeadFromTreeAnnotation(mention));
        return mention;
    }

    public static StanfordCorenlpToken findFirstToken(JCas aJCas, int begin, int end) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end)) {
            return token;
        }
        return null;
    }


    public static StanfordCorenlpToken findFirstToken(Annotation anno) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, anno)) {
            return token;
        }
        return null;
    }

    public static Word findFirstWord(JCas jcas, int begin, int end, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(jcas, Word.class, begin, end)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }

    public static Word findFirstWord(Annotation anno, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(Word.class, anno)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }

    public static StanfordCorenlpToken findHeadFromRange(JCas view, int begin, int end) {
        StanfordTreeAnnotation largestContainingTree = findLargest(JCasUtil.selectCovered(view,
                StanfordTreeAnnotation.class, begin, end));
        return findHeadFromTree(largestContainingTree);
    }

    public static StanfordCorenlpToken findHeadFromTreeAnnotation(Annotation anno) {
        return findHeadFromTree(findLargestContainingTree(anno));
    }

    public static StanfordTreeAnnotation findLargestContainingTree(Annotation anno) {
        return findLargest(JCasUtil.selectCovered(StanfordTreeAnnotation.class, anno));
    }

    public static StanfordCorenlpToken findHeadFromTree(StanfordTreeAnnotation tree) {
        if (tree != null) {
            if (tree.getIsLeaf()) {
                return findFirstToken(tree);
            } else {
                return tree.getHead();
            }
        } else {
            return null;
        }
    }

    public static <T extends Annotation> T findLargest(List<T> annos) {
        T largestAnno = null;
        for (T anno : annos) {
            if (largestAnno == null) {
                largestAnno = anno;
            } else if (largestAnno.getEnd() - largestAnno.getBegin() < anno.getEnd() - anno
                    .getBegin()) {
                largestAnno = anno;
            }
        }
        return largestAnno;
    }
}