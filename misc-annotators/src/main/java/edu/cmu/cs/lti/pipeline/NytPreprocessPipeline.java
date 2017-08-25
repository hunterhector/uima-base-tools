package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.AnnotationRemover;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 7/18/17
 * Time: 7:07 PM
 *
 * @author Zhengzhong Liu
 */
public class NytPreprocessPipeline {
    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String workingDir = argv[0];
        String entityResultDir = argv[1];

        CollectionReaderDescription xmiReader = CustomCollectionReaderFactory.createGzippedXmiReader(
                typeSystemDescription, FileUtils.joinPaths(workingDir, "tokenized"));

        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(
                AnnotationRemover.class, typeSystemDescription,
                AnnotationRemover.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA, AnnotatedNytReader
                        .ABSTRACT_VIEW_NAME},
                AnnotationRemover.PARAM_TARGET_ANNOTATIONS, new Class[]{
                        StanfordCorenlpSentence.class,
                        StanfordCorenlpToken.class
                }
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, AnnotatedNytReader.ABSTRACT_VIEW_NAME
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                workingDir, "parsed");

//        AnalysisEngineDescription linker = AnalysisEngineFactory.createEngineDescription(
//                TagmeEntityLinkerResultAnnotator.class, typeSystemDescription,
//                TagmeEntityLinkerResultAnnotator.PARAM_ENTITY_RESULT_FOLDER, entityResultDir,
//                TagmeEntityLinkerResultAnnotator.PARAM_USE_TOKEN, true,
//                TagmeEntityLinkerResultAnnotator.PARAM_ADDITIONAL_VIEW, AnnotatedNytReader.ABSTRACT_VIEW_NAME
//        );
//
//        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
//                workingDir, "preprocessed");

        new BasicPipeline(xmiReader, true, true, 5, remover, stanfordAnalyzer, writer).run();
    }
}
