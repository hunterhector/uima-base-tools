package edu.cmu.cs.lti.annotators;

import caevo.*;
import caevo.Main.DatasetType;
import caevo.sieves.Sieve;
import caevo.tlink.TLink;
import caevo.util.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static caevo.Main.serializedGrammar;

/**
 * Created with IntelliJ IDEA.l
 * Date: 1/26/17
 * Time: 2:38 PM
 *
 * @author Zhengzhong Liu
 */
public class CaevoAnnotator extends AbstractLoggingAnnotator {
    public static final String PARAM_RAW_TEXT_DIR = "rawTextDir";

    @ConfigurationParameter(name = PARAM_RAW_TEXT_DIR)
    private File rawTextDir;

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

    private TimexClassifier timexClassifier;
    private TextEventClassifier eventClassifier;

    private String outpath = "sieve-output.xml";

    SieveDocuments thedocsUnchanged; // for evaluating if TLinks are in the input

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

    public void markupAll(SieveDocuments docs) {
        markupEvents(docs);
        markupTimexes(docs);
        // Try to determine DCT based on relevant property settings
        // TODO: use reflection method parallel to how sieves are chosen to choose the right DCTHeuristic method
        if (dctHeuristic == "setFirstDateAsDCT") {
            for (SieveDocument doc : docs.getDocuments()) {
                DCTHeursitics.setFirstDateAsDCT(doc);  // only if there isn't already a DCT specified!
            }
        }
        runSieves(docs);
    }

    private Sieve[] createAllSieves(String[] stringClasses) {
        Sieve sieves[] = new Sieve[stringClasses.length];
        for (int xx = 0; xx < stringClasses.length; xx++) {
            sieves[xx] = createSieveInstance(stringClasses[xx]);
            System.out.println("Added sieve: " + stringClasses[xx]);
        }
        return sieves;
    }

    /**
     * Turns a string class name into an actual Sieve Instance of the class.
     *
     * @param sieveClass
     * @return
     */
    private Sieve createSieveInstance(String sieveClass) {
        try {
            Class<?> c = Class.forName("caevo.sieves." + sieveClass);
            Sieve sieve = (Sieve) c.newInstance();
            return sieve;
        } catch (InstantiationException e) {
            System.out.println("ERROR: couldn't load sieve: " + sieveClass);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.out.println("ERROR: couldn't load sieve: " + sieveClass);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: couldn't load sieve: " + sieveClass);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("ERROR: couldn't load sieve: " + sieveClass);
            e.printStackTrace();
        }
        return null;
    }


    public SieveDocuments getDataset(DatasetType type, SieveDocuments docs) {
        SieveDocuments dataset;
        if (type == DatasetType.TRAIN)
            dataset = Evaluate.getTrainSet(docs);
        else if (type == DatasetType.DEV)
            dataset = Evaluate.getDevSet(docs);
        else if (type == DatasetType.TEST)
            dataset = Evaluate.getTestSet(docs);
        else // ALL
            dataset = docs;

        // Fix DCTs that aren't 24-hour days.
        if (force24hrDCT) force24hrDCTs(docs);

        return dataset;
    }

    private void force24hrDCTs(SieveDocuments docs) {
        if (docs != null) {
            for (SieveDocument doc : docs.getDocuments()) {
                List<Timex> dcts = doc.getDocstamp();
                if (dcts != null) {
                    for (Timex dct : dcts)
                        Util.force24hrTimex(dct);
                }
            }
        }
    }


    /**
     * Run the sieve pipeline on the given documents.
     */
    public void runSieves(SieveDocuments thedocs) {
        // Remove all TLinks because we will add our own.
        thedocs.removeAllTLinks();

        // Start with zero links.
        List<TLink> currentTLinks = new ArrayList<TLink>();
        Map<String, TLink> currentTLinksHash = new HashMap<String, TLink>();

        // Create all the sieves first.
        Sieve sieves[] = createAllSieves(sieveClasses);

        // Statistics collection.
        SieveStats stats[] = new SieveStats[sieveClasses.length];
        Map<String, SieveStats> sieveNameToStats = new HashMap<String, SieveStats>();
        for (int i = 0; i < sieveClasses.length; i++) {
            stats[i] = new SieveStats(sieveClasses[i]);
            sieveNameToStats.put(sieveClasses[i], stats[i]);
        }

        // Data
        SieveDocuments docs = getDataset(dataset, thedocs);

        // Do each file independently.
        for (SieveDocument doc : docs.getDocuments()) {
            System.out.println("Processing " + doc.getDocname() + "...");
//			System.out.println("Number of gold links: " + thedocsUnchanged.getDocument(doc.getDocname()).getTlinks()
// .size());

            // Loop over the sieves in order.
            for (int xx = 0; xx < sieves.length; xx++) {
                Sieve sieve = sieves[xx];
                if (sieve == null) continue;
                System.out.println("\tSieve " + sieve.getClass().toString());

                // Run this sieve
                List<TLink> newLinks = sieve.annotate(doc, currentTLinks);
                if (debug) System.out.println("\t\t" + newLinks.size() + " new links.");
//				if( debug ) System.out.println("\t\t" + newLinks);
                stats[xx].addProposedCount(newLinks.size());

                // Verify the links as non-conflicting.
                int numRemoved = removeConflicts(currentTLinksHash, newLinks);
                if (debug) System.out.println("\t\tRemoved " + numRemoved + " proposed links.");
//				if( debug ) System.out.println("\t\t" + newLinks);
                stats[xx].addRemovedCount(numRemoved);

                if (newLinks.size() > 0) {
                    // Add the good links to our current list.
                    addProposedToCurrentList(sieveClasses[xx], newLinks, currentTLinks, currentTLinksHash);
                    //currentTLinks.addAll(newLinks);

                    // Run Closure
                    if (useClosure) {
                        List<TLink> closedLinks = closureExpand(sieveClasses[xx], currentTLinks, currentTLinksHash);
                        if (debug) System.out.println("\t\tClosure produced " + closedLinks.size() + " links.");
                        //					if( debug ) System.out.println("\t\tclosed=" + closedLinks);
                        stats[xx].addClosureCount(closedLinks.size());
                    }
                }
                if (debug) System.out.println("\t\tDoc now has " + currentTLinks.size() + " links.");
            }

            // Add links to InfoFile.
            doc.addTlinks(currentTLinks);
//			if( debug ) System.out.println("Adding links: " + currentTLinks);
            currentTLinks.clear();
            currentTLinksHash.clear();
        }

        System.out.println("Writing output: " + outpath);
        docs.writeToXML(new File(outpath));

        // Evaluate it if the input file had tlinks in it.
        if (thedocsUnchanged != null)
            Evaluate.evaluate(thedocsUnchanged, docs, sieveClasses, sieveNameToStats);
    }


    private void addProposedToCurrentList(String sieveName, List<TLink> proposed, List<TLink> current, Map<String,
            TLink> currentHash) {
        for (TLink newlink : proposed) {
            if (currentHash.containsKey(newlink.getId1() + newlink.getId2())) {
                System.out.println("MAIN WARNING: overwriting " + currentHash.get(newlink.getId1() + newlink.getId2()
                ) + " with " + newlink);
            }
            current.add(newlink);
            currentHash.put(newlink.getId1() + newlink.getId2(), newlink);
            currentHash.put(newlink.getId2() + newlink.getId1(), newlink);
            newlink.setOrigin(sieveName);
        }
    }


    /**
     * DESTRUCTIVE FUNCTION (links may have new TLink objects appended to it)
     * Run transitive closure and add any inferred links.
     *
     * @param links The list of TLinks to expand with transitive closure.
     * @return The list of new links from closure (these are already added to the given lists)
     */
    private List<TLink> closureExpand(String sieveName, List<TLink> links, Map<String, TLink> linksHash) {
        List<TLink> newlinks = closure.computeClosure(links, false);
        addProposedToCurrentList(sieveName, newlinks, links, linksHash);
        return newlinks;
    }

    /**
     * DESTRUCTIVE FUNCTION (proposedLinks will be modified)
     * Removes any links from the proposed list that already have links between the same pairs in currentLinks.
     *
     * @param currentLinksHash The list of current "good" links.
     * @param proposedLinks    The list of proposed new links.
     * @return The number of links removed.
     */
    private int removeConflicts(Map<String, TLink> currentLinksHash, List<TLink> proposedLinks) {
        List<TLink> removals = new ArrayList<TLink>();

        // Remove duplicates.
        int duplicates = removeDuplicatesAndInvalids(proposedLinks);
        if (debug && duplicates > 0) System.out.println("\t\tRemoved " + duplicates + " duplicate proposed links.");

        for (TLink proposed : proposedLinks) {
            // Look for a current link that conflicts with this proposed link.
            TLink current = currentLinksHash.get(proposed.getId1() + proposed.getId2());
            if (current != null && current.coversSamePair(proposed))
                removals.add(proposed);
        }

        for (TLink remove : removals)
            proposedLinks.remove(remove);

        return removals.size() + duplicates;
    }

    /**
     * DESTRUCTIVE FUNCTION (proposedLinks will be modified)
     * Remove a link from the given list if another link already exists in the list
     * and covers the same event or time pair.
     *
     * @param proposedLinks A list of TLinks to check for duplicates.
     * @return The number of duplicates found.
     */
    private int removeDuplicatesAndInvalids(List<TLink> proposedLinks) {
        if (proposedLinks == null || proposedLinks.size() < 2)
            return 0;

        List<TLink> removals = new ArrayList<TLink>();
        Set<String> seenNew = new HashSet<String>();

        for (TLink proposed : proposedLinks) {
            // Make sure we have a valid link with 2 events!
            if (proposed.getId1() == null || proposed.getId2() == null ||
                    proposed.getId1().length() == 0 || proposed.getId2().length() == 0) {
                removals.add(proposed);
                System.out.println("WARNING (proposed an invalid link): " + proposed);
            }
            // Remove any proposed links that are duplicates of already proposed links.
            else if (seenNew.contains(proposed.getId1() + proposed.getId2())) {
                removals.add(proposed);
                System.out.println("WARNING (proposed the same link twice): " + proposed);
            }
            // Normal link. Keep it.
            else {
                seenNew.add(proposed.getId1() + proposed.getId2());
                seenNew.add(proposed.getId2() + proposed.getId1());
            }
        }

        for (TLink remove : removals)
            proposedLinks.remove(remove);

        return removals.size();
    }

    /**
     * Assumes the SieveDocuments has its text parsed.
     */
    public void markupEvents(SieveDocuments info) {
        if (eventClassifier == null) {
            eventClassifier = new TextEventClassifier(info, wordnet);
            eventClassifier.loadClassifiers();
        }
        eventClassifier.extractEvents();
    }

    /**
     * Assumes the SieveDocuments has its text parsed.
     */
    public void markupTimexes(SieveDocuments info) {
        if (timexClassifier == null)
            timexClassifier = new TimexClassifier(info);
        timexClassifier.markupTimex3();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        File rawInput = new File(rawTextDir, UimaConvenience.getDocId(aJCas));

        SieveDocuments docs = new SieveDocuments();
        SieveDocument doc = Tempeval3Parser.rawTextFileToParsed(rawInput.getPath(), parser, gsf);

        docs.addDocument(doc);

        TextEventClassifier eventClassifier = new TextEventClassifier(docs, wordnet);
        eventClassifier.loadClassifiers();

        eventClassifier.extractEvents();

        markupAll(docs);
    }

    public static void main(String[] args) throws IOException, UIMAException {
        if (args.length < 2) {
            System.out.println("Please provide parent, base input directory");
            System.exit(1);
        }

        String parentInput = args[0]; //"data";

        // Parameters for the writer
        String baseInput = args[1]; //"01_event_tuples"

        String paramBaseOutputDirName = "caevo_parsed";

        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription annotator = AnalysisEngineFactory.createEngineDescription(
                CaevoAnnotator.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName);

        SimplePipeline.runPipeline(reader, annotator, writer);

    }
}
