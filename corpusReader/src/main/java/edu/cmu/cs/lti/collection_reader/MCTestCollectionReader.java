package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.MCAnswerChoice;
import edu.cmu.cs.lti.script.type.MCQuestion;
import edu.cmu.cs.lti.script.type.MCStory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/3/15
 * Time: 2:23 PM
 */
public class MCTestCollectionReader extends AbstractSourceDocumentCollectionReader {

    public static final String PARAM_MC_TSV_PATH = "mcTsvPath";

    public static final String PARAM_MC_ANS_PATH = "mcAnsPath";

    public static final String COMPONENT_ID = MCTestCollectionReader.class.getSimpleName();

    @ConfigurationParameter(name = PARAM_MC_TSV_PATH)
    String mcTsvPath;

    @ConfigurationParameter(name = PARAM_MC_ANS_PATH, mandatory = false)
    String mcAnsPath;

    Iterator<String> taskIter;

    Iterator<String> answerIter;

    boolean hasAnswer = false;

    private int processed;
    private int totalTasks;
    private final int numChoices = 4;

    private static String className = MCTestCollectionReader.class.getSimpleName();

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        List<String> mcTasks = null;
        try {
            mcTasks = FileUtils.readLines(new File(mcTsvPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mcAnsPath != null) {
            List<String> mcAnswers = null;
            try {
                mcAnswers = FileUtils.readLines(new File(mcAnsPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mcTasks.size() != mcAnswers.size()) {
                throw new IllegalArgumentException("Task size and answer size don't match");
            }
            answerIter = mcAnswers.iterator();
            hasAnswer = true;
        }
        totalTasks = mcTasks.size();
        processed = 0;
        taskIter = mcTasks.iterator();
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        annotateTask(jCas, taskIter.next());
        setSourceDocumentInformation(jCas, new File(mcTsvPath).toURI().toString(), 0, processed, !taskIter.hasNext());
        if (hasAnswer) {
            annotateAnswer(jCas, answerIter.next());
        }
        processed++;
    }

    private void annotateTask(JCas jCas, String task) {
        String[] parts = task.split("\t");
        StringBuilder sb = new StringBuilder();

        int offset = 0;

        String artcleId = parts[0];
        String prop = parts[1];
        String story = formateStory(parts[2]);

        MCStory mcStory = new MCStory(jCas, offset, story.length());
        UimaAnnotationUtils.finishAnnotation(mcStory, COMPONENT_ID, 0, jCas);

        sb.append(story).append("\n\n");
        offset += story.length() + 2;

        int choiceCounter = 0;
        String[] qaSet = new String[numChoices + 1];
        for (int i = 3; i < parts.length; i++) {
            if (choiceCounter == 0) {
                qaSet = new String[numChoices + 1];
            }
            qaSet[choiceCounter] = parts[i];

            choiceCounter++;

            if (choiceCounter == numChoices + 1) {
                choiceCounter = 0;
                String questionStr = annotateQuestion(jCas, qaSet, offset, i / (numChoices + 1));
                sb.append(questionStr).append("\n");
                offset += questionStr.length() + 1;
            }
        }


        Article article = new Article(jCas, 0, sb.length());
        UimaAnnotationUtils.finishAnnotation(article, COMPONENT_ID, artcleId, jCas);

        jCas.setDocumentText(sb.toString());
    }

    private String formateStory(String story) {
        return story.replace("\\newline", "\n");
    }

    private String annotateQuestion(JCas jCas, String[] qaSet, int offset, int qid) {
        StringBuilder sb = new StringBuilder();

        String[] strParts = qaSet[0].split(":");

        String questionStr = strParts[1];

        MCQuestion question = new MCQuestion(jCas, offset, offset + questionStr.length());
        UimaAnnotationUtils.finishAnnotation(question, COMPONENT_ID, qid, jCas);
        sb.append(questionStr).append("\n");
        offset += questionStr.length() + 1;

        question.setIsMultipleChoice(strParts[0].equals("multiple"));

        List<MCAnswerChoice> choices = new ArrayList<>();

        for (int i = 1; i < qaSet.length; i++) {
            MCAnswerChoice choice = new MCAnswerChoice(jCas, offset, offset + qaSet[i].length());
            UimaAnnotationUtils.finishAnnotation(choice, COMPONENT_ID, qid + "_" + i, jCas);
            offset += qaSet[i].length() + 1;
            sb.append(qaSet[i]).append("\n");
            choices.add(choice);
        }
        question.setMcAnswerChoices(FSCollectionFactory.createFSArray(jCas, choices));
        return sb.toString();
    }

    private void annotateAnswer(JCas jCas, String answer) {
        String[] correctAnswers = answer.split("\t");
        int i = 0;
        for (MCQuestion question : JCasUtil.select(jCas, MCQuestion.class)) {
            String correctAnswer = correctAnswers[i];
            question.getMcAnswerChoices(correctAnswer.charAt(0) - 'A').setIsCorrect(true);
            i++;
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return taskIter.hasNext();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(processed, totalTasks, Progress.ENTITIES)};
    }

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        // Parameters for the writer
        String paramParentOutputDir = "data/mc_test/160.dev+train";
        String paramBaseOutputDirName = "plain";
        String paramOutputFileSuffix = null;

        String paramTypeSystemDescriptor = "TaskMCTestTypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                MCTestCollectionReader.class, typeSystemDescription,
                MCTestCollectionReader.PARAM_MC_ANS_PATH, "data/mc_test/MCTest/mc160.train.ans",
                MCTestCollectionReader.PARAM_MC_TSV_PATH, "data/mc_test/MCTest/mc160.train.tsv"
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                paramOutputFileSuffix);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(className + " successfully completed.");
    }
}
