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

    private int numDocsProduced;

    // Count number of documents submitted to each engine.
    private    AtomicInteger[] numSubmittedDocs;

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

        // Number of threads for the workers, one additional for producer, one additional for dispatcher.
        manager = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
        taskDispatcher = (ThreadPoolExecutor) Executors.newFixedThreadPool(numWorkers);

        availableCASes = new ArrayBlockingQueue<>(numWorkers * 2);
        rawTaskQueue = new ArrayBlockingQueue<>(numWorkers);
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

        logger.info(String.format("Created %d CASes, half for reading, half for buffering.", availableCASes.size()));

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
        ProcessTrace performanceTrace = runCasConsumers(engines);

        manager.shutdown();
        manager.awaitTermination(10, TimeUnit.MINUTES);

        taskDispatcher.shutdown();
        taskDispatcher.awaitTermination(10, TimeUnit.MINUTES);

        numDocsProduced = docsProduced.get();
        logger.info(String.format("Number of documents produced: %d.", numDocsProduced));

        showProgress(numSubmittedDocs);

        if (withStats) {
            if (performanceTrace != null) {
                logger.info("Process complete, the full processing trace is as followed:");
                logger.info("\n" + performanceTrace.toString());
            }
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
//                        logger.debug(String.format("Engine %d trying to obtain lock.", finalEngineId));
                        synchronized (lock) {
//                            logger.debug(String.format("Engine %d successfully obtained lock.", finalEngineId));
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

        AtomicBoolean noNewDocs = new AtomicBoolean(false);

        final BlockingQueue<ProcessElement> processingQueue = new ArrayBlockingQueue<>(numWorkers);
        final ConcurrentLinkedQueue<ProcessElement> processingBuffer = new ConcurrentLinkedQueue<>();

        AtomicInteger numRunningTasks = new AtomicInteger(0);

        // The consumer manger thread that check available jobs.
        manager.submit(
                () -> {
                    while (true) {
                        // The submitter will be responsible to check whether there are available task.
                        logger.debug("Taking a partial job from buffer");
                        ProcessElement partialJob = processingBuffer.poll();
                        if (partialJob != null) {
                            // Check whether there are partial jobs first.
                            logger.debug("Placing the partial job to queue");
                            processingQueue.offer(partialJob);
                        } else if (!noNewDocs.get()) {
                            try {
                                ProcessElement rawTask = rawTaskQueue.take();
                                logger.debug("Taking a raw task from queue.");
                                if (rawTask.isPoison) {
                                    noNewDocs.set(true);
                                    logger.info("Encounter poison now.");
                                } else {
                                    logger.debug("Placing a new job to queue");
                                    processingQueue.offer(rawTask);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // If there are no new docs and all current jobs are submitted, we shut down the executor.

                        // The tasks can be in on of the following states:
                        // 1. Raw docs, in this case, noNewDocs will be false.
                        // 2. In the processing queue, in this case, the processing queue is not empty.
                        // 3. Taken out from queue, but not finished, in this case, numRunningTasks is non zero.
                        // 4. Finished, thus not in the processing queue or raw task queue.

                        // To judge whether it is done, we check whether no docs are in the first 3 conditions.
                        if (noNewDocs.get() && processingQueue.isEmpty() && numRunningTasks.get() == 0) {
                            logger.info(String.format("All docs submitted. Total number of jobs executed: %d.",
                                    taskDispatcher.getTaskCount()));
                            // Offer a poison.
                            processingQueue.offer(new ProcessElement());
                            break;
                        }
                    }
                }
        );

        // The thread that actually submit jobs.
        manager.submit(
                () -> {
                    while (true) {
                        try {
                            logger.debug(String.format("Worker waiting for next task."));

                            ProcessElement nextTask = processingQueue.take();
                            if (nextTask.isPoison) {
                                break;
                            }

                            // Dispatch jobs to the correct step engines.
                            taskDispatcher.submit(
                                    () -> {
                                        // Record the current running task number.
                                        numRunningTasks.incrementAndGet();

                                        int taskStep = nextTask.step.get();

                                        // Run the next engine using one of the available thread in the executor pool.
                                        CAS cas = nextTask.cas;
                                        ProcessTrace t = analysisFunctions.get(taskStep).apply(cas);

                                        if (nextTask.increment()) {
                                            // If the tuple has not gone through all steps, put it to the buffer, the
                                            // submitter will submit it to the job queue again.
                                            processingBuffer.offer(nextTask);
                                        } else {
                                            // Finished cas are reused by putting back to the available pool.
                                            cas.reset();
                                            logger.debug(String.format("Placing CAS back to queue."));
                                            availableCASes.offer(cas);
                                            logger.debug(String.format("Available CAS is now %d.", availableCASes.size()));
                                        }

                                        combineTrace(processTrace, t);
                                        int submittedCount = numSubmittedDocs[taskStep].incrementAndGet();
                                        numRunningTasks.decrementAndGet();

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
                }
        );

        return processTrace;
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
        return manager.submit(
                () -> {
                    AtomicInteger numDocuments = new AtomicInteger();
                    try {
                        while (cReader.hasNext()) {
                            // Get a available container, blocked if non available.
                            logger.debug(String.format("Number of available cases %d", availableCASes.size()));
                            CAS currentContainer = availableCASes.take();
                            // Fill the container with actual document input.
                            cReader.getNext(currentContainer);
                            // Put element for processed in the queue. This will block the thread if necessary.
                            rawTaskQueue.offer(new ProcessElement(currentContainer, engines.length));
                            numDocuments.incrementAndGet();
                        }

                        // Offer a dummy poison element, which does not contain anything, just mark end of queue.
                        rawTaskQueue.offer(new ProcessElement());
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
