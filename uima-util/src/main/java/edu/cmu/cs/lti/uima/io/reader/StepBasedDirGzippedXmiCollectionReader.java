package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class StepBasedDirGzippedXmiCollectionReader extends AbstractCollectionReader {

    public static final String PARAM_INPUT_VIEW_NAME = "ViewName";

    private String inputViewName;

    private int currentDocIndex;

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        inputViewName = (String) getConfigParameterValue(PARAM_INPUT_VIEW_NAME);

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
    public void getNext(JCas aJCas) throws IOException, CollectionException {
        try {
            if (!StringUtils.isEmpty(inputViewName)) {
                aJCas = aJCas.getView(inputViewName);
            }
        } catch (Exception e) {
            throw new CollectionException(e);
        }

        File currentFile = files.get(currentDocIndex);
        currentDocIndex++;

        GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(currentFile));
        try {
            XmiCasDeserializer.deserialize(gzipIn, aJCas.getCas(), !failOnUnknownType);
            gzipIn.close();
        } catch (SAXException e) {
            throw new CollectionException(e);
        }
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