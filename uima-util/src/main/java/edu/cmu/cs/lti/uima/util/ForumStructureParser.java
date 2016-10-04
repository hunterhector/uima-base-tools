package edu.cmu.cs.lti.uima.util;

import com.google.common.collect.ArrayListMultimap;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;


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
}
