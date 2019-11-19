package edu.cmu.cs.lti.collection_reader;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
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

    class NodeMeta {
        String node;
    }

    class ArgMeta extends NodeMeta {
        boolean incorporated;
        boolean succeeding;
        boolean implicit;
    }

    class EventMeta extends NodeMeta {
        boolean from_gc;
    }

    class JEventMention {
        String id;
        String annotation;
        String text;
        List<Span> spans;
        List<JArgument> arguments;
        String type;
        EventMeta meta;
    }

    class JArgument {
        String arg;
        String role;
        ArgMeta meta;
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

    private TObjectIntMap<String> predicateCounts = new TObjectIntHashMap<>();
    private TObjectIntMap<String> predicateWithImplicit = new TObjectIntHashMap<>();
    private TObjectIntMap<String> slotsWithImplicit = new TObjectIntHashMap<>();

    private TObjectIntMap<String> counters;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        gson = new Gson();
        counters = new TObjectIntHashMap<>();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        logger.info("Printing corpus statistics");

        String[] predicates = predicateWithImplicit.keys(new String[]{});
        Arrays.sort(predicates);

        System.out.println("Predicate\tPredicate Count\tImplicit Predicates\tImplicit Slots");

        int[] sums = new int[3];
        for (String pred : predicates) {
            System.out.println(String.format("%s\t%d\t%d\t%d",
                    pred,
                    predicateCounts.get(pred),
                    predicateWithImplicit.get(pred),
                    slotsWithImplicit.get(pred)
            ));

            sums[0] += predicateCounts.get(pred);
            sums[1] += predicateWithImplicit.get(pred);
            sums[2] += slotsWithImplicit.get(pred);
        }

        System.out.println(String.format("%s\t%d\t%d\t%d", "total", sums[0], sums[1], sums[2]));

        logger.info("Other statistics for the corpus:");
        counters.forEachEntry((s, i) -> {
            logger.info(String.format("%s : %d", s, i));
            return true;
        });
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


                UimaAnnotationUtils.addMeta(aJCas, evm, "node", jMention.meta.node);
                UimaAnnotationUtils.addMeta(aJCas, evm, "from_gc", Boolean.toString(jMention.meta.from_gc));

                if (jMention.meta.from_gc) {
                    counters.adjustOrPutValue("GC predicates", 1, 1);
                }

                // There may be duplicated arguments with the gold ones, we will reuse those.
                List<EventMentionArgumentLink> argLinks = new ArrayList<>();
                Map<Pair<Integer, Integer>, EventMentionArgumentLink> argumentLinkMap = new HashMap<>();
                FSList existingArgsFS = evm.getArguments();
                if (existingArgsFS != null) {
                    for (EventMentionArgumentLink link :
                            FSCollectionFactory.create(existingArgsFS, EventMentionArgumentLink.class)) {
                        argLinks.add(link);
                        argumentLinkMap.put(Pair.of(link.getArgument().getBegin(), link.getArgument().getEnd()), link);
                    }
                }

                StanfordCorenlpToken eventHead = (StanfordCorenlpToken) evm.getHeadWord();

                Set<String> implicitArgs = new HashSet<>();
                String predText = eventHead.getLemma().toLowerCase();
                if (predText.equals("small-investor")) {
                    predText = "investor";
                }

                for (JArgument argument : jMention.arguments) {
                    EntityMention argumentEntity = id2Ent.get(argument.arg);
                    Word argHead = argumentEntity.getHead();

                    if (argHead.getPos().equals("TO") || argHead.getPos().equals("IN")) {
                        argumentEntity.setHead(UimaNlpUtils.findNonPrepHeadInRange(
                                aJCas, eventHead, argHead, argumentEntity));
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

                    argumentLink.setArgumentRole(simplifyRole(argument.role));

                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "incorporated",
                            Boolean.toString(argument.meta.incorporated));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "succeeding",
                            Boolean.toString(argument.meta.succeeding));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "implicit",
                            Boolean.toString(argument.meta.implicit));
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "source", jMention.type);
                    UimaAnnotationUtils.addMeta(aJCas, argumentLink, "node", argument.meta.node);

                    if (argument.meta.implicit) {
                        implicitArgs.add(argument.role);
                        counters.adjustOrPutValue("Implicit Arguments", 1, 1);
                    }
                }

                predicateCounts.adjustOrPutValue(predText, 1, 1);
                if (!implicitArgs.isEmpty()) {
                    predicateWithImplicit.adjustOrPutValue(predText, 1, 1);
                    slotsWithImplicit.adjustOrPutValue(predText, implicitArgs.size(), implicitArgs.size());
                }

                evm.setArguments(FSCollectionFactory.createFSList(aJCas, argLinks));
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
