package edu.cmu.cs.lti.uima.annotator;

import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A dummy processor that only sleeps for a random while, and add some random annotations, used for testing.
 * Date: 2019-02-14
 * Time: 10:20
 *
 * @author Zhengzhong Liu
 */
public class SleepDummy extends AbstractLoggingAnnotator {
    public static final String PARAM_DUMMY_NAME = "dummyName";

    @ConfigurationParameter(name = PARAM_DUMMY_NAME)
    private String dummyName;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        logger.info("I am " + dummyName);
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        int textLength = jCas.getDocumentText().length();

        Random rand = new Random();

        for (int i = 0; i < 10; i++) {
            int offset1 = rand.nextInt(textLength);
            int offset2 = rand.nextInt(textLength);

            int begin = Math.min(offset1, offset2);
            int end = Math.max(offset1, offset2);

            EntityMention randomMention = new EntityMention(jCas, begin, end);
            UimaAnnotationUtils.finishAnnotation(randomMention, COMPONENT_ID, i, jCas);
        }

        // Now just sleep for a random while.
        int n = rand.nextInt(500) + 1;
        try {
//            logger.info(String.format("Dummy [%s] will sleep for %d milliseconds on doc [%s]", dummyName, n,
//                    UimaConvenience.getDocumentName(jCas)));
            TimeUnit.MILLISECONDS.sleep(n);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info(dummyName + " is finished.");
    }


    public static void main(String[] args) throws UIMAException {
        String inputPath = args[0];
        String outputPath = args[1];

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputPath
        );

        int numDummies = 6;
        AnalysisEngineDescription[] dummies = new AnalysisEngineDescription[numDummies];
        for (int i = 0; i < numDummies; i++) {
            dummies[i] = AnalysisEngineFactory.createEngineDescription(SleepDummy.class, typeSystemDescription,
                    SleepDummy.PARAM_DUMMY_NAME, String.format("Tester_%d", i),
                    SleepDummy.MULTI_THREAD, true
            );
        }
        new BasicPipeline(reader, outputPath, "test_out", 1, dummies).run();
    }
}
