package edu.cmu.cs.lti.uima.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.utils.CollectionUtils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UimaNlpUtils {
    private static final Logger logger = LoggerFactory.getLogger(UimaNlpUtils.class);


    public static void mergeSameHeadEntities(JCas aJCas) {
        ArrayListMultimap<Word, EntityMention> entityHeadMap = ArrayListMultimap.create();

        for (EntityMention entityMention : JCasUtil.select(aJCas, EntityMention.class)) {
            entityHeadMap.put(entityMention.getHead(), entityMention);
        }

        for (Map.Entry<Word, Collection<EntityMention>> sameHeadEntities : entityHeadMap.asMap().entrySet()) {
            Word headword = sameHeadEntities.getKey();

            Collection<EntityMention> entityMentions = sameHeadEntities.getValue();
            if (entityMentions.size() > 1) {
                // Now merge.
                Set<EntityMention> allMentions = new HashSet<>();
                Set<EntityMention> represents = new HashSet<>();

                String componentId = null;

                for (EntityMention entityMention : entityMentions) {
                    Entity theEntity = entityMention.getReferingEntity();
                    represents.add(theEntity.getRepresentativeMention());
                    componentId = theEntity.getComponentId();
                    theEntity.removeFromIndexes();
                    allMentions.addAll(FSCollectionFactory.create(theEntity.getEntityMentions(),
                            EntityMention.class));
                }

                Entity theEntity = new Entity(aJCas);
                theEntity.setEntityMentions(FSCollectionFactory.createFSArray(aJCas, allMentions));

                int earliestStart = aJCas.getDocumentText().length();
                for (EntityMention represent : represents) {
                    if (represent.getBegin() <= earliestStart) {
                        earliestStart = represent.getBegin();
                        theEntity.setRepresentativeMention(represent);
                        theEntity.setRepresentativeString(represent.getCoveredText());
                    }
                }

                UimaAnnotationUtils.finishTop(theEntity, componentId + "_merged", 0, aJCas);

                for (EntityMention theMention : allMentions) {
                    // Set the reference to the new Entity object.
                    theMention.setReferingEntity(theEntity);
                }
            }
        }
    }

    static Map<String, Integer> priority = new HashMap<>();

    static {
        priority.put("ORGANIZATION", 0);
        priority.put("PERSON", 1);
        priority.put("LOCATION", 2);
        priority.put("TIME", 3);
        priority.put("MONEY", 4);
        priority.put("DATE", 5);
        priority.put("DURATION", 6);
        priority.put("PERCENT", 7);
        priority.put("NUMBER", 8);
        priority.put("SEt", 9);
        priority.put("MISC", 10);
    }

    public static void voteNerType(JCas aJCas) {

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            TObjectIntHashMap<String> nerTypes = new TObjectIntHashMap<>();
            for (EntityMention entityMention : FSCollectionFactory.create(entity.getEntityMentions(),
                    EntityMention.class)) {
                if (entityMention.getEntityType() != null) {
                    nerTypes.adjustOrPutValue(entityMention.getEntityType(), 1, 1);
                }
            }

            List<String> mostFrequentTypes = CollectionUtils.findMaxCount(nerTypes);

            String votedType = null;

            if (mostFrequentTypes.size() == 1) {
                votedType = mostFrequentTypes.get(0);
            } else if (mostFrequentTypes.size() > 1) {
                // Resolving with priority.

                int highestPriority = priority.size();

                for (String type : mostFrequentTypes) {
                    if (priority.containsKey(type)) {
                        Integer p = priority.get(type);
                        if (p < highestPriority) {
                            highestPriority = p;
                            votedType = type;
                        }
                    }
                }
            }
            entity.setEntityType(votedType);
        }
    }

    public static <T extends Word> T getDepHead(T word) {
        if (word.getHeadDependencyRelations() == null) {
            return null;
        }

        for (Dependency dependency : FSCollectionFactory.create(word.getHeadDependencyRelations(), Dependency.class)) {
            return (T) dependency.getHead();
        }
        return null;
    }

    public static <T extends Word> T findPrepTarget(T predHead, T prepWord) {
        T target = findPrepTargetFromStanford(predHead, prepWord);
        if (target == null) {
            target = findPrepTargetFromUD(predHead, prepWord);
        }
        return target == null ? prepWord : target;
    }

    /**
     * Find the head of the phase without the preposition, within the range of argPhrase. If argPhrase is null then
     * there will not be a range.
     *
     * @param jcas
     * @param predHead
     * @param prepWord
     * @param argPhrase
     * @param <T>
     * @return
     */
    public static <T extends Word> T findNonPrepHeadInRange(JCas jcas, T predHead, T prepWord, Annotation argPhrase) {
        T theHead = findPrepTarget(predHead, prepWord);

        // Find a prep
        if (theHead != prepWord) {
            if (argPhrase != null) {
                if (!(theHead.getBegin() >= argPhrase.getBegin() && theHead.getEnd() <= argPhrase.getEnd())) {
                    theHead = prepWord;
                }
            }
        }

        if (argPhrase != null && theHead == prepWord) {
            theHead = (T) findHeadFromStanfordWithoutLeadingPrep(jcas, argPhrase);
        }

        return theHead;
    }

    public static <T extends Word> T findPrepTargetFromUD(T predHead, T prepWord) {
        for (Map.Entry<String, T> depWord : getDepChildByDep(predHead).entrySet()) {
            String depType = depWord.getKey();
            T predChild = depWord.getValue();

            String[] indirectDepParts = depType.split(":");
            if (indirectDepParts.length > 1) {
                if (indirectDepParts[indirectDepParts.length - 1].equals(prepWord.getLemma().toLowerCase())) {
                    if (getDependentWords(predChild).contains(prepWord)) {
                        return predChild;
                    }
                }
            }
        }
        return null;
    }

    public static <T extends Word> T findPrepTargetFromStanford(T depHead, T prepWord) {
        String depRel = "prep_" + prepWord.getLemma();
        T res = (T) findChildOfDep(depHead, depRel);
        if (res == null) {
            depRel = "prepc_" + prepWord.getLemma();
            res = (T) findChildOfDep(depHead, depRel);
        }
        return res;
    }

    public static Word findChildOfDep(Word depHead, String targetDep) {
        FSList childFS = depHead.getChildDependencyRelations();
        if (childFS != null) {
            for (Dependency dep : FSCollectionFactory.create(childFS,
                    Dependency.class)) {
                if (dep.getDependencyType().equals(targetDep)) {
                    return dep.getChild();
                }
            }
        }
        return null;
    }

    public static String findDirectDep(Word head, Word child) {
        FSList childFS = head.getChildDependencyRelations();
        if (childFS != null) {
            for (Dependency dep : FSCollectionFactory.create(childFS,
                    Dependency.class)) {
                if (dep.getChild().equals(child)) {
                    return dep.getDependencyType();
                }
            }
        }
        return null;
    }


    public static String getLemmatizedAnnotation(Annotation a) {
        StringBuilder builder = new StringBuilder();
        String spliter = "";
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, a)) {
            builder.append(spliter);
            builder.append(token.getLemma());
            spliter = " ";
        }
        return builder.toString();
    }

    public static String getLemmatizedHead(Annotation a) {
        StanfordCorenlpToken headword = findHeadFromStanfordAnnotation(a);
        if (headword == null) {
            return a.getCoveredText().replace("\t", " ").replace("\n", " ");
        } else {
            return headword.getLemma().toLowerCase();
        }
    }

    public static <T extends Word> Map<String, T> getDepChildByDep(T head) {
        Map<String, T> childByDep = new HashMap<>();

        FSList depFS = head.getChildDependencyRelations();
        if (depFS != null) {
            for (Dependency dependency : FSCollectionFactory.create(depFS, Dependency.class)) {
                String dep = dependency.getDependencyType();
                T child = (T) dependency.getChild();
                childByDep.put(dep, child);
            }
        }
        return childByDep;
    }

    public static Map<Word, String> getDepChildren(Word head) {
        Map<Word, String> children = new HashMap<>();
        if (head == null || head.getChildDependencyRelations() == null) {
            return children;
        }

        for (Dependency dependency : FSCollectionFactory.create(head.getChildDependencyRelations(),
                Dependency.class)) {
            String dep = dependency.getDependencyType();
            Word child = dependency.getChild();
            children.put(child, dep);
        }
        return children;
    }

    public static String getPredicate(Word head, List<Word> complements, boolean keepXcomp) {
        FSList childDeps = head.getChildDependencyRelations();

        String complementPart = "";
        String negationPart = "";
        String particle_part = "";

        if (childDeps != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(childDeps,
                    StanfordDependencyRelation.class)) {

                if (keepXcomp && dep.getDependencyType().equals("xcomp")) {
                    Word complementNode = dep.getChild();
                    complementPart = "_" + complementNode.getLemma();
                    // Complement node contains additional subjects.
                    complements.add(complementNode);
                }

                if (dep.getDependencyType().equals("prt")) {
                    particle_part = "_" + dep.getChild().getLemma();
                }

                if (dep.getDependencyType().equals("neg")) {
                    negationPart = "not_";
                }
            }
        }
        return (negationPart + head.getLemma() + particle_part + complementPart).toLowerCase();
    }

    public static void addToEntityCluster(JCas aJCas, Entity entity, List<EntityMention> newMentions) {
        for (EntityMention newMention : newMentions) {
            newMention.setReferingEntity(entity);
        }
        entity.setEntityMentions(UimaConvenience.extendFSArray(aJCas, entity.getEntityMentions(), newMentions,
                EntityMention.class));
    }


    public static Map<Word, EntityMention> indexEntityMentions(JCas jcas) {
        Map<Word, EntityMention> mentions = new HashMap<>();
        for (EntityMention entityMention : JCasUtil.select(jcas, EntityMention.class)) {
            Word head = entityMention.getHead();
            if (head == null) {
                head = UimaNlpUtils.findHeadFromStanfordAnnotation(entityMention);
                entityMention.setHead(head);
            }
            mentions.put(head, entityMention);
        }
        return mentions;
    }

    public static Table<Integer, Integer, EventMention> indexEventMentions(JCas jcas) {
        Table<Integer, Integer, EventMention> mentions = HashBasedTable.create();
        for (EventMention mention : JCasUtil.select(jcas, EventMention.class)) {
            mentions.put(mention.getBegin(), mention.getEnd(), mention);
        }
        return mentions;
    }

    public static Map<EntityMention, EventMentionArgumentLink> indexArgs(EventMention eventMention) {
        Map<EntityMention, EventMentionArgumentLink> argHead2Arg = new HashMap<>();
        FSList existingArgsFS = eventMention.getArguments();
        if (existingArgsFS != null) {
            Collection<EventMentionArgumentLink> existingArgs = FSCollectionFactory.create(eventMention
                    .getArguments(), EventMentionArgumentLink.class);
            for (EventMentionArgumentLink existingArg : existingArgs) {
                EntityMention en = existingArg.getArgument();
                argHead2Arg.put(en, existingArg);
            }
        }
        return argHead2Arg;
    }

    public static EventMentionArgumentLink createArg(JCas aJCas, EntityMentionManager manager,
                                                     EventMention eventMention,
                                                     int begin, int end, String componentId) {
        EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
        EntityMention argumentMention = manager.getOrCreateEntity(aJCas, begin, end, componentId);
        argumentLink.setArgument(argumentMention);
        argumentLink.setEventMention(eventMention);
        UimaAnnotationUtils.finishTop(argumentLink, componentId, 0, aJCas);
        return argumentLink;
    }

    public static EventMentionArgumentLink addEventArgument(
            JCas aJCas, EventMention eventMention, EntityMentionManager manager,
            Map<EntityMention, EventMentionArgumentLink> existingArgs, List<EventMentionArgumentLink> argumentLinks,
            Word newArgHeadWord, String component_id) {
        return addEventArgument(
                aJCas, eventMention, manager, existingArgs, argumentLinks, newArgHeadWord, newArgHeadWord, component_id
        );
    }

    public static EventMentionArgumentLink addEventArgument(
            JCas aJCas, EventMention eventMention, EntityMentionManager manager,
            Map<EntityMention, EventMentionArgumentLink> existingArgs, List<EventMentionArgumentLink> argumentLinks,
            ComponentAnnotation newArgAnnotation, Word newArgHeadWord, String component_id) {
        EventMentionArgumentLink argumentLink;

        // This ensures that if we are adding an argument with the same span, we will always get the same head word
        // that is already stored, instead of creating a new one.
        EntityMention existingMention = manager.getEntity(newArgAnnotation, newArgHeadWord);

        if (existingMention != null && existingArgs.containsKey(existingMention)) {
            argumentLink = existingArgs.get(existingMention);
        } else {
            argumentLink = UimaNlpUtils.createArg(aJCas, manager, eventMention,
                    newArgAnnotation.getBegin(), newArgAnnotation.getEnd(), component_id);
            argumentLinks.add(argumentLink);
        }
        return argumentLink;
    }

    public static ArgumentMention createArgMention(JCas jcas, int begin, int end, String componentId) {
        ArgumentMention mention = new ArgumentMention(jcas, begin, end);
        UimaAnnotationUtils.finishAnnotation(mention, componentId, 0, jcas);
        mention.setHead(findHeadFromStanfordWithoutLeadingPrep(jcas, mention));
        return mention;
    }

    public static void cleanEntityMentionMetaData(JCas aJCas, List<EntityMention> allMentions, String componentId) {
        //Sort and assign id to mentions.
        allMentions.sort(Comparator.comparingInt(Annotation::getBegin));
        int mentionIdx = 0;

        for (EntityMention mention : allMentions) {
            mention.setId(String.valueOf(mentionIdx++));
            if (mention.getReferingEntity() == null) {
                Entity entity = new Entity(aJCas);
                entity.setEntityMentions(new FSArray(aJCas, 1));
                entity.setEntityMentions(0, mention);
                mention.setReferingEntity(entity);
                entity.setRepresentativeMention(mention);
                UimaAnnotationUtils.finishTop(entity, componentId, 0, aJCas);
            }

            if (mention.getHead() == null) {
                mention.setHead(findHeadFromStanfordAnnotation(mention));
            }
        }
    }

    public static StanfordCorenlpToken findFirstToken(JCas aJCas, int begin, int end) {
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, begin, end)) {
            return token;
        }
        return null;
    }


    public static <T extends Word> T findFirstToken(Annotation anno, Class<T> clazz) {
        for (T token : JCasUtil.selectCovered(clazz, anno)) {
            return token;
        }
        return null;
    }


    public static <T extends Word> List<T> findCoveringTokens(Annotation anno, Class<T> clazz) {
        List<T> words = new ArrayList<>();

        List<T> coveringTokens = JCasUtil.selectCovering(clazz, anno);
        words.addAll(coveringTokens);

        return words;
    }

    public static Word findFirstWord(JCas jcas, int begin, int end, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(jcas, Word.class, begin, end)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }

    public static Word findFirstWord(Annotation anno, String targetComponentId) {
        for (Word token : JCasUtil.selectCovered(Word.class, anno)) {
            if (token.getComponentId().equals(targetComponentId)) {
                return token;
            }
        }
        return null;
    }


    public static CharacterAnnotation findHeadCharacterFromZparAnnotation(Annotation anno) {
        return findHeadFromTree(findLargestContainingTree(anno, ZparTreeAnnotation.class), CharacterAnnotation.class);
    }

    public static StanfordCorenlpToken findHeadFromStanfordWithoutLeadingPrep(JCas aJCas, Annotation anno) {
        List<StanfordCorenlpToken> remaining = new ArrayList<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, anno)) {
            if (!isPrepWord(token)) {
                remaining.add(token);
            }
        }

        if (remaining.size() == 0) {
            return findHeadFromStanfordAnnotation(anno);
        } else {
            int newBegin = remaining.get(0).getBegin();
            int newEnd = remaining.get(remaining.size() - 1).getEnd();
            Annotation tempAnno = new Annotation(aJCas, newBegin, newEnd);
            StanfordCorenlpToken head = findHeadFromStanfordAnnotation(tempAnno);
            tempAnno.removeFromIndexes();
            return head;
        }
    }

    public static StanfordCorenlpToken findHeadInRange(Annotation anno, StanfordCorenlpToken word,
                                                       Set<StanfordCorenlpToken> visits) {
        StanfordCorenlpToken itsHead = getDepHead(word);
        if (itsHead == null) {
            return word;
        } else if (visits.contains(itsHead)) {
            return word;
        }

        visits.add(word);
        if (itsHead.getBegin() >= anno.getBegin() && itsHead.getEnd() <= anno.getEnd()) {
            return findHeadInRange(anno, itsHead, visits);
        } else {
            return word;
        }
    }

    public static StanfordCorenlpToken findOverlapTokenInParentTree(ParseTreeAnnotation childTree, Annotation anno) {
        if (childTree == null) {
            return null;
        }

        ParseTreeAnnotation parent = childTree.getParent();

        if (parent == null) {
            return null;
        }

        Word parentHead = parent.getHead();
        StanfordCorenlpToken ancestorToken = findOverlapTokenInParentTree(parent, anno);

        if (ancestorToken == null) {
            if (parentHead.getBegin() >= anno.getBegin() && parentHead.getEnd() <= anno.getEnd()) {
                if (!isPrepWord(parentHead))
                    return (StanfordCorenlpToken) parentHead;
                else
                    return null;
            } else {
                return null;
            }
        } else {
            return ancestorToken;
        }
    }

    public static StanfordCorenlpToken findHeadFromStanfordAnnotation(Annotation anno) {
        StanfordTreeAnnotation largestTree = findLargestTreeWithoutLeadingPrep(anno, StanfordTreeAnnotation.class);
        StanfordCorenlpToken headWord = findOverlapTokenInParentTree(largestTree, anno);

        if (headWord == null) {
            headWord = findHeadFromTree(largestTree, StanfordCorenlpToken.class);
        }

        if (headWord == null) {
            headWord = UimaConvenience.selectCoveredFirst(anno, StanfordCorenlpToken.class);
        }

        if (headWord == null) {
            List<StanfordCorenlpToken> coveringTokens = JCasUtil.selectCovering(StanfordCorenlpToken.class, anno);
            if (coveringTokens.size() > 0) {
                headWord = JCasUtil.selectCovering(StanfordCorenlpToken.class, anno).get(0);
            }
        }

        if (headWord == null) {
            List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, anno);

            TObjectIntMap<StanfordCorenlpToken> coveringTokenCount = new TObjectIntHashMap<>();

            for (CharacterAnnotation character : characters) {
                coveringTokenCount.increment(JCasUtil.selectCovering(StanfordCorenlpToken.class, character).get(0));
            }

            int maxCount = 0;
            for (TObjectIntIterator<StanfordCorenlpToken> iter = coveringTokenCount.iterator(); iter.hasNext(); ) {
                iter.advance();
                if (iter.value() > maxCount) {
                    headWord = iter.key();
                    maxCount = iter.value();
                }
            }
        }

        // From constituent head to dependency head.
        if (headWord != null) {
            headWord = findHeadInRange(anno, headWord, new HashSet<>());
        }

        return headWord;
    }

    public static boolean compatibleMentions(EntityMention mention1, EntityMention mention2) {
        if (isProperNoun(mention1.getHead()) && isProperNoun(mention2.getHead())) {
            return true;
        } else return couldBeMoney(mention1.getHead()) && couldBeMoney(mention2.getHead());
    }

    public static boolean isProperNoun(Word word) {
        return word.getPos().equals("NNP") || word.getPos().equals("NNPS");
    }

    public static boolean couldBeMoney(Word word) {
        return word.getPos().equals("$") || word.getPos().equals("CD");
    }

    public static ParseTreeAnnotation findLeftTree(ParseTreeAnnotation tree, Word word) {
        if (tree.getBegin() < word.getBegin()) {
            return tree;
        } else if (tree.getParent() == null) {
            return null;
        } else {
            return findLeftTree(tree.getParent(), word);
        }
    }

    public static Word findWhTarget(Word word) {
        StanfordTreeAnnotation tree = findLargestContainingTree(word, StanfordTreeAnnotation.class);
        ParseTreeAnnotation leftParent = findLeftTree(tree, word);

        if (leftParent != null) {
            Word parentHead = leftParent.getHead();
            if (parentHead.getPos().startsWith("N")) {
                return parentHead;
            }
        }
        return null;
    }

    public static boolean isPrepWord(Word word) {
        String pos = word.getPos();
        return pos.equals("IN") || pos.equals("TO") || pos.equals("RP");
    }

    public static boolean isWhWord(Word word) {
        String pos = word.getPos();
        return pos.equals("WDT") || pos.equals("WP") || pos.equals("WP$");
    }

    public static <T extends ParseTreeAnnotation> T findLargestTreeWithoutLeadingPrep(Annotation anno, Class<T> clazz) {
        List<T> nonPrepTrees = new ArrayList<>();
        List<T> allTrees = new ArrayList<>();
        for (T t : JCasUtil.selectCovered(clazz, anno)) {
            boolean startWithPrep = false;
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, t)) {
                if (isPrepWord(token)) {
                    startWithPrep = true;
                }
                break;
            }

            if (!startWithPrep) {
                nonPrepTrees.add(t);
            }

            allTrees.add(t);
        }

        if (nonPrepTrees.size() > 0) {
            return findLargest(nonPrepTrees);
        } else {
            return findLargest(allTrees);
        }
    }

    public static <T extends ParseTreeAnnotation> T findLargestContainingTree(Annotation anno, Class<T> clazz) {
        return findLargest(JCasUtil.selectCovered(clazz, anno));
    }

    public static List<Word> getDependentWords(Word word) {
        List<Word> dependentTokens = new ArrayList<>();

        FSList childDeps = word.getChildDependencyRelations();
        if (childDeps != null) {
            for (Dependency dep : FSCollectionFactory.create(childDeps, StanfordDependencyRelation.class)) {
                dependentTokens.add(dep.getChild());
            }
        }

        return dependentTokens;
    }

    /**
     * Get dependent words with a specific word type.
     *
     * @param word      The head word.
     * @param wordClass The word type class.
     * @param <T>       The word type class name.
     * @return
     */
    public static <T extends Word> List<T> getDependentWords(T word, Class<T> wordClass) {
        List<T> dependentTokens = new ArrayList<>();

        FSList childDeps = word.getChildDependencyRelations();
        if (childDeps != null) {
            for (Dependency dep : FSCollectionFactory.create(childDeps, Dependency.class)) {
                dependentTokens.add((T) dep.getChild());
            }
        }
        return dependentTokens;
    }

    public static <T extends Word> T findHeadFromTree(ParseTreeAnnotation tree, Class<T> clazz) {
        if (tree != null) {
            if (tree.getIsLeaf()) {
                return findFirstToken(tree, clazz);
            } else {
                return (T) tree.getHead();
            }
        } else {
            return null;
        }
    }

    public static <T extends Annotation> T findLargest(List<T> annos) {
        T largestAnno = null;
        for (T anno : annos) {
            if (largestAnno == null) {
                largestAnno = anno;
            } else if (largestAnno.getEnd() - largestAnno.getBegin() < anno.getEnd() - anno
                    .getBegin()) {
                largestAnno = anno;
            }
        }
        return largestAnno;
    }

}
