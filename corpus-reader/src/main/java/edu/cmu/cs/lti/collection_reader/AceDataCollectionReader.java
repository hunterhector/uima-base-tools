/**
 *
 */
package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.ForumStructureParser;
import edu.cmu.cs.lti.uima.util.NoiseTextFormatter;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.javatuples.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code refactored from ClearTk for reading Ace2005 corpus documents
 * The engine may have several assumption over the folder hierarchy. The current version
 * assumes input from ACE2005-TrainingData-V6.0 (LDC2005E18).
 * <p/>
 * NOTE: please put apf.v5.1.1.dtd file at the same folder level of apf.xml files
 *
 * @author Zhengzhong Liu, Hector
 */
public class AceDataCollectionReader extends AbstractCollectionReader {

    public final static String PARAM_ACE_DATA_PATH = "aceDataPath";

    public final static String PARAM_ACE_TYPES = "aceTypesToRead";

    public final static String PARAM_ACE_DATA_STATUS = "aceDataStatus";

    @ConfigurationParameter(mandatory = true, description = "The path of the directory that contains the ACE data " +
            "with various formats, for exapmle ../ACE2005/ACE2005-TrainingData-V6.0/English ",
            name = PARAM_ACE_DATA_PATH)
    private String aceDatPath;

    private File apfDtd;

    @ConfigurationParameter(mandatory = false, description = "By default we read in all the types, it can be 'nw', " +
            "'bn', 'bc','wl','un',cts'", name = PARAM_ACE_TYPES)
    private Set<String> aceTypesToRead;

    // this is assumed value for the ACE2005 training data
    @ConfigurationParameter(name = PARAM_ACE_DATA_STATUS, defaultValue = "timex2norm")
    private String inputAceDataStatus;

    // suffix of the file containing the plain text
    private final String textFileSuffix = ".sgm";

    private Map<String, File[]> aceFilesByType;

    private List<File> aceFilesToRead;

    private int aceFileIndex = 0;

    private File currentSGMFile;

    private static final String TAG_REGEX = "<.*?>";

    private Pattern tagPattern = Pattern.compile(TAG_REGEX, Pattern.MULTILINE | Pattern.DOTALL);

    private Pattern ampPattern = Pattern.compile(Pattern.quote("&amp;"));

    private String[] tagsToRemove = {"docid", "doctype", "poster", "postdate", "endtime", "datetime"};

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        File dataDirectory = new File(aceDatPath);

        logger.info("Reading data from : " + dataDirectory);

        try {
            aceFilesByType = getContentDirectoryByType(dataDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        apfDtd = new File(dataDirectory.getParent(), "dtd/apf.v5.1.1.dtd");

        aceFilesToRead = new ArrayList<>();
        for (Entry<String, File[]> typeAndFiles : aceFilesByType.entrySet()) {
            logger.info(String.format("Reading %d files of type %s.", typeAndFiles.getValue().length,
                    typeAndFiles.getKey()));
            aceFilesToRead.addAll(Arrays.asList(typeAndFiles.getValue()));
        }

        logger.info(aceFilesToRead.size() + " files to read in total");
    }

    /**
     * Get the file paths but organized them by the text type, we assume that all sub-directories
     * under this directory are all corpus documents, and each directory store one type, this is the
     * same with the ACE2005 LDC distribution
     * <p/>
     * In ACE2005 English documents, the types are as followed, see data README for details
     * <p/>
     * - Newswire (NW)
     * - Broadcast News (BN)
     * - Broadcast Conversation (BC)
     * - Weblog (WL)
     * - Usenet Newsgroups/Discussion Forum (UN)
     * - Conversational Telephone Speech (CTS)
     *
     * @param directoryOfLanguage The input directory, at Language level
     */
    private Map<String, File[]> getContentDirectoryByType(File directoryOfLanguage) throws IOException {
        if (!directoryExist(directoryOfLanguage))
            throw new IllegalArgumentException(
                    "Input not a directory (or not exist), cannot get file list from each source : " +
                            directoryOfLanguage.getCanonicalPath());

        Map<String, File[]> contentDirectories = new HashMap<String, File[]>();

        for (File typeDir : directoryOfLanguage.listFiles()) {
            if (typeDir.isDirectory()) {
                String type = typeDir.getName();
                if (aceTypesToRead == null || aceTypesToRead.contains(type)) {
                    String contentDirPath = typeDir.getAbsolutePath() + "/" + inputAceDataStatus;
                    File contentDir = new File(contentDirPath);
                    if (directoryExist(contentDir)) {
                        contentDirectories.put(type, contentDir.listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith(textFileSuffix);
                            }
                        }));
                    }
                }
            }
        }

        return contentDirectories;
    }

    private boolean directoryExist(File dir) {
        return dir.exists() && dir.isDirectory();
    }

    private File getNextSGMFile() {
        if (currentSGMFile != null)
            return currentSGMFile;
        while (aceFileIndex < aceFilesToRead.size()) {
            File sgmFile = aceFilesToRead.get(aceFileIndex++);
            if (sgmFile.getName().endsWith(textFileSuffix)) {
                currentSGMFile = sgmFile;
                return sgmFile;
            }
        }
        return null;
    }

    private File getAPFFile(File sgmFile) {
        String apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "entities.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        apfFileName = sgmFile.getPath();
        apfFileName = sgmFile.getPath().substring(0, apfFileName.length() - 3) + "mentions.apf.xml";
        if (new File(apfFileName).exists())
            return new File(apfFileName);

        return null;
    }

    private String handleMetaTags(String sgmText) {
//        logger.info("Origin length is " + sgmText.length());
        ArrayListMultimap<String, net.htmlparser.jericho.Element> tagsByName = ForumStructureParser.indexTagByName
                (sgmText);

//        logger.info(sgmText);

        for (String tagName : tagsToRemove) {
            if (tagsByName.containsKey(tagName)) {
                for (net.htmlparser.jericho.Element tag : tagsByName.get(tagName)) {
                    int removeBegin = tag.getStartTag().getEnd();
                    int removeEnd = tag.getEndTag().getBegin();
                    int contentLength = tag.getContent().toString().length();

//                    logger.info("["+sgmText.substring(0, removeBegin)+"]");
//                    DebugUtils.pause();
//                    logger.info("["+tag.getContent().toString()+"]");
//                    DebugUtils.pause();
//                    logger.info("["+sgmText.substring(removeEnd)+"]");
//                    DebugUtils.pause();

                    sgmText = sgmText.substring(0, removeBegin) + StringUtils.repeat(" ", contentLength) +
                            sgmText.substring(removeEnd);
                }
            }
        }

//        logger.info("After it is " + sgmText.length());
//
//        logger.info(sgmText);
//        DebugUtils.pause();

        return sgmText;
    }

    /**
     * It seems that ACE gold standard annotation are corresponding to the raw text without tags
     *
     * @param sgmText
     * @return
     */
    private String getDocumentText(String sgmText) {
        StringBuffer rawDocumentText = new StringBuffer(sgmText);
        Matcher tagMatcher = tagPattern.matcher(rawDocumentText);
        String documentText = tagMatcher.replaceAll("");

        return new NoiseTextFormatter(documentText).multiNewLineBreaker(language).getText();
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        try {
            // We need the next sgm file which will typically be 'currentSGMFile' - but we
            // will call getNextSGMFile() to be safe.
            File sgmFile = getNextSGMFile();
            // Setting currentSGMFile to null tells getNextSGMFile to get the next sgm file
            // rather than simply returning the current value.
            currentSGMFile = null;

            String sgmText = FileUtils.readFileToString(sgmFile);

            if (inputViewName != null) {
                try {
                    JCas inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
                    inputView.setDocumentText(sgmText);
                } catch (AnalysisEngineProcessException e) {
                    throw new CollectionException(e);
                }
            }


            // Create a view to store golden standard information.
            JCas goldStandardView = aJCas.createView(goldStandardViewName);
            String documentText = getDocumentText(handleMetaTags(sgmText));

            // Add document text to both view.
            goldStandardView.setDocumentText(documentText);
            aJCas.setDocumentText(documentText);

            SAXBuilder builder = new SAXBuilder();
            builder.setDTDHandler(null);

            InputSource inputSource = apfDtd.exists() ? new InputSource(new FileReader(apfDtd)) : new InputSource();

            builder.setEntityResolver((publicId, systemId) -> inputSource);

            // Source document information are useful to reach the golden standard file while annotating.
            UimaAnnotationUtils.setSourceDocumentInformation(aJCas, sgmFile.toURI().toString(), (int) sgmFile.length(),
                    0, !hasNext());

            File apfFile = getAPFFile(sgmFile);
            Document apfDoc = builder.build(apfFile);

            Element apfDocument = apfDoc.getRootElement().getChild("document");
            List<Element> apfEntities = apfDocument.getChildren("entity");
            List<Element> apfEvents = apfDocument.getChildren("event");

            Pair<Map<String, Entity>, Map<String, EntityMention>> aceIds = annotateEntities(apfEntities,
                    goldStandardView, documentText);

            Map<String, Entity> id2Entities = aceIds.getValue0();
            Map<String, EntityMention> id2EntityMentions = aceIds.getValue1();

            JCasUtil.select(goldStandardView, EntityMention.class);

            annotateEvents(apfEvents, goldStandardView, documentText, id2Entities, id2EntityMentions);

            Article article = new Article(aJCas);
            UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, aJCas);
            article.setArticleName(StringUtils.removeEnd(sgmFile.getName(), textFileSuffix));
            article.setLanguage(language);

//            if (inputViewName != null) {
//                try {
//                    JCas inputView = ViewCreatorAnnotator.createViewSafely(aJCas, inputViewName);
//                    inputView.setDocumentText(documentText);
//                } catch (AnalysisEngineProcessException e) {
//                    throw new CollectionException(e);
//                }
//            }
        } catch (CASException ce) {
            throw new CollectionException(ce);
        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(aceFileIndex, aceFilesToRead.size(), Progress.ENTITIES)};
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#hasNext()
     */
    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return getNextSGMFile() != null;
    }

    private void annotateEvents(List<Element> apfEvents, JCas aJCas, String documentText,
                                Map<String, Entity> id2Entities, Map<String, EntityMention> id2EntityMentions) {
        for (Element apfEvent : apfEvents) {
            Event event = new Event(aJCas);
            event.setEventType(apfEvent.getAttributeValue("TYPE"));
            event.setEventSubtype(apfEvent.getAttributeValue("SUBTYPE"));
            event.setId(apfEvent.getAttributeValue("ID"));
            event.setModality(apfEvent.getAttributeValue("MODALITY"));
            event.setPolarity(apfEvent.getAttributeValue("POLARITY"));
            event.setGenericity(apfEvent.getAttributeValue("GENERICITY"));
            event.setTense(apfEvent.getAttributeValue("TENSE"));
            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, 0, aJCas);

            // annotate arguments
            List<Element> eventArguments = apfEvent.getChildren("event_argument");
            List<EventArgumentLink> argumentLinks = new ArrayList<>();

            for (Element argument : eventArguments) {
                String argumentId = argument.getAttributeValue("REFID");

                EventArgumentLink argumentLink = new EventArgumentLink(aJCas);
                argumentLink.setEvent(event);
                argumentLink.setEntity(id2Entities.get(argumentId));
                argumentLink.setArgumentRole(argument.getAttributeValue("ROLE"));
                argumentLink.setComponentId(COMPONENT_ID);
                argumentLink.addToIndexes();
                argumentLinks.add(argumentLink);
                UimaAnnotationUtils.finishTop(argumentLink, COMPONENT_ID, 0, aJCas);
            }

            event.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));

            // annotate event mentions
            List<EventMention> mentions = new ArrayList<EventMention>();
            List<Element> eventMentions = apfEvent.getChildren("event_mention");
            for (Element eventMention : eventMentions) {
                Element mentionExtentNode = eventMention.getChild("extent").getChild("charseq");
                int mentionExtentStart = Integer.parseInt(mentionExtentNode.getAttributeValue("START"));
                int mentionExtentEnd = Integer.parseInt(mentionExtentNode.getAttributeValue("END"));

                EventMentionContext evmExtent = new EventMentionContext(aJCas);
                evmExtent.setBegin(mentionExtentStart);
                evmExtent.setEnd(mentionExtentEnd + 1);
                evmExtent.setComponentId(COMPONENT_ID);
                evmExtent.addToIndexes();

                // note that we annotate event mention with the anchor, then attach things to it
                Element mentionAnchor = eventMention.getChild("anchor").getChild("charseq");
                int anchorStart = Integer.parseInt(mentionAnchor.getAttributeValue("START"));
                int anchorEnd = Integer.parseInt(mentionAnchor.getAttributeValue("END"));

                EventMention mention = new EventMention(aJCas, anchorStart, anchorEnd + 1);
                mention.setMentionContext(evmExtent);
                mention.setId(eventMention.getAttributeValue("ID"));

                List<EventMentionArgumentLink> evmArgumentLinks = new ArrayList<EventMentionArgumentLink>();
                for (Element evmArugment : eventMention.getChildren("event_mention_argument")) {
                    String argumentId = evmArugment.getAttributeValue("REFID");
                    EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
                    argumentLink.setEventMention(mention);
                    EntityMention argumentEm = id2EntityMentions.get(argumentId);
                    argumentLink.setArgument(argumentEm);
                    argumentLink.setArgumentRole(evmArugment.getAttributeValue("ROLE"));
                    argumentLink.setComponentId(COMPONENT_ID);
                    argumentLink.addToIndexes();
                    evmArgumentLinks.add(argumentLink);
                }

                mention.setArguments(FSCollectionFactory.createFSList(aJCas, evmArgumentLinks));
                UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, 0, aJCas);

                mentions.add(mention);
            }

            for (EventMention mention : mentions) {
                mention.setReferringEvent(event);
                //annotate mention with the full type
                mention.setEventType(event.getEventType() + "_" + event.getEventSubtype());
            }

            event.setEventMentions(UimaConvenience.makeFsArray(mentions, aJCas));
        }
    }

    private Pair<Map<String, Entity>, Map<String, EntityMention>> annotateEntities(
            List<Element> apfEntities, JCas aJCas, String documentText) {
        Map<String, Entity> id2Entity = new HashMap<String, Entity>();
        Map<String, EntityMention> id2EntityMention = new HashMap<String, EntityMention>();

        for (Element apfEntity : apfEntities) {
            Entity namedEntity = new Entity(aJCas);
            namedEntity.setEntityType(apfEntity.getAttributeValue("TYPE"));
            namedEntity.setEntitySubtype(apfEntity.getAttributeValue("SUBTYPE"));
            namedEntity.setEntityClass(apfEntity.getAttributeValue("CLASS"));
            String entityId = apfEntity.getAttributeValue("ID");
            namedEntity.setAnnotationId(entityId);

            UimaAnnotationUtils.finishTop(namedEntity, COMPONENT_ID, 0, aJCas);


            // annotate mentions
            List<EntityMention> mentions = new ArrayList<EntityMention>();
            List<Element> entityMentions = apfEntity.getChildren("entity_mention");
            for (int j = 0; j < entityMentions.size(); j++) {
                Element entityMention = entityMentions.get(j);
                int start = Integer.parseInt(entityMention.getChild("extent").getChild("charseq")
                        .getAttributeValue("START"));
                int end = Integer.parseInt(entityMention.getChild("extent").getChild("charseq")
                        .getAttributeValue("END"));
                String givenText = entityMention.getChild("extent").getChild("charseq").getText();
                String parsedText = documentText.substring(start, end + 1);
                Matcher ampMatcher = ampPattern.matcher(parsedText);
                parsedText = ampMatcher.replaceAll("&");

                EntityMention mention = new EntityMention(aJCas, start, end + 1);
                mention.setEntityType(entityMention.getAttributeValue("TYPE"));
                String mentionAceId = entityMention.getAttributeValue("ID");
                mention.setId(mentionAceId);

                int headStart = Integer.parseInt(entityMention.getChild("head").getChild("charseq")
                        .getAttributeValue("START"));
                int headEnd = Integer.parseInt(entityMention.getChild("head").getChild("charseq")
                        .getAttributeValue("END"));

                mention.setHeadAnnotation(new ComponentAnnotation(aJCas, headStart, headEnd + 1));

                UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, 0, aJCas);

                mentions.add(mention);

                id2EntityMention.put(mentionAceId, mention);

                givenText = givenText.replaceAll("\\s+", " ");
                parsedText = givenText.replaceAll("\\s+", " ");
            }


            namedEntity.setEntityMentions(UimaConvenience.makeFsArray(mentions, aJCas));
            id2Entity.put(entityId, namedEntity);
        }

        return new Pair<>(id2Entity, id2EntityMention);
    }


}
