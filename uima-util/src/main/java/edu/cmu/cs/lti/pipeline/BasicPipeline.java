package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.io.writer.StepBasedDirGzippedXmiWriter;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
    // Must be at least 1.
    private final int numWorkers;
    private final BlockingQueue<ProcessElement> rawTaskQueue;
    private final boolean robust;
    private CollectionReaderDescription readerDescription;
    private CollectionReaderDescription outputReader;
    private CollectionReader cReader;
    private AnalysisEngineDescription[] analysisEngineDescs;
    private AnalysisEngine[] engines;
    private boolean withStats;
    //    private ExecutorService executor;
    private ThreadPoolExecutor manager;
    private ThreadPoolExecutor taskDispatcher;

    private BlockingQueue<CAS> availableCASes;
    // Obtain a token before access the CAS queue.
    private Semaphore casTokens;

    // Count number of documents submitted to each engine.
    private AtomicInteger[] numSubmittedDocs;

    private ProcessTrace processTrace;


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

        // 3 threads for the manager, one for producer, one to take task and convert, one to run the dispatcher.
        manager = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        taskDispatcher = (ThreadPoolExecutor) Executors.newFixedThreadPool(numWorkers);

        availableCASes = new ArrayBlockingQueue<>(numWorkers);
        casTokens = new Semaphore(numWorkers);

        rawTaskQueue = new ArrayBlockingQueue<>(numWorkers);

        processTrace = new ProcessTrace_impl();
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

        numSubmittedDocs = new AtomicInteger[engines.length];

        // Create multiple cas containers.
        TypeSystem typeSystem = null;
        for (int i = 0; i < numWorkers * 2; i++) {
            CAS cas = CasCreationUtils.createCas(metaData);
            availableCASes.offer(CasCreationUtils.createCas(metaData));
            typeSystem = cas.getTypeSystem();
        }

        logger.info(String.format("Created %d CASes.", availableCASes.size()));

        // The reader can take type system from any CAS (they are the same.).
        cReader.typeSystemInit(typeSystem);
    }

    /**
     * Process the next CAS in the reader.
     */
    public void process() throws ExecutionException, InterruptedException {
        Future<Integer> docsProduced = runProducer();

        // Waiting for the last batch of jobs to terminate.
        logger.info("Start running on consumers.");
        Future<Integer> rawTaskCount = runCasConsumers(engines);

        logger.info(String.format("Number of documents produced: %d.", docsProduced.get()));
        logger.info(String.format("Number of raw task processed: %d.", rawTaskCount.get()));

        // Then shutdown the manager itself.
        logger.info("Shutting down manager");
        shutdownExecutor(manager, 10, TimeUnit.SECONDS);
        logger.info("Manager shut.");

        showProgress(numSubmittedDocs);

        if (withStats) {
            if (processTrace != null) {
                logger.info("Process complete, the full processing trace is as followed:");
                logger.info("\n" + processTrace.toString());
            }
        }
    }

    /**
     * This method runs the consumers to process the CASes. It will be blocked until all tasks are done.
     *
     * @param engines List of engines to applied on the CASes.
     * @return
     */
    private Future<Integer> runCasConsumers(AnalysisEngine[] engines) {
        // When the engine does not support multi thread, it will need to obtain its lock.
        Object[] locks = new Object[engines.length];
        for (int i = 0; i < engines.length; i++) {
            locks[i] = new Object();
        }

        List<Function<CAS, ProcessTrace>> analysisFunctions = new ArrayList<>();

        for (int engineId = 0; engineId < engines.length; engineId++) {
            AnalysisEngine engine = engines[engineId];
            boolean multiThread = (boolean) engine.getConfigParameterValue(AbstractAnnotator.MULTI_THREAD);

            Object lock = locks[engineId];

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

        for (int i = 0; i < engines.length; i++) {
            numSubmittedDocs[i] = new AtomicInteger();
        }

        AtomicBoolean moreDocs = new AtomicBoolean(true);

        final BlockingQueue<ProcessElement> processingQueue = new ArrayBlockingQueue<>(numWorkers);

        final AtomicInteger numPendingTasks = new AtomicInteger();

        // The consumer manger thread that check available jobs.
        Future<Integer> rawTaskSubmitted = manager.submit(
                () -> {
                    int taskCount = 0;
                    while (true) {
                        // The submitter will be responsible to check whether there are available task.
                        if (moreDocs.get()) {
                            try {
                                logger.debug("Manager taking a raw task from queue.");
                                ProcessElement rawTask = rawTaskQueue.take();
                                if (rawTask.isPoison) {
                                    moreDocs.set(false);
                                    logger.info("Encounter poison now.");
                                } else {
                                    logger.debug("Placing a raw job to queue");
                                    logger.debug(String.format("Number of active threads: %d.",
                                            taskDispatcher.getActiveCount()));
                                    processingQueue.offer(rawTask);
                                    numPendingTasks.incrementAndGet();
                                    logger.debug(String.format("Increment pending to: %d.", numPendingTasks.get()));
                                    taskCount += 1;
                                    logger.debug(String.format("Queue now contain %d tasks.", processingQueue.size()));
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        if (numPendingTasks.get() == 0) {
                            logger.info("All pending tasks finished, sending shutdown signals to dispatcher.");
                            // Annotation task could take a while, set a longer timeout here.
                            shutdownExecutor(taskDispatcher, 20, TimeUnit.MINUTES);
                            logger.info("Dispatcher shut.");
                            break;
                        }
                    }
                    return taskCount;
                }
        );

        // The submit thread.
        manager.submit(
                () -> {
                    int taskCount = 0;

                    while (moreDocs.get() || numPendingTasks.get() > 0) {
                        try {
                            if (processingQueue.isEmpty()){
                                continue;
                            }
                            ProcessElement nextTask = processingQueue.take();
                            // Dispatch jobs to the correct step engines.
                            taskDispatcher.submit(
                                    () -> {
                                        int taskStep = nextTask.step.get();

                                        // Run the next engine using one ofq the available thread in the executor pool.
                                        CAS cas = nextTask.cas;
                                        ProcessTrace t = analysisFunctions.get(taskStep).apply(cas);

                                        if (nextTask.increment()) {
                                            // If the tuple has not gone through all steps, put it to the buffer, the
                                            // submitter will submit it to the job queue again.
                                            logger.debug(String.format("Offer a task to queue for step [%d].",
                                                    nextTask.step.get()));
                                            // Possible block when the processing queue is full.
                                            processingQueue.offer(nextTask);
                                            logger.debug(String.format("Queue now contains %d jobs.",
                                                    processingQueue.size()));
                                        } else {
                                            // Finished CASes are reused by putting back to the available pool.
                                            cas.reset();
                                            logger.debug("Placing CAS back to queue.");
                                            availableCASes.offer(cas);
                                            casTokens.release();
                                            logger.debug(String.format("Available CAS is now %d.",
                                                    availableCASes.size()));
                                            // The task go through the whole life cycle and finished.
                                            numPendingTasks.decrementAndGet();
                                            logger.debug("Decrement pending documents to: " + numPendingTasks.get());
                                        }
                                        combineTrace(processTrace, t);
                                        int submittedCount = numSubmittedDocs[taskStep].incrementAndGet();

                                        if (taskStep == analysisFunctions.size() - 1) {
                                            if (submittedCount % 100 == 0) {
                                                showProgress(numSubmittedDocs);
                                            }
                                        }
                                    });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    logger.debug("Submitter thread done.");
                    return taskCount;
                }
        );

        return rawTaskSubmitted;
    }

    private void showProgress(AtomicInteger[] processedCounters) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Number of active threads: %d.\n", taskDispatcher.getActiveCount()));
        sb.append("Showing current annotation progress.");
        for (int i = 0; i < processedCounters.length; i++) {
            AtomicInteger counter = processedCounters[i];
            String processName = engines[i].getMetaData().getName();
            sb.append(String.format("\n - Annotated by %s: %d.", processName, counter.get()));
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

        // When the numRunningTasks reach numWorkers, the producer will stop adding new tasks, since the workers may
        // need the cas for recycling.
        return manager.submit(
                () -> {
//                    AtomicInteger numDocuments = new AtomicInteger();
                    int numDocuments = 0;
                    try {
                        while (cReader.hasNext()) {
                            // Acquire the semaphore for accessing the cas queue.
                            // It will not produce new tasks if all cas tokens are produced.
                            casTokens.acquire();
                            logger.debug(String.format("Available permits %d", casTokens.availablePermits()));
                            // Get a available container, blocked if non available.
                            logger.debug(String.format("Take from available CASes: %d", availableCASes.size()));
                            CAS currentContainer = availableCASes.take(); //potential blocking
                            logger.debug(String.format("Available CASes now: %d", availableCASes.size()));
                            // Fill the container with actual document input.
                            cReader.getNext(currentContainer);
                            // Put element for processed in the queue. This will block the thread if necessary.
                            rawTaskQueue.offer(new ProcessElement(currentContainer, engines.length));
                            numDocuments++;
                        }

                        // Offer a dummy poison element, which does not contain anything, just mark end of queue.
                        rawTaskQueue.offer(new ProcessElement());
                    } catch (IOException | CollectionException e) {
                        // Errors in thread should terminate the program.
                        e.printStackTrace();
                        System.exit(1);
                    }
                    logger.debug("Producer thread done.");
                    return numDocuments;
                }
        );
    }

    private void shutdownExecutor(ExecutorService service, int timeout, TimeUnit unit) {
        service.shutdown();
        try {
            logger.info("Waiting for termination: " + service.toString());
            if (!service.awaitTermination(timeout, unit)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
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

    private class ProcessElement {
        final int maxStep;
        CAS cas;
        AtomicInteger step;
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
}
