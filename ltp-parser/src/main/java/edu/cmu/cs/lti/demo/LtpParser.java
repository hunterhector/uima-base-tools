package edu.cmu.cs.lti.demo;

import edu.hit.ir.ltp4j.*;

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
    public static void main(String[] argv) {
        loadAll();

        String shortSent = "上网流量超标了我女朋友都不知道。";
        String longSent =
                "上网流量超标了我女朋友都不知道，还经常抱着刷网页，收费贵死了，不过这个倒也认了，好歹确实是自己花掉的，最烦的是一些随意扣费，都是恶意软件还有就是一些莫名其妙的额外服务，我不查不知道，一查才知道有些还是每月定期扣费，同时还要产生大量流量，卧槽！";
        String someSent = "被残忍杀害的女人不配有人权，这是美国民主自由的精神";
        String nerSent = "美国总统奥巴马访问中国。中国国际广播电台创办于1941年。中央电视台呢？";

        testAll(shortSent);
//        testAll(longSent);
        testAll(someSent);
        testAll(nerSent);

//        testParser();
//        testSegment();
//        testNer();
//        testSrl();


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

    private static void testAll(String sourceSent) {
        List<String> words = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        List<String> ners = new ArrayList<>();
        List<Integer> heads = new ArrayList<>();
        List<String> depRels = new ArrayList<>();
        List<Pair<Integer, List<Pair<String, Pair<Integer, Integer>>>>> srls = new ArrayList<>();

        System.out.println("Source sentence is " + sourceSent);

        System.out.println("Segmenting");
        int size = Segmentor.segment(sourceSent, words);

        for (int i = 0; i < words.size(); i++) {
            System.out.println(i + " : " + words.get(i));
        }

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

        System.out.println("NER results.");
        for (int i = 0; i < words.size(); i++) {
            System.out.println(words.get(i) + " " + ners.get(i));
        }

        System.out.println("Parser results.");

        for (int i = 0; i < size; i++) {
            System.out.print(heads.get(i) + ":" + depRels.get(i));
            if (i == size - 1) {
                System.out.println();
            } else {
                System.out.print("        ");
            }
        }

        System.out.println("SRL results");
        for (int i = 0; i < srls.size(); ++i) {
            System.out.println(srls.get(i).first + ":");
            for (int j = 0; j < srls.get(i).second.size(); ++j) {
                System.out.println("   type = " + srls.get(i).second.get(j).first + " beg = " + srls.get(i).second
                        .get(j).second.first + " end = " + srls.get(i).second.get(j).second.second);
            }
        }
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
