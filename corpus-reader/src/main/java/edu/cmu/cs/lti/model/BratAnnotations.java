package edu.cmu.cs.lti.model;

import com.google.common.collect.ArrayListMultimap;

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
    private Map<String, TextBound> tid2TextBound;
    private List<String> eventIds;
    private List<String> eventTextBoundIds;
    private ArrayListMultimap<String, BratAttribute> id2Attribute;
    private List<BratRelation> relations;

    private ArrayListMultimap<String, String> textBoundId2EventId;

    public BratAnnotations() {
        tid2TextBound = new HashMap<>();
        id2Attribute = ArrayListMultimap.create();
        eventIds = new ArrayList<>();
        eventTextBoundIds = new ArrayList<>();
        relations = new ArrayList<>();
        textBoundId2EventId = ArrayListMultimap.create();
    }

    public static class TextBound{
        public final MultiSpan spans;
        public final String type;
        public final String text;

        public TextBound(MultiSpan spans, String type, String text){
            this.spans = spans;
            this.type = type;
            this.text = text;
        }
    }

    public void addTextBound(String id, TextBound textBound) {
        tid2TextBound.put(id, textBound);
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

    public Map<String, TextBound> getTid2TextBound() {
        return tid2TextBound;
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
