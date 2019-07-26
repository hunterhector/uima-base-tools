package edu.cmu.cs.lti.uima.util;

import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.*;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.HashMap;
import java.util.Map;

public class EntityMentionManager {
    private Map<Word, EntityMention> headMentionIndices;
    private Map<Span, EntityMention> spanMentionIndices;

    public EntityMentionManager(JCas jcas) {
        headMentionIndices = createHeadIndex(jcas);
        spanMentionIndices = createSpanIndex(jcas);
    }

    public EntityMention getEntity(Word head) {
        return getEntity(head, head);
    }

    public EntityMention getEntity(ComponentAnnotation fullAnno, Word head) {
        Span fullSpan = Span.of(fullAnno.getBegin(), fullAnno.getEnd());
        if (spanMentionIndices.containsKey(fullSpan)) {
            return spanMentionIndices.get(fullSpan);
        }

        if (headMentionIndices.containsKey(head)) {
            return headMentionIndices.get(head);
        }

        return null;
    }

    public EntityMention getOrCreateEntity(JCas jcas, int begin, int end, String componentId) {
        if (spanMentionIndices.containsKey(Span.of(begin, end))) {
            return spanMentionIndices.get(Span.of(begin, end));
        }

        ComponentAnnotation dummy = new ComponentAnnotation(jcas, begin, end);
        StanfordCorenlpToken dummyHead = UimaNlpUtils.findHeadFromStanfordWithoutLeadingPrep(jcas, dummy);

        EntityMention mention;
        if (headMentionIndices.containsKey(dummyHead)) {
            mention = headMentionIndices.get(dummyHead);
            if (mention.getHead() == null) {
                mention.setHead(dummyHead);
            }
        } else {
            mention = new EntityMention(jcas, begin, end);
            UimaAnnotationUtils.finishAnnotation(mention, componentId, 0, jcas);
            StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordWithoutLeadingPrep(jcas, mention);
            mention.setHead(head);
            headMentionIndices.put(head, mention);
        }

        spanMentionIndices.put(Span.of(mention.getBegin(), mention.getEnd()), mention);
        return mention;
    }

    private Map<Word, EntityMention> createHeadIndex(JCas jcas) {
        Map<Word, EntityMention> mentions = new HashMap<>();
        for (EntityMention entityMention : JCasUtil.select(jcas, EntityMention.class)) {
            Word head = entityMention.getHead();
            if (head == null) {
                head = UimaNlpUtils.findHeadFromStanfordAnnotation(entityMention);
                entityMention.setHead(head);
            }
            if (head != null) {
                mentions.put(head, entityMention);
            }
        }
        return mentions;
    }

    private Map<Span, EntityMention> createSpanIndex(JCas jcas) {
        Map<Span, EntityMention> mentions = new HashMap<>();
        for (EntityMention entityMention : JCasUtil.select(jcas, EntityMention.class)) {
            mentions.put(Span.of(entityMention.getBegin(), entityMention.getEnd()), entityMention);
        }
        return mentions;
    }
}
