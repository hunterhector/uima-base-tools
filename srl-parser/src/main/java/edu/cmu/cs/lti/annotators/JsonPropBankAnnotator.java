package edu.cmu.cs.lti.annotators;

import com.google.gson.Gson;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.SemanticArgument;
import edu.cmu.cs.lti.script.type.SemanticRelation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Take the output of a JSON SRL Annotator output.
 * Date: 9/3/18
 * Time: 3:42 PM
 * <p>
 * Example:
 * {
 * "ARG0": {
 * "text": "Russia",
 * "span": [
 * 145,
 * 151
 * ]
 * },
 * "V": {
 * "text": "shooting",
 * "span": [
 * 156,
 * 164
 * ]
 * },
 * "ARG1": {
 * "text": "a Malaysia Airlines plane which was hit by a missile over Ukraine, killing all 295 people on board",
 * "span": [
 * 170,
 * 268
 * ]
 * }
 * },
 *
 * @author Zhengzhong Liu
 */
public class JsonPropBankAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_JSON_SRL_INPUT_DIR = "jsonSrlInput";
    @ConfigurationParameter(name = PARAM_JSON_SRL_INPUT_DIR)
    private String srlDataDir;

    public static final String PARAM_INPUT_COMPONENT_NAME = "inputComponentName";
    @ConfigurationParameter(name = PARAM_INPUT_COMPONENT_NAME, mandatory = false)
    private String inputComponentName;

    private Gson gson;

    class SRLDoc {
        String docid;
        List<List<Target>> srl;

        class Target {
            String role;
            String text;
            List<Integer> span;
        }
    }

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        gson = new Gson();

        if (inputComponentName == null) {
            inputComponentName = COMPONENT_ID;
        }

        logger.info("Loading SRL output at " + srlDataDir);
        logger.info("SRL component is " + inputComponentName);

        if (!new File(srlDataDir).exists()) {
            logger.error("Cannot find finished SRL runs, will not add from there.");
        }
    }

    @Override
    public void process(JCas aJCas) {
        UimaConvenience.printProcessLog(aJCas, logger);

        String articleName = UimaConvenience.getArticleName(aJCas);
        File jsonFile = new File(srlDataDir, articleName + ".json");

        if (!jsonFile.exists()) {
            return;
        }

        try {
            FileReader reader = new FileReader(jsonFile);
            SRLDoc doc = gson.fromJson(reader, SRLDoc.class);
            annotateSRL(aJCas, doc);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void annotateSRL(JCas aJCas, SRLDoc doc) {
        for (List<SRLDoc.Target> targets : doc.srl) {
            List<Integer> verbSpan = null;
            Map<String, List<Integer>> argSpans = new HashMap<>();

            for (SRLDoc.Target target : targets) {
                if (target.role.equals("V")) {
                    verbSpan = target.span;
                } else {
                    argSpans.put(target.role, target.span);
                }
            }

            if (verbSpan != null) {
                StanfordCorenlpToken srlHead = UimaConvenience.selectCoveredFirst(
                        aJCas, verbSpan.get(0), verbSpan.get(1), StanfordCorenlpToken.class);

                if (srlHead != null) {
                    List<SemanticRelation> relations = new ArrayList<>();
                    for (Map.Entry<String, List<Integer>> argEntry : argSpans.entrySet()) {
                        String role = argEntry.getKey();
                        SemanticArgument argument = new SemanticArgument(aJCas, argEntry.getValue().get(0),
                                argEntry.getValue().get(1));
                        argument.setHead(UimaNlpUtils.findHeadFromStanfordAnnotation(argument));
                        UimaAnnotationUtils.finishAnnotation(argument, inputComponentName, 0, aJCas);

                        SemanticRelation rel = new SemanticRelation(aJCas);
                        rel.setChild(argument);
                        rel.setHead(srlHead);
                        rel.setPropbankRoleName(role);
                        UimaAnnotationUtils.finishTop(rel, inputComponentName, 0, aJCas);

                        relations.add(rel);
                    }

                    srlHead.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, relations));
                }
            }
        }
    }

    public static void main(String[] args) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String inputPath = args[0];
        String srlInput = args[1];
        String outputBase = args[2];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, inputPath);
        AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(
                JsonPropBankAnnotator.class, typeSystemDescription,
                JsonPropBankAnnotator.PARAM_INPUT_COMPONENT_NAME, "allennlp",
                JsonPropBankAnnotator.PARAM_JSON_SRL_INPUT_DIR, srlInput
        );

        new BasicPipeline(reader, outputBase, "srl", annotator).run();
    }
}
