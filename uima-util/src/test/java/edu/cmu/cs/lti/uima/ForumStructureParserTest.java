package edu.cmu.cs.lti.uima;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.uima.util.ForumStructureParser;
import net.htmlparser.jericho.Element;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/10/16
 * Time: 4:04 PM
 *
 * @author Zhengzhong Liu
 */
public class ForumStructureParserTest {
    @Test
    public void getTagSpansTest() throws ResourceInitializationException, AnalysisEngineProcessException {
        ArrayListMultimap<String, Element> tag2Spans = ForumStructureParser.indexTagByName(testString());

        Assert.assertEquals(tag2Spans.get("headline").size(), 1);
        Assert.assertEquals(tag2Spans.get("headline").get(0).getBegin(), 83);
        Assert.assertEquals(tag2Spans.get("quote").size(), 2);
        Assert.assertEquals(tag2Spans.get("quote").get(0).getBegin(), 1166);
        Assert.assertEquals(tag2Spans.get("quote").get(1).getBegin(), 1378);

        Element firstQuote = tag2Spans.get("quote").get(0);
        String firstQuoteStr = "<quote orig_author=\"sx5\">\n这货在中国 民意汹涌下妥妥的枪毙～～\n</quote>";
        Assert.assertEquals(testString().substring(firstQuote.getBegin(), firstQuote.getEnd()), firstQuoteStr);
    }

    private String testString() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<doc id=\"CMN_DF_000181_20141021_F000000C9\">\n" +
                "<headline>\n" +
                "西方民主与正义的重大胜利——评刀锋战士一案\n" +
                "</headline>\n" +
                "<post id=\"p1\" author=\"寒春江雪\" datetime=\"2014-10-21T21:29:00\">\n" +
                "刀锋战士和女友在一张床上睡觉，睡至酣处，忽然听到卫生间有响动，醒来发现女友不在身旁，门窗完好无损，他一瞬间就判断，卫生间是小偷，至于女友人间蒸发那就不管了，反正卫生间是小偷，然后举起枪，也不问一声里边是什么人，不管女友是否在里边，直接开枪，一声尖叫传来，小偷竟然是女的，那也不管，再开三枪，打死再说……\n" +
                "　　\n" +
                "如此简单的作案情节，显然是误伤，一目了然，而且可以申报正当防卫，可是就是有人捣乱，竟然有邻居说听到了女人的哭喊尖叫声，然后才传来枪声。\n" +
                "　　\n" +
                "这个捣乱的邻居让这个案子变复杂了，居然审了半年。好在民主与正义是不容玷污的，经过律师反复的盘查，邻居终于承认他听到的尖叫像女声，但他不能证明一定是女人发出的，这就够了，律师指出，那也有可能是刀锋战士在极度恐惧下捏着嗓子发出的叫声。\n" +
                "　　\n" +
                "至此，真相大白，邻居在作伪证，不予采纳，10月21日，刀锋战士以过失杀人罪被判处5年监禁，需要在监狱度过8-20个月，其余刑期可以在家中执行。\n" +
                "　　\n" +
                "这是西方民主与正义的重大胜利，这说明除非亲眼目睹作案过程并拿录像机录下来，否则任何人都有可能作伪证陷害好人，此案值得载入史册，作为排除伪证的经典案例编入法律教材。\n" +
                "</post>\n" +
                "<post id=\"p2\" author=\"乡间山人 \" datetime=\"2014-10-21T21:54:00\">\n" +
                "当然,教材可是得出钱才能编好呀,所以出了大钱,教材就编得非常好了\n" +
                "</post>\n" +
                "<post id=\"p3\" author=\"sx5\" datetime=\"2014-10-21T21:58:00\">\n" +
                "这货在中国 民意汹涌下妥妥的枪毙～～\n" +
                "</post>\n" +
                "<post id=\"p4\" author=\"vvhgg2012\" datetime=\"2014-10-21T22:05:00\">\n" +
                "这TM法律不瞎扯淡嘛\n" +
                "\n" +
                "不过我想了想辛普森案 在想一想老美花样繁多的法律（老美普通人真的看不过来 我想）于是又淡定了\n" +
                "</post>\n" +
                "<post id=\"p5\" author=\"寡人交友不慎 \" datetime=\"2014-10-21T22:09:00\">\n" +
                "奇！真神奇！这都什么和什么啊！？\n" +
                "</post>\n" +
                "<post id=\"p6\" author=\"李升饭\" datetime=\"2014-10-21T23:02:00\">\n" +
                "\n" +
                "<quote orig_author=\"sx5\">\n" +
                "这货在中国 民意汹涌下妥妥的枪毙～～\n" +
                "</quote>\n" +
                "\n" +
                "不用什么民意。\n" +
                "</post>\n" +
                "<post id=\"p7\" author=\"满面菜色\" datetime=\"2014-10-21T23:04:00\">\n" +
                "啥世道啊。\n" +
                "</post>\n" +
                "<post id=\"p8\" author=\"u_95707644\" datetime=\"2014-10-21T23:29:00\">\n" +
                "\n" +
                "<quote orig_author=\"sx5\">\n" +
                "这货在中国 民意汹涌下妥妥的枪毙～～\n" +
                "</quote>\n" +
                "\n" +
                "你没看明白楼主的意思\n" +
                "</post>\n" +
                "<post id=\"p9\" author=\"振华挺华2 \" datetime=\"2014-10-22T07:41:00\">\n" +
                "法制国家，实质就是一群没有个人领导魅力和天才战略的脑残人的想法，符合四年一度“低能总统”管理P" +
                "民的体制。为了领导和奴役无知的人民，讨好和配合资本家制定繁杂的宪法、法律法规、各种制度，马的，几千万条之多（谁记的住啊？我就一个农民学不会！这么多的【宪法、法律、法规、各种制度】条文在你身边，你说你自由，谁信呀！！），反正多到P民做梦都犯法为止，让P民认知，这是资本家的财物（万恶的庄园、土地和吃人血的专利、产权），你们不能反抗，不能这样那样，把原本自由自在（穷得叮当响和目不识丁）的人民套上层层框框，好让P民听话，让资本家顺顺当当剪P民的羊毛。但经济却在“高福利和吵吵闹闹的皿煮”下沦落崩溃。纵观历史，这种压抑的社会不长久，那天一个英雄人物（朱元璋、毛泽东）就会揭竿而起，把这些条条框框打烂，让人民又过上自由自在的生活，经济在专制的庇护下高速增长，【要成功，只有专制的理念】，呵呵！可惜，慢慢又滋生一群没有个人领导魅力和天才战略的脑残人（统称为“皿煮人仕”），篡党夺权之后，社会又循环到条条框框去，腐败现象又慢慢滋生了，悲哀啊！！！！！！所以，社会主义国家就应【简化】宪法、法律法规、各种制度，“公有为根，私有为叶”,让人民更自由和幸福，不用看“律师啥折腾,两边吃”。　国有企业赚钱，那些收入都是国家收入，作为国民你完全有权利要求花这些钱。但中国私有化之后，都是私企啦！，人家再暴利也是个人收入，跟你屁民没关系，你根本没资格抱怨。资本家吃喝嫖赌比官员更奢靡,你搞得清楚其中的差别吗？看看吧，美国最大的两个地主的土地有多大？---比北京市面积还大，这就是霉粉和JY、皿煮孬种鼓吹的皿煮和私有化！！！中国人能同意吗？？？（待续）\n" +
                "</post>\n" +
                "<post id=\"p10\" author=\"Mr没事做 \" datetime=\"2014-10-22T07:57:00\">\n" +
                "这也行？\n" +
                "</post>\n" +
                "<post id=\"p11\" author=\"索罗斯门徒2019\" datetime=\"2014-10-22T12:02:00\">\n" +
                "被残忍杀害的女人不配有人权，这是美国民主自由的精神\n" +
                "</post>";
    }
}
