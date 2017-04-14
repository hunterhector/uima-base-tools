package edu.cmu.cs.lti.annotators;

import caevo.*;
import caevo.Main.DatasetType;
import caevo.sieves.Sieve;
import caevo.tlink.TLink;
import caevo.util.*;
import com.google.common.io.Files;
import edu.cmu.cs.lti.caevo.GoldStandardEventClassifier;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.timeml.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionRelation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;
import org.uimafit.util.JCasUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static caevo.Main.serializedGrammar;

/**
 * Mainly modify from the Caevo's Main class to create an annotator here.
 * Date: 1/26/17
 * Time: 2:38 PM
 *
 * @author Zhengzhong Liu
 */
public class CaevoAnnotator extends AbstractLoggingAnnotator {
    // Classpath to the caeveo resources.
    public static final String CAEVO_RESOURCE_DIR = "caevo_resources";

    private File caevoDir;
    private boolean debug;
    private boolean useClosure;
    private boolean force24hrDCT;
    private String dctHeuristic;
    private Closure closure;
    private String[] sieveClasses;
    private Main.DatasetType dataset = Main.DatasetType.ALL;
    private LexicalizedParser parser;
    private GrammaticalStructureFactory gsf;

    private TimexClassifier timexClassifier;
    private TextEventClassifier eventClassifier;

    private GoldStandardEventClassifier goldEventClassifier;

    private File tempProcessDir;

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

        try {
            CaevoProperties.load(propertyFile.getPath());
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
            logger.info("ERROR: couldn't load Closure utility.");
            ex.printStackTrace();
            System.exit(1);
        }

        // Load WordNet for any and all sieves.
        Main.wordnet = new WordNet();

        // Load the sieve list.
        sieveClasses = loadSieveList();

        parser = Ling.createParser(serializedGrammar);

        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        gsf = tlp.grammaticalStructureFactory();

        tempProcessDir = Files.createTempDir();

        logger.info(String.format("Dataset: %s; Debug: %s; Closure: %s", dataset, debug, closure));
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        // Remove the cleaned directory.
        try {
            FileUtils.deleteDirectory(tempProcessDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] loadSieveList() {
        String filename = new File(caevoDir, "default.sieves").getPath();

        logger.info("Reading sieve list from: " + filename);

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
            logger.info("ERROR: no sieve list found");
            ex.printStackTrace();
            System.exit(1);
        }

        String[] arr = new String[sieveNames.size()];
        return sieveNames.toArray(arr);
    }

    private Span getSpan(JCas aJCas, SieveDocument doc, int sentenceId, int tokenId) {
        ArrayList<StanfordCorenlpSentence> sentences = new ArrayList<>(JCasUtil.select(aJCas,
                StanfordCorenlpSentence.class));
        StanfordCorenlpSentence sent = sentences.get(sentenceId);
        CoreLabel token = doc.getSentences().get(sentenceId).tokens().get(tokenId);
        return Span.of(sent.getBegin() + token.beginPosition(), sent.getBegin() + token.endPosition());
    }

    private void markupAll(JCas aJCas) throws IOException, SAXException, ParserConfigurationException {
        SieveDocuments docs = new SieveDocuments();
        SieveDocument doc = rawTextToParsed(aJCas, UimaConvenience.getDocId(aJCas), parser, gsf);
        docs.addDocument(doc);

        logger.info("Marking events.");
        Map<String, EventMention> eidMapping = markupEvents(docs, aJCas);

        int sindex = 0;
        for (List<TextEvent> textEvents : doc.getEventsBySentence()) {
            for (TextEvent textEvent : textEvents) {
                Span eventSpan = getSpan(aJCas, doc, sindex, textEvent.getIndex());
                Event timemlEvent = new Event(aJCas, eventSpan.getBegin(), eventSpan.getEnd());
                timemlEvent.setAspect(textEvent.getAspect().name());
                timemlEvent.setModality(textEvent.getModality());
                timemlEvent.setPolarity(textEvent.getPolarity().name());
                timemlEvent.setTense(textEvent.getTense().name());
                timemlEvent.setEventInstanceId(textEvent.getEiid());
                UimaAnnotationUtils.finishAnnotation(timemlEvent, COMPONENT_ID, 0, aJCas);
            }
            sindex++;
        }

        logger.info("Marking TimeEx");
        markupTimexes(docs);
        // Try to determine DCT based on relevant property settings
        // TODO: use reflection method parallel to how sieves are chosen to choose the right DCTHeuristic method
        if (Objects.equals(dctHeuristic, "setFirstDateAsDCT")) {
            DCTHeursitics.setFirstDateAsDCT(doc);  // only if there isn't already a DCT specified!
        }

        logger.info("Running Sieves.");
        runSieves(docs);

        for (TLink tLink : doc.getTlinks()) {
            String fromEiid = tLink.getId1();
            String toEiid = tLink.getId2();
            String relation = tLink.getRelation().name();

            if (!relation.equals("VAGUE")) {
                if (eidMapping.containsKey(fromEiid) && eidMapping.containsKey(toEiid)) {
                    EventMention fromEvent = eidMapping.get(fromEiid);
                    EventMention toEvent = eidMapping.get(toEiid);
//                    logger.info(String.format("%s --%s--> %s", fromEvent.getCoveredText(), relation,
//                            toEvent.getCoveredText()));

                    EventMentionRelation emr = new EventMentionRelation(aJCas);
                    emr.setHead(fromEvent);
                    emr.setChild(toEvent);
                    emr.setRelationType("TIMEML_" + relation);
                    UimaAnnotationUtils.finishTop(emr, COMPONENT_ID, 0, aJCas);
                }
            }
        }

        logger.info(String.format("Number of events found: %d, mapped to KBP events: %d",
                doc.getEvents().size(), eidMapping.size()));
    }

    private Sieve[] createAllSieves(String[] stringClasses) {
        Sieve sieves[] = new Sieve[stringClasses.length];
        for (int xx = 0; xx < stringClasses.length; xx++) {
            sieves[xx] = createSieveInstance(stringClasses[xx]);
            logger.info("Added sieve: " + stringClasses[xx]);
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
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                e) {
            logger.info("ERROR: couldn't load sieve: " + sieveClass);
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
            logger.info("Processing " + doc.getDocname() + "...");
//			logger.info("Number of gold links: " + thedocsUnchanged.getDocument(doc.getDocname()).getTlinks()
// .size());

            // Loop over the sieves in order.
            for (int xx = 0; xx < sieves.length; xx++) {
                Sieve sieve = sieves[xx];
                if (sieve == null) continue;
                logger.info("\tSieve " + sieve.getClass().toString());

                // Run this sieve
                List<TLink> newLinks = sieve.annotate(doc, currentTLinks);
                if (debug) logger.info("\t\t" + newLinks.size() + " new links.");
//				if( debug ) logger.info("\t\t" + newLinks);
                stats[xx].addProposedCount(newLinks.size());

                // Verify the links as non-conflicting.
                int numRemoved = removeConflicts(currentTLinksHash, newLinks);
                if (debug) logger.info("\t\tRemoved " + numRemoved + " proposed links.");
//				if( debug ) logger.info("\t\t" + newLinks);
                stats[xx].addRemovedCount(numRemoved);

                if (newLinks.size() > 0) {
                    // Add the good links to our current list.
                    addProposedToCurrentList(sieveClasses[xx], newLinks, currentTLinks, currentTLinksHash);
                    //currentTLinks.addAll(newLinks);

                    // Run Closure
                    if (useClosure) {
                        List<TLink> closedLinks = closureExpand(sieveClasses[xx], currentTLinks, currentTLinksHash);
                        if (debug) logger.info("\t\tClosure produced " + closedLinks.size() + " links.");
                        //					if( debug ) logger.info("\t\tclosed=" + closedLinks);
                        stats[xx].addClosureCount(closedLinks.size());
                    }
                }
                if (debug) logger.info("\t\tDoc now has " + currentTLinks.size() + " links.");
            }

            // Add links to InfoFile.
            doc.addTlinks(currentTLinks);
//			if( debug ) logger.info("Adding links: " + currentTLinks);
            currentTLinks.clear();
            currentTLinksHash.clear();
        }

//        File outputFile = new File(tempProcessDir, "sieve-output.xml");
//        logger.info("Writing output: " + outputFile);
//        docs.writeToXML(outputFile);
//
//        return outputFile;
    }

    private void addProposedToCurrentList(String sieveName, List<TLink> proposed, List<TLink> current, Map<String,
            TLink> currentHash) {
        for (TLink newlink : proposed) {
            if (currentHash.containsKey(newlink.getId1() + newlink.getId2())) {
                logger.info("MAIN WARNING: overwriting " + currentHash.get(newlink.getId1() + newlink.getId2()
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
        if (debug && duplicates > 0) logger.info("\t\tRemoved " + duplicates + " duplicate proposed links.");

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
                logger.info("WARNING (proposed an invalid link): " + proposed);
            }
            // Remove any proposed links that are duplicates of already proposed links.
            else if (seenNew.contains(proposed.getId1() + proposed.getId2())) {
                removals.add(proposed);
                logger.info("WARNING (proposed the same link twice): " + proposed);
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
    private Map<String, EventMention> markupEvents(SieveDocuments info, JCas aJCas) {
        if (eventClassifier == null) {
            eventClassifier = new TextEventClassifier(info, Main.wordnet);
            eventClassifier.loadClassifiers();
        }

        // Add timeout here.
        eventClassifier.extractEvents(info);

        if (goldEventClassifier == null) {
            goldEventClassifier = new GoldStandardEventClassifier(Main.wordnet);
            goldEventClassifier.loadClassifiers();
        }

        Map<String, EventMention> eidMapping = new HashMap<>();

        goldEventClassifier.extractEvents(aJCas, info, eidMapping);

        return eidMapping;
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
        UimaConvenience.printProcessLog(aJCas);
        try {
            markupAll(aJCas);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private SieveDocument rawTextToParsed(JCas aJCas, String filename, LexicalizedParser parser,
                                          GrammaticalStructureFactory gsf) {
        List<List<HasWord>> sentencesNormInvertible = new ArrayList<>();
        sentencesNormInvertible.addAll(convertSentence(aJCas));

        SieveDocument sdoc = new SieveDocument(filename);

        int sid = 0;
        for (List<HasWord> sent : sentencesNormInvertible) {
            Pair<String, String> parseDep = Tempeval3Parser.parseDep(sent, parser, gsf);
            List<CoreLabel> cls = new ArrayList<CoreLabel>();
            for (HasWord word : sent) cls.add((CoreLabel) word);
            sdoc.addSentence(Tempeval3Parser.buildString(sent, 0, sent.size()), cls, parseDep.first(),
                    parseDep.second(), null, null);
            sid++;
        }

        return sdoc;
    }

    private List<List<HasWord>> convertSentence(JCas aJCas) {
        List<List<HasWord>> sentences = new ArrayList<List<HasWord>>();

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            List<HasWord> convertedSent = tokenize(sentence.getCoveredText());
            sentences.add(convertedSent);
        }
        return sentences;
    }

    public static List<HasWord> tokenize(String str) {
        List<HasWord> tokens = new ArrayList<>();

        StringReader reader = new StringReader(str);
        DocumentPreprocessor dp = new DocumentPreprocessor(reader);
        TokenizerFactory<? extends HasWord> factory = null;

        factory = PTBTokenizer.factory(true, true);

        String options = "invertible=true";

        factory.setOptions(options);
        dp.setTokenizerFactory(factory);

        for (List<HasWord> sent : dp) {
            tokens.addAll(sent);
        }
        return tokens;
    }

    public static void main(String[] args) throws IOException, UIMAException {
        if (args.length < 2) {
            System.out.println("Please provide parent, base input directory");
            System.exit(1);
        }

        String parentInput = args[0]; //"data";

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

        AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_RELATIONS, true
        );

        AnalysisEngineDescription caevo = AnalysisEngineFactory.createEngineDescription(
                CaevoAnnotator.class, typeSystemDescription
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(
                parentInput, paramBaseOutputDirName);

        SimplePipeline.runPipeline(reader, goldAnnotator, caevo, writer);
    }
}
