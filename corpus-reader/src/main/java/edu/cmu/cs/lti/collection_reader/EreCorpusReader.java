package edu.cmu.cs.lti.collection_reader;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.*;
import edu.cmu.cs.lti.util.NuggetFormat;
import net.htmlparser.jericho.Element;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Read rich ERE style data.
 * Date: 1/25/16
 * Time: 5:32 PM
 *
 * @author Zhengzhong Liu
 */
public class EreCorpusReader extends AbstractCollectionReader {
    public static final String PARAM_SOURCE_TEXT_DIR = "sourceTextDir";
    @ConfigurationParameter(name = PARAM_SOURCE_TEXT_DIR)
    private File sourceTextDir;

    public static final String PARAM_ERE_ANNOTATION_DIR = "ereAnnotationDir";
    @ConfigurationParameter(name = PARAM_ERE_ANNOTATION_DIR)
    private File ereAnnotationDir;

    public static final String PARAM_SOURCE_EXT = "sourceExtension";
    @ConfigurationParameter(name = PARAM_SOURCE_EXT)
    private String sourceExt;

    public static final String PARAM_ERE_ANNOTATION_EXT = "ereSourceExtension";
    @ConfigurationParameter(name = PARAM_ERE_ANNOTATION_EXT)
    private String ereExt;

    public static final String PARAM_ERE_EVENT_SPLIT_DOC = "ereEventSplitDoc";
    @ConfigurationParameter(name = PARAM_ERE_EVENT_SPLIT_DOC, defaultValue = "false")
    private boolean ereEventSplitDoc;

    public static final String PARAM_REMOVE_QUOTES = "removeQuotes";
    @ConfigurationParameter(name = PARAM_REMOVE_QUOTES)
    private boolean removeQuotes;

    public static final String PARAM_QUOTED_AREA_FILE = "quotedAreaFile";
    @ConfigurationParameter(name = PARAM_QUOTED_AREA_FILE, mandatory = false)
    private File quotedAreaFile;

    private List<Pair<File, List<File>>> sourceAndAnnotationFiles;

    private List<Integer> annotationOffsets;

    private int fileIndex;

    private DocumentBuilder documentBuilder;
    private ArrayListMultimap<String, Span> quotesFromFile;

    private boolean noAnnotation = false;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        sourceAndAnnotationFiles = new ArrayList<>();

        if (sourceTextDir != null) {
            logger.info(String.format("Looking for source files in %s.", sourceTextDir.getAbsolutePath()));
        }

        if (ereAnnotationDir != null) {
            logger.info("Looking for ere files in : " + ereAnnotationDir.getAbsolutePath());
        } else {
            logger.info("Annotations are not provided.");
            noAnnotation = true;
        }

        List<File> sourceFiles = new ArrayList<>(FileUtils.listFiles(sourceTextDir, new String[]{sourceExt}, false));
        logger.info(String.format("ERE reader found %d source files with extension [%s] from %s", sourceFiles.size(),
                sourceExt, sourceTextDir));


        Collection<File> ereFiles = null;
        if (!noAnnotation) {
            ereFiles = FileUtils.listFiles(ereAnnotationDir, new String[]{ereExt}, false);
            logger.info(String.format("ERE reader found %d [%s] files in %s", ereFiles.size(), ereExt,
                    ereAnnotationDir));
        }

        List<String> baseNames = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            baseNames.add(sourceFile.getName().replaceAll("." + sourceExt + "$", ""));
        }

        if (noAnnotation) {
            for (File sourceFile : sourceFiles) {
                sourceAndAnnotationFiles.add(Pair.of(sourceFile, new ArrayList<>()));
            }
        } else {
            for (int i = 0; i < sourceFiles.size(); i++) {
                File sourceFile = sourceFiles.get(i);
                String baseName = baseNames.get(i);

                boolean found = false;

                List<File> correspondingEreFiles = new ArrayList<>();

                for (File ereFile : ereFiles) {
                    if (ereFile.getName().startsWith(baseName)) {
                        found = true;
                        correspondingEreFiles.add(ereFile);
                    }
                }

                if (ereEventSplitDoc) {
                    // When split doc, the annotations between them are not related.
                    for (File correspondingEreFile : correspondingEreFiles) {
                        sourceAndAnnotationFiles.add(Pair.of(sourceFile, Arrays.asList(correspondingEreFile)));
                    }
                } else {
                    // When not splitting, the annotations between them are considered related.
                    sourceAndAnnotationFiles.add(Pair.of(sourceFile, correspondingEreFiles));
                }

                if (!found) {
                    logger.warn("No ere file found for " + sourceFile.getName());
                }
            }
        }

        if (quotedAreaFile != null) {
            try {
                quotesFromFile = getQuotesFromFile();
            } catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }


        logger.info(String.format("%d files are going to be read.", sourceAndAnnotationFiles.size()));
        fileIndex = 0;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

//        sourceIterator = sourceAndAnnotationFiles.keySet().iterator();
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        Pair<File, List<File>> sourceAnnoPair = sourceAndAnnotationFiles.get(fileIndex);
        File sourceFile = sourceAnnoPair.getKey();


        int sourceBeginOffset = 0;
        String sourceFileStr = FileUtils.readFileToString(sourceFile);

        List<File> ereFiles = sourceAnnoPair.getValue();
        if (ereEventSplitDoc) {
            if (ereFiles.size() > 0) {
                Span cmpSpan = getCmpSpan(sourceFile, ereFiles.get(0));
                sourceBeginOffset = cmpSpan.getBegin();
                sourceFileStr = sourceFileStr.substring(cmpSpan.getBegin(), cmpSpan.getEnd());
            }
        }

        UimaAnnotationUtils.setSourceDocumentInformation(jCas, sourceFile.toURI().toURL().toString(),
                (int) sourceFile.length(), sourceBeginOffset, true);

        String cleanedText = new NoiseTextFormatter(sourceFileStr).cleanAll(language);
        ArrayListMultimap<String, Element> tagsByName = ForumStructureParser.indexTagByName(sourceFileStr);

        List<Span> quotedSpans = quotedAreaFile == null ? ForumStructureParser.getQuotesFromElement(tagsByName) :
                quotesFromFile.get(sourceFile.getName());

        String documentText = removeQuotes ? ForumStructureParser.removeQuoteStr(cleanedText, quotedSpans) : cleanedText;

        Article article = new Article(jCas);
        UimaAnnotationUtils.finishAnnotation(article, 0, documentText.length(), COMPONENT_ID, 0, jCas);
        article.setArticleName(StringUtils.removeEnd(sourceFile.getName(), "." + sourceExt));
        article.setLanguage(language);

        if (sourceFileStr.length() != documentText.length()) {
            throw new CollectionException(new Exception(String.format(
                    "Length difference after cleaned, before : %d, " + "after : %d",
                    sourceFileStr.length(), documentText.length())));
        }

        if (inputViewName != null) {
            try {
                JCas inputView = ViewCreatorAnnotator.createViewSafely(jCas, inputViewName);
                inputView.setDocumentText(sourceFileStr);
                ForumStructureParser.annotateTagAreas(inputView, tagsByName, COMPONENT_ID);
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

        UimaAnnotationUtils.setSourceDocumentInformation(goldView, sourceFile.toURI().toURL().toString(),
                (int) sourceFile.length(), sourceBeginOffset, true);

        jCas.setDocumentText(documentText);
        goldView.setDocumentText(documentText);

        ForumStructureParser.annotateTagAreas(jCas, tagsByName, COMPONENT_ID);
        ForumStructureParser.annotateTagAreas(goldView, tagsByName, COMPONENT_ID);

        for (File ereFile : ereFiles) {
            try {
                annotateGoldStandard(goldView, ereFile, sourceBeginOffset);
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }

        fileIndex++;
    }

    private ArrayListMultimap<String, Span> getQuotesFromFile() throws IOException {
        ArrayListMultimap<String, Span> quotedSpans = ArrayListMultimap.create();

        for (String line : FileUtils.readLines(quotedAreaFile)) {
            String[] fields = line.split("\t");
            quotedSpans.put(fields[0], Span.of(Integer.valueOf(fields[1]), Integer.valueOf(fields[2])));
        }

        return quotedSpans;
    }


//    private List<Span> getQuotesFromElement(ArrayListMultimap<String, Element> tagsByName) {
//        List<Span> quotedSpans = new ArrayList<>();
//        for (Element quote : tagsByName.get("quote")) {
//            quotedSpans.add(Span.of(quote.getBegin(), quote.getEnd()));
//        }
//        return quotedSpans;
//    }
//
//    private String removeQuoteStr(String original, List<Span> quotedAreas) {
//        StringBuilder sb = new StringBuilder(original);
//        for (Span quoteArea : quotedAreas) {
//            for (int i = quoteArea.getBegin(); i < quoteArea.getEnd(); i++) {
//                sb.setCharAt(i, ' ');
//            }
//        }
//        return sb.toString();
//    }
//
//    private void annotateTagAreas(JCas aJCas, ArrayListMultimap<String, Element> tagsByName) {
//        for (Map.Entry<String, Element> tagByName : tagsByName.entries()) {
//            Element tag = tagByName.getValue();
//            TaggedArea area = new TaggedArea(aJCas, tag.getBegin(), tag.getEnd());
//            area.setTagName(tagByName.getKey());
//
//            Attributes attributes = tag.getAttributes();
//
//            StringArray attributeNames = new StringArray(aJCas, attributes.size());
//            StringArray attributeValues = new StringArray(aJCas, attributes.size());
//
//            for (int i = 0; i < attributes.size(); i++) {
//                Attribute attribute = attributes.get(i);
//                attributeNames.set(i, attribute.getKey());
//                attributeValues.set(i, attribute.getValue());
//            }
//
//            area.setTagAttributeNames(attributeNames);
//            area.setTagAttributeValues(attributeValues);
//
//            UimaAnnotationUtils.finishAnnotation(area, COMPONENT_ID, 0, aJCas);
//        }
//    }

    private Span getCmpSpan(File sourceFile, File ereFile) {
        String sourceBasename = sourceFile.getName().replaceAll("." + sourceExt + "$", "");
        String ereBasename = ereFile.getName().replaceAll("." + ereExt + "$", "");

        String[] offsetStrs = ereBasename.replace(sourceBasename + "_", "").split("-");

        int begin = Integer.parseInt(offsetStrs[0]);
        int end = Integer.parseInt(offsetStrs[1]);

        return Span.of(begin, end + 1);
    }

    private void annotateGoldStandard(JCas goldView, File ereFile, int beginOffset) throws IOException, SAXException {
        Document document = documentBuilder.parse(ereFile);
        Map<String, EntityMention> id2EntityMention = new HashMap<>();
        annotateEntity(goldView, document, id2EntityMention, beginOffset);
        annotateFillers(goldView, document, id2EntityMention, beginOffset);
        annotateRelations(goldView, document, id2EntityMention);

        // The following two are all coreference clusters, really based on what the annotation system is.
        annotateEvents(goldView, document, id2EntityMention, beginOffset);
        annotateEventHoppers(goldView, document, id2EntityMention, beginOffset);
    }

    /**
     * Older ERE annotation call the node Event.
     *
     * @param view
     * @param document
     * @param id2EntityMention
     * @param beginOffset
     */
    private void annotateEvents(JCas view, Document document, Map<String, EntityMention> id2EntityMention, int
            beginOffset) {
        NodeList hopperNodes = document.getElementsByTagName("event");

        for (int hopperNodeIndex = 0; hopperNodeIndex < hopperNodes.getLength(); hopperNodeIndex++) {
            Node hopperNode = hopperNodes.item(hopperNodeIndex);
            String hopperId = getAttribute(hopperNode, "id");

            List<EventMention> mentionCluster = new ArrayList<>();
            for (Node eventMentionNode : getSubNodes(hopperNode, "event_mention")) {
                String mentionId = getAttribute(eventMentionNode, "id");
                String mentionType = getAttribute(eventMentionNode, "type");
                String mentionSubType = getAttribute(eventMentionNode, "subtype");
                String mergedType = NuggetFormat.canonicalType(mentionType, mentionSubType);

                Node triggerNode = getSubNode(eventMentionNode, "trigger");
                int triggerStart = Integer.parseInt(getAttribute(triggerNode, "offset")) - beginOffset;
                int triggerLength = Integer.parseInt(getAttribute(triggerNode, "length"));
                int triggerEnd = triggerStart + triggerLength;

                if (validateAnnotation(view, triggerStart, triggerEnd, triggerNode.getTextContent())) {
                    EventMention mention = new EventMention(view, triggerStart, triggerEnd);
                    mention.setEventType(mergedType);
                    mention.setRealisType("None");

                    List<Node> argNodes = getSubNodes(getSubNode(eventMentionNode, "args"), "arg");
                    for (Node argNode : argNodes) {
//                        String argEntityId = getAttribute(argNode, "entity_id");
                        String argEntityMentionId = getAttribute(argNode, "entity_mention_id");
                        String argRole = getAttribute(argNode, "type");

                        EventMentionArgumentLink link = new EventMentionArgumentLink(view);
                        link.setArgumentRole(argRole);
                        EntityMention argEntityMention = id2EntityMention.get(argEntityMentionId);
                        link.setArgument(argEntityMention);
                        link.setEventMention(mention);
                        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, 0, view);
                    }

                    List<Node> placeNodes = getSubNodes(getSubNode(eventMentionNode, "places"), "place");
                    for (Node placeNode : placeNodes) {
                        String argEntityMentionId = getAttribute(placeNode, "entity_mention_id");
                        EventMentionArgumentLink link = new EventMentionArgumentLink(view);
                        link.setArgumentRole("place");
                        EntityMention argEntityMention = id2EntityMention.get(argEntityMentionId);
                        link.setArgument(argEntityMention);
                        link.setEventMention(mention);
                        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, 0, view);
                    }

                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionId, view);
                    mentionCluster.add(mention);
                }
            }

            Event event = new Event(view);
            event.setEventMentions(FSCollectionFactory.createFSArray(view, mentionCluster));
            for (EventMention eventMention : mentionCluster) {
                eventMention.setReferringEvent(event);
            }
            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, hopperId, view);
        }
    }

    /**
     * Later ERE annotation call the node Hoppers instead of Events.
     *
     * @param view
     * @param document
     * @param id2EntityMention
     * @param beginOffset
     */
    private void annotateEventHoppers(JCas view, Document document, Map<String, EntityMention> id2EntityMention, int
            beginOffset) {
        NodeList hopperNodes = document.getElementsByTagName("hopper");

        for (int hopperNodeIndex = 0; hopperNodeIndex < hopperNodes.getLength(); hopperNodeIndex++) {
            Node hopperNode = hopperNodes.item(hopperNodeIndex);
            String hopperId = getAttribute(hopperNode, "id");

            List<EventMention> mentionCluster = new ArrayList<>();
            for (Node eventMentionNode : getSubNodes(hopperNode, "event_mention")) {
                String mentionId = getAttribute(eventMentionNode, "id");
                String mentionType = getAttribute(eventMentionNode, "type");
                String mentionSubType = getAttribute(eventMentionNode, "subtype");

                String mergedType = NuggetFormat.canonicalType(mentionType, mentionSubType);
                String realisStatus = NuggetFormat.canonicalType(getAttribute(eventMentionNode, "realis"));

                Node triggerNode = getSubNode(eventMentionNode, "trigger");
                int triggerStart = Integer.parseInt(getAttribute(triggerNode, "offset")) - beginOffset;
                int triggerLength = Integer.parseInt(getAttribute(triggerNode, "length"));
                int triggerEnd = triggerStart + triggerLength;

                if (validateAnnotation(view, triggerStart, triggerEnd, triggerNode.getTextContent())) {
                    EventMention mention = new EventMention(view, triggerStart, triggerEnd);
                    mention.setEventType(mergedType);
                    mention.setRealisType(realisStatus);

                    List<Node> argNodes = getSubNodes(eventMentionNode, "em_arg");
                    for (Node argNode : argNodes) {
//                        String argEntityId = getAttribute(argNode, "entity_id");
                        String argEntityMentionId = getAttribute(argNode, "entity_mention_id");
                        if (argEntityMentionId == null) {
                            argEntityMentionId = getAttribute(argNode, "filler_id");
                        }

                        String argRole = NuggetFormat.canonicalType(getAttribute(argNode, "role"));
                        String realis = NuggetFormat.canonicalType(getAttribute(argNode, "realis"));

                        EventMentionArgumentLink link = new EventMentionArgumentLink(view);
                        link.setArgumentRole(argRole);
                        EntityMention argEntityMention = id2EntityMention.get(argEntityMentionId);
                        link.setArgument(argEntityMention);
                        link.setEventMention(mention);
                        link.setRealis(realis);
                        UimaAnnotationUtils.finishTop(link, COMPONENT_ID, 0, view);
                    }

                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, mentionId, view);
                    mentionCluster.add(mention);
                } else {
                    logger.warn(String.format("Omit event mention [%s], range [%d-%d], at doc [%s]",
                            triggerNode.getTextContent(), triggerStart, triggerEnd, UimaConvenience.getDocId(view)));
                }
            }

            Event event = new Event(view);
            event.setEventMentions(FSCollectionFactory.createFSArray(view, mentionCluster));
            for (EventMention eventMention : mentionCluster) {
                eventMention.setReferringEvent(event);
            }

            UimaAnnotationUtils.finishTop(event, COMPONENT_ID, hopperId, view);
        }
    }

    private void annotateRelations(JCas view, Document document, Map<String, EntityMention> id2EntityMention) {
        NodeList relationNodes = document.getElementsByTagName("relation");

        for (int relationNodeIndex = 0; relationNodeIndex < relationNodes.getLength(); relationNodeIndex++) {
            Node relationNode = relationNodes.item(relationNodeIndex);
            String relationId = getAttribute(relationNode, "id");
            String relationType = getAttribute(relationNode, "type");
            String relationSubType = getAttribute(relationNode, "subtype");

            String mergedType = NuggetFormat.canonicalType(relationType, relationSubType);

            Node relationMentionNode = getSubNode(relationNode, "relation_mention");

            // Assume relation have only 2 argument.
            Node relationArg1 = getSubNode(relationMentionNode, "rel_arg1");
            Node relationArg2 = getSubNode(relationMentionNode, "rel_arg2");

            if (relationArg1 != null && relationArg2 != null) {
                String arg1ElementId = getAttribute(relationArg1, "entity_mention_id");
                String arg2ElementId = getAttribute(relationArg2, "entity_mention_id");

                EntityMentionRelation mentionRelation = new EntityMentionRelation(view);
                mentionRelation.setHead(id2EntityMention.get(arg1ElementId));
                mentionRelation.setChild(id2EntityMention.get(arg2ElementId));
                mentionRelation.setRelationType(mergedType);
                UimaAnnotationUtils.finishTop(mentionRelation, COMPONENT_ID, relationId, view);
            }

            // Or spelled as arg. We currently ignore such relations.
            List<Node> relationsArgs = getSubNodes(relationMentionNode, "rel_arg1");
            for (Node relationsArg : relationsArgs) {
                //        <arg type="role" entity_id="ent-12744800" entity_mention_id="m-138">总理</arg>
            }

        }
    }

    private void annotateFillers(JCas view, Document document, Map<String, EntityMention> id2EntityMention, int
            beginOffset) {
        NodeList fillerNodes = document.getElementsByTagName("filler");

        for (int fillerNodeIndex = 0; fillerNodeIndex < fillerNodes.getLength(); fillerNodeIndex++) {
            Node fillerNode = fillerNodes.item(fillerNodeIndex);
            String fillerId = getAttribute(fillerNode, "id");
            int offset = Integer.parseInt(getAttribute(fillerNode, "offset")) - beginOffset;
            int length = Integer.parseInt(getAttribute(fillerNode, "length"));
            int end = offset + length;
            String type = getAttribute(fillerNode, "type");

            if (validateAnnotation(view, offset, end, fillerNode.getTextContent())) {
                EntityMention filler = new EntityMention(view);
                filler.setEntityType(type);
                filler.setId(fillerId);
                UimaAnnotationUtils.finishAnnotation(filler, COMPONENT_ID, fillerId, view);

                id2EntityMention.put(fillerId, filler);
            }
        }
    }

    private List<EntityMention> annotateEntity(JCas view, Document document,
                                               Map<String, EntityMention> id2EntityMention, int beginOffset) {
        NodeList entityNodes = document.getElementsByTagName("entity");

        List<EntityMention> entityMentions = new ArrayList<>();
        for (int entityIndex = 0; entityIndex < entityNodes.getLength(); entityIndex++) {
            Node entityNode = entityNodes.item(entityIndex);
            String entityId = getAttribute(entityNode, "id");
            String entityType = getAttribute(entityNode, "type");

            List<Node> entityMentionNodes = getSubNodes(entityNode, "entity_mention");

            for (Node entityMentionNode : entityMentionNodes) {
                String entityMentionId = getAttribute(entityMentionNode, "id");
                int entityMentionStart = Integer.parseInt(getAttribute(entityMentionNode, "offset")) - beginOffset;
                int entityMentionLength = Integer.parseInt(getAttribute(entityMentionNode, "length"));
                int entityMentionEnd = entityMentionStart + entityMentionLength;

                String mentionText = "";

                Node mentionTextNode = getSubNode(entityMentionNode, "mention_text");
                if (mentionTextNode != null) {
                    mentionText = mentionTextNode.getNodeValue();
                }

                if (validateAnnotation(view, entityMentionStart, entityMentionEnd, mentionText)) {
                    EntityMention mention = new EntityMention(view, entityMentionStart, entityMentionEnd);
                    mention.setEntityType(entityType);
                    id2EntityMention.put(entityMentionId, mention);
                    entityMentions.add(mention);
                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, entityMentionId, view);
                }
            }

            Entity entity = new Entity(view);
            entity.setEntityMentions(FSCollectionFactory.createFSArray(view, entityMentions));
            entity.setEntityType(entityType);
            UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, entityId, view);
        }
        return entityMentions;
    }

    private String getAttribute(Node node, String attributeName) {
        NamedNodeMap entityMentionAttributes = node.getAttributes();
        Node attributeNode = entityMentionAttributes.getNamedItem(attributeName);

        if (attributeNode == null) {
            return null;
        }

        return attributeNode.getNodeValue();
    }

    private Node getSubNode(Node node, String subNodeName) {
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node subNode = node.getChildNodes().item(i);
            if (subNode.getNodeName().equals(subNodeName)) {
                return subNode;
            }
        }
        return null;
    }

    private List<Node> getSubNodes(Node node, String subNodeName) {
        List<Node> subnodes = new ArrayList<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node subnode = node.getChildNodes().item(i);
            if (subnode.getNodeName().equals(subNodeName)) {
                subnodes.add(subnode);
            }
        }
        return subnodes;
    }

    private boolean validateAnnotation(JCas view, int start, int end, String expectedText) {
        String originString = view.getDocumentText().substring(start, end).replaceAll("\\n", " ");
        if (originString.equals(expectedText)) {
            return true;
        } else {
            logger.warn(String.format("Original string [%s] is not equal to expected [%s].", originString,
                    expectedText));
            return false;
        }
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        return fileIndex < sourceAndAnnotationFiles.size();
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(fileIndex, sourceAndAnnotationFiles.size(), Progress.ENTITIES)};
    }
}
