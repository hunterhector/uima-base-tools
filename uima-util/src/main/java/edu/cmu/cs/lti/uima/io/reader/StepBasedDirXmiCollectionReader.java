package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.CasSerialization;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.IOException;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class StepBasedDirXmiCollectionReader extends AbstractCollectionReader {
    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    private int currentDocIndex;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        currentDocIndex = 0;
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#hasNext()
     */
    public boolean hasNext() {
        return currentDocIndex < files.size();
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     */
    public void getNext(JCas jCas) throws IOException, CollectionException {
        try {
            CasSerialization.readXmi(jCas, files.get(currentDocIndex));
        } catch (CollectionException e) {
            logger.info("Found exceptions in document: " + files.get(currentDocIndex).getCanonicalPath());
            e.printStackTrace();
        }

        currentDocIndex++;
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() throws IOException {
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
     */
    public Progress[] getProgress() {
        return new Progress[]{new ProgressImpl(currentDocIndex, files.size(), Progress.ENTITIES)};
    }

}