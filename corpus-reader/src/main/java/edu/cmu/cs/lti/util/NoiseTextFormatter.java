package edu.cmu.cs.lti.util;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/26/15
 * Time: 5:36 PM
 */
public class NoiseTextFormatter {
    private static ExtractorBase extractor = ArticleExtractor.getInstance();

    private static String[] forumPatterns = {"<post[^<]*>", "<quote[^<]*>", "< / quote>", "< / post>", "<img[^<]*>", "<a\\shref=[^>]*>[^<]*<\\s*/\\s*a>"};

    private static String[] newsDiscardPattern = {
            "<DOCID>[^<]*<\\s/\\sDOCID>", "<DOCTYPE[^>]*>[^<]*<\\s/\\sDOCTYPE>", "<KEYWORD>", "<\\s/\\sKEYWORD>",
            "<BODY>", "<TEXT>", "< / TEXT>", "< / BODY>", "<DOC>", "< / DOC>", "< / DOC", "<P>", "< / P>",
            "<DATETIME>[^<]*<\\s/\\sDATETIME>", "<DATELINE>[^<]*<\\s/\\sDATELINE>", "<DOC[^>]*>", "<HEADLINE>", "< / HEADLINE>"
    };

    private static String sentenceEndFixer = "[^\\p{Punct}](\\n)[\\s|\\n]*\\n";

    private String text;

    public NoiseTextFormatter(String input) {
        this.text = input;
    }

    public NoiseTextFormatter cleanForum() {
        cleanTextWithPatterns(forumPatterns);
        return this;
    }

    public NoiseTextFormatter cleanNews() {
        cleanTextWithPatterns(newsDiscardPattern);
        return this;
    }

    public String getText() {
        return text;
    }

    public void cleanTextWithPatterns(String[] patterns) {
        replaceMatchedWithChar(patterns, ' ');
    }

    public void replaceMatchedWithChar(String[] patterns, char c) {
        if (text.isEmpty()) {
            return;
        }

        for (String pattern : patterns) {
            Pattern p = Pattern.compile("(" + pattern + ")", Pattern.DOTALL);
            Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String found = m.group(1);
//                System.out.println(found);
                String replacementStr = StringUtils.repeat(c, found.length());
                m.appendReplacement(sb, replacementStr);
            }
            m.appendTail(sb);
            if (sb.length() != 0) {
                text = sb.toString();
            }
        }
    }

    public static void main(String[] args) throws BoilerpipeProcessingException, IOException, SAXException, TikaException {
        String noisyText = FileUtils.readFileToString(
                new File("/Users/zhengzhongliu/Documents/projects/uima-base-tools/data/event_mention_detection/" +
                        "LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/source/" +
//                        "XIN_ENG_20030609.0118.tkn.txt"));
//                        "XIN_ENG_20100304.0019.tkn.txt"));
                        "052fe72e4bb7b33ca69dd0dfd01fc442.cmp.tkn.txt"));

        NoiseTextFormatter formatter = new NoiseTextFormatter(noisyText);
        String ruleCleaned = formatter.cleanForum().cleanNews().getText();

        System.out.println("=== Rule results ===");
        System.out.println(ruleCleaned);

        System.out.println("Original length " + noisyText.length());
        System.out.println("Rule length " + ruleCleaned.length());
    }
}
