package edu.cmu.cs.lti.uima.io;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/14/17
 * Time: 10:06 AM
 *
 * @author Zhengzhong Liu
 */
public class IOUtils {
    public static String indexBasedSegFunc(String filename) {
        int docindex = Integer.parseInt(filename);
        int first_layer = docindex / 100000;
        int second_layer = docindex / 1000;
        return String.format("%d/%d/", first_layer, second_layer);
    }
}
