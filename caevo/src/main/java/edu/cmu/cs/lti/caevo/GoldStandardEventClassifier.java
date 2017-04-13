package edu.cmu.cs.lti.caevo;

import caevo.*;
import caevo.util.TreeOperator;
import caevo.util.Util;
import caevo.util.WordNet;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.util.JCasUtil;

import java.util.*;

/**
 * Adding gold standard events to Caevo.
 *
 * @author Zhengzhong Liu
 */
public class GoldStandardEventClassifier {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String baseModelName = "event.classifier";

    private final WordNet wordnet;

    private Classifier<String, String> tenseClassifier = null;
    private Classifier<String, String> aspectClassifier = null;
    private Classifier<String, String> classClassifier = null;

    public GoldStandardEventClassifier(WordNet wordnet) {
        this.wordnet = wordnet;
    }

    /**
     * Read all serialized classifiers into memory.
     */
    public void loadClassifiers() {
        String base = "/models/" + baseModelName;
        tenseClassifier = Util.readClassifierFromFile(this.getClass().getResource(base + "-tense"));
        aspectClassifier = Util.readClassifierFromFile(this.getClass().getResource(base + "-aspect"));
        classClassifier = Util.readClassifierFromFile(this.getClass().getResource(base + "-class"));
    }

    public void extractEvents(JCas aJCas, SieveDocuments docs, Map<String, EventMention> eventMapping) {
        ArrayList<StanfordCorenlpSentence> uimaSents = new ArrayList<>(JCasUtil.select(aJCas,
                StanfordCorenlpSentence.class));

        for (SieveDocument doc : docs.getDocuments()) {
            logger.info("Processing doc " + doc.getDocname());

            List<SieveSentence> sentences = doc.getSentences();
            List<List<TypedDependency>> alldeps = doc.getAllDependencies();

            List<List<TextEvent>> allExtractedEvents = doc.getEventsBySentence();

            int maxId = 0;
            for (List<TextEvent> events : allExtractedEvents) {
                for (TextEvent event : events) {
                    int id = Integer.parseInt(event.getEiid().substring(2));
                    if (id > maxId) {
                        maxId = id;
                    }
                }
            }

            int eventi = maxId + 1;
            int sid = 0;

            ArrayList<EventMention> goldMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));

            for (SieveSentence sent : sentences) {
                StanfordCorenlpSentence uimaSent = uimaSents.get(sid);

                List<EventMention> goldMentionInSent = new ArrayList<>();

                for (EventMention goldMention : goldMentions) {
                    int goldBegin = goldMention.getBegin() - uimaSent.getBegin();

                    if (goldBegin > 0 && goldMention.getEnd() < uimaSent.getEnd()) {
                        goldMentionInSent.add(goldMention);
                    }
                }

                Tree tree = sent.getParseTree();
                List<TextEvent> events = new ArrayList<>();
                Set<Integer> timexIndices = indicesCoveredByTimexes(sent.timexes());

                List<TextEvent> existingEvents = sent.events();

                Map<Integer, String> extractedEventIds = new HashMap<>();
                for (TextEvent existingEvent : existingEvents) {
                    extractedEventIds.put(existingEvent.getIndex(), existingEvent.getEiid());
                }

                if (tree != null && tree.size() > 1) {
                    // Each token.
                    int wordi = 1; // first word is index 1
                    for (CoreLabel token : sent.tokens()) {
                        // Skip tokens that are already tagged by a timex.
                        if (!timexIndices.contains(wordi)) {
                            EventMention foundEvent = findEvent(goldMentionInSent, token, uimaSent.getBegin());

                            if (foundEvent != null) {
                                // Already extracted by Caevo extractor, we record the mapping.
                                if (extractedEventIds.containsKey(wordi)) {
                                    eventMapping.put(extractedEventIds.get(wordi), foundEvent);
                                } else {
                                    String tokenStr = token.getString(CoreAnnotations.OriginalTextAnnotation.class);
                                    TextEvent event = new TextEvent(tokenStr, "e" + eventi, sid, wordi);

                                    String eiid = "ei" + eventi;

                                    event.addEiid(eiid);

                                    // Set the event attributes.
                                    RVFDatum<String, String> datum = wordToDatum(sent, tree, alldeps.get(sid), wordi);
                                    //                System.out.println("datum: " + datum);
                                    //                System.out.println("\taspect: " + aspectClassifier.classOf
                                    // (datum));

                                    event.setTense(TextEvent.Tense.valueOf(tenseClassifier.classOf(datum)));
                                    event.setAspect(TextEvent.Aspect.valueOf(aspectClassifier.classOf(datum)));
                                    event.setTheClass(TextEvent.Class.valueOf(classClassifier.classOf(datum)));
                                    events.add(event);

                                    eventMapping.put(eiid, foundEvent);
                                    //                System.out.println("Created event: " + event);
                                    eventi++;
                                }
                            }
                        }
                        wordi++;
                    }
                }

                if (events.size() > 0) {
                    doc.addEvents(sid, events);
                }
                sid++;
            }

            logger.info("Number of events : " + (eventi - 1));
        }
    }

    private EventMention findEvent(List<EventMention> goldMentions, CoreLabel token, int offset) {
        int begin = token.beginPosition();
        int end = token.endPosition();


        for (EventMention mention : goldMentions) {
            int mBegin = mention.getBegin() - offset;
            int mEnd = mention.getEnd() - offset;

            if (begin >= mBegin && end <= mEnd) {
                return mention;
            }
        }
        return null;
    }

    /**
     * 1 indexed. The first token in a sentence is index 1.
     *
     * @return All token indices that are covered by a timex.
     */
    private Set<Integer> indicesCoveredByTimexes(List<Timex> timexes) {
        Set<Integer> indices = new HashSet<Integer>();
        if (timexes != null)
            for (Timex timex : timexes) {
                for (int ii = 0; ii < timex.getTokenLength(); ii++)
                    indices.add(timex.getTokenOffset() + ii);
            }
        return indices;
    }

    private RVFDatum<String, String> wordToDatum(SieveSentence sentence, Tree tree, List<TypedDependency> deps, int
            wordi) {
        Counter<String> features = getEventFeatures(sentence, tree, deps, wordi);
        RVFDatum<String, String> datum = new RVFDatum<String, String>(features, null);
        return datum;
    }

    /**
     * Extract features for a single token in a sentence in order to identify whether or
     * not it is an event.
     *
     * @param sentence  The sentence data structure with all parse information filled in.
     * @param wordIndex Starting from 1.
     * @return
     */
    private Counter<String> getEventFeatures(SieveSentence sentence, Tree tree, List<TypedDependency> deps, int
            wordIndex) {
        Counter<String> features = new ClassicCounter<String>();
        List<CoreLabel> tokens = sentence.tokens();//sentence.sentence().toLowerCase().split("\\s+");
        int size = tokens.size();

        String token = tokens.get(wordIndex - 1).getString(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
        String tokenPre1 = "<s>";
        String tokenPre2 = "<s>";
        if (wordIndex > 1)
            tokenPre1 = tokens.get(wordIndex - 2).getString(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
        if (wordIndex > 2)
            tokenPre2 = tokens.get(wordIndex - 3).getString(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
        String tokenPost1 = "</s>";
        String tokenPost2 = "</s>";
        if (wordIndex < size)
            tokenPost1 = tokens.get(wordIndex).getString(CoreAnnotations.OriginalTextAnnotation.class).toLowerCase();
        if (wordIndex < size - 1)
            tokenPost2 = tokens.get(wordIndex + 1).getString(CoreAnnotations.OriginalTextAnnotation.class)
                    .toLowerCase();

        // N-grams.
        features.incrementCount(token);
        features.incrementCount(tokenPre1 + "-" + token);
        features.incrementCount(tokenPre2 + "-" + tokenPre1 + "-" + token);

        // N-grams before the target token.
        features.incrementCount("PRE-" + tokenPre1);
        features.incrementCount("PRE-" + tokenPre2 + "-" + tokenPre1);

        // N-grams following the target token.
        features.incrementCount("POST-" + tokenPost1);
        features.incrementCount("POST-" + tokenPost1 + "-" + tokenPost2);

        // POS n-grams. (1, 2, 3-gram)
        String pos = TreeOperator.indexToPOSTag(tree, wordIndex);
        String posPre1 = "<s>";
        String posPre2 = "<s>";
        if (wordIndex > 1) posPre1 = TreeOperator.indexToPOSTag(tree, wordIndex - 1);
        if (wordIndex > 2) posPre2 = TreeOperator.indexToPOSTag(tree, wordIndex - 2);
        features.incrementCount(pos);
        features.incrementCount(posPre1 + "-" + pos);
        features.incrementCount(posPre2 + "-" + posPre1 + "-" + pos);

        // WordNet lookup
        features.incrementCount("LEM-" + wordnet.lemmatizeTaggedWord(token, pos));
        if (pos != null && pos.startsWith("NN")) features.incrementCount("IS-WORDNET-EV-" + wordnet.isNounEvent(token));

        // Parse path to Sentence node.
        String path = pathToSTag(tree, wordIndex);
        features.incrementCount("PATH-" + path);

        // Typed Dependency triples with which this word is involved.
        for (TypedDependency dep : deps) {
            if (dep.gov().index() == wordIndex)
                features.incrementCount("DEPG-" + dep.reln());
            else if (dep.dep().index() == wordIndex)
                features.incrementCount("DEPD-" + dep.reln());
        }

        return features;
    }

    /**
     * Find the path from the current word, up to the first seen S node.
     *
     * @param tree
     * @param wordIndex
     * @return
     */
    private String pathToSTag(Tree tree, int wordIndex) {
        Tree subtree = TreeOperator.indexToSubtree(tree, wordIndex);
        if (subtree == null) {
            System.out.println("ERROR: couldn't find subtree for word index " + wordIndex + " in tree: " + tree);
            return null;
        }
        List<String> tags = new ArrayList<String>();
        tags.add(subtree.label().value());

        Tree parentTree = subtree.parent(tree);
        String tag = "";
        while (parentTree != null && !tag.equalsIgnoreCase("S") && !tag.equalsIgnoreCase("SBAR")) {
            tag = parentTree.label().value();
            tags.add(tag);
            parentTree = parentTree.parent(tree);
        }

        // Built the feature string by reversing the list.
        String path = tags.get(tags.size() - 1);
        for (int xx = tags.size() - 2; xx >= 0; xx--)
            path += "-" + tags.get(xx);

        return path;
    }
}
