package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class XmiCollectionReader extends AbstractCollectionReader {

    private static final String DEFAULT_FILE_SUFFIX = "xmi";
    private int currentDocIndex;

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

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    /**
     * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
     */
    public void getNext(JCas jCas) throws IOException, CollectionException {
//        try {
//            if (!StringUtils.isEmpty(inputViewName)) {
//                aCAS = aCAS.getView(inputViewName);
//            }
//        } catch (Exception e) {
//            throw new CollectionException(e);
//        }

        File currentFile = files.get(currentDocIndex++);

        FileInputStream inputStream = new FileInputStream(currentFile);
        try {
            XmiCasDeserializer.deserialize(inputStream, jCas.getCas(), !failOnUnknownType);
        } catch (SAXException e) {
            throw new CollectionException(e);
        } finally {
            inputStream.close();
        }
    }

    /**
     * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
     */
    public void close() throws IOException {
    }
}