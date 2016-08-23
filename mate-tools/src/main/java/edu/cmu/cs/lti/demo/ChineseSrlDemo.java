package edu.cmu.cs.lti.demo;

import se.lth.cs.srl.SemanticRoleLabeler;
import se.lth.cs.srl.corpus.Sentence;
import se.lth.cs.srl.io.CoNLL09Writer;
import se.lth.cs.srl.io.DepsOnlyCoNLL09Reader;
import se.lth.cs.srl.io.SentenceReader;
import se.lth.cs.srl.io.SentenceWriter;
import se.lth.cs.srl.languages.Language;
import se.lth.cs.srl.pipeline.Pipeline;
import se.lth.cs.srl.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/4/16
 * Time: 9:48 PM
 *
 * @author Zhengzhong Liu
 */
public class ChineseSrlDemo {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        long startTime=System.currentTimeMillis();

        ZipFile modelFile = new ZipFile(new File(args[0]));
        SemanticRoleLabeler srl = Pipeline.fromZipFile(modelFile);

        Language.setLanguage(Language.L.chi);

        modelFile.close();

        SentenceWriter writer = new CoNLL09Writer(new File(args[2]));
        SentenceReader reader = new DepsOnlyCoNLL09Reader(new File(args[1]));

        int senCount = 0;
        for (Sentence s : reader) {
            senCount++;
            if (senCount % 100 == 0)
                System.out.println("Parsing sentence " + senCount);
            srl.parseSentence(s);
            writer.write(s);
        }
        writer.close();
        reader.close();
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Done.");
        System.out.println(srl.getStatus());
        System.out.println();
        System.out.println("Total execution time: " + Util.insertCommas(totalTime) + "ms");
    }
}
