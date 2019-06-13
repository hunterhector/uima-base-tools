package edu.cmu.cs.lti.uima.util;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
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
//    private static ExtractorBase extractor = ArticleExtractor.getInstance();

    private static String[] forumPatterns = {"<post[^<]*>", "<quote[^<]*>", "<\\s?/\\s?quote>", "<\\s?/\\s?post>",
            "<img[^<]*>",
            "<a\\s?href=[^>]*>", "<\\s?/\\s?a>"};

    private static String[] newsDiscardPattern = {
            "<DOCID>[^<]*<\\s/\\sDOCID>", "<DOCTYPE[^>]*>[^<]*<\\s?/\\sDOCTYPE>", "<KEYWORD>", "<\\s?/\\s?KEYWORD>",
            "<BODY>", "<TEXT>", "<\\s?/\\s?TEXT>", "<\\s?/\\s?BODY>", "<DOC>", "<\\s?/\\s?DOC>?",
            "<DATE_TIME>[^<]*<\\s?/\\s*DATE_TIME>", "<AUTHOR>", "</AUTHOR>",
            "<DATETIME>[^<]*<\\s?/\\s*DATETIME>", "<DATELINE>[^<]*<\\s?/\\s*DATELINE>", "<DOC[^>]*>", "<HEADLINE>",
            "<\\s?/\\s?HEADLINE>", "<\\s{0,6}/\\s{0,3}body>"
    };

    private static String[] breakablePattern = {
            "<\\s{0,3}P>", "<\\s?/\\s{0,3}P>", "<\\s{0,6}p>", "<\\s{0,6}/\\s{0,3}p>"
    };

    private String[] xmlPattern = {
            "<\\?xml.*\\?>"
    };


    private String xml10pattern = "[^"
            + "\u0009\r\n"
            + "\u0020-\uD7FF"
            + "\uE000-\uFFFD"
            + "\ud800\udc00-\udbff\udfff"
            + "]";

    private String invalidXmlPattern = xml10pattern;

    private String emoticonPattern = "[\ud83c\udf00-\ud83d\ude4f]|[\ud83d\ude80-\ud83d\udeff]";

    private String text;

    private int originalLength;

    public NoiseTextFormatter(String input) {
        this.text = input;
        this.originalLength = input.length();
    }

    public NoiseTextFormatter cleanWithPattern(String... patterns) {
        cleanTextWithPatterns(patterns);
        return this;
    }

    public NoiseTextFormatter cleanForum() {
        cleanTextWithPatterns(forumPatterns);
        return this;
    }

    public NoiseTextFormatter cleanNews() {
        cleanTextWithPatterns(newsDiscardPattern);
        return this;
    }

    public NoiseTextFormatter possibleStopBreaker(String language) {
        char stopSymbol = '.';
        if (language.equals("zh")) {
            stopSymbol = '。';
        }
        replaceMatchedWithChar(breakablePattern, stopSymbol);
        return this;
    }

    public NoiseTextFormatter cleanXMLHeader() {
        cleanTextWithPatterns(xmlPattern);
        return this;
    }

    public NoiseTextFormatter cleanEscapeCharacter() {
        String unescapeText = StringEscapeUtils.unescapeXml(text);
        text = edu.cmu.cs.lti.utils.StringUtils.matchText(text, unescapeText);
        return this;
    }

    public NoiseTextFormatter cleanXMLCharacters() {
        text = CasSerialization.cleanText(text);
        return this;
    }

    public NoiseTextFormatter cleanXmlWithPattern(){
        text = text.replaceAll(invalidXmlPattern, " ");
        return this;
    }

    public NoiseTextFormatter cleanEmoticon(){
        text = text.replaceAll(emoticonPattern, " ");
        return this;
    }

    public String cleanBasic(){
        cleanEmoticon().cleanXmlWithPattern().cleanXMLCharacters();
        return text;
    }

    public String cleanAll(String language) {
        cleanEscapeCharacter().cleanXMLCharacters().possibleStopBreaker(language).cleanForum().cleanNews()
                .cleanXMLHeader().multiNewLineBreaker(language).cleanEmoticon().cleanXmlWithPattern();
        if (text.length() != originalLength) {
            System.out.println(String.format(
                    "[ERROR] cleaned text length is %d, not the same as original length %d."
                    , text.length(), originalLength)
            );
        }

        return text;
    }

    public String cleanAll() {
        return cleanAll("en");
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
            Pattern p = Pattern.compile("(" + pattern + ")", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
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

    /**
     * For multiple consecutive new lines, replace first one with a period. Only one new line will be replaced by a
     * period, which ensure the offset of the rest won't change.
     */
    public NoiseTextFormatter multiNewLineBreaker() {
        return multiNewLineBreaker("en");
    }


    /**
     * For multiple consecutive new lines, replace first one with a period. Only one new line will be replaced by a
     * period, which ensure the offset of the rest won't change.
     */
    public NoiseTextFormatter multiNewLineBreaker(String language) {
        String stopSymbol = ".";
        String pattern = "([^\\p{Punct}\\s]\\h*)(\\n)(\\s*\\n+)";

        if (language.equals("zh")) {
            stopSymbol = "。";
            pattern = "([^，。！？\\s]\\h*)(\\n)(\\s*\\n+)";
        }

        text = text.replaceAll(pattern, "$1" + stopSymbol + "$3");
        return this;
    }

    public static void main(String[] args) throws IOException {
        String inputFile = args[0];

        String noisyText = FileUtils.readFileToString(new File(inputFile));

        String language = "en";

        NoiseTextFormatter formatter = new NoiseTextFormatter(noisyText);

        String ruleCleaned = formatter.cleanAll(language);

        System.out.println("=== Rule results ===");
        System.out.println(ruleCleaned);

        System.out.println("Original length " + noisyText.length());
        System.out.println("Rule length " + ruleCleaned.length());
    }
}
