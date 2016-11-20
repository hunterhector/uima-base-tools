package edu.cmu.cs.lti.util;

import edu.cmu.cs.lti.model.*;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/8/16
 * Time: 4:49 PM
 *
 * @author Zhengzhong Liu
 */
public class BratFormat {
    public static BratAnnotations parseBratAnnotations(List<String> bratAnnotations){
        BratAnnotations bratAnno = new BratAnnotations();

        for (String line : bratAnnotations) {
            String[] parts = line.trim().split("\t");
            String annoId = parts[0];
            if (annoId.startsWith(BratConstants.textBoundPrefix)) {
                bratAnno.addTextBound(annoId, getSpanAndType(parts[1]));
            } else if (annoId.startsWith(BratConstants.eventPrefix)) {
                bratAnno.addEventMention(annoId, parts[1].split(":")[1]);
            } else if (annoId.startsWith(BratConstants.attributePrefix)) {
                BratAttribute attribute = new BratAttribute(line);
                bratAnno.addAttribute(attribute);
            } else if (annoId.startsWith(BratConstants.relationPrefix)) {
                BratRelation relation = new BratRelation(line);
                bratAnno.addRelation(relation);
            }
        }

        return bratAnno;
    }

    private static Pair<List<Span>, String> getSpanAndType(String spanText) {
        String[] typeAndSpan = spanText.split(" ", 2);
        String type = typeAndSpan[0];
        String[] spanStrs = typeAndSpan[1].split(";");

        List<Span> spans = new ArrayList<>();
        for (String spanStr : spanStrs) {
            String[] spanTexts = spanStr.split(" ");
            spans.add(Span.of(Integer.parseInt(spanTexts[0]), Integer.parseInt(spanTexts[1])));
        }
        return Pair.with(spans, type);
    }


}
