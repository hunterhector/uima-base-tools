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
    private Map<String, Pair<List<Span>, String>> textBoundId2SpanAndType;
    private List<String> eventIds;
    private List<String> eventTextBounds;
    private ArrayListMultimap<String, BratAttribute> id2Attribute;
    private List<BratRelation> relations;

    public BratAnnotations() {
        textBoundId2SpanAndType = new HashMap<>();
        id2Attribute = ArrayListMultimap.create();
        eventIds = new ArrayList<>();
        eventTextBounds = new ArrayList<>();
        relations = new ArrayList<>();
    }

    public void addTextBound(String id, Pair<List<Span>, String> spanAndType){
        textBoundId2SpanAndType.put(id, spanAndType);
    }

    public void addEventMention(String eventId, String textBoundId){
        eventIds.add(eventId);
        eventTextBounds.add(textBoundId);
    }

    public void addAttribute(BratAttribute attribute){
        id2Attribute.put(attribute.attributeHost, attribute);
    }

    public void addRelation(BratRelation relation){
        relations.add(relation);
    }

    public Map<String, Pair<List<Span>, String>> getTextBoundId2SpanAndType() {
        return textBoundId2SpanAndType;
    }

    public List<String> getEventIds() {
        return eventIds;
    }

    public List<String> getEventTextBounds() {
        return eventTextBounds;
    }

    public ArrayListMultimap<String, BratAttribute> getId2Attribute() {
        return id2Attribute;
    }

    public List<BratRelation> getRelations() {
        return relations;
    }
}
