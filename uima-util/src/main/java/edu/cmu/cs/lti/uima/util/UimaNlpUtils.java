package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.script.type.*;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UimaNlpUtils {
    private static final Logger logger = LoggerFactory.getLogger(UimaNlpUtils.class);

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

    public static EntityMention createEntityMention(JCas jcas, int begin, int end, String componentId) {
        EntityMention mention = new EntityMention(jcas, begin, end);
        UimaAnnotationUtils.finishAnnotation(mention, componentId, null, jcas);
        mention.setHead(findHeadFromStanfordAnnotation(mention));
        return mention;
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

//    public static StanfordCorenlpToken findHeadFromRange(JCas view, int begin, int end) {
//        StanfordTreeAnnotation largestContainingTree = findLargest(JCasUtil.selectCovered(view,
//                StanfordTreeAnnotation.class, begin, end));
//        return findHeadFromTree(largestContainingTree, StanfordCorenlpToken.class);
//    }

    public static CharacterAnnotation findHeadCharacterFromZparAnnotation(Annotation anno) {
        return findHeadFromTree(findLargestContainingTree(anno, ZparTreeAnnotation.class), CharacterAnnotation.class);
    }

    public static StanfordCorenlpToken findHeadFromStanfordAnnotation(Annotation anno) {
        StanfordCorenlpToken headWord = findHeadFromTree(findLargestContainingTree(anno,
                StanfordTreeAnnotation.class), StanfordCorenlpToken.class);

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

        return headWord;
    }

    public static <T extends ParseTreeAnnotation> T findLargestContainingTree(
            Annotation anno, Class<T> clazz) {
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