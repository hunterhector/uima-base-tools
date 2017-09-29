package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.BasicAnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;
import edu.illinois.cs.cogcomp.srl.SemanticRoleLabeler;
import edu.illinois.cs.cogcomp.srl.config.SrlConfigurator;
import edu.illinois.cs.cogcomp.srl.core.SRLType;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Created with IntelliJ IDEA.
 * Date: 9/25/17
 * Time: 10:25 PM
 *
 * @author Zhengzhong Liu
 */
public class SRLAnnotator extends AbstractLoggingAnnotator {

    private BasicAnnotatorService myPipeline;
    private BasicAnnotatorService pipeline;

    private Map<String, Annotator> initAnnotators(ResourceManager rm, boolean useLazyInitialization) throws
            IOException {
        Map<String, Annotator> viewGenerators = new HashMap<>();

        Properties verbProps = new Properties();
        String verbType = SRLType.Verb.name();
        verbProps.setProperty(SrlConfigurator.SRL_TYPE.key, verbType);
        ResourceManager verbRm = new ResourceManager(verbProps);
        rm = Configurator.mergeProperties(rm, verbRm);
        try {
            SemanticRoleLabeler verbSrl = new SemanticRoleLabeler(rm, useLazyInitialization);
            for (String v : verbSrl.getRequiredViews()) {
                logger.info("Verb srl requires " + v);
            }
            viewGenerators.put(verbSrl.getViewName(), verbSrl);
        } catch (Exception e) {
            throw new IOException("SRL verb cannot init: " + e.getMessage());
        }

        return viewGenerators;
    }

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

//        Map<String, String> nonDefaultValues = new HashMap<>();
//        nonDefaultValues.put(PipelineConfigurator.USE_SRL_VERB.key,
//                Configurator.TRUE);
//        nonDefaultValues.put(PipelineConfigurator.USE_SRL_NOM.key,
//                Configurator.TRUE);
//        nonDefaultValues.put(PipelineConfigurator.USE_SRL_PREP.key,
//                Configurator.TRUE);
//        nonDefaultValues.put(PipelineConfigurator.USE_SRL_COMMA.key,
//                Configurator.TRUE);
//
//        ResourceManager rm = new PipelineConfigurator().getConfig(nonDefaultValues);
//
//        try {
//            Map<String, Annotator> annotators = initAnnotators(rm, true);
//            TextAnnotationBuilder taBldr =
//                    new TokenizerTextAnnotationBuilder(new StatefulTokenizer(true));
//            myPipeline = new SentencePipeline(taBldr, annotators, rm);
//        } catch (IOException | AnnotatorException e) {
//            throw new ResourceInitializationException(e);
//        }


        try {
            pipeline = PipelineFactory.buildPipeline(ViewNames.POS, ViewNames.LEMMA, ViewNames.NER_CONLL,
                    ViewNames.SHALLOW_PARSE, ViewNames.PARSE_STANFORD, ViewNames.SRL_VERB);
        } catch (IOException | AnnotatorException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
//        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
//            try {
//                myPipeline.createAnnotatedTextAnnotation("", "", sentence.getCoveredText());
//            } catch (AnnotatorException e) {
//                e.printStackTrace();
//            }
//        }

        try {
            TextAnnotation ta = pipeline.createAnnotatedTextAnnotation(UimaConvenience.getDocId(aJCas), "body", aJCas
                    .getDocumentText());

            View srlView = ta.getView(ViewNames.SRL_VERB);
            for (Constituent constituent : srlView.getConstituents()) {
                int start = constituent.getStartCharOffset();
                int end = constituent.getEndCharOffset();
                logger.info("Constituent " + constituent.getTokenizedSurfaceForm());
            }
        } catch (AnnotatorException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        String parentInput = args[0]; //"data";
        // Parameters for the writer
        String baseInput = args[1]; //"01_event_tuples"

        String paramBaseOutputDirName = "srl_parsed";

        String typeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(
                SRLAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName);

        SimplePipeline.runPipeline(reader, parser, writer);
    }
}
