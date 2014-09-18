package edu.cmu.cs.lti.uima.util;

import java.util.Collection;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.ComponentTOP;

public class UimaAnnotationUtils {
  public static void finishAnnotation(ComponentAnnotation anno, int begin, int end,
          String componentId, String id, JCas aJCas) {
    anno.setBegin(begin);
    anno.setEnd(end);
    anno.setComponentId(componentId);
    anno.setId(id);
    anno.addToIndexes(aJCas);
  }

  public static void finishAnnotation(ComponentAnnotation anno, String componentId, String id,
          JCas aJCas) {
    anno.setComponentId(componentId);
    anno.setId(id);
    anno.addToIndexes(aJCas);
  }

  public static void finishAnnotation(ComponentAnnotation anno, int begin, int end,
          String componentId, int id, JCas aJCas) {
    anno.setBegin(begin);
    anno.setEnd(end);
    anno.setComponentId(componentId);
    anno.setId(Integer.toString(id));
    anno.addToIndexes();
  }

  public static void finishAnnotation(ComponentAnnotation anno, String componentId, int id,
          JCas aJCas) {
    anno.setComponentId(componentId);
    anno.setId(Integer.toString(id));
    anno.addToIndexes(aJCas);
  }

  public static void finishTop(ComponentTOP anno, String componentId, String id, JCas aJCas) {
    anno.setComponentId(componentId);
    anno.setId(id);
    anno.addToIndexes(aJCas);
  }

  public static void finishTop(ComponentTOP anno, String componentId, int id, JCas aJCas) {
    anno.setComponentId(componentId);
    anno.setId(Integer.toString(id));
    anno.addToIndexes(aJCas);
  }

  public static <T extends Annotation> T selectCoveredSingle(Annotation anno, Class<T> clazz) {
    T singleAnno = null;
    List<T> coveredAnnos = JCasUtil.selectCovered(clazz, anno);
    if (coveredAnnos.size() > 1) {
      System.err.println(String.format(
              "Annotation [%s] contains more than one subspan of type [%s]", anno.getCoveredText(),
              clazz.getSimpleName()));
    }

    if (coveredAnnos.size() == 0) {
      System.err.println(String.format(
              "Annotation [%s] contains does not have subspans of type [%s]",
              anno.getCoveredText(), clazz.getSimpleName()));
    } else {
      singleAnno = coveredAnnos.get(0);
    }

    return singleAnno;
  }

  public static <T extends ComponentAnnotation> void assignAnnotationIds(Collection<T> annos) {
    int id = 0;
    for (ComponentAnnotation anno : annos) {
      anno.setId(Integer.toString(id++));
    }
  }

  public static <T extends ComponentTOP> void assignTopIds(Collection<T> annos) {
    int id = 0;
    for (ComponentTOP anno : annos) {
      anno.setId(Integer.toString(id++));
    }
  }
}
