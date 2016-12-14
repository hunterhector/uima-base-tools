package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.MultiSpan;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.util.NuggetFormat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.javatuples.Triplet;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collection Reader for TBF format data. This retrieves relevant annotations from the gold standard, including event
 * mentions and coreference relations (if exists).
 * <p>
 * User: zhengzhongliu
 * Date: 1/21/15
 * Time: 11:40 PM
 */

public class TbfEventDataReader extends AbstractCollectionReader {
    public static final String PARAM_SOURCE_TEXT_DIRECTORY = "SourceTextDirectory";

    public static final String PARAM_TOKEN_DIRECTORY = "TokenizationDirectory";

    public static final String PARAM_GOLD_STANDARD_FILE = "GoldStandardFile";

    public static final String PARAM_TOKEN_EXT = "TokenExtension";

    public static final String PARAM_SOURCE_EXT = "SourceExtension";

    public static final String startOfDocument = "#BeginOfDocument";

    public static final String endOfDocument = "#EndOfDocument";

    public static final String COMPONENT_ID = TbfEventDataReader.class.getSimpleName();

    private int currentPointer;

    private List<Triplet<String, File, File>> fileList;

    private Triplet<String, File, File> currentFile;

    private boolean hasGoldStandard = false;

    private ArrayListMultimap<String, String> goldStandards;

    private static String className = TbfEventDataReader.class.getSimpleName();

    @ConfigurationParameter(name = PARAM_GOLD_STANDARD_FILE, mandatory = false)
    private File annotationFile = null;

    @ConfigurationParameter(name = PARAM_TOKEN_EXT, mandatory = false)
    String tokenExt;

    @ConfigurationParameter(name = PARAM_SOURCE_EXT)
    String sourceExt;

    @ConfigurationParameter(name = PARAM_SOURCE_TEXT_DIRECTORY)
    File sourceTextDir;

    @ConfigurationParameter(name = PARAM_TOKEN_DIRECTORY, mandatory = false)
    File tokenDir;

    private static int numMentionsProcessed = 0;

    private boolean useToken;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        if (annotationFile != null) {
            hasGoldStandard = true;
            logger.info("Loading gold standard file from " + annotationFile.getPath());
            try {
                goldStandards = splitGoldStandard(annotationFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("Looking for data in source text directory : " + sourceTextDir.getPath());
        Map<String, File> sourceBaseNames = getBaseNames(sourceTextDir, sourceExt);
        Map<String, File> tokenBaseNames = null;

        if (tokenDir == null) {
            logger.info("No tokenization provided.");
            useToken = false;
        } else {
            logger.info("Looking for data in token text directory : " + tokenDir.getPath());
            tokenBaseNames = getBaseNames(tokenDir, tokenExt);
            useToken = true;
        }

        fileList = new ArrayList<>();

        for (Map.Entry<String, File> sourceBaseNameFile : sourceBaseNames.entrySet()) {
            String baseName = sourceBaseNameFile.getKey();
            File sourceFile = sourceBaseNameFile.getValue();
            if (tokenBaseNames != null) {
                if (tokenBaseNames.containsKey(baseName)) {
                    fileList.add(new Triplet<>(baseName, sourceFile, tokenBaseNames.get(baseName)));
                } else {
                    System.err.println("token based name not found " + baseName);
                }
            } else {
                fileList.add(new Triplet<>(baseName, sourceFile, null));
            }
        }

        currentPointer = 0;
    }

    private ArrayListMultimap<String, String> splitGoldStandard(File goldStandardFile) throws IOException {
        String currentDocId = "";

        ArrayListMultimap<String, String> goldStandards = ArrayListMultimap.create();

        for (String line : FileUtils.readLines(goldStandardFile)) {
            line = line.trim();
            if (line.startsWith(startOfDocument)) {
                currentDocId = line.split(" ")[1];
            } else if (!line.startsWith(endOfDocument)) {
                goldStandards.put(currentDocId, line);
            }
        }

        return goldStandards;
    }

    private Map<String, File> getBaseNames(File dir, final String ext) {
        logger.info("Finding " + ext + " in " + dir);
        File[] fileList = dir.listFiles((dir1, name) -> {
            return name.toLowerCase().endsWith(ext);
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

        UimaAnnotationUtils.setSourceDocumentInformation(jCas, currentFile.getValue1().toURI().toURL().toString(),
                (int) currentFile.getValue1().length(), 0, true);

        String sourceFileStr = FileUtils.readFileToString(getSourceFile());
        String documentText = new NoiseTextFormatter(sourceFileStr).cleanAll();

        if (sourceFileStr.length() != documentText.length()) {
            throw new CollectionException(new Exception(String.format(
                    "Length difference after cleaned, before : %d, " + "after : %d",
                    sourceFileStr.length(), documentText.length())));
        }

        if (inputViewName != null) {
            try {
                JCas inputView = ViewCreatorAnnotator.createViewSafely(jCas, inputViewName);
                inputView.setDocumentText(sourceFileStr);
            } catch (AnalysisEngineProcessException e) {
                throw new CollectionException(e);
            }
        }

        JCas goldView = null;
        try {
            goldView = jCas.createView(goldStandardViewName);
        } catch (CASException e) {
            throw new CollectionException(e);
        }

        jCas.setDocumentText(documentText);
        goldView.setDocumentText(documentText);

        ArrayListMultimap<EventMention, String> mention2TokenId;
        if (hasGoldStandard) {
            mention2TokenId = annotateGoldStandard(goldView, getBaseName());
            if (useToken) {
                annotateTokens(goldView, mention2TokenId);
            }
        }

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, sourceFileStr.length(), COMPONENT_ID, 0, jCas);
        article.setArticleName(getBaseName());
        article.setLanguage(language);
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


    private List<Span> getRegionsFromTokens(ArrayList<String> sortedTids, Map<String, Span> tokenSpans) {
        List<Span> regions = new ArrayList<>();

        int lastTid = -2;
        int begin = -1;
        int end = -1;
        for (String sortedTid : sortedTids) {
            int tid = extractInt(sortedTid);
            Span tokenSpan = tokenSpans.get(sortedTid);

            if (begin == -1) {
                // Reading the first token.
                begin = tokenSpan.getBegin();
                end = tokenSpan.getEnd();
            } else {
                // Reading following tokens.

                if (tid - lastTid == 1) {
                    // This token is just after previous one, extending the region.
                    end = tokenSpan.getEnd();
                } else {
                    // This token is not after previous one, adding a new region.
                    regions.add(Span.of(begin, end));
                    begin = tokenSpan.getBegin();
                    end = tokenSpan.getEnd();
                }
            }
            lastTid = tid;
        }

        // Adding the last region.
        if (begin != -1) {
            regions.add(Span.of(begin, end));
        }

        return regions;
    }

    private int extractInt(String s) {
        String num = s.replaceAll("\\D", "");
        // return 0 if no digits found
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    private void annotateTokens(JCas goldView, ArrayListMultimap<EventMention, String> mention2TokenIds)
            throws IOException {
        File tokenFile = getTokenFile();

        Map<String, Span> tokenSpans = readTokenFile(tokenFile);

        for (Map.Entry<EventMention, Collection<String>> mention2Tids : mention2TokenIds.asMap().entrySet()) {
            EventMention mention = mention2Tids.getKey();

            ArrayList<String> sortedTids = new ArrayList<>(mention2Tids.getValue());

            sortedTids.sort(Comparator.comparingInt(this::extractInt));

            List<Span> regionSpans = getRegionsFromTokens(sortedTids, tokenSpans);

            FSArray mentionRegions = new FSArray(goldView, regionSpans.size());
            mention.setRegions(mentionRegions);

            int begin = -1;
            int end = -1;

            for (int i = 0; i < regionSpans.size(); i++) {
                Span regionSpan = regionSpans.get(i);
                ComponentAnnotation region = new ComponentAnnotation(goldView,
                        regionSpan.getBegin(), regionSpan.getEnd());

                if (begin == -1 || region.getBegin() < begin) {
                    begin = region.getBegin();
                }

                if (end == -1 || region.getEnd() > end) {
                    end = region.getEnd();
                }

                region.addToIndexes();
                mention.setRegions(i, region);
            }

            UimaAnnotationUtils.finishAnnotation(mention, begin, end, COMPONENT_ID, 0, goldView);
        }
    }

    private Map<String, Span> readTokenFile(File tokenFile) throws IOException {
        int lineNum = 0;

        Map<String, Span> tokenSpans = new HashMap<>();

        for (String line : FileUtils.readLines(tokenFile)) {
            if (lineNum != 0) {
                String[] parts = line.split("\t");
                if (parts.length == 4) {
                    String tId = parts[0];
                    int tokenBegin = Integer.parseInt(parts[2]);
                    int tokenEnd = Integer.parseInt(parts[3]);

                    tokenSpans.put(tId, Span.of(tokenBegin, tokenEnd));
                }
            }
            lineNum++;
        }

        return tokenSpans;
    }

    private ArrayListMultimap<EventMention, String> annotateGoldStandard(JCas goldView, String baseName) throws
            IOException {
//        ArrayListMultimap<String, EventMention> tokenId2EventMention = ArrayListMultimap.create();

        ArrayListMultimap<EventMention, String> mention2TokenIds = ArrayListMultimap.create();

        List<String[]> corefAnnos = new ArrayList<>();
        Map<String, EventMention> id2Mentions = new HashMap<>();

        for (String goldAnno : goldStandards.asMap().get(baseName)) {
            if (goldAnno.startsWith("#")) {
                // The line is a comment.
                continue;
            }
            String[] annos = goldAnno.split("\t");
            if (annos.length == 0) {
                continue;
            }

            if (annos[0].startsWith("@")) {
                // This is an relation line.
                if (annos[0].equals("@Coreference")) {
                    corefAnnos.add(annos);
                }
            } else if (annos.length >= 7) {
                String eid = annos[2];
                String spanField = annos[3];
                String eventType = NuggetFormat.canonicalType(annos[5]);
                String realisType = NuggetFormat.canonicalType(annos[6]);

                EventMention mention = new EventMention(goldView);
                mention.setEventType(eventType);
                mention.setRealisType(realisType);
                mention.setId(eid);

                id2Mentions.put(eid, mention);

                if (useToken) {
                    for (String tid : spanField.split(",")) {
                        mention2TokenIds.put(mention, tid);
                    }
                } else {
                    MultiSpan multiSpan = getCharacterSpans(spanField);

                    mention.setRegions(new FSArray(goldView, multiSpan.size()));
                    for (int i = 0; i < multiSpan.size(); i++) {
                        Span s = multiSpan.get(i);
                        Annotation region = new Annotation(goldView, s.getBegin(), s.getEnd());
                        mention.setRegions(i, region);
                        region.addToIndexes();
                    }

                    Span range = multiSpan.getRange();
                    UimaAnnotationUtils.finishAnnotation(mention, range.getBegin(), range.getEnd(), COMPONENT_ID,
                            0, goldView);
                }
                numMentionsProcessed++;
            }
        }

        for (String[] corefAnno : corefAnnos) {
            // The assumption here is that each annotation contains a full cluster, with transitive resolved.
            String[] mentionIds = corefAnno[2].split(",");

            Event event = new Event(goldView);

            List<EventMention> corefMentions = new ArrayList<>();
            for (String mentionId : mentionIds) {
                EventMention mention = id2Mentions.get(mentionId);
                corefMentions.add(mention);
                mention.setReferringEvent(event);
            }
            event.setEventMentions(FSCollectionFactory.createFSArray(goldView, corefMentions));
            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, goldView);
        }

        return mention2TokenIds;
    }

    private MultiSpan getCharacterSpans(String spanField) {
        List<Span> spans = new ArrayList<>();
        for (String span : spanField.split(";")) {
            String[] charSpan = span.split(",");
            if (charSpan.length == 2) {
                spans.add(Span.of(Integer.parseInt(charSpan[0]), Integer.parseInt(charSpan[1])));
            }
        }

        return new MultiSpan(spans);
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

        String goldStandardPath = "data/mention/LDC/LDC2015R26/data/tbf/EvalEventHopper20150903.tbf";
        String plainTextPath = "data/mention/LDC/LDC2015R26/data/source";
        String tokenMapPath = "data/mention/LDC/LDC2015R26/data/tkn";


        // Parameters for the writer
        String paramParentOutputDir = "data/event_mention_detection";
        String paramBaseOutputDirName = "plain";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardPath,
                TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextPath,
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapPath,
                TbfEventDataReader.PARAM_TOKEN_EXT, ".tab"
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                paramParentOutputDir, paramBaseOutputDirName, 0,
                paramOutputFileSuffix);

        // Run the pipeline.
        try {
            SimplePipeline.runPipeline(reader
//                    , writer
            );
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Number of mentions " + numMentionsProcessed);
    }
}