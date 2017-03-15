package edu.cmu.cs.lti.collection_reader;

import com.google.common.base.Joiner;
import edu.jhu.agiga.*;
import edu.stanford.nlp.trees.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Read gigaword and produce a text file for further training (language models or word embedding).
 *
 * @author Zhengzhong Liu
 */
public class GigawordAsCorpus {
    private final BufferedWriter output;
    private final ArrayList<File> gzFileList;
    private AgigaPrefs prefs = new AgigaPrefs();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private int counter;


    public GigawordAsCorpus(File inputDir, File outputFile) throws IOException {
        prefs.setAll(false);
        prefs.setHeadline(true);
        prefs.setWord(true);

        if (!(inputDir.exists() && inputDir.isDirectory())) {
            throw new FileNotFoundException(inputDir.getAbsolutePath());
        }

        String[] exts = new String[1];
        exts[0] = "xml.gz";

        gzFileList = new ArrayList<>(FileUtils.listFiles(inputDir, exts, true));

        File outputDir = outputFile.getParentFile();

        logger.info(outputDir.getPath());

        if (!outputDir.isDirectory()) {
            outputDir.mkdirs();
        }

        output = new BufferedWriter(new FileWriter(outputFile));
    }

    public void build() throws IOException {
        if (gzFileList.size() > 0) {
            for (File file : gzFileList) {
                processOneGzFile(file);
            }
        }
        output.close();
        System.out.println();
    }

    private void processOneGzFile(File file) throws IOException {
        StreamingDocumentReader dReader = new StreamingDocumentReader(file.getPath(), prefs);
        for (AgigaDocument doc : dReader) {
            doc.getDocId();
            if (doc.getHeadline() != null) {
                String headline = formatHeadline(doc);
                output.write(headline);
                output.write(" ");
            }
            output.write(getDocumentText(doc));
            output.write(" ");
            counter++;
            System.out.print("\rNumber of file processed: " + counter);
        }
    }

    private String formatHeadline(AgigaDocument doc) throws IOException {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (String line : doc.getHeadline().split("\n")) {
            String headline = parsedSentenceToText(line);
            sb.append(sep);
            sb.append(headline);
            sep = " ";
        }

        return sb.toString().toLowerCase();
    }

    private String getDocumentText(AgigaDocument doc) {
        StringBuilder builder = new StringBuilder();

        String sep = "";
        for (AgigaSentence aSent : doc.getSents()) {
            for (AgigaToken aToken : aSent.getTokens()) {
                String tokenSurface = aToken.getWord();
                builder.append(tokenSurface);
                builder.append(sep);
                sep = " ";
            }
        }
        return builder.toString();
    }


    private String parsedSentenceToText(String parsedText) throws IOException {
        TreeFactory tf = new LabeledScoredTreeFactory();
        StringReader r = new StringReader(parsedText);
        TreeReader tr = new PennTreeReader(r, tf);
        Tree tree = tr.readTree();

        if (tree != null) {
            return Joiner.on(" ").join(tree.getLeaves().stream().map(Tree::nodeString).collect(Collectors.toList()));
        } else {
            return null;
        }
    }

    public static void main(String[] argv) throws IOException {
        GigawordAsCorpus corpus = new GigawordAsCorpus(new File(argv[0]), new File(argv[1]));
        corpus.build();
        System.out.println("Process done.");
    }
}