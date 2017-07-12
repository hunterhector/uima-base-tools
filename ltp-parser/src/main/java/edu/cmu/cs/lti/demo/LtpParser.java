package edu.cmu.cs.lti.demo;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.utils.StringUtils;
import edu.hit.ir.ltp4j.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/5/16
 * Time: 10:21 PM
 *
 * @author Zhengzhong Liu
 */
public class LtpParser {
    public static void main(String[] argv) throws IOException {
        loadAll();

        String paragraph;

        if (argv.length > 0) {
            System.out.println("Reading string from input file.");
            paragraph = FileUtils.readFileToString(new File(argv[0]));
        } else {
            String shortSent = "上网流量超标了我女朋友都不知道    。\n";
            String longSent =
                    "  上网流量超标了我女朋友都不知道，还经常抱着刷网页，收费贵死了，不过这个倒也认了，好歹确实是自己花掉的，最烦的是一些随意扣费，都是恶意软件还有就是一些莫名其妙的额外服务，我不查不知道，一查才知道有些还是每月定期扣费，同时还要产生大量流量，卧槽！";
            String someSent = "被残忍杀害的女人不配有人权，   这是美国民主自由的精神。";
            String nerSent = "美国总统奥巴马访问中国。中国国际广播电台创办于1941年。中央电视台呢？";

            String randomSent = "差距的原因我觉得是2本书的准备时间和目的都不一样新的书很短时间成书肯定不够成熟而且目的大家可能忽略了西西出新书其实是竞选的广告。";

            String sent2 = "相反，美国确实直接、或间接通过军事手段推翻了希特勒、萨达姆、米洛舍维奇等独裁政 权，使全球独裁国家的数量从120多个减 少到现在的不到20个国家。";

            paragraph = shortSent + longSent + someSent + nerSent + randomSent + sent2;
        }

        ArrayList<String> sents = new ArrayList<>();

        SplitSentence.splitSentence(paragraph, sents);

        System.out.println("Found " + sents.size() + " sentences");

        List<String> allWords = new ArrayList<>();

        for (String sent : sents) {
            if (!sent.isEmpty()) {

                List<String> words = new ArrayList<>();
                System.out.println("Source sentence is " + sent);
                System.out.println("Segmenting");
                int size = Segmentor.segment(sent, words);

                allWords.addAll(words);

                testAll(words);
            }
        }

        String wordStr = Joiner.on("").join(allWords);

        int[] offsets = StringUtils.translateOffset(paragraph, wordStr);

        int currentOffset = 0;
        for (String word : allWords) {
            int begin_char = currentOffset;
            int end_char = currentOffset + word.length() - 1;

            int begin_base_char = offsets[begin_char];
            int end_base_char = offsets[end_char];

//            System.out.println(String.format("Begin character is %d -> %d, end character is %d -> %d", begin_char,
//                    begin_base_char, end_char, end_base_char));

            currentOffset += word.length();

            String baseStr = paragraph.substring(begin_base_char, end_base_char + 1);

            if (!word.equals(baseStr)) {
                System.out.println(String.format("Word is %s, not equal to %s.", word, baseStr));
            }
        }


        releaseAll();
    }

    private static void loadAll() {
        if (Segmentor.create("../models/ltp_models/ltp_data/cws.model") < 0) {
            System.err.println("CWS loading failed");
            return;
        }

        if (Postagger.create("../models/ltp_models/ltp_data/pos.model") < 0) {
            System.err.println("SRL loading failed");
            return;
        }

        if (Parser.create("../models/ltp_models/ltp_data/parser.model") < 0) {
            System.err.println("load failed");
            return;
        }

        if (SRL.create("../models/ltp_models/ltp_data/srl") < 0) {
            System.err.println("SRL loading failed");
            return;
        }

        if (NER.create("../models/ltp_models/ltp_data/ner.model") < 0) {
            System.err.println("load failed");
            return;
        }
    }

    private static void releaseAll() {
        Segmentor.release();
        Postagger.release();
        Parser.release();
        NER.release();
        SRL.release();
    }

    private static void testAll(List<String> words) {
        int size = words.size();

        List<String> tags = new ArrayList<>();
        List<String> ners = new ArrayList<>();
        List<Integer> heads = new ArrayList<>();
        List<String> depRels = new ArrayList<>();
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<>();

//        for (int i = 0; i < words.size(); i++) {
//            System.out.println(i + " : " + words.get(i));
//        }

        System.out.println("POS tagging");
        Postagger.postag(words, tags);
        System.out.println("NER tagging");
        NER.recognize(words, tags, ners);
        System.out.println("Parsing");
        Parser.parse(words, tags, heads, depRels);
        System.out.println("SRL parsing");
        List<Integer> headsForSrl = heads.stream().map(head -> head - 1).collect(Collectors.toList());
        SRL.srl(words, tags, ners, headsForSrl, depRels, srls);
        System.out.println("Done.");


        System.out.println(Joiner.on("").join(words));
        System.out.println(words.size() +" [" +  Joiner.on("],[").join(words)+"]");
        System.out.println(tags.size() + " " + Joiner.on(" ").join(tags));
        System.out.println(heads.size() + " " + Joiner.on(" ").join(heads));
        System.out.println(depRels.size() + " " + Joiner.on(" ").join(depRels));
    }

    private static void testNer() {
        List<String> words = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        List<String> ners = new ArrayList<String>();
        words.add("中国");
        tags.add("ns");
        words.add("国际");
        tags.add("n");
        words.add("广播");
        tags.add("n");
        words.add("电台");
        tags.add("n");
        words.add("创办");
        tags.add("v");
        words.add("于");
        tags.add("p");
        words.add("1941年");
        tags.add("m");
        words.add("12月");
        tags.add("m");
        words.add("3日");
        tags.add("m");
        words.add("。");
        tags.add("wp");

        NER.recognize(words, tags, ners);

        for (int i = 0; i < words.size(); i++) {
            System.out.println(words.get(i) + " " + ners.get(i));
        }
    }

    private static void testSegment() {
        String sent = "我是中国人,\n 他也 是。";
        List<String> words = new ArrayList<>();
        int size = Segmentor.segment(sent, words);

        for (int i = 0; i < size; i++) {
            System.out.print(i + " " + words.get(i));
            if (i == size - 1) {
                System.out.println();
            } else {
                System.out.print("\t");
            }
        }
    }

    private static void testParser() {
        System.out.println("Testing parser");
        List<String> words = new ArrayList<String>();
        List<String> tags = new ArrayList<String>();
        words.add("一把手");
        tags.add("n");
        words.add("亲自");
        tags.add("d");
        words.add("过问");
        tags.add("v");
        words.add("。");
        tags.add("wp");
        List<Integer> heads = new ArrayList<Integer>();
        List<String> deprels = new ArrayList<String>();

        int size = Parser.parse(words, tags, heads, deprels);

        for (int i = 0; i < size; i++) {
            System.out.print(heads.get(i) + ":" + deprels.get(i));
            if (i == size - 1) {
                System.out.println();
            } else {
                System.out.print("        ");
            }
        }
    }

    private static void testSrl() {
        ArrayList<String> words = new ArrayList<>();
        words.add("一把手");
        words.add("亲自");
        words.add("过问");
        words.add("。");
        ArrayList<String> tags = new ArrayList<>();
        tags.add("n");
        tags.add("d");
        tags.add("v");
        tags.add("wp");
        ArrayList<String> ners = new ArrayList<>();
        ners.add("O");
        ners.add("O");
        ners.add("O");
        ners.add("O");
        ArrayList<Integer> heads = new ArrayList<>();
        heads.add(2);
        heads.add(2);
        heads.add(-1);
        heads.add(2);
        ArrayList<String> deprels = new ArrayList<>();
        deprels.add("SBV");
        deprels.add("ADV");
        deprels.add("HED");
        deprels.add("WP");
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<>();
        SRL.srl(words, tags, ners, heads, deprels, srls);
        for (int i = 0; i < srls.size(); ++i) {
            System.out.println(srls.get(i).first + ":");
            for (int j = 0; j < srls.get(i).second.size(); ++j) {
                System.out.println("   type = " + srls.get(i).second.get(j).first + " beg = " + srls.get(i).second
                        .get(j).second.first + " end = " + srls.get(i).second.get(j).second.second);
            }
        }
    }

}
