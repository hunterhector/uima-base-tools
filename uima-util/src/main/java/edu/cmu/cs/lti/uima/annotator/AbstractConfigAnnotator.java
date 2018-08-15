package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.utils.Configuration;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/8/18
 * Time: 12:07 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractConfigAnnotator extends AbstractLoggingAnnotator {
    protected static Configuration config;

    public static void setConfig(Configuration config) {
        AbstractConfigAnnotator.config = config;
    }
}
