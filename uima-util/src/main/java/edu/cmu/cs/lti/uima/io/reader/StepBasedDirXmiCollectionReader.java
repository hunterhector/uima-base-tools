package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.util.CasSerialization;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import java.io.File;
import java.io.IOException;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class StepBasedDirXmiCollectionReader extends AbstractStepBasedDirReader {
    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    public static final String PARAM_BASE_NAME_FILE_FILTER = "BaseNameFileFilter";
    @ConfigurationParameter(name = PARAM_BASE_NAME_FILE_FILTER, mandatory = false)
    File baseNameFileFilter;

    private int currentDocIndex;

    @Override
    protected String defaultFileSuffix() {
        return "xmi";
    }

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
            if (!StringUtils.isEmpty(inputViewName)) {
                jCas = jCas.getView(inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        CasSerialization.readXmi(jCas, files.get(currentDocIndex));
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