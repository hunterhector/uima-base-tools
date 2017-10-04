package edu.illinois.cs.cogcomp.annotation;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/3/17
 * Time: 10:48 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class UimaSentenceAnnotator extends Annotator {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected ConcurrentMap<String, Integer> docSentenceId;

    public UimaSentenceAnnotator(String viewName) {
        super(viewName, new String[0]);
    }

    @Override
    public void initialize(ResourceManager rm) {
        docSentenceId = new ConcurrentHashMap<>();
    }

    protected int getNextSentenceId(String docid) {
        docSentenceId.putIfAbsent(docid, 0);
        int sentenceId = docSentenceId.get(docid);
        docSentenceId.put(docid, sentenceId + 1);
        return sentenceId;
    }

    protected void removeDoc(String docid) {
        docSentenceId.remove(docid);
    }
}
