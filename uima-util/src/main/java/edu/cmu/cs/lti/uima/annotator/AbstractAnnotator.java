package edu.cmu.cs.lti.uima.annotator;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:11 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AbstractAnnotator extends JCasAnnotator_ImplBase {
    public final static String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public final static String PARAM_ENCODING = "encoding";

    @ConfigurationParameter(mandatory = false, description = "The view name for the golden standard view", name =
            PARAM_GOLD_STANDARD_VIEW_NAME)
    protected String goldStandardViewName;

    @ConfigurationParameter(mandatory = false, description = "Specify the encoding of the input", name = PARAM_ENCODING)
    protected String encoding;

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String DEFAULT_GOLD_STANDARD_NAME = "GoldStandard";

    public final String COMPONENT_ID = this.getClass().getSimpleName();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        if (goldStandardViewName == null) {
            goldStandardViewName = DEFAULT_GOLD_STANDARD_NAME;
        }
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }
    }

}
