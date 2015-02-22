package edu.cmu.cs.lti.util;

import com.google.common.base.Joiner;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/26/15
 * Time: 5:36 PM
 */
public class NoiseTextFormatter {
    private static ExtractorBase extractor = ArticleExtractor.getInstance();

    private static String[] forumPatterns = {"<post", "<quote", "< / quote", "< / post>"};

    public static String articleExtract(String text) throws BoilerpipeProcessingException, SAXException {
        return extractor.getText(text);
    }

    public static String cleanForum(String docText) {
        String[] lines = docText.split("\n");
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            cleanedLines.add(replaceMathedTokenWithChar(line, forumPatterns, '-'));
        }
        return Joiner.on("\n").join(cleanedLines);
    }


    public static String replaceMathedTokenWithChar(String line, String[] patterns, char c) {
        for (String pattern : patterns) {
            if (line.startsWith(pattern)) {
                char[] replaced = new char[line.length()];
                int index = 0;
                for (char origin : line.toCharArray()) {
                    if (origin == ' ') {
                        replaced[index] = origin;
                    } else {
                        replaced[index] = c;
                    }
                    index++;
                }
                for (int i = replaced.length - 2; i > 0; i--) {
                    if (replaced[i] == ' ') {
                        break;
                    }
                    replaced[i] = '!';
                }
                return String.valueOf(replaced);
            }
        }

        return line;
    }

    public static void main(String[] args) throws BoilerpipeProcessingException, IOException, SAXException {
        System.out.println(
                articleExtract(FileUtils.readFileToString(
                        new File("/Users/zhengzhongliu/Documents/projects/uimafied-tools/data/event_mention_detection/" +
                                "LDC2014E121_DEFT_Event_Nugget_Evaluation_Training_Data/data/source/" +
                                "2f5ee4e363c30678dc3b55caf43bc63d.cmp.tkn.txt"))));
    }

}
