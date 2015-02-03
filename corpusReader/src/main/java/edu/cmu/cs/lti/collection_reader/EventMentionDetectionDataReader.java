package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.util.NoiseTextFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.javatuples.Triplet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/21/15
 * Time: 11:40 PM
 */
public class EventMentionDetectionDataReader extends JCasCollectionReader_ImplBase {
    public static final String PARAM_SOURCE_TEXT_DIRECTORY = "SourceTextDirectory";

    public static final String PARAM_TOKEN_DIRECTORY = "TokenizationDirectory";

    public static final String PARAM_GOLD_STANDARD_FILE = "GoldStandardFile";

    public static final String PARAM_TOKEN_EXT = "TokenExtension";

    public static final String PARAM_SOURCE_EXT = "SourceExtension";

    public static final String startOfDocument = "#BeginOfDocument";

    public static final String endOfDocument = "#EndOfDocument";

    public static final String goldStandardViewName = "goldStandard";

    public static final String componentId = EventMentionDetectionDataReader.class.getSimpleName();

    private String sourceExt;

    private String tokenExt;

    private int currentPointer;

    private List<Triplet<String, File, File>> fileList;

    private Triplet<String, File, File> currentFile;

    private boolean hasGoldStandard = false;

    private ArrayListMultimap<String, String> goldStandards;

    private static String className = EventMentionDetectionDataReader.class.getSimpleName();


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        File sourceTextDir = new File(((String) getConfigParameterValue(PARAM_SOURCE_TEXT_DIRECTORY)).trim());
        File tokenDir = new File((String) getConfigParameterValue(PARAM_TOKEN_DIRECTORY));

        Object annotationPath = getConfigParameterValue(PARAM_GOLD_STANDARD_FILE);
        if (annotationPath != null) {
            File annotationFile = new File((String) annotationPath);
            hasGoldStandard = true;
            try {
                goldStandards = splitGoldStandard(annotationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        tokenExt = (String) getConfigParameterValue(PARAM_TOKEN_EXT);
        sourceExt = (String) getConfigParameterValue(PARAM_SOURCE_EXT);

        Map<String, File> sourceBaseNames = getBaseNames(sourceTextDir, sourceExt);
        Map<String, File> tokenBaseNames = getBaseNames(tokenDir, tokenExt);

        fileList = new ArrayList<>();

        for (Map.Entry<String, File> sourceBaseNameFile : sourceBaseNames.entrySet()) {
            String baseName = sourceBaseNameFile.getKey();
            File sourceFile = sourceBaseNameFile.getValue();
            if (tokenBaseNames.containsKey(baseName)) {
                fileList.add(new Triplet<>(baseName, sourceFile, tokenBaseNames.get(baseName)));
            } else {
                System.err.println("token based name not found " + baseName);
            }
        }

        currentPointer = 0;
    }

    private ArrayListMultimap<String, String> splitGoldStandard(File goldStandardFile) throws IOException {
        String currentDocId = "";

        ArrayListMultimap<String, String> goldStandards = ArrayListMultimap.create();

        for (String line : FileUtils.readLines(goldStandardFile)) {
            if (line.startsWith(startOfDocument)) {
                currentDocId = line.split(" ")[1];
            } else if (!line.startsWith(endOfDocument)) {
                goldStandards.put(currentDocId, line);
            }
        }

        return goldStandards;
    }

    private Map<String, File> getBaseNames(File dir, final String ext) {
        File[] fileList = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(ext);
            }
        });

        Map<String, File> baseName2File = new HashMap<>();

        for (File file : fileList) {
            baseName2File.put(StringUtils.removeEnd(file.getName(), ext), file);
        }

        return baseName2File;
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        currentFile = fileList.get(currentPointer);
        currentPointer++;

        SourceDocumentInformation enSrcDocInfo = new SourceDocumentInformation(jCas);
        enSrcDocInfo.setUri(currentFile.getValue1().toURI().toURL().toString());
        enSrcDocInfo.setOffsetInSource(0);
        enSrcDocInfo.setDocumentSize((int) currentFile.getValue1().length());
        enSrcDocInfo.setLastSegment(true);
        enSrcDocInfo.addToIndexes();

        String sourceFileStr = FileUtils.readFileToString(getSourceFile());
        String documentText = NoiseTextFormatter.cleanForum(sourceFileStr);

        JCas goldView = null;
        try {
            goldView = jCas.createView(goldStandardViewName);
        } catch (CASException e) {
            e.printStackTrace();
        }

        jCas.setDocumentText(documentText);
        goldView.setDocumentText(documentText);

        ArrayListMultimap<String, EventMention> tokenId2EventMention;
        if (hasGoldStandard) {
            tokenId2EventMention = annotateGoldStandard(goldView, getBaseName());
        } else {
            tokenId2EventMention = ArrayListMultimap.create();
        }

        annotateTokens(jCas, goldView, tokenId2EventMention);

        for (Map.Entry<String, Collection<EventMention>> mentionEntry : tokenId2EventMention.asMap().entrySet()) {
            for (EventMention mention : mentionEntry.getValue()) {
                int start = -1;
                int end = -1;
                for (Word word : FSCollectionFactory.create(mention.getMentionTokens(), Word.class)) {
                    if (start == -1 || word.getBegin() < start) {
                        start = word.getBegin();
                    }
                    if (end == -1 || word.getEnd() > end) {
                        end = word.getEnd();
                    }
                }

                mention.setBegin(start);
                mention.setEnd(end);
            }
        }

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, sourceFileStr.length(), componentId, 0, jCas);
        article.setArticleName(getBaseName());
        article.setLanguage("en");
    }

    private File getTokenFile() {
        return currentFile.getValue2();
    }

    private File getSourceFile() {
        return currentFile.getValue1();
    }

    private String getBaseName() {
        return currentFile.getValue0();
    }

    private void annotateTokens(JCas aJCas, JCas goldView, ArrayListMultimap<String, EventMention> tokenId2EventMention) throws IOException {
        File tokenFile = getTokenFile();

        int lineNum = 0;

        for (String line : FileUtils.readLines(tokenFile)) {
            if (lineNum != 0) {
                String[] parts = line.split("\t");
                if (parts.length == 4) {
                    String tId = parts[0];
//                    String tokenStr = parts[1];
                    int tokenBegin = Integer.parseInt(parts[2]);
                    int tokenEnd = Integer.parseInt(parts[3]) + 1;

                    Word word = new Word(aJCas, tokenBegin, tokenEnd);
                    UimaAnnotationUtils.finishAnnotation(word, componentId, tId, aJCas);

                    if (tokenId2EventMention.containsKey(tId)) {
                        for (EventMention tokenMention : tokenId2EventMention.get(tId)) {
                            tokenMention.setMentionTokens(UimaConvenience.appendFSList(goldView, tokenMention.getMentionTokens(), word, Word.class));
                        }
                    }
                }
            }
            lineNum++;
        }
    }

    private ArrayListMultimap<String, EventMention> annotateGoldStandard(JCas goldView, String baseName) throws IOException {
        ArrayListMultimap<String, EventMention> tokenId2EventMention = ArrayListMultimap.create();
        for (String goldAnno : goldStandards.asMap().get(baseName)) {
            String[] annos = goldAnno.split("\t");
            if (annos.length == 8) {
                String eid = annos[2];
                String tokenIds = annos[3];
                String eventType = annos[5];
                String realisType = annos[6];

                EventMention mention = new EventMention(goldView);
                UimaAnnotationUtils.finishAnnotation(mention, componentId, eid, goldView);
                mention.setEventType(eventType);
                mention.setRealisType(realisType);

                for (String tid : tokenIds.split(",")) {
                    tokenId2EventMention.put(tid, mention);
                }
            }
        }
        return tokenId2EventMention;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return currentPointer < fileList.size();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentPointer, fileList.size(), Progress.ENTITIES)};
    }

    public static void main(String[] args) throws UIMAException {
        System.out.println(className + " started...");

        String paramInputDir =
                "/Users/zhengzhongliu/Documents/projects" +
                        "/cmu-script/event-mention-detection" +
                        "/data/Event-mention-detection-2014" +
                        "/LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/";

        String goldStandardFilePath = paramInputDir + "converted.tbf";
        String sourceDataPath = paramInputDir + "source";
        String tokenDataPath = paramInputDir + "token_offset";

        // Parameters for the writer
        String paramParentOutputDir = "data/event_mention_detection";
        String paramBaseOutputDirName = "plain";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                EventMentionDetectionDataReader.class, typeSystemDescription,
                EventMentionDetectionDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                EventMentionDetectionDataReader.PARAM_SOURCE_EXT, ".tkn.txt",
                EventMentionDetectionDataReader.PARAM_SOURCE_TEXT_DIRECTORY, sourceDataPath,
                EventMentionDetectionDataReader.PARAM_TOKEN_DIRECTORY, tokenDataPath,
                EventMentionDetectionDataReader.PARAM_TOKEN_EXT, ".txt.tab"
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