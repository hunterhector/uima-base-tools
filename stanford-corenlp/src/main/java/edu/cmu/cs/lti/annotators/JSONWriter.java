package edu.cmu.cs.lti.annotators;

import com.google.gson.Gson;
import edu.cmu.cs.lti.model.Span;
import edu.cmu.cs.lti.script.type.Article;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordEntityMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 6/7/17
 * Time: 2:49 PM
 *
 * @author Zhengzhong Liu
 */
public class JSONWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_PATH = "outputPath";

    @ConfigurationParameter(name = PARAM_OUTPUT_PATH)
    private File outputFile;

    private BufferedWriter writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        File dir = outputFile.getParentFile();
        FileUtils.ensureDirectory(dir);


        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    class JsonEntityMention {
        String surface;
        List<Integer> loc = new ArrayList<>();
        int head;
        String source;
    }

    class JsonEntity {
        List<JsonEntityMention> mentions = new ArrayList<>();
    }

    class Document {
        String docno;
        List<JsonEntity> coreferences = new ArrayList<>();
    }

    private int getNumTokensInTitle(JCas aJCas) {
        String[] parts = aJCas.getDocumentText().split("\\r\\n|\\n|\\r", 2);
        String title = parts[0];
        return title.split(" ").length;
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        int numTokenInTitle = getNumTokensInTitle(aJCas);

        int i = 0;
        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            token.setIndex(i);
            i++;
        }

        Document document = new Document();
        document.docno = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        UimaConvenience.printProcessLog(aJCas);
        logger.info("Number of tokens in title is " + numTokenInTitle);

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            Collection<StanfordEntityMention> mentions = FSCollectionFactory.create(entity.getEntityMentions(),
                    StanfordEntityMention.class);
            if (mentions.size() > 1) {
                JsonEntity jsonEntity = new JsonEntity();
                for (StanfordEntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(),
                        StanfordEntityMention.class)) {
                    JsonEntityMention jsonEntityMention = new JsonEntityMention();
                    jsonEntityMention.surface = mention.getCoveredText();

                    Span tokenSpan = getTokenSpan(mention);
                    int headTokenIndex = UimaNlpUtils.findHeadFromStanfordAnnotation(mention).getIndex();

                    if (headTokenIndex < numTokenInTitle) {
                        jsonEntityMention.loc.add(tokenSpan.getBegin());
                        jsonEntityMention.loc.add(tokenSpan.getEnd());
                        jsonEntityMention.head = headTokenIndex;
                        jsonEntityMention.source = "title";
                    } else {
                        jsonEntityMention.loc.add(tokenSpan.getBegin() - numTokenInTitle);
                        jsonEntityMention.loc.add(tokenSpan.getEnd() - numTokenInTitle);
                        jsonEntityMention.head = headTokenIndex - numTokenInTitle;
                        jsonEntityMention.source = "body";
                    }

                    jsonEntity.mentions.add(jsonEntityMention);
                }
                document.coreferences.add(jsonEntity);
            }
        }

        Gson gson = new Gson();
        String jsonStr = gson.toJson(document);

        try {
            writer.write(jsonStr);
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Span getTokenSpan(StanfordEntityMention mention) {
        int beginToken = -1;
        int endToken = -1;

        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, mention)) {
            if (beginToken == -1) {
                beginToken = token.getIndex();
            }

            if (endToken == -1) {
                endToken = token.getIndex();
            }

            if (token.getIndex() < beginToken) {
                beginToken = token.getIndex();
            }

            if (token.getIndex() > endToken) {
                endToken = token.getIndex();
            }
        }
        return Span.of(beginToken, endToken);
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
