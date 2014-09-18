package edu.cmu.cs.lti.uima.util;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;

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
      return largestContainingTree.getHead();
    } else {
      return null;
    }
  }
}