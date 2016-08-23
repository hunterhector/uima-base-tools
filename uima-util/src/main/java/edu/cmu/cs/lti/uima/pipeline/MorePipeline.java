package edu.cmu.cs.lti.uima.pipeline;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.util.CasCreationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.util.LifeCycleUtil.close;
import static org.apache.uima.fit.util.LifeCycleUtil.destroy;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/3/16
 * Time: 10:20 AM
 *
 * @author Zhengzhong Liu
 */
public class MorePipeline {
    public static void runPipelineWithMultiReaderDesc(final Iterable<CollectionReaderDescription> readerDescs,
                                                      final AnalysisEngineDescription... descs) throws UIMAException, IOException {
        // Create the components
        List<CollectionReader> readers = new ArrayList<>();
        for (CollectionReaderDescription readerDesc : readerDescs) {
            readers.add(CollectionReaderFactory.createReader(readerDesc));
        }

        try {
            // Run the pipeline
            runPipelineWithMultiReaders(readers, descs);
        } finally {
            for (CollectionReader reader : readers) {
                close(reader);
                destroy(reader);
            }
        }
    }

    public static void runPipelineWithMultiReaders(final Iterable<CollectionReader> readers,
                                                      final AnalysisEngineDescription... descs)
            throws UIMAException, IOException {
        // Create AAE
        final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);

        // Instantiate AAE
        final AnalysisEngine aae = createEngine(aaeDesc);

        // Create CAS from merged metadata
        try {
            for (CollectionReader reader : readers) {
                final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()));
                reader.typeSystemInit(cas.getTypeSystem());
                // Process
                while (reader.hasNext()) {
                    reader.getNext(cas);
                    aae.process(cas);
                    cas.reset();
                }
            }
            // Signal end of processing
            aae.collectionProcessComplete();
        } finally {
            // Destroy
            aae.destroy();
        }

    }

    
}
