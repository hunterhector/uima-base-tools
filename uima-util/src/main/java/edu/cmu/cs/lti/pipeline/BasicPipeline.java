package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/15
 * Time: 5:22 PM
 *
 * @author Zhengzhong Liu
 */
public class BasicPipeline {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private CollectionReaderDescription readerDescription;
    private CollectionReaderDescription outputReader;

    private CollectionReader cReader;
//    private CAS mergedCas;

//    private AnalysisEngineDescription aggregateAnalysisEngineDesc;
//    private AnalysisEngine aggregateAnalysisEngine;

    private AnalysisEngineDescription[] analysisEngineDescs;
    private AnalysisEngine[] engines;

    private boolean withOutput;

    private File fullOutputDir;

    private boolean withStats;

    private ProcessTrace performanceTrace = null;

    // Must be at least 1.
    private final int maxThread = 1;

    private final BlockingQueue<CAS> sharedQueue = new ArrayBlockingQueue<>(maxThread);
    private ExecutorService executor = Executors.newFixedThreadPool(maxThread + 1);

    private Queue<CAS> availableCASes = new ArrayDeque<>();

    private boolean allFilesRead = false;

    public BasicPipeline(ProcessorWrapper wrapper) throws UIMAException,
            CpeDescriptorException, SAXException, IOException {
        this(wrapper, false, null, null);
    }

    public BasicPipeline(ProcessorWrapper wrapper, String workingDir, String outputDir) throws UIMAException,
            CpeDescriptorException, SAXException, IOException {
        this(wrapper, false, null, null);
    }

    public BasicPipeline(ProcessorWrapper wrapper, boolean withStats, String workingDir, String outputDir) throws
            UIMAException, CpeDescriptorException, SAXException, IOException {
        readerDescription = wrapper.getCollectionReader();
        AnalysisEngineDescription[] processors = wrapper.getProcessors();
        AnalysisEngineDescription[] engineDescriptions;

        if (workingDir != null && outputDir != null) {
            AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(workingDir, outputDir);
            engineDescriptions = ArrayUtils.add(processors, writer);

            outputReader = CustomCollectionReaderFactory.createXmiReader(workingDir, outputDir);
            withOutput = true;

            fullOutputDir = new File(workingDir, outputDir);
        } else {
            withOutput = false;
            engineDescriptions = processors;
        }

        this.withStats = withStats;
//        aggregateAnalysisEngineDesc = createEngineDescription(engineDescriptions);
        analysisEngineDescs = engineDescriptions;
    }

    /**
     * Run processor from provided reader.
     *
     * @throws IOException
     * @throws UIMAException
     */
    public void run() throws IOException, UIMAException {
        initialize();
        try {
            process();
        } catch (ExecutionException | InterruptedException e) {
            throw new UIMAException(e);
        }
        complete();

        if (withStats) {
            if (performanceTrace != null) {
                logger.info("Process complete, the full processing trace is as followed:");
                logger.info(performanceTrace.toString());
            }
        }
    }


    /**
     * Run processor from provided reader, write processed CAS as XMI to the given directory.
     *
     * @return A reader description for the processed output.
     * @throws UIMAException
     * @throws IOException
     */
    public CollectionReaderDescription runWithOutput() throws UIMAException, IOException {
        if (withOutput) {
            logger.info("Processing with output at : " + fullOutputDir.getCanonicalPath());
            run();
            return outputReader;
        } else {
            throw new IllegalAccessError("Pipeline is not initialized with output.");
        }
    }


    /**
     * Initialize the processor so that process can be called multiple times.
     *
     * @throws ResourceInitializationException
     */
    public void initialize() throws ResourceInitializationException {
        // Create Reader.
        cReader = CollectionReaderFactory.createReader(readerDescription);

        List<ResourceMetaData> metaData = new ArrayList<>();
        metaData.add(cReader.getMetaData());

        engines = new AnalysisEngine[analysisEngineDescs.length];
        for (int i = 0; i < analysisEngineDescs.length; i++) {
            AnalysisEngine engine = createEngine(analysisEngineDescs[i]);
            engines[i] = engine;
            metaData.add(engine.getMetaData());
        }

        // Create multiple cas containers.
        TypeSystem typeSystem = null;
        for (int i = 0; i < maxThread; i++) {
            CAS cas = CasCreationUtils.createCas(metaData);
            availableCASes.add(CasCreationUtils.createCas(metaData));
            typeSystem = cas.getTypeSystem();
        }

        logger.info("Created " + availableCASes.size() + " cas for processing.");

        // The reader can take type system from any CAS (they are the same.).
        cReader.typeSystemInit(typeSystem);
    }

    /**
     * Process the next CAS in the reader.
     */
    public void process() throws ExecutionException, InterruptedException {
        Future<Integer> docsProcessed = runProducer();
        performanceTrace = runCasConsumers(engines);
        int numProcessed = docsProcessed.get();
        logger.info("Got results from producer, all file read is set to " + allFilesRead);
        allFilesRead = true;
        logger.info("Number documents processed : " + numProcessed);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdown();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private class ProcessElement {
        CAS cas;
        int level;
        int maxLevel;

        ProcessElement(CAS cas, int maxLevel) {
            this.cas = cas;
            level = 0;
            this.maxLevel = maxLevel;
        }

        boolean increment() {
            level += 1;
            return level < maxLevel;
        }
    }

    /**
     * This method runs the consumers to process the CASes. It will be blocked until
     *
     * @param engines
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private ProcessTrace runCasConsumers(AnalysisEngine[] engines) throws ExecutionException, InterruptedException {
        Queue<ProcessElement> processedCas = new ArrayBlockingQueue<>(maxThread);

        // Now submit multiple jobs.
        ProcessTrace processTrace = new ProcessTrace_impl();

        // The consumer manger thread that submit jobs.
        executor.submit(
                () -> {
                    // TODO: Need to find a way to terminate the jobs.
                    // The actual workers doing the job.
                    executor.execute(
                            () -> {
                                while (true) {
                                    logger.info("Running from consumer thread " + Thread.currentThread().getName());
                                    logger.info("Shared Queue size " + sharedQueue.size());

                                    ProcessTrace t = null;

                                    if (allFilesRead && sharedQueue.isEmpty()) {
                                        logger.info("All files read is set to " + allFilesRead);
                                        logger.info("All jobs finished.");
                                        break;
                                    }

                                    try {
                                        // Take a element from the task queue, or from the middle products.
                                        ProcessElement executionTuple;
                                        if (processedCas.isEmpty()) {
                                            executionTuple = new ProcessElement(sharedQueue.take(), engines.length);
                                        } else {
                                            executionTuple = processedCas.poll();
                                        }

                                        logger.info("Taking execution tuple: " + executionTuple);

                                        // Run the correct engine using one of the available thread in the executor
                                        // pool.
                                        CAS cas = executionTuple.cas;
                                        int level = executionTuple.level;
                                        t = engines[level].process(cas);

                                        logger.info("Tuple executed with engine " + engines[level].getMetaData()
                                                .getName() + " at level " + level);

                                        if (executionTuple.increment()) {
                                            // If the tuple is still not finished, put it back to the list again.
                                            processedCas.add(executionTuple);
                                            logger.info("Not finished yet, place back to processed cas");
                                        } else {
                                            // Finished cas are reused by putting back to the available pool.
                                            cas.reset();
                                            availableCASes.add(cas);
                                            logger.info("Finished, put into available cas.");
                                        }
                                    } catch (InterruptedException | AnalysisEngineProcessException e) {
                                        e.printStackTrace();
                                    }

                                    combineTrace(processTrace, t);
                                }
                            }
                    );
                }
        );

        return processTrace;
    }

    /**
     * Ensure aggregation is synchronized.
     *
     * @param base
     * @param newTrace
     */
    private synchronized void combineTrace(ProcessTrace base, ProcessTrace newTrace) {
        base.aggregate(newTrace);
    }

    /**
     * Produce CAS items for processor to run. The producer will be blocked for the following two reasons:
     * 1. Shared Queue is full, consumers haven't used up the queue elements yet.
     * 2. No available cas container, consumers haven't returned the containers yet.
     */
    private Future<Integer> runProducer() {
        return executor.submit(
                () -> {
                    AtomicInteger numDocuments = new AtomicInteger();
                    logger.info("Producer thread started.");
                    try {
                        while (cReader.hasNext()) {
                            while (availableCASes.isEmpty()) {
                                // When there is no available CAS, that means all of them are being processed by the
                                // consumer.
                                // We should wait until they are ready;

                                logger.info("Checking for next available cas.");
                            }

                            // Get a available container.
                            CAS currentContainer = availableCASes.poll();
                            // Fill the container with actual document input.
                            cReader.getNext(currentContainer);
                            // Put element for processed in the queue. This will block the thread if necessary.
                            sharedQueue.offer(currentContainer);

                            numDocuments.incrementAndGet();
                            logger.info("Take a cas container for the next document, available cas number : " +
                                    availableCASes.size() + ", task queue is " + sharedQueue);
                        }
                    } catch (IOException | CollectionException e) {
                        e.printStackTrace();
                    }

                    return numDocuments.get();
                }
        );
    }

//    /**
//     * Process the next CAS in the reader.
//     */
//    public void process() {
//        try {
//            // Process.
//            while (cReader.hasNext()) {
//                cReader.getNext(mergedCas);
//                ProcessTrace trace = aggregateAnalysisEngine.process(mergedCas);
//                mergedCas.reset();
//
//                if (aggregateTrace == null) {
//                    aggregateTrace = trace;
//                } else {
//                    aggregateTrace.aggregate(trace);
//                }
//            }
//        } catch (AnalysisEngineProcessException | CollectionException | IOException e) {
//            e.printStackTrace();
//            // Destroy.
//            aggregateAnalysisEngine.destroy();
//        }
//    }

    /**
     * Complete the process.
     *
     * @throws AnalysisEngineProcessException
     */
    public void complete() throws AnalysisEngineProcessException {
        try {
            // Signal end of processing.
//            aggregateAnalysisEngine.collectionProcessComplete();
            for (AnalysisEngine engine : engines) {
                engine.collectionProcessComplete();
            }
        } finally {
            // Destroy.
//            aggregateAnalysisEngine.destroy();
            for (AnalysisEngine engine : engines) {
                engine.destroy();
            }
        }
    }
}