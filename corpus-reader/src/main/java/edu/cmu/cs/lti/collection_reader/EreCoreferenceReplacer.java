package edu.cmu.cs.lti.collection_reader;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.util.NuggetFormat;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
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
public class EreCoreferenceReplacer extends AbstractLoggingAnnotator {
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

    private Map<String, List<File>> source2AnnotationFiles;
    private DocumentBuilder documentBuilder;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        source2AnnotationFiles = new HashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        List<File> ereFiles = new ArrayList<>(FileUtils.listFiles(ereAnnotationDir, new String[]{ereExt}, false));
        logger.info(String.format("ERE reader found %d [%s] files in %s", ereFiles.size(), ereExt,
                ereAnnotationDir));

        List<File> sourceFiles = new ArrayList<>(FileUtils.listFiles(sourceTextDir, new String[]{sourceExt}, false));

        for (int i = 0; i < sourceFiles.size(); i++) {
            File sourceFile = sourceFiles.get(i);
            String baseName = sourceFile.getName().replaceAll("." + sourceExt + "$", "");

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
                    source2AnnotationFiles.put(sourceFile.getName(), Arrays.asList(correspondingEreFile));
                }
            } else {
                // When not splitting, the annotations between them are considered related.
                source2AnnotationFiles.put(sourceFile.getName(), correspondingEreFiles);
            }

            if (!found) {
                logger.warn("No ere file found for " + sourceFile.getName());
            }
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        File sourceFile = new File(JCasUtil.selectSingle(aJCas, SourceDocumentInformation.class).getUri());
        JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, aJCas);
        List<File> ereFiles = source2AnnotationFiles.get(sourceFile.getName());

        int sourceBeginOffset = 0;
        if (ereEventSplitDoc) {
            if (ereFiles.size() > 0) {
                Span cmpSpan = getCmpSpan(sourceFile, ereFiles.get(0));
                sourceBeginOffset = cmpSpan.getBegin();
            }
        }

        for (File ereFile : ereFiles) {
            try {
                annotateGoldStandard(goldView, ereFile, sourceBeginOffset);
            } catch (SAXException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Span getCmpSpan(File sourceFile, File ereFile) {
        String sourceBasename = sourceFile.getName().replaceAll("." + sourceExt + "$", "");
        String ereBasename = ereFile.getName().replaceAll("." + ereExt + "$", "");

        String[] offsetStrs = ereBasename.replace(sourceBasename + "_", "").split("-");

        int begin = Integer.parseInt(offsetStrs[0]);
        int end = Integer.parseInt(offsetStrs[1]);

        return Span.of(begin, end + 1);
    }

    private void annotateGoldStandard(JCas goldView, File ereFile, int beginOffset) throws IOException, SAXException {
        // Remove existing.
        removeFromIndex(goldView, EventMention.class);
        removeFromIndex(goldView, Event.class);
        removeFromIndex(goldView, EntityMention.class);
        removeFromIndex(goldView, Entity.class);
        removeFromIndex(goldView, EventMentionArgumentLink.class);
        removeFromIndex(goldView, EntityMentionRelation.class);

        Document document = documentBuilder.parse(ereFile);
        Map<String, EntityMention> id2EntityMention = new HashMap<>();
        annotateEntity(goldView, document, id2EntityMention, beginOffset);
        annotateFillers(goldView, document, id2EntityMention, beginOffset);
        annotateRelations(goldView, document, id2EntityMention);

        // The following two are all coreference clusters, really based on what the annotation system is.
        annotateEvents(goldView, document, id2EntityMention, beginOffset);
        annotateEventHoppers(goldView, document, id2EntityMention, beginOffset);
    }

    private <T extends TOP> void removeFromIndex(JCas jcas, Class<T> clazz) {
        List<T> annos = UimaConvenience.getAnnotationList(jcas, clazz);
        for (T anno : annos) {
            anno.removeFromIndexes();
        }
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

    public static void main(String[] argv) throws UIMAException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, argv[0]);

        Configuration ereConfig = new Configuration(new File(argv[1]));
        Configuration bratConfig = new Configuration(new File(argv[2]));

        AnalysisEngineDescription ereAnnotator = AnalysisEngineFactory
                .createEngineDescription(
                        EreCoreferenceReplacer.class, typeSystemDescription,
                        EreCoreferenceReplacer.PARAM_ERE_ANNOTATION_DIR,
                        ereConfig.get("edu.cmu.cs.lti.data.annotation.path"),
                        EreCoreferenceReplacer.PARAM_SOURCE_TEXT_DIR,
                        ereConfig.get("edu.cmu.cs.lti.data.source.path"),
                        EreCoreferenceReplacer.PARAM_ERE_ANNOTATION_EXT,
                        ereConfig.get("edu.cmu.cs.lti.data.annotation.extension"),
                        EreCoreferenceReplacer.PARAM_SOURCE_EXT,
                        ereConfig.get("edu.cmu.cs.lti.data.source.extension"),
                        EreCoreferenceReplacer.PARAM_ERE_EVENT_SPLIT_DOC,
                        ereConfig.getBoolean("edu.cmu.cs.lti.data.event.split_doc", false)
                );

        AnalysisEngineDescription afterAnnotator = AnalysisEngineFactory
                .createEngineDescription(
                        AfterLinkGoldStandardAnnotator.class, typeSystemDescription,
                        AfterLinkGoldStandardAnnotator.PARAM_ANNOTATION_DIR,
                        bratConfig.get("edu.cmu.cs.lti.data.annotation.path")
                );


        AnalysisEngineDescription addSpan = AnalysisEngineFactory.createEngineDescription(EventSpanProcessor.class,
                typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(argv[3], argv[4]);
        SimplePipeline.runPipeline(reader, ereAnnotator, afterAnnotator, addSpan, writer);
    }
}
