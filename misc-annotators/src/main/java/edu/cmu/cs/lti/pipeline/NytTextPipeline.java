package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.NytTextWriter;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 7/18/17
 * Time: 7:07 PM
 *
 * @author Zhengzhong Liu
 */
public class NytTextPipeline {
    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String paramInputDir = argv[0];
        String outputDir = argv[1];
        String ignoreFile = argv[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AnnotatedNytReader.class, typeSystemDescription,
                AnnotatedNytReader.PARAM_DATA_PATH, paramInputDir,
                AnnotatedNytReader.PARAM_FILE_EXTENSION, ".tgz",
                AnnotatedNytReader.PARAM_RECURSIVE, true,
                AnnotatedNytReader.PARAM_FULL_PATH_IGNORES, ignoreFile
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_SPLIT_ONLY, true,
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                outputDir, "tokenized");
//        SimplePipeline.runPipeline(reader, stanfordAnalyzer, writer);
        AnalysisEngineDescription textWriter = AnalysisEngineFactory.createEngineDescription(
                NytTextWriter.class, typeSystemDescription,
                NytTextWriter.PARAM_OUTPUT_FILE, new File(outputDir, "nyt.json")
        );

        new BasicPipeline(reader, true, true, 5, stanfordAnalyzer, writer, textWriter).run();
    }
}
