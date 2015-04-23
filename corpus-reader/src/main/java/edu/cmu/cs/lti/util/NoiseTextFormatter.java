package edu.cmu.cs.lti.util;

import com.google.common.base.Joiner;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    private static String[] forumPatterns = {"<post[^<]*>", "<quote[^<]*>", "< / quote>", "< / post>", "<img[^<]*>", "<a href=*>"};

    public static String extractArticle(String text) throws BoilerpipeProcessingException, SAXException {
        return extractor.getText(text);
    }

    public static String cleanForum(String docText) {
        //apply the newline even as many times as possible to make it identical to input
        String[] lines = docText.split("\n", -1);

        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            cleanedLines.add(replaceMatchedWithChar(line, forumPatterns, ' '));
        }

        return Joiner.on("\n").join(cleanedLines);
    }

    public static String cleanTag(String docText, String encoding) throws IOException, TikaException, SAXException {
        InputStream stream = IOUtils.toInputStream(docText);
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        } finally {
            stream.close();
        }
    }

    public static String replaceMatchedWithChar(String line, String[] patterns, char c) {
        if (line.isEmpty()) {
            return line;
        }

        for (String pattern : patterns) {
            Pattern p = Pattern.compile("("+pattern+")");
            Matcher m = p.matcher(line);
            StringBuffer sb = new StringBuffer();

            while (m.find()) {
                String found = m.group(1);
                String replacementStr = StringUtils.repeat(c, found.length());
                m.appendReplacement(sb, replacementStr);
            }
            m.appendTail(sb);
            if (sb.length() != 0) {
                line = sb.toString();
            }
        }
        return line;
    }

    public static void main(String[] args) throws BoilerpipeProcessingException, IOException, SAXException, TikaException {
        String noisyText = FileUtils.readFileToString(
                new File("/Users/zhengzhongliu/Documents/projects/uima-base-tools/data/event_mention_detection/" +
                        "LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/source/" +
                        "2f5ee4e363c30678dc3b55caf43bc63d.cmp.tkn.txt"));

        System.out.println("=== Rule results ===");
        String ruleCleaned = cleanForum(noisyText);
//        System.out.println(ruleCleaned);


        System.out.println("Original length " + noisyText.length());
        System.out.println("Rule length " + ruleCleaned.length());
    }
}
