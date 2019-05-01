package edu.cmu.cs.lti.collection_reader;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/22/18
 * Time: 12:07 PM
 *
 * @author Zhengzhong Liu
 */
public class JsonEventDataReader extends AbstractLoggingAnnotator {
    public static final String PARAM_JSON_ANNO_DIR = "inputDir";
    @ConfigurationParameter(name = PARAM_JSON_ANNO_DIR)
    private File annoDir;

    public static final String PARAM_CLEANUP_ENTITY = "cleanUpEntity";
    @ConfigurationParameter(name = PARAM_CLEANUP_ENTITY)
    private boolean cleanUpEntity;

    private Gson gson;

    class AnnoDoc {
        String text;
        List<JEvent> events;
        List<JEntity> entities;
    }

    class JEvent {
        String id;
        String annotation;
        List<JEventMention> mentions;
    }

    class JEventMention {
        String id;
        String annotation;
        String text;
        List<Span> spans;
        List<JArgument> arguments;
        String type;
    }

    class JArgument {
        String arg;
        String role;
        ArgMeta meta;
    }

    class ArgMeta {
        boolean incorporated;
        boolean succeeding;
        boolean implicit;
    }

    class JEntity {
        String id;
        String annotation;
        List<JEntityMention> mentions;
        String type;
    }

    class JEntityMention {
        String id;
        String annotation;
        String text;
        String type;
        List<Span> spans;
    }

    class Span {
        int begin;
        int end;
    }

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        gson = new Gson();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String docid = UimaConvenience.getArticleName(aJCas);

        File annotationFile = new File(annoDir, docid + ".json");

        if (annotationFile.exists()) {
            try {
                AnnoDoc annoDoc = gson.fromJson(FileUtils.readFileToString(annotationFile), AnnoDoc.class);
                if (cleanUpEntity) {
                    UimaNlpUtils.mergeSameHeadEntities(aJCas);
                    UimaNlpUtils.voteNerType(aJCas);
                }
                addAnnotations(aJCas, annoDoc);
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        } else {
            logger.warn(String.format("Cannot find [%s].", annotationFile.getPath()));
        }
    }

    private Pair<Integer, Integer> getBoundary(List<Span> spans) {
        int earliestBegin = Integer.MAX_VALUE;
        int latestEnd = 0;

        for (Span span : spans) {
            if (span.begin < earliestBegin) {
                earliestBegin = span.begin;
            }
            if (span.end > latestEnd) {
                latestEnd = span.end;
            }
        }

        return Pair.of(earliestBegin, latestEnd);
    }

    private void annotateSpan(JCas aJCas, DiscontinuousComponentAnnotation anno, List<Span> spans) {
        anno.setRegions(new FSArray(aJCas, spans.size()));

        Pair<Integer, Integer> be = getBoundary(spans);
        int earliestBegin = be.getLeft();
        int latestEnd = be.getRight();

        for (int spanIndex = 0; spanIndex < spans.size(); spanIndex++) {
            Span span = spans.get(spanIndex);
            Annotation region = new Annotation(aJCas, span.begin, span.end);
            anno.setRegions(spanIndex, region);
        }
        anno.setBegin(earliestBegin);
        anno.setEnd(latestEnd);
    }

    private void addToEventCluster(JCas aJCas, Event event, List<EventMention> newMentions) {
        for (EventMention newMention : newMentions) {
            newMention.setReferringEvent(event);
        }
        event.setEventMentions(UimaConvenience.extendFSArray(aJCas, event.getEventMentions(), newMentions,
                EventMention.class));
    }


    private void addAnnotations(JCas aJCas, AnnoDoc annoDoc) {
        Map<String, EntityMention> id2Ent = new HashMap<>();

        // First create an index of the original mentions.
        Map<Word, EventMention> eventHeadMap = new HashMap<>();
        Map<Pair, EventMention> eventSpanMap = new HashMap<>();
        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            eventHeadMap.put(eventMention.getHeadWord(), eventMention);
            eventSpanMap.put(Pair.of(eventMention.getBegin(), eventMention.getEnd()), eventMention);
        }

        Map<Word, EntityMention> entityHeadMap = new HashMap<>();
        Map<Pair, EntityMention> entitySpanMap = new HashMap<>();
        ArrayListMultimap<Word, EntityMention> entityWordMap = ArrayListMultimap.create();
        for (EntityMention entityMention : JCasUtil.select(aJCas, EntityMention.class)) {
            entityHeadMap.put(entityMention.getHead(), entityMention);
            entitySpanMap.put(Pair.of(entityMention.getBegin(), entityMention.getEnd()), entityMention);
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, entityMention)) {
                entityWordMap.put(token, entityMention);
            }
        }

        for (JEntity jEntity : annoDoc.entities) {
            Entity entity = null;

            List<EntityMention> newMentions = new ArrayList<>();

            // Here we map mentions from jMentions to uima Mentions.
            for (JEntityMention jMention : jEntity.mentions) {
                Pair<Integer, Integer> boundaries = getBoundary(jMention.spans);

                EntityMention mention;

                if (entitySpanMap.containsKey(boundaries)) {
                    // Found entity in the exact boundary.
                    mention = entitySpanMap.get(boundaries);
                    entity = mention.getReferingEntity();
                } else {
                    mention = new EntityMention(aJCas);
                    mention.setEntityType(jMention.type);
                    annotateSpan(aJCas, mention, jMention.spans);
                    StanfordCorenlpToken entityHead = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    mention.setHead(entityHead);

                    if (entityHeadMap.containsKey(entityHead)) {
                        // Found entity mention sharing head, consider this mention to be coreferential.
                        EntityMention existingMention = entityHeadMap.get(entityHead);
                        entity = existingMention.getReferingEntity();
                    } else if (entityWordMap.containsKey(entityHead)) {
                        // There is an entity mention that refers this entity, let's see how to deal with it.
                        List<EntityMention> coveringMentions = entityWordMap.get(entityHead);
                        for (EntityMention coveringMention : coveringMentions) {
                            // We can at least deal with proper nouns.
                            if (UimaNlpUtils.compatibleMentions(mention, coveringMention)) {
                                entity = coveringMention.getReferingEntity();
                            }
                            if (entity != null) {
                                // Once we found 1 potential cluster, then that's it.
                                break;
                            }
                        }
                    }
                    newMentions.add(mention);
                    UimaAnnotationUtils.finishAnnotation(mention, COMPONENT_ID, jMention.id, aJCas);
                    entityHeadMap.put(entityHead, mention);
                    entitySpanMap.put(Pair.of(mention.getBegin(), mention.getEnd()), mention);
                }

                id2Ent.put(jMention.id, mention);
            }

            if (entity == null) {
                // No existing entity found for them, create a new one.
                createNewEntities(aJCas, newMentions, jEntity.id);
            } else {
                // Existing entity found, add the new mentions to it.
                UimaNlpUtils.addToEntityCluster(aJCas, entity, newMentions);
            }
        }

        for (JEvent jEvent : annoDoc.events) {
            Event event = null;

            List<EventMention> newMentions = new ArrayList<>();
            for (JEventMention jMention : jEvent.mentions) {
                Pair<Integer, Integer> boundaries = getBoundary(jMention.spans);

                EventMention evm;
                if (eventSpanMap.containsKey(boundaries)) {
                    // This is an seen event.
                    evm = eventSpanMap.get(boundaries);
                    event = evm.getReferringEvent();
                } else {
                    evm = new EventMention(aJCas);
                    evm.setEventType(jMention.type);
                    annotateSpan(aJCas, evm, jMention.spans);
                    // This requires stanford annotation first.
                    StanfordCorenlpToken eventHead = UimaNlpUtils.findHeadFromStanfordAnnotation(evm);
                    evm.setHeadWord(eventHead);

                    if (eventHeadMap.containsKey(eventHead)) {
                        EventMention existingMention = eventHeadMap.get(eventHead);
                        event = existingMention.getReferringEvent();
                    }

                    newMentions.add(evm);
                    UimaAnnotationUtils.finishAnnotation(evm, COMPONENT_ID, jMention.id, aJCas);
                }

                // Check the frame information from previous parsers.
                StanfordCorenlpToken eventHead = (StanfordCorenlpToken) evm.getHeadWord();
                FSList eventHeadArgsFS = eventHead.getChildSemanticRelations();

                evm.setFrameName(eventHead.getFrameName());

                // There may be duplicated arguments with the gold ones.
                Map<Pair<Integer, Integer>, EventMentionArgumentLink> argumentLinkMap = new HashMap<>();

                if (eventHeadArgsFS != null) {
                    List<EventMentionArgumentLink> eventArgs = new ArrayList<>();
                    for (SemanticRelation relation : FSCollectionFactory.create(eventHeadArgsFS,
                            SemanticRelation.class)) {
                        EventMentionArgumentLink argumentLink = new EventMentionArgumentLink((aJCas));
                        SemanticArgument argument = relation.getChild();

                        Pair<Integer, Integer> argumentBoundary = Pair.of(argument.getBegin(), argument.getEnd());

                        EntityMention argumentEntityMention;
                        if (entitySpanMap.containsKey(argumentBoundary)) {
                            argumentEntityMention = entitySpanMap.get(argumentBoundary);
                        } else {
                            argumentEntityMention = UimaNlpUtils.createArgMention(aJCas, argument
                                    .getBegin(), argument.getEnd(), argument.getComponentId());
                            Word argumentHead = argumentEntityMention.getHead();

                            if (entityHeadMap.containsKey(argumentHead)) {
                                // Found entity mention sharing head, consider this mention to be coreferential.
                                EntityMention existingMention = entityHeadMap.get(argumentHead);
                                Entity entity = existingMention.getReferingEntity();
                                UimaNlpUtils.addToEntityCluster(aJCas, entity, Arrays.asList(argumentEntityMention));
                            } else {
                                // This is a new entity without clear cluster, create a singleton entity.
                                createNewEntities(aJCas, Arrays.asList(argumentEntityMention), "0");
                            }

                            entitySpanMap.put(Pair.of(argumentEntityMention.getBegin(),
                                    argumentEntityMention.getEnd()), argumentEntityMention);
                            entityHeadMap.put(argumentHead, argumentEntityMention);
                        }

                        argumentLink.setArgument(argumentEntityMention);
                        eventArgs.add(argumentLink);

                        if (relation.getPropbankRoleName() != null) {
                            argumentLink.setPropbankRoleName(relation.getPropbankRoleName());
                        }

                        if (relation.getFrameElementName() != null) {
                            argumentLink.setFrameElementName(relation.getFrameElementName());
                        }

                        argumentLinkMap.put(Pair.of(argument.getBegin(), argument.getEnd()), argumentLink);
                        UimaAnnotationUtils.finishTop(argumentLink, relation.getComponentId(), 0, aJCas);
                    }

                    evm.setArguments(FSCollectionFactory.createFSList(aJCas, eventArgs));
                }

                // Add the gold standard arguments to the mention.
                List<EventMentionArgumentLink> argLinks = new ArrayList<>();
                for (JArgument argument : jMention.arguments) {
                    EntityMention argumentEntity = id2Ent.get(argument.arg);
                    Word argHead = argumentEntity.getHead();

                    if (argHead.getPos().equals("TO") || argHead.getPos().equals("IN")) {
                        argumentEntity.setHead(UimaNlpUtils.findPrepTarget(eventHead, argHead));
                    }

                    EventMentionArgumentLink argumentLink;
                    if (argumentLinkMap.containsKey(Pair.of(argumentEntity.getBegin(), argumentEntity.getEnd()))) {
                        argumentLink = argumentLinkMap.get(Pair.of(argumentEntity.getBegin(), argumentEntity.getEnd()));
                    } else {
                        argumentLink = new EventMentionArgumentLink(aJCas);
                        argumentLink.setEventMention(evm);
                        // All the argument entities are already included in the entity set.
                        argumentLink.setArgument(argumentEntity);
                        UimaAnnotationUtils.finishTop(argumentLink, COMPONENT_ID, 0, aJCas);
                        argLinks.add(argumentLink);
                    }

                    argumentLink.setPropbankRoleName(simplifyRole(argument.role));
                    argumentLink.setComponentId(COMPONENT_ID); // Mark this as gold standard component.

                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "incorporated",
                            Boolean.toString(argument.meta.incorporated));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "succeeding",
                            Boolean.toString(argument.meta.succeeding));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "implicit",
                            Boolean.toString(argument.meta.implicit));

                }
                evm.setArguments(UimaConvenience.extendFSList(aJCas, evm.getArguments(), argLinks,
                        EventMentionArgumentLink.class));
            }

            if (event == null) {
                event = new Event(aJCas);
                event.setEventMentions(FSCollectionFactory.createFSArray(aJCas, newMentions));
                for (EventMention newMention : newMentions) {
                    newMention.setReferringEvent(event);
                }
                UimaAnnotationUtils.finishTop(event, COMPONENT_ID, jEvent.id, aJCas);
            } else {
                // Existing event found.
                addToEventCluster(aJCas, event, newMentions);
            }
        }
    }

    private String simplifyRole(String roleName) {
        // We simplify the PropBank like roles here.
        if (roleName.startsWith("arg")) {
            String[] role_parts = roleName.split("-");

            String arg_type = role_parts[0];

            if (Character.isDigit(arg_type.charAt(3))) {
                // Core arg types.
                return role_parts[0];
            }

            List<String> simplified_parts = new ArrayList<>();
            for (String part : role_parts) {
                if (part.startsWith("h") && part.length() > 1 && Character.isDigit(part.charAt(1))) {
                    // h0, h1, the hyphenated annotation by NomBank.
                } else {
                    simplified_parts.add(part);
                }
            }
            return Joiner.on('-').join(simplified_parts);
        }
        return roleName;
    }

    private Entity createNewEntities(JCas aJCas, List<EntityMention> newMentions, String entityId) {
        Entity entity = new Entity(aJCas);
        entity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, newMentions));
        for (EntityMention newMention : newMentions) {
            newMention.setReferingEntity(entity);
        }
        entity.setRepresentativeMention(newMentions.get(0));
        UimaAnnotationUtils.finishTop(entity, COMPONENT_ID, entityId, aJCas);

        return entity;
    }

}
