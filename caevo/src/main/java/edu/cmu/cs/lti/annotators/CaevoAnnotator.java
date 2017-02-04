package edu.cmu.cs.lti.annotators;

import caevo.*;
import caevo.util.CaevoProperties;
import caevo.util.Ling;
import caevo.util.WordNet;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static caevo.Main.serializedGrammar;

/**
 * Created with IntelliJ IDEA.
 * Date: 1/26/17
 * Time: 2:38 PM
 *
 * @author Zhengzhong Liu
 */
public class CaevoAnnotator extends AbstractLoggingAnnotator {
    public static final String CAEVO_RESOURCE_DIR = "caevo_resources";

    private File caevoDir;
    private String infopath;
    private boolean debug;
    private boolean useClosure;
    private boolean force24hrDCT;
    private String dctHeuristic;
    private Closure closure;
    private WordNet wordnet;
    private String[] sieveClasses;
    private Main.DatasetType dataset = Main.DatasetType.ALL;
    private LexicalizedParser parser;
    private GrammaticalStructureFactory gsf;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        URL caevoResourceUrl = this.getClass().getClassLoader().getResource(CAEVO_RESOURCE_DIR);

        try {
            caevoDir = new File(caevoResourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new ResourceInitializationException(e);
        }

        File propertyFile = new File(caevoDir, "default.properties");
        File sievesFile = new File(caevoDir, "cleartk.sieves");

        try {
            CaevoProperties.load(propertyFile.getPath());
            infopath = CaevoProperties.getString("Main.info", null);
            // Overwrite these globals if they are in the properties file.
            debug = CaevoProperties.getBoolean("Main.debug", debug);
            useClosure = CaevoProperties.getBoolean("Main.closure", useClosure);
            dataset = Main.DatasetType.valueOf(CaevoProperties.getString("Main.dataset", dataset.toString())
                    .toUpperCase());
            force24hrDCT = CaevoProperties.getBoolean("Main.force24hrdct", force24hrDCT);
            dctHeuristic = CaevoProperties.getString("Main.dctHeuristic", dctHeuristic);

        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }


        try {
            closure = new Closure();
        } catch (IOException ex) {
            System.out.println("ERROR: couldn't load Closure utility.");
            ex.printStackTrace();
            System.exit(1);
        }

        // Load WordNet for any and all sieves.
        wordnet = new WordNet();

        // Load the sieve list.
        sieveClasses = loadSieveList();

        parser = Ling.createParser(serializedGrammar);

        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        gsf = tlp.grammaticalStructureFactory();


    }

    private String[] loadSieveList() {
        String filename = System.getProperty("sieves");
        if (filename == null) filename = "default.sieves";

        System.out.println("Reading sieve list from: " + filename);

        List<String> sieveNames = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.matches("^\\s*$") && !line.matches("^\\s*//.*$")) {
                    // Remove trailing comments if they exist.
                    if (line.indexOf("//") > -1)
                        line = line.substring(0, line.indexOf("//"));
                    String name = line.trim();
                    sieveNames.add(name);
                }
            }
            reader.close();
        } catch (Exception ex) {
            System.out.println("ERROR: no sieve list found");
            ex.printStackTrace();
            System.exit(1);
        }

        String[] arr = new String[sieveNames.size()];
        return sieveNames.toArray(arr);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        File tempInput;
        try {
            tempInput = File.createTempFile("caevo_input", ".tmp");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        SieveDocuments docs = new SieveDocuments();
        SieveDocument doc = Tempeval3Parser.rawTextFileToParsed(tempInput.getPath(), parser, gsf);

        docs.addDocument(doc);

        TextEventClassifier eventClassifier = new TextEventClassifier(docs, wordnet);
        eventClassifier.loadClassifiers();

        eventClassifier.extractEvents();


    }
}
