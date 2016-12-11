package edu.cmu.cs.lti.model;

import com.google.common.collect.ArrayListMultimap;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/9/16
 * Time: 3:38 PM
 *
 * @author Zhengzhong Liu
 */
public class BratAnnotations {
    private Map<String, Pair<MultiSpan, String>> textBoundId2SpanAndType;
    private List<String> eventIds;
    private List<String> eventTextBoundIds;
    private ArrayListMultimap<String, BratAttribute> id2Attribute;
    private List<BratRelation> relations;

    private ArrayListMultimap<String, String> textBoundId2EventId;

    public BratAnnotations() {
        textBoundId2SpanAndType = new HashMap<>();
        id2Attribute = ArrayListMultimap.create();
        eventIds = new ArrayList<>();
        eventTextBoundIds = new ArrayList<>();
        relations = new ArrayList<>();
        textBoundId2EventId = ArrayListMultimap.create();
    }

    public void addTextBound(String id, Pair<MultiSpan, String> spanAndType) {
        textBoundId2SpanAndType.put(id, spanAndType);
    }

    public void addEventMention(String eventId, String textBoundId) {
        eventIds.add(eventId);
        eventTextBoundIds.add(textBoundId);
        textBoundId2EventId.put(textBoundId, eventId);
    }

    public void addAttribute(BratAttribute attribute) {
        id2Attribute.put(attribute.attributeHost, attribute);
    }

    public void addRelation(BratRelation relation) {
        relations.add(relation);
    }

    public Map<String, Pair<MultiSpan, String>> getTextBoundId2SpanAndType() {
        return textBoundId2SpanAndType;
    }

    public List<String> getEventIds() {
        return eventIds;
    }

    public List<String> getEventTextBoundIds() {
        return eventTextBoundIds;
    }

    public ArrayListMultimap<String, BratAttribute> getId2Attribute() {
        return id2Attribute;
    }

    public List<BratRelation> getRelations() {
        return relations;
    }

    public List<String> getEventIds(String textBoundId) {
        return textBoundId2EventId.get(textBoundId);
    }
}
