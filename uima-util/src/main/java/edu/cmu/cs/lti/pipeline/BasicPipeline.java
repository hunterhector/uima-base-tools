package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
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

    private boolean withOutput;

    private File fullOutputDir;

    private boolean withStats;

    private ProcessTrace performanceTrace = null;

    // Must be at least 1.
    private final int numWorkers;

    private final BlockingQueue<ProcessElement> taskQueue;

    private final boolean robust;

    //    private ExecutorService executor;
    private ThreadPoolExecutor executor;

    private BlockingQueue<CAS> availableCASes;

    private int numInputFiles;

    public BasicPipeline(CollectionReaderDescription reader, AnalysisEngineDescription... processors) throws
            UIMAException, CpeDescriptorException, SAXException, IOException {
        this(reader, false, true, 10, null, null, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, int numWorkers, AnalysisEngineDescription... processors)
            throws UIMAException, CpeDescriptorException, SAXException, IOException {
        this(reader, false, true, numWorkers, null, null, processors);
    }


    public BasicPipeline(CollectionReaderDescription reader, String workingDir, String outputDir,
                         AnalysisEngineDescription... processors) throws
            UIMAException, CpeDescriptorException, SAXException, IOException {
        this(reader, false, true, 10, workingDir, outputDir, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         AnalysisEngineDescription... processors) throws SAXException, UIMAException,
            CpeDescriptorException, IOException {
        this(reader, robust, withStats, numWorkers, null, null, processors);
    }

    public BasicPipeline(CollectionReaderDescription reader, boolean robust, boolean withStats, int numWorkers,
                         String workingDir, String outputDir, AnalysisEngineDescription... processors) throws
            UIMAException, CpeDescriptorException, SAXException, IOException {
        readerDescription = reader;
        AnalysisEngineDescription[] engineDescriptions;

        this.robust = robust;
        this.numWorkers = numWorkers;
        if (robust) {
            logger.info("Set to robust mode, will ignore all exceptions and continue.");
        }

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
        analysisEngineDescs = engineDescriptions;

        // Number of threads for the workers, one additional for producer, one additional for dispatcher.
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numWorkers + 2);
        availableCASes = new ArrayBlockingQueue<>(numWorkers);
        taskQueue = new ArrayBlockingQueue<>(numWorkers);
    }

    public static BasicPipeline getRobust(CollectionReaderDescription reader, String workingDir,
                                          String outputDir, AnalysisEngineDescription... processors)
            throws SAXException, UIMAException, CpeDescriptorException, IOException {
        return new BasicPipeline(reader, true, true, 10, workingDir, outputDir, processors);
    }

    /**
     * Run processor from provided reader.
     *
     * @throws IOException
     * @throws UIMAException
     */
    public BasicPipeline run() throws IOException, UIMAException {
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
        performanceTrace = runCasConsumers(engines);

        numInputFiles = docsProcessed.get();
//        allFilesRead = true;
        logger.info("Number documents submitted: " + numInputFiles);

        // Waiting for the last batch of jobs to terminate.
        logger.info("Waiting for jobs to terminate.");

        logger.info("number of task in queue: " + taskQueue.size());
        logger.info("number of available case: " + availableCASes.size());

        executor.awaitTermination(30, TimeUnit.MINUTES);

        if (withStats) {
            if (performanceTrace != null) {
                logger.info("Process complete, the full processing trace is as followed:");
                logger.info("\n" + performanceTrace.toString());
            }
        }
    }

    private class ProcessElement {
        CAS cas;
        int level;
        int maxLevel;

        boolean isPoison;

        /**
         * A new process element will all have a level of 1, which means it is not processed before.
         */
        ProcessElement() {
            isPoison = true;
        }

        /**
         * A new process element will all have a level of 1, which means it is not processed before.
         *
         * @param cas      The actual content.
         * @param maxLevel Total number of process needed to get it done.
         */
        ProcessElement(CAS cas, int maxLevel) {
            this.cas = cas;
            level = 0;
            this.maxLevel = maxLevel;
            isPoison = false;
        }

        /**
         * Called when processed once, increment the process counter by 1.
         *
         * @return whether the element is processed enough times so that it is done.
         */
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
        // Now submit multiple jobs.
        ProcessTrace processTrace = new ProcessTrace_impl();

        Object lock = new Object();

        List<Function<CAS, ProcessTrace>> analysisFunctions = new ArrayList<>();

        List<AtomicInteger> processedCounters = new ArrayList<>();

        for (AnalysisEngine engine : engines) {
            boolean multiThread = (boolean) engine.getConfigParameterValue(AbstractAnnotator.MULTI_THREAD);

            processedCounters.add(new AtomicInteger());

            Function<CAS, ProcessTrace> func = cas -> {
                try {
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
//        int[] counts = new int[engines.length];
        AtomicInteger[] counts = new AtomicInteger[engines.length];

        for (int i = 0; i < engines.length; i++) {
            counts[i] = new AtomicInteger();
        }

        MutableBoolean noNewJobs = new MutableBoolean(false);

        // The consumer manger thread that submit jobs.
        executor.execute(
                () -> {
                    while (true) {
                        // If there are no new jobs and all current jobs are done, we shut down the executor.
                        if (noNewJobs.booleanValue()) {
                            logger.info(String.format("Jobs submitted : %s.", noNewJobs.booleanValue()));
                            boolean jobFinished = counts[counts.length - 1].get() >= numInputFiles;
                            if (jobFinished) {
                                executor.shutdown();
//                                logger.info(String.format("Level %d count is %d, larger than %d.",
//                                        counts.length - 1, counts[counts.length - 1].get(), numInputFiles));
//                                logger.info(String.format("Level larger : %s",
//                                        counts[counts.length - 1].get() >= numInputFiles));
//                                logger.info(String.format("Job finished : %s", jobFinished));
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
                            noNewJobs.setValue(true);
                            // Remember do not process the poison. It kills stuff.
                            continue;
                        }

                        ProcessElement executionTuple = nextTask;

                        counts[executionTuple.level].incrementAndGet();

                        // The actual workers doing the job.
                        executor.submit(
                                () -> {
                                    // Run the correct engine using one of the available thread in the executor
                                    // pool.
                                    CAS cas = executionTuple.cas;
                                    int level = executionTuple.level;

                                    ProcessTrace t = analysisFunctions.get(level).apply(cas);

                                    if (executionTuple.increment()) {
                                        // If the tuple is still not finished, put it back to the list again.
                                        taskQueue.offer(executionTuple);
                                    } else {
                                        // Finished cas are reused by putting back to the available pool.
                                        cas.reset();
                                        availableCASes.add(cas);
                                    }

                                    int count = processedCounters.get(level).incrementAndGet();

                                    if (level == processedCounters.size() - 1) {
                                        if (count % 100 == 0) {
                                            showProgress(processedCounters);
                                        }
                                    }

                                    combineTrace(processTrace, t);
                                }
                        );
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < counts.length; i++) {
                        sb.append("\n to engine ").append(engines[i].getMetaData().getName()).append(":")
                                .append(counts[i]);
                    }

                    logger.info("Submitter thread finished, number jobs submitted:" + sb.toString());
                }
        );

        return processTrace;
    }

    private void showProgress(List<AtomicInteger> processedCounters) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Number of active threads: %d.\n", executor.getActiveCount()));
        sb.append("Showing current annotation progress.");
        for (int i = 0; i < processedCounters.size(); i++) {
            AtomicInteger counter = processedCounters.get(i);
            String processName = engines[i].getMetaData().getName();
            sb.append(String.format("\nAnnotated by %s: %d.", processName,
                    counter.get()));
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
        // The producer is done when it reads all the files.
        return executor.submit(
                () -> {
                    AtomicInteger numDocuments = new AtomicInteger();
                    try {
                        while (cReader.hasNext()) {
                            // Get a available container.
                            CAS currentContainer = availableCASes.take();
                            // Fill the container with actual document input.
                            cReader.getNext(currentContainer);
                            // Put element for processed in the queue. This will block the thread if necessary.
                            taskQueue.offer(new ProcessElement(currentContainer, engines.length));
                            numDocuments.incrementAndGet();
                        }

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