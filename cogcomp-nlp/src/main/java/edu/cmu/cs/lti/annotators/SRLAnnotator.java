package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.illinois.cs.cogcomp.annotation.*;
import edu.illinois.cs.cogcomp.chunker.main.ChunkerAnnotator;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.ner.NERAnnotator;
import edu.illinois.cs.cogcomp.ner.NerAnnotatorManager;
import edu.illinois.cs.cogcomp.nlp.utility.UimaTokenTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.pipeline.main.SentencePipeline;
import edu.illinois.cs.cogcomp.srl.SemanticRoleLabeler;
import edu.illinois.cs.cogcomp.srl.config.SrlConfigurator;
import edu.illinois.cs.cogcomp.srl.core.SRLType;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created with IntelliJ IDEA.
 * Date: 9/25/17
 * Time: 10:25 PM
 *
 * @author Zhengzhong Liu
 */
public class SRLAnnotator extends AbstractLoggingAnnotator {

    private BasicAnnotatorService pipeline;

    // This should not be public, but it is simpler this way.
    public static ConcurrentMap<String, JCas> docCas;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        docCas = new ConcurrentHashMap<>();

        try {
            logger.info("Adding view builders.");
            pipeline = addViewBuilders();
            logger.info("Finished.");
        } catch (AnnotatorException e) {
            e.printStackTrace();
        }
    }

    private SentencePipeline addViewBuilders() throws AnnotatorException {
        // Build tokens first
        UimaTokenTextAnnotationBuilder tokenBuilder = new UimaTokenTextAnnotationBuilder();

        Map<String, Annotator> viewGenerators = new HashMap<>();
        ResourceManager rm = new AnnotatorServiceConfigurator().getDefaultConfig();

        // POSAnnotator
        viewGenerators.put(ViewNames.POS, new UimaPOSAnnotator());
        // IllinoisLemmatizer
        viewGenerators.put(ViewNames.LEMMA, new UimaLemmaAnnotator());
        // NERAnnotator
        NERAnnotator nerConll = NerAnnotatorManager.buildNerAnnotator(rm, ViewNames.NER_CONLL);
//        viewGenerators.put(ViewNames.NER_CONLL, new UimaNerAnnotator());
        viewGenerators.put(ViewNames.NER_CONLL, nerConll);

        // ChunkerAnnotator, use the original UIUC chunker.
        viewGenerators.put(ViewNames.SHALLOW_PARSE, new ChunkerAnnotator());
        // ParserAnnotator
        viewGenerators.put(ViewNames.PARSE_STANFORD, new UimaStanfordDepAnnotator());

        // SRL_VERB
        Properties verbProps = new Properties();
        String verbType = SRLType.Verb.name();
        verbProps.setProperty(SrlConfigurator.SRL_TYPE.key, verbType);
        ResourceManager verbRm = new ResourceManager(verbProps);
        rm = Configurator.mergeProperties(rm, verbRm);
        try {
            SemanticRoleLabeler verbSrl = new SemanticRoleLabeler(rm, false);
            viewGenerators.put(ViewNames.SRL_VERB, verbSrl);
        } catch (Exception e) {
            throw new AnnotatorException("SRL verb cannot init: " + e.getMessage());
        }

        return new SentencePipeline(tokenBuilder, viewGenerators, rm);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String docid = UimaConvenience.getArticleName(aJCas);

        docCas.putIfAbsent(docid, aJCas);

        UimaConvenience.printProcessLog(aJCas);

        try {
            TextAnnotation ta = pipeline.createAnnotatedTextAnnotation("Corpus",
                    docid, aJCas.getDocumentText());

            View srlView = ta.getView(ViewNames.SRL_VERB);
            for (Constituent constituent : srlView.getConstituents()) {
                int start = constituent.getStartCharOffset();
                int end = constituent.getEndCharOffset();
                logger.info("Constituent " + constituent.getTokenizedSurfaceForm());
            }
        } catch (AnnotatorException e) {
            e.printStackTrace();
        }

        // Now finish using this.
        docCas.remove(docid);
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

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, parentInput,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, baseInput,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(
                SRLAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName);

        SimplePipeline.runPipeline(reader, parser, writer);
    }
}
