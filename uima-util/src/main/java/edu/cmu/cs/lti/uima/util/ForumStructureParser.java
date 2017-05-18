package edu.cmu.cs.lti.uima.util;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.TaggedArea;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Annotate forum structure
 *
 * @author Zhengzhong Liu
 */
public class ForumStructureParser {
    public static ArrayListMultimap<String, Element> indexTagByName(String forumText) {

        Source source = new Source(forumText);

        ArrayListMultimap<String, Element> tag2Spans = ArrayListMultimap.create();

        for (Element element : source.getAllElements()) {
            tag2Spans.put(element.getName(), element);
        }

        return tag2Spans;
    }

    public static List<Span> getQuotesFromElement(ArrayListMultimap<String, Element> tagsByName) {
        List<Span> quotedSpans = new ArrayList<>();
        for (Element quote : tagsByName.get("quote")) {
            quotedSpans.add(Span.of(quote.getBegin(), quote.getEnd()));
        }
        return quotedSpans;
    }

    public static String removeQuoteStr(String original, List<Span> quotedAreas) {
        StringBuilder sb = new StringBuilder(original);
        for (Span quoteArea : quotedAreas) {
            for (int i = quoteArea.getBegin(); i < quoteArea.getEnd(); i++) {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }

    public static void annotateTagAreas(JCas aJCas, ArrayListMultimap<String, Element> tagsByName, String componentId) {
        for (Map.Entry<String, Element> tagByName : tagsByName.entries()) {
            Element tag = tagByName.getValue();
            TaggedArea area = new TaggedArea(aJCas, tag.getBegin(), tag.getEnd());
            area.setTagName(tagByName.getKey());

            Attributes attributes = tag.getAttributes();

            StringArray attributeNames = new StringArray(aJCas, attributes.size());
            StringArray attributeValues = new StringArray(aJCas, attributes.size());

            for (int i = 0; i < attributes.size(); i++) {
                Attribute attribute = attributes.get(i);
                attributeNames.set(i, attribute.getKey());
                attributeValues.set(i, attribute.getValue());
            }

            area.setTagAttributeNames(attributeNames);
            area.setTagAttributeValues(attributeValues);
            UimaAnnotationUtils.finishAnnotation(area, componentId, 0, aJCas);
        }
    }
}
