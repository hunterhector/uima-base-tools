package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.UiucSrlArgument;
import edu.cmu.cs.lti.script.type.UiucSrlPredicate;
import edu.cmu.cs.lti.script.type.UiucSrlRelation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.illinois.cs.cogcomp.annotation.*;
import edu.illinois.cs.cogcomp.chunker.main.ChunkerAnnotator;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
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
import org.uimafit.util.FSCollectionFactory;

import java.io.IOException;
import java.util.*;
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
            logger.info("Finished adding view builders");
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

        // SRL_NOM
        Properties nomProps = new Properties();
        String nomType = SRLType.Nom.name();
        nomProps.setProperty(SrlConfigurator.SRL_TYPE.key, nomType);
        ResourceManager nomRm = new ResourceManager(nomProps);
        rm = Configurator.mergeProperties(rm, nomRm);

        // We need DEPENDENCY view for this, we don't want to process too much for now.
//        // SRL_PREP
//        PrepSRLAnnotator prepSRLAnnotator = new PrepSRLAnnotator();
//        viewGenerators.put(ViewNames.SRL_PREP, prepSRLAnnotator);

//        // SRL_COMMA
//        CommaLabeler commaLabeler = new CommaLabeler();
//        viewGenerators.put(ViewNames.SRL_COMMA, commaLabeler);

        try {
            SemanticRoleLabeler nomSrl = new SemanticRoleLabeler(rm, false);
            viewGenerators.put(ViewNames.SRL_NOM, nomSrl);
        } catch (Exception e) {
            e.printStackTrace();
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
            getSRLFromView(ta, aJCas, ViewNames.SRL_VERB);
            getSRLFromView(ta, aJCas, ViewNames.SRL_NOM);
        } catch (AnnotatorException e) {
            e.printStackTrace();
        }

        // Now finish using this.
        docCas.remove(docid);
    }

    private void getSRLFromView(TextAnnotation ta, JCas aJCas, String viewName) {
        View srlView = ta.getView(viewName);

        ArrayListMultimap<UiucSrlPredicate, Relation> predArgs = ArrayListMultimap.create();
        Map<Constituent, UiucSrlArgument> argMap = new HashMap<>();

        for (Constituent constituent : srlView.getConstituents()) {
            List<Relation> relations = constituent.getOutgoingRelations();

            if (constituent.getLabel().equals("Predicate")) {
                UiucSrlPredicate predicate = new UiucSrlPredicate(aJCas, constituent.getStartCharOffset(),
                        constituent.getEndCharOffset());
                UimaAnnotationUtils.finishAnnotation(predicate, COMPONENT_ID, 0, aJCas);

                for (Relation relation : relations) {
                    predArgs.put(predicate, relation);
                    Constituent target = relation.getTarget();

                    if (!argMap.containsKey(target)) {
                        UiucSrlArgument argument = new UiucSrlArgument(aJCas,
                                target.getStartCharOffset(), target.getEndCharOffset());
                        UimaAnnotationUtils.finishAnnotation(argument, COMPONENT_ID, 0, aJCas);
                        argMap.put(target, argument);
                    }
                }
            }
        }

        for (Map.Entry<UiucSrlPredicate, Collection<Relation>> predArg : predArgs.asMap().entrySet()) {
            UiucSrlPredicate predicate = predArg.getKey();

            List<UiucSrlRelation> uimaRelations = new ArrayList<>();
            for (Relation relation : predArg.getValue()) {
                UiucSrlRelation uimaRelation = new UiucSrlRelation(aJCas);
                uimaRelation.setSemanticAnnotation(relation.getRelationName());
                uimaRelation.setConfidence(relation.getScore());
                uimaRelation.setPropbankRoleName(relation.getRelationName());
                UiucSrlArgument uimaArg = argMap.get(relation.getTarget());
                uimaRelation.setChild(uimaArg);
                uimaRelation.setHead(predicate);
                UimaAnnotationUtils.finishTop(uimaRelation, COMPONENT_ID, 0, aJCas);
                uimaRelations.add(uimaRelation);
            }
            predicate.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas, uimaRelations));
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
