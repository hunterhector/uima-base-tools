package edu.illinois.cs.cogcomp.annotation;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/25/17
 * Time: 11:47 PM
 *
 * @author Zhengzhong Liu
 */
public class SRLTester {
    private void test() throws IOException, AnnotatorException {
        BasicAnnotatorService pipeline = PipelineFactory.buildPipeline(ViewNames.SRL_VERB);

        Annotator annotator = pipeline.viewProviders.get(ViewNames.SRL_VERB);

        for (String v : annotator.getRequiredViews()) {
            System.out.println("Requiring " + v);
        }

//        Requiring POS
//        Requiring NER_CONLL
//        Requiring LEMMA
//        Requiring SHALLOW_PARSE
//        Requiring PARSE_STANFORD

        // TODO move our own annotations to these views.
    }

    public static void main(String[] argv) throws IOException, AnnotatorException {
        SRLTester tester = new SRLTester();
        tester.test();
    }
}
