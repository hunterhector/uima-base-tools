package edu.cmu.cs.lti.collection_reader;

import com.google.common.base.Joiner;
import com.nytlabs.corpus.NYTCorpusDocument;
import com.nytlabs.corpus.NYTCorpusDocumentParser;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 8/25/14
 * Time: 4:35 PM
 */
public class AnnotatedNytReader extends AbstractCollectionReader {

    private NYTCorpusDocumentParser parser = new NYTCorpusDocumentParser();

    private int currentIndex = 0;
    private int fileOffset;
    private String fileUrl;
    private String fileName;
    private long fileSize;

    private TarArchiveInputStream currentTar;
    private byte[] currentFileContent;
    private boolean expire;
    private boolean hasNextTar;

    private File tempFile;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        currentIndex = 0;
        logger.info("Number of original document collection: " + files.size());
        expire = true;
        hasNextTar = true;

        if (files.size() > 0) {
            try {
                readTarFile();
                readNextContent();
                tempFile = File.createTempFile("nyt", ".xml");
                tempFile.deleteOnExit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        expire = true;
        IOUtils.write(currentFileContent, new FileOutputStream(tempFile));
        NYTCorpusDocument document = parser.parseNYTCorpusDocumentFromFile(tempFile, false);

        String headline = document.getHeadline();
        String onlineHeadline = document.getOnlineHeadline();

        String articleAbstract = document.getArticleAbstract();

        List<String> biographicalCategories = document.getBiographicalCategories();

        String dateLine = document.getDateline();

        String dayOfWeek = document.getDayOfWeek();
        int month = document.getPublicationMonth();
        int day = document.getPublicationDayOfMonth();

        List<String> generalOnlineDescriptors = document.getGeneralOnlineDescriptors();
        List<String> onlineDescriptors = document.getOnlineDescriptors();

        String normalizedByline = document.getNormalizedByline();

        List<String> descriptors = document.getDescriptors();
        List<String> taxoClassifiers = document.getTaxonomicClassifiers();

        List<String> names = document.getNames();

        List<String> locations = document.getLocations();
        List<String> onlineLocations = document.getOnlineLocations();
        List<String> people = document.getPeople();
        List<String> onlinePeople = document.getOnlinePeople();
        List<String> organizations = document.getOrganizations();
        List<String> onlineOrganizations = document.getOnlineOrganizations();

        List<String> authoredWorks = document.getOnlineTitles();
        List<String> typesOfMaterial = document.getTypesOfMaterial();

        int columnNumber = document.getColumnNumber();

        List<String> leadParagraphs = getParagraphs(document.getLeadParagraph());

        List<String> bodyParagraphs = getParagraphs(document.getBody());

        int numberLeading = leadParagraphs.size();

        String bodySep = "\n\n";
        String titleBodySep = ".\n\n";

        int leadingSize = 0;
        for (int i = 0; i < numberLeading; i++) {
            leadingSize += bodyParagraphs.get(i).length();
        }
        leadingSize += bodySep.length() * (numberLeading - 1);

        String documentText;
        int titleLength;
        if (headline != null) {
            documentText = headline + titleBodySep + Joiner.on(bodySep).join(bodyParagraphs);
            titleLength = headline.length() + 1;
        } else {
            titleBodySep = "\n\n";
            documentText = titleBodySep + Joiner.on("\n\n").join(bodyParagraphs);
            titleLength = 0;
        }

        int bodyStart = titleLength + titleBodySep.length() - 1;

        jCas.setDocumentText(documentText);

        Headline uimaHeadline = new Headline(jCas, 0, titleLength);
        Body body = new Body(jCas, bodyStart, documentText.length());
        NytMetadata metadata = new NytMetadata(jCas);

        LeadingParagraph leadingParagraph = new LeadingParagraph(jCas, bodyStart, bodyStart + leadingSize);

        metadata.setAbstract(articleAbstract);
        metadata.setHeadline(headline);
        metadata.setOnlineHeadline(onlineHeadline);
        metadata.setNormalizedByline(normalizedByline);
        metadata.setDayOfWeek(dayOfWeek);
        metadata.setMonth(month);
        metadata.setDay(day);

        metadata.setBiographicalCategories(FSCollectionFactory.createStringList(jCas, biographicalCategories));
        metadata.setGeneralOnlineDescriptors(FSCollectionFactory.createStringList(jCas, generalOnlineDescriptors));
        metadata.setOnlineDescriptors(FSCollectionFactory.createStringList(jCas, onlineDescriptors));
        metadata.setDescriptors(FSCollectionFactory.createStringList(jCas, descriptors));
        metadata.setTaxonomyClassifiers(FSCollectionFactory.createStringList(jCas, taxoClassifiers));
        metadata.setNames(FSCollectionFactory.createStringList(jCas, names));
        metadata.setLocations(FSCollectionFactory.createStringList(jCas, locations));
        metadata.setOnlineLocations(FSCollectionFactory.createStringList(jCas, onlineLocations));
        metadata.setPeople(FSCollectionFactory.createStringList(jCas, people));
        metadata.setOnlinePeople(FSCollectionFactory.createStringList(jCas, onlinePeople));
        metadata.setOrganizations(FSCollectionFactory.createStringList(jCas, organizations));
        metadata.setOnlineOrganizations(FSCollectionFactory.createStringList(jCas, onlineOrganizations));
        metadata.setAuthoredWorks(FSCollectionFactory.createStringList(jCas, authoredWorks));
        metadata.setTypeOfMaterials(FSCollectionFactory.createStringList(jCas, typesOfMaterial));
        metadata.setColumnNumber(columnNumber);


        Article article = new Article(jCas, 0, documentText.length());
        article.setArticleDate(dateLine);
        article.setHeadLine(headline);
        article.setLanguage("en");
        article.setMetadata(metadata);
        article.setArticleName(FilenameUtils.getBaseName(fileName));

        UimaAnnotationUtils.finishAnnotation(uimaHeadline, COMPONENT_ID, 0, jCas);
        UimaAnnotationUtils.finishAnnotation(body, COMPONENT_ID, 0, jCas);
        UimaAnnotationUtils.finishAnnotation(article, COMPONENT_ID, 0, jCas);
        UimaAnnotationUtils.finishAnnotation(leadingParagraph, COMPONENT_ID, 0, jCas);

        UimaAnnotationUtils.setSourceDocumentInformation(jCas, fileUrl, documentText.length(), fileOffset, false);
        readNextContent();
    }

    private List<String> getParagraphs(String text) {

        List<String> paragraphs = new ArrayList<>();

        if (text == null) {
            return paragraphs;
        }

        String[] parts = text.split("\n");

        for (String part : parts) {
            String partText = part.trim();
            if (!partText.isEmpty()) {
                if (!partText.endsWith(".")) {
                    paragraphs.add(partText + ".");
                } else {
                    paragraphs.add(partText);
                }
            }
        }
        return paragraphs;
    }


    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return currentFileContent != null;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    private void readNextContent() throws IOException {
        if (expire) {
            TarArchiveEntry entry;
            do {
                entry = currentTar.getNextTarEntry();
                if (entry == null) {
                    readTarFile();
                    if (hasNextTar) {
                        readNextContent();
                    } else {
                        // The whole reading is done.
                        currentFileContent = null;
                    }
                    return;
                }
            } while (entry.isDirectory());

            fileOffset++;
            fileName = entry.getName();
            currentFileContent = new byte[(int) entry.getSize()];
            currentTar.read(currentFileContent, 0, currentFileContent.length);
        }
    }

    private void readTarFile() throws IOException {
        if (currentIndex < files.size()) {
            hasNextTar = true;
            File currentFile = files.get(currentIndex);
            fileUrl = currentFile.toURI().toURL().toString();
            logger.info("Reading compressed package: " + currentFile);
            currentTar = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(currentFile)));
            fileOffset = 0;
            currentIndex++;
        } else {
            hasNextTar = false;
        }
    }

    public static void main(String[] argv) throws IOException, UIMAException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String paramInputDir = argv[0];
        String outputDir = argv[1];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                AnnotatedNytReader.class, typeSystemDescription,
                AnnotatedNytReader.PARAM_DATA_PATH, paramInputDir,
                AnnotatedNytReader.PARAM_FILE_EXTENSION, ".tgz",
                AnnotatedNytReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(outputDir, "raw");
        SimplePipeline.runPipeline(reader, writer);
    }
}
