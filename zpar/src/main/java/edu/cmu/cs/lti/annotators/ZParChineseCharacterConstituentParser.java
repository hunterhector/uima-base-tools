package edu.cmu.cs.lti.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.type.CharacterAnnotation;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.ZparTreeAnnotation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.DataForwarder;
import edu.stanford.nlp.trees.AbstractCollinsHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.international.pennchinese.ChineseSemanticHeadFinder;
import edu.stanford.nlp.util.IntPair;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/15/16
 * Time: 10:07 AM
 *
 * @author Zhengzhong Liu
 */
public class ZParChineseCharacterConstituentParser extends AbstractLoggingAnnotator {

    private Process zpar;
    private BufferedReader zparOutput;
    private OutputStream zparInput;

    public static final String PARAM_CHINESE_MODEL = "chineseModel";

    public static final String PARAM_ZPAR_BIN_PATH = "zparBin";

    private final String charLabelSep = "ddd";

    @ConfigurationParameter(name = PARAM_CHINESE_MODEL)
    private String chineseModelPath;

    @ConfigurationParameter(name = PARAM_ZPAR_BIN_PATH)
    private String zparBin;

    private AbstractCollinsHeadFinder hf;

    private boolean zparInitializationSuccess = false;


    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        logger.info("ZPar bin is at " + zparBin);
        logger.info("ZPar model located at " + chineseModelPath);
        boolean setExe = new File(zparBin).setExecutable(true);

        if (!setExe) {
            throw new IllegalAccessError("Cannot make the ZPar binary executable.");
        }

        String[] command = {zparBin, chineseModelPath, "-oz"};

        try {
            zpar = new ProcessBuilder(command).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        zparOutput = new BufferedReader(new InputStreamReader(zpar.getInputStream()));

        String s;
        try {
            while ((s = zparOutput.readLine()) != null) {
                logger.info(s);
                if (s.contains("initialized.")) {
                    zparInitializationSuccess = true;
                    break;
                }
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        try {
            printError();
        } catch (IOException e) {
            e.printStackTrace();
        }

        zparInput = zpar.getOutputStream();

        // Trying to use the Stanford head finder to find token level heads.
        hf = new ChineseSemanticHeadFinder();

        if (zparInitializationSuccess) {
            logger.info("Successfully initialized ZPar thread.");
        } else {
            logger.info("Cannot initialize ZPar thread.");
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        if (zparInitializationSuccess) {
            for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
                try {
                    String parsedSent = getParse(sentence);
                    annotate(aJCas, sentence, parsedSent);
                    logger.debug(parsedSent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getParse(StanfordCorenlpSentence sentence) throws IOException {
        List<String> parses = new ArrayList<>();

        // Zpar will consider each line as an input, and will also try to parse full-width spaces. We here remove them.
        // Note that the regex is whitespace and the full width space.
        String sentenceText = sentence.getCoveredText().replaceAll("[\\s　]", " ").replaceAll("#", ".");
        logger.debug("Processing " + sentenceText);

        new DataForwarder(new BufferedReader(new InputStreamReader(IOUtils.toInputStream(
                sentenceText, "UTF-8"))), zparInput).start();

        int numCharacter = JCasUtil.selectCovered(CharacterAnnotation.class, sentence).size();

        logger.debug("Waiting for parsing.");

        String line;
        int numParsedChars = 0;

        // Timeout the zpar parsing after 100 seconds.
        long startTime = System.currentTimeMillis();
        while ((line = zparOutput.readLine()) != null || (System.currentTimeMillis() - startTime) < 100000) {
            logger.debug("Original parse line is : " + line);

            // First we replace brackets to -LRB- and -RRB-.
            // To detect whether they are parse brackets or not, we use the label before it.
            // We then repalce # with our own determiner, cuz  punctuations will be ignored by the tree reader.
            String replacedParse = line.replaceAll("#i\\s\\(", "#i -LRB-").replaceAll("#b\\s\\(", "#b -LRB-")
                    .replaceAll("#i\\s\\)", "#i -RRB-").replaceAll("#b\\s\\)", "#b -RRB-").replaceAll("#", "ddd");

            logger.debug("Replace parse is : " + replacedParse);
            Tree parseTree = Tree.valueOf(replacedParse);
            numParsedChars += parseTree.getLeaves().size();

            parses.add(replacedParse);

            logger.debug(String.format("Number of input character : %d, number of parsed character: %d.",
                    numCharacter, numParsedChars));

            if (numParsedChars >= numCharacter) {
                // Sometimes the Zpar parser will have more characters that needed.
                break;
            }
        }

        if (numParsedChars != numCharacter) {
            logger.warn(String.format("Number of parsed character [%d] is not the same as number of actual " +
                    "character [%d].", numParsedChars, numCharacter));
            logger.warn("Original sentence is " + sentenceText);
            logger.warn("Parsed result contains the following: ");
            for (String parse : parses) {
                logger.error(parse);
            }
        }

        if (parses.size() == 1) {
            return parses.get(0);
        } else {
            return "(IP " + Joiner.on(" ").join(parses) + ")";
        }
    }

    private void annotate(JCas aJCas, StanfordCorenlpSentence sentence, String parse) {
        List<CharacterAnnotation> characters = JCasUtil.selectCovered(CharacterAnnotation.class, sentence);

        Tree parseTree = Tree.valueOf(parse);
        parseTree.setSpans();

        int numTreeLeaves = parseTree.getLeaves().size();

        if (characters.size() == numTreeLeaves) {
            logger.debug("Going to annotate the parse into the sentence.");
            annotateParse(aJCas, null, parseTree, characters);
            logger.debug("Done.");
        } else {
            logger.warn(String.format("Unequal parse leaves (%d) vs. sentence length (%d), not adding to JCas.",
                    numTreeLeaves, characters.size()));
        }
    }

    private ZparTreeAnnotation annotateParse(JCas aJCas, ZparTreeAnnotation parent, Tree parseTree,
                                             List<CharacterAnnotation> characters) {
        IntPair span = parseTree.getSpan();
        int from = span.getSource();
        int to = span.getTarget();

        ZparTreeAnnotation zparTree = new ZparTreeAnnotation(aJCas);

        zparTree.setBegin(characters.get(from).getBegin());
        zparTree.setEnd(characters.get(to).getEnd());
        zparTree.setIsLeaf(parseTree.isLeaf());
        zparTree.setParent(parent);
        zparTree.setIsRoot(parent == null);

        String[] labelParts = parseTree.label().value().split(charLabelSep);

        boolean isCharacterParse = labelParts.length == 2;

        zparTree.setPennTreeLabel(labelParts[0]);

        // Find head.
        Tree headTree = null;
        if (!isCharacterParse && !parseTree.isLeaf()) {
            headTree = hf.determineHead(parseTree);
        }

        ZparTreeAnnotation headZparTree = null;
        List<ZparTreeAnnotation> childAnnotations = new ArrayList<>();
        for (Tree child : parseTree.children()) {
            ZparTreeAnnotation childAnnotation = annotateParse(aJCas, zparTree, child, characters);
            childAnnotations.add(childAnnotation);

            if (headTree != null && headTree.equals(child)) {
                headZparTree = childAnnotation;
            }
        }

        zparTree.setChildren(FSCollectionFactory.createFSArray(aJCas, childAnnotations));

        if (isCharacterParse) {
            String charParLabel = labelParts[1];
            zparTree.setAdditionalCharacterLabel(charParLabel);

            if (childAnnotations.size() == 0) {
                logger.debug("The current tree has no children but it is a non terminal");
                logger.debug(parseTree.toString());
            }

            if (charParLabel.equals("t")) {
                // The unary full word label.
                headZparTree = childAnnotations.get(0);
            } else if (charParLabel.equals("x") || charParLabel.equals("y") || charParLabel.equals("z")) {
                // Word structure are binarized, so we have exactly two child here.
                if (charParLabel.equals("x")) {
                    // This is the even case, we take the head to be the left one, but one should know to extract
                    // features from all.
                    headZparTree = childAnnotations.get(0);
                } else if (charParLabel.equals("y")) {
                    headZparTree = childAnnotations.get(1);
                } else {
                    headZparTree = childAnnotations.get(0);
                }
            } else if (charParLabel.equals("b") || charParLabel.equals("i")) {
                // The character annotation, indicate begin or inside.
                headZparTree = childAnnotations.get(0);
                CharacterAnnotation childCharacter = (CharacterAnnotation) headZparTree.getHead();
                if (charParLabel.equals("b")) {
                    childCharacter.setIsBegin(true);
                }
                childCharacter.setPos(zparTree.getPennTreeLabel());
            } else {
                logger.error(String.format("Encounter unknown character parsing label %s.", charParLabel));
                headZparTree = null;
            }
        }

        if (headZparTree != null) {
            zparTree.setHead(headZparTree.getHead());
            zparTree.setHeadTree(headZparTree);
        } else {
            zparTree.setHead(characters.get(from));
        }

        UimaAnnotationUtils.finishAnnotation(zparTree, COMPONENT_ID, 0, aJCas);
        return zparTree;
    }

    private void printError() throws IOException {
        InputStream errorStream = zpar.getErrorStream();
        if (errorStream.available() > 0) {
            logger.error(IOUtils.toString(errorStream));
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            printError();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        zpar.destroy();
    }
}
