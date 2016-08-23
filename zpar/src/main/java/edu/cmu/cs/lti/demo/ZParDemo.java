package edu.cmu.cs.lti.demo;

import edu.cmu.cs.lti.utils.DataForwarder;
import edu.cmu.cs.lti.utils.DebugUtils;
import edu.stanford.nlp.trees.Tree;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/15/16
 * Time: 10:21 AM
 *
 * @author Zhengzhong Liu
 */
public class ZParDemo {

    public static void main(String[] argv) throws IOException {

//        String text = "（史记、汉书、资治通鉴都有记载）\n" +
//                "　　汉武帝时(公元前111年)，设立了交州(今广州)。";
//
//        System.out.println(text.replaceAll("[\\s　]", ""));

        DebugUtils.pause();

        String os = System.getProperty("os.name").toLowerCase();

        String zparBinPath;

        if (os.contains("mac")) {
            zparBinPath = "zpar-07-mac/dist/zpar.zh";
        } else if (os.contains("nix") || os.contains("nux")) {
            zparBinPath = "zpar-linux/dist/zpar.zh";
        } else {
            throw new NotImplementedException(String.format("Not implemented for OS %s.", os));
        }

        String zparBin = "../models/zpar/" + zparBinPath;
        boolean setExe = new File(zparBin).setExecutable(true);

        if (!setExe) {
            throw new IllegalAccessError("Cannot make the ZPar binary executable.");
        }

        String chineseModel = "../models/zpar/chinese";

        String[] command = {zparBin, chineseModel, "-oz"};

        Process zpar = new ProcessBuilder(command).start();

        String[] inputSentences = {
                "没办法，哥实在是被弄怕了！",
                "中美在沪签订贸易合作。",
                "全面私有化就是死路一条。",
                "(全面私有化就是死路一条)。",
                "(全面私有化就是死路一条)",
                "-LRB-全面私有化就是死路一条-RRB-",
                "一行写不下,\n再来一行",
                "。’",
                "。。。"
        };

        BufferedReader zparOutput = new BufferedReader(new InputStreamReader(zpar.getInputStream()));

        String s;
        while ((s = zparOutput.readLine()) != null) {
            System.out.println(s);
            if (s.contains("initialized.")) {
                break;
            }
        }

        System.out.println("Initialized.");

        OutputStream zparInput = zpar.getOutputStream();

        for (String inputSentence : inputSentences) {
            System.out.println("Processing " + inputSentence);

            inputSentence = inputSentence.replaceAll("\n", " ");
            int numInputChars = inputSentence.replaceAll("\\s", "").length();

            new DataForwarder(new BufferedReader(new InputStreamReader(IOUtils.toInputStream(inputSentence, "UTF-8"))
            ), zparInput).start();

            List<String> parses = new ArrayList<>();

            String line;
            int numParsedChars = 0;
            while ((line = zparOutput.readLine()) != null) {
                System.out.println("Next line is : " + line);
                String replacedParse = line.replaceAll("#b\\s\\(", "#b -LRB-").replaceAll("#b\\s\\)", "#b -RRB-")
                        .replaceAll("#", "ddd");
                Tree parseTree = Tree.valueOf(replacedParse);
                numParsedChars += parseTree.getLeaves().size();

                parses.add(replacedParse);

                if (numParsedChars == numInputChars) {
                    break;
                }
            }

            System.out.println("Parses are : ");
            for (String parse : parses) {
                System.out.println(parse);
            }

        }

        System.out.println("Finished processing.");

        InputStream errorStream = zpar.getErrorStream();
        for (int i = 0; i < errorStream.available(); i++) {
            System.out.println("" + errorStream.read());
        }

        zpar.destroy();
    }
}
