package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.io.writer.StepBasedDirGzippedXmiWriter;
import org.apache.commons.lang.mutable.MutableBoolean;
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
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;

/**
 * Implements a parallel pipeline. This is different from the default UIMA parallel mechanism that it run the
 *
 * @author Zhengzhong Liu
 * @<<code>process(JCas)</code> step in a multi-thread manner. This save memory and preparation time needed for the
 * @<code>initialize(final UimaContext context)</code> step. This require the process step to be thread safe, and it
 * should not modify the initialized variables. The judgement is left to the caller.
 * <p>
 * The @<code>process(JCas)</code> step is called synchronized by default. To use the actual multi-thread
 * computation, one should set the @<code>MULTI_THREAD</code> configuration parameter in
 * @<code>AbstractAnnotator</code> to true.
 */
public class BasicPipeline {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private CollectionReaderDescription readerDescription;
    private CollectionReaderDescription outputReader;

    private CollectionReader cReader;

    private AnalysisEngineDescription[] analysisEngineDescs;
    private AnalysisEngine[] engines;

    private boolean withStats;

    // Must be at least 1.
    private final int numWorkers;

    private final BlockingQueue<ProcessElement> taskQueue;

    private final boolean robust;

    //    private ExecutorService executor;
    private ThreadPoolExecutor executor;

    private BlockingQueue<CAS> availableCASes;

    private int numInputFiles;

    public BasicPipeline(CollectionReaderDescription reader, AnalysisEngineDescription... processors) throws
            UIMAException {
        this(reader, false, true, 8, null, null, processors);
        logger.info("Using a default of 8 workers");

    }

    public BasicPipeline(CollectionReaderDescription reader, int numWorkers, AnalysisEngineDescription... processors)
            throws UIMAException, CpeDescriptorException, SAXException, IOException {
        this(reader, false, true, numWorkers, null, null, processors);
    }


    public BasicPipeline(CollectionReaderDescription reader, String workingDir, String outputDir,
                         AnalysisEngineDescription... processors) throws
            UIMAException {
        this(reader, false, true, 8, workingDir, outputDir, processors);
        logger.info("Using a default of 8 workers");
    }

    public BasicPipeline(CollectionReaderDescription reader, String workingDir, String outputDir, int numWorkers,
                         AnalysisEngineDescription... processors) throws UIMAException {
        this(reader, false, true, numWorkers, workingDir, outputDir, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         AnalysisEngineDescription... processors) throws UIMAException {
        this(reader, robust, withStats, numWorkers, null, null, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         String workingDir, String outputDir, AnalysisEngineDescription... processors) throws
            UIMAException {
        this(reader, robust, withStats, numWorkers, workingDir, outputDir, false, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         String workingDir, String outputDir, boolean zipOutput,
                         AnalysisEngineDescription... processors)
            throws UIMAException {
        this(reader, robust, withStats, numWorkers, getWriter(workingDir, outputDir, zipOutput), workingDir, outputDir,
                processors
        );
    }

    private static AnalysisEngineDescription getWriter(String workingDir, String outputDir, boolean zipOutput)
            throws ResourceInitializationException {
        if (outputDir == null || workingDir == null) {
            return null;
        }

        if (zipOutput) {
            return AnalysisEngineFactory.createEngineDescription(
                    StepBasedDirGzippedXmiWriter.class,
                    StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, workingDir,
                    StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, outputDir,
                    AbstractLoggingAnnotator.MULTI_THREAD, true
            );
        } else {
            return CustomAnalysisEngineFactory.createXmiWriter(workingDir, outputDir);
        }
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         AnalysisEngineDescription outputWriter, String workingDir, String outputDir,
                         AnalysisEngineDescription... processors)
            throws UIMAException {
        readerDescription = reader;
        AnalysisEngineDescription[] engineDescriptions;

        this.robust = robust;
        this.numWorkers = numWorkers;
        if (robust) {
            logger.info("Set to robust mode, will ignore all exceptions and continue.");
        }

        if (outputWriter == null) {
            engineDescriptions = processors;
        } else {
            engineDescriptions = ArrayUtils.add(processors, outputWriter);
        }

        this.withStats = withStats;
        analysisEngineDescs = engineDescriptions;

        outputReader = CustomCollectionReaderFactory.createRecursiveXmiReader(workingDir, outputDir);

        // Number of threads for the workers, one additional for producer, one additional for dispatcher.
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numWorkers + 2);
        availableCASes = new ArrayBlockingQueue<>(numWorkers);
        taskQueue = new ArrayBlockingQueue<>(numWorkers);
    }


    public static BasicPipeline getRobust(CollectionReaderDescription reader, String workingDir,
                                          String outputDir, int numWorkers, AnalysisEngineDescription... processors)
            throws UIMAException {
        return new BasicPipeline(reader, true, true, numWorkers, workingDir, outputDir, processors);
    }

    /**
     * Run processor from provided reader.
     *
     * @throws UIMAException
     */
    public BasicPipeline run() throws UIMAException {
        initialize();
        try {
            process();
        } catch (ExecutionException | InterruptedException e) {
            throw new UIMAException(e);
        }
        complete();

        return this;
    }

    public CollectionReaderDescription getOutput() {
        return outputReader;
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
        for (int i = 0; i < numWorkers; i++) {
            CAS cas = CasCreationUtils.createCas(metaData);
            availableCASes.offer(CasCreationUtils.createCas(metaData));
            typeSystem = cas.getTypeSystem();
        }

        logger.info("Created " + availableCASes.size() + " CASes for processing.");

        // The reader can take type system from any CAS (they are the same.).
        cReader.typeSystemInit(typeSystem);
    }

    /**
     * Process the next CAS in the reader.
     */
    public void process() throws ExecutionException, InterruptedException {
        Future<Integer> docsProcessed = runProducer();
        ProcessTrace performanceTrace = runCasConsumers(engines);

        logger.info("Number documents submitted: " + numInputFiles);

        // Waiting for the last batch of jobs to terminate.
        logger.info("Waiting for jobs to terminate.");

        logger.debug("number of task in queue: " + taskQueue.size());
        logger.debug("number of available case: " + availableCASes.size());

        numInputFiles = docsProcessed.get();

        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    logger.error("Executor Pool did not terminate.");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        if (withStats) {
            if (performanceTrace != null) {
                logger.info("Process complete, the full processing trace is as followed:");
                logger.info("\n" + performanceTrace.toString());
            }
        }
    }

    private class ProcessElement {
        CAS cas;
        AtomicInteger step;
        final int maxStep;

        // The poison flag is to mark the last element in the queue.
        // We only set up a dummy poison element, it does not contain any thing to process.
        boolean isPoison;

        /**
         * Create a poison dummy element, not for processing.
         */
        ProcessElement() {
            isPoison = true;
            maxStep = -1;
        }

        /**
         * A new process element will all have a step number of 0, which means it is not processed before.
         *
         * @param cas      The actual content.
         * @param maxLevel Total number of process needed to get it done.
         */
        ProcessElement(CAS cas, int maxLevel) {
            this.cas = cas;
            step = new AtomicInteger(0);
            this.maxStep = maxLevel;
            isPoison = false;
        }

        /**
         * Called when processed, increment the step by 1.
         *
         * @return whether the element is processed enough times so that it is done.
         */
        boolean increment() {
            int newStep = step.incrementAndGet();
            return newStep < maxStep;
        }
    }

    /**
     * This method runs the consumers to process the CASes. It will be blocked until
     *
     * @param engines
     * @return
     */
    private ProcessTrace runCasConsumers(AnalysisEngine[] engines) {
        // Now submit multiple jobs.
        ProcessTrace processTrace = new ProcessTrace_impl();

        Object lock = new Object();

        List<Function<CAS, ProcessTrace>> analysisFunctions = new ArrayList<>();

//        List<AtomicInteger> processedCounters = new ArrayList<>();

        for (AnalysisEngine engine : engines) {
            boolean multiThread = (boolean) engine.getConfigParameterValue(AbstractAnnotator.MULTI_THREAD);

//            processedCounters.add(new AtomicInteger());

            Function<CAS, ProcessTrace> func = cas -> {
                try {
                    if (cas == null) {
                        // Does this happen? Why?
                        logger.info("Get a null cas somehow");
                        return null;
                    }

                    if (multiThread) {
                        return engine.process(cas);
                    } else {
                        synchronized (lock) {
                            return engine.process(cas);
                        }
                    }
                } catch (Throwable e) {
                    // Aggressively catch everything.
                    e.printStackTrace();
                    if (robust) {
                        logger.info("Ignoring errors.");
                        // Ignore any exceptions just to make the process continue.
                    } else {
                        // Errors in thread should terminate the program.
                        System.exit(1);
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            };
            analysisFunctions.add(func);
        }

        // Count number of documents processed by each engine.
        AtomicInteger[] numProcessedDocs = new AtomicInteger[engines.length];

        for (int i = 0; i < engines.length; i++) {
            numProcessedDocs[i] = new AtomicInteger();
        }

        MutableBoolean noNewDocs = new MutableBoolean(false);

        // The consumer manger thread that submit jobs.
        executor.execute(
                () -> {
                    while (true) {
                        // If there are no new jobs and all current jobs are done, we shut down the executor.
                        if (noNewDocs.booleanValue()) {
                            logger.debug(String.format("Last step processed %d docs, input file size is %d.",
                                    numProcessedDocs[numProcessedDocs.length - 1].get(), numInputFiles));
//                            showProgress(numProcessedDocs);
                            boolean jobFinished = numProcessedDocs[numProcessedDocs.length - 1].get() >= numInputFiles;
                            if (jobFinished) {
                                executor.shutdown();
                                logger.info("Shut down executor, do not take more jobs.");
                                break;
                            }
                        }

                        // The submitter will be responsible to check whether there are available task.
                        ProcessElement nextTask;
                        try {
                            nextTask = taskQueue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }

                        // Get the poison item, all elements are read. No more new jobs coming.
                        if (nextTask.isPoison) {
                            noNewDocs.setValue(true);
                            logger.info("Encounter poison now.");
                            // Remember do not process the poison. This is empty and only shows the end of queue.
                            continue;
                        }

                        ProcessElement executionTuple = nextTask;

                        // TODO: If we add the count here, then the job might not get submitted, then we might
                        //  terminate early.
                        int count = numProcessedDocs[executionTuple.step.get()].incrementAndGet();

                        // The actual workers doing the job.
                        executor.submit(
                                () -> {
                                    // Run the next engine using one of the available thread in the executor pool.
                                    CAS cas = executionTuple.cas;
                                    int step = executionTuple.step.get();

                                    // Run the next step in the analysis functions.
                                    ProcessTrace t = analysisFunctions.get(step).apply(cas);

//                                    int count = numProcessedDocs[step].incrementAndGet();

                                    if (executionTuple.increment()) {
                                        // If the tuple has not gone through all steps, put it back to the list
                                        // again.
                                        taskQueue.offer(executionTuple);
                                    } else {
                                        // Finished cas are reused by putting back to the available pool.
                                        cas.reset();
                                        availableCASes.add(cas);
                                    }

                                    // Since we've finished processed this cas at this step, now we count the docs
                                    // processed by the engine.

                                    // Showing progress when the last engine (last step) processed 100 documents.
                                    if (step == engines.length - 1) {
                                        logger.info(String.format("Step %s processed %d files", step, count));
                                        if (count % 100 == 0) {
                                            showProgress(numProcessedDocs);
                                        }
                                    }
                                    combineTrace(processTrace, t);
//                                    logger.info(String.format("Active count %d, complete count %d, core pool size %d",
//                                            executor.getActiveCount(), executor.getCompletedTaskCount(),
//                                            executor.getCorePoolSize()));
                                });
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < numProcessedDocs.length; i++) {
                        sb.append("\n to engine ").append(engines[i].getMetaData().getName()).append(":")
                                .append(numProcessedDocs[i]);
                    }
                    logger.info("Submitter thread finished, number jobs submitted:" + sb.toString());
                }
        );

        return processTrace;
    }

    private void showProgress(AtomicInteger[] processedCounters) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Number of active threads: %d.\n", executor.getActiveCount()));
        sb.append("Showing current annotation progress.");
        for (int i = 0; i < processedCounters.length; i++) {
            AtomicInteger counter = processedCounters[i];
            String processName = engines[i].getMetaData().getName();
            sb.append(String.format("\nAnnotated by %s: %d.", processName, counter.get()));
        }
        logger.info(sb.toString());
    }

    /**
     * Ensure aggregation is synchronized.
     *
     * @param base
     * @param newTrace
     */
    private synchronized void combineTrace(ProcessTrace base, ProcessTrace newTrace) {
        if (newTrace != null) {
            base.aggregate(newTrace);
        }
    }

    /**
     * Produce CAS items for processor to run. The producer will be blocked for the following two reasons:
     * 1. Shared Queue is full, consumers haven't used up the queue elements yet.
     * 2. No available cas container, consumers haven't returned the containers yet.
     */
    private Future<Integer> runProducer() {
        // An producer thread that read from the reader, and use the available cas containers to hold the input.
        // The CAS containers are put in a blocking queue (taskQueue), when the task queue is full, the producer will
        // be blocked.
        // The available CAS are placed in another blocking queue (availableCASes), if there is no available CAS, the
        // producer will be blocked.
        // The producer will add a poison element at the end of all documents, indicating no new docs will be read.
        return executor.submit(
                () -> {
                    AtomicInteger numDocuments = new AtomicInteger();
                    try {
                        while (cReader.hasNext()) {
                            // Get a available container, blocked if non available.
                            CAS currentContainer = availableCASes.take();
                            // Fill the container with actual document input.
                            cReader.getNext(currentContainer);
                            // Put element for processed in the queue. This will block the thread if necessary.
                            taskQueue.offer(new ProcessElement(currentContainer, engines.length));
                            numDocuments.incrementAndGet();
                        }

                        // Offer a dummy poison element, which does not contain anything, just mark end of queue.
                        taskQueue.offer(new ProcessElement());
                    } catch (IOException | CollectionException e) {
                        // Errors in thread should terminate the program.
                        e.printStackTrace();
                        System.exit(1);
                    }
                    return numDocuments.get();
                }
        );
    }

    /**
     * Complete the process.
     *
     * @throws AnalysisEngineProcessException
     */
    public void complete() throws AnalysisEngineProcessException {
        try {
            // Signal end of processing.
            for (AnalysisEngine engine : engines) {
                engine.collectionProcessComplete();
            }
        } finally {
            // Destroy.
            for (AnalysisEngine engine : engines) {
                engine.destroy();
            }
        }
    }
}