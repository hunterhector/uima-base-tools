package edu.cmu.cs.lti.uima.annotator;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/11/15
 * Time: 5:19 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractCollectionReader extends JCasCollectionReader_ImplBase {
    public final static String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public final static String PARAM_ENCODING = "encoding";

    @ConfigurationParameter(mandatory = false, description = "The view name for the golden standard view", name =
            PARAM_GOLD_STANDARD_VIEW_NAME)
    protected String goldStandardViewName;

    @ConfigurationParameter(mandatory = false, description = "Specify the encoding of the input", name = PARAM_ENCODING)
    protected String encoding;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String DEFAULT_GOLD_STANDARD_NAME = "GoldStandard";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }

        if (goldStandardViewName == null) {
            goldStandardViewName = DEFAULT_GOLD_STANDARD_NAME;
        }
    }
}
