package edu.cmu.cs.lti.uima.util;

import org.apache.uima.jcas.tcas.Annotation;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/12/15
 * Time: 5:13 PM
 *
 * @author Zhengzhong Liu
 */
public class Comparators {

    public static class AnnotationBeginComparator<T extends Annotation> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return o1.getBegin() - o2.getBegin();
        }
    }
}
