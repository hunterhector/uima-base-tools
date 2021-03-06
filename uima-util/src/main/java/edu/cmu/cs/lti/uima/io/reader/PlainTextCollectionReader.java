package edu.cmu.cs.lti.uima.io.reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.*;
import edu.cmu.cs.lti.utils.Configuration;
import net.htmlparser.jericho.Element;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection reader for plain text documents.
 *
 * @author Jun Araki
 * @author zhengzhongliu
 */
public class PlainTextCollectionReader extends AbstractCollectionReader {

    public static final String PARAM_INPUT_VIEW_NAME = "InputViewName";

    public static final String PARAM_INPUTDIR = "InputDirectory";

    public static final String PARAM_TEXT_SUFFIX = "TextSuffix";

    public static final String PARAM_DO_NOISE_FILTER = "NoiseFilter";

    @ConfigurationParameter(name = PARAM_INPUTDIR)
    private String inputDirPath;

    @ConfigurationParameter(name = PARAM_TEXT_SUFFIX, mandatory = false)
    private String suffix;

    @ConfigurationParameter(name = PARAM_DO_NOISE_FILTER, defaultValue = "true")
    private boolean doNoiseFilter;

    public static final String PARAM_REMOVE_QUOTES = "removeQuotes";
    @ConfigurationParameter(name = PARAM_REMOVE_QUOTES, defaultValue = "true")
    private boolean removeQuotes;

    public static final String PARAM_QUOTED_AREA_FILE = "quotedAreaFile";
    @ConfigurationParameter(name = PARAM_QUOTED_AREA_FILE, mandatory = false)
    private File quotedAreaFile;


    private ArrayList<File> textFiles;

    private int currentDocIndex;

    private ArrayListMultimap<String, Span> quotesFromFile;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public void initialize(UimaContext context) throws ResourceInitializationException {
        File directory = new File(inputDirPath);
        if (!directory.exists() || directory.isFile()) {
            logger.error("Cannot find directory at : " + inputDirPath);
            throw new ResourceInitializationException();
        }

        if (suffix == null) {
            suffix = "";
        } else {
            logger.info("Reading files with suffix: " + suffix);
        }

        currentDocIndex = 0;
        textFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory() && files[i].getName().endsWith(suffix)) {
                textFiles.add(files[i]);
            }
        }

        logger.info(String.format("Number of files loaded: %d", textFiles.size()));

        if (removeQuotes) {
            logger.info("Quoted content will be removed in default view.");
        }

        if (quotedAreaFile != null) {
            logger.info("Quoted content are read from file.");
            try {
                quotesFromFile = ReaderUtils.getQuotesFromFile(quotedAreaFile);
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
    }


    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, textFiles.size(), Progress.ENTITIES)};
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        JCas inputView = null;
        try {
            if (!StringUtils.isEmpty(inputViewName)) {
                inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        // open input stream to file
        File file = textFiles.get(currentDocIndex++);
        String originalText = FileUtils.file2String(file, encoding);

        // Basic XML cleaning is necessary.
        originalText = new NoiseTextFormatter(originalText).cleanBasic();

        String cleanedText = doNoiseFilter ? new NoiseTextFormatter(originalText).cleanAll() : originalText;

        ArrayListMultimap<String, Element> tagsByName = ForumStructureParser.indexTagByName(originalText);

//        List<Span> quotedSpans = ForumStructureParser.getQuotesFromElement(tagsByName);
        List<Span> quotedSpans = quotedAreaFile == null ? ForumStructureParser.getQuotesFromElement(tagsByName) :
                quotesFromFile.get(StringUtils.removeEnd(file.getName(), "." + suffix));

        String documentText = removeQuotes ? ForumStructureParser.removeQuoteStr(cleanedText, quotedSpans) :
                cleanedText;

        // put document in CAS
        if (inputView != null) {
            // This view is intended to be used in order to put an original document text to a view other
            // than the default view.
            inputView.setDocumentText(originalText);
        }

        aJCas.setDocumentText(documentText);

        Article article = new Article(aJCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, aJCas);
        article.setArticleName(FilenameUtils.getBaseName(file.getName()));
        article.setLanguage(language);

        // Also store location of source document in CAS. This information is critical
        // if CAS Consumers will need to know where the original document contents are located.
        // For example, the Semantic Search CAS Indexer writes this information into the
        // search index that it creates, which allows applications that use the search index to
        // locate the documents that satisfy their semantic queries.
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(aJCas);
        srcDocInfo.setUri(file.toURI().toURL().toString());
        srcDocInfo.setOffsetInSource(0);
        srcDocInfo.setDocumentSize((int) file.length());
        srcDocInfo.setLastSegment(currentDocIndex == textFiles.size());
        srcDocInfo.addToIndexes();
    }


    public boolean hasNext() {
        return currentDocIndex < textFiles.size();
    }


    public static void main(String[] args) throws IOException, UIMAException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, args[0],
                PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, "txt"
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(args[1], args[2]);

        SimplePipeline.runPipeline(reader, writer);

    }

}
