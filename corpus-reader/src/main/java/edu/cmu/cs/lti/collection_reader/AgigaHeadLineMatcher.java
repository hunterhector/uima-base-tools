package edu.cmu.cs.lti.collection_reader;

import com.google.common.base.Joiner;
import edu.jhu.agiga.AgigaDocument;
import edu.jhu.agiga.AgigaPrefs;
import edu.jhu.agiga.StreamingDocumentReader;
import edu.stanford.nlp.trees.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/5/16
 * Time: 11:50 PM
 *
 * @author Zhengzhong Liu
 */
public class AgigaHeadLineMatcher {
    private final BufferedWriter output;
    private final List<File> gzFileList;
    private AgigaPrefs prefs = new AgigaPrefs();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AgigaHeadLineMatcher(File inputDir, File outputFile) throws IOException {
        prefs.setAll(false);
        prefs.setHeadline(true);


        if (!(inputDir.exists() && inputDir.isDirectory())) {
            throw new FileNotFoundException(inputDir.getAbsolutePath());
        }

//        gzFileList = inputDir.listFiles(new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                return name.toLowerCase().endsWith(".gz");
//            }
//        });


        String[] exts = new String[1];
        exts[0] = "xml.gz";

        gzFileList = new ArrayList<>(FileUtils.listFiles(inputDir, exts, true));

        File outputDir = outputFile.getParentFile();
        if (!outputDir.isDirectory()) {
            outputDir.mkdirs();
        }

        output = new BufferedWriter(new FileWriter(outputFile));
    }

    public void run() throws IOException {
        if (gzFileList.size() > 0) {
            for (File file : gzFileList) {
                logger.info("Processing " + file.getName());
                readOneGzFile(file);
                logger.info("Processing done.");
            }
        }
    }

    private void readOneGzFile(File file) throws IOException {
        StreamingDocumentReader dReader = new StreamingDocumentReader(file.getPath(), prefs);
        for (AgigaDocument doc : dReader) {
            doc.getDocId();
            if (doc.getHeadline() != null) {
                String headline = parsedSentenceToText(doc.getHeadline());
                if (headline != null) {
                    output.write(String.format("%s\t%s\n", doc.getDocId(), headline));
                }
            }
        }
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
        AgigaHeadLineMatcher matcher = new AgigaHeadLineMatcher(new File(argv[0]), new File(argv[1]));
        matcher.run();

        System.out.println("All processing done.");
    }
}
