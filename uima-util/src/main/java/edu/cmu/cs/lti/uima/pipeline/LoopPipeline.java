package edu.cmu.cs.lti.uima.pipeline;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.util.CasCreationUtils;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

/**
 * Created with IntelliJ IDEA.
 * Date: 5/1/15
 * Time: 4:43 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class LoopPipeline {

    protected abstract boolean checkStopCriteria();

    protected abstract void stopActions();

    public void runLoopPipeline(final CollectionReader reader,
                                final AnalysisEngineDescription... descs) throws UIMAException, IOException {
        // Create AAE
        final AnalysisEngineDescription aaeDesc = createEngineDescription(descs);

        // Instantiate AAE
        final AnalysisEngine aae = createEngine(aaeDesc);

        // Create CAS from merged metadata
        final CAS cas = CasCreationUtils.createCas(asList(reader.getMetaData(), aae.getMetaData()));
        reader.typeSystemInit(cas.getTypeSystem());

        try {
            while (!checkStopCriteria()) {
                // Process
                while (reader.hasNext()) {
                    reader.getNext(cas);
                    aae.process(cas);
                    cas.reset();
                }
                // Signal end of processing
                aae.collectionProcessComplete();
            }
            // End of all loops
            stopActions();
        } finally {
            aae.destroy();
        }
    }
}
