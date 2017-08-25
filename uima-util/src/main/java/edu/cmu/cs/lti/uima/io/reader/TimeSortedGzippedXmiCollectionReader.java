package edu.cmu.cs.lti.uima.io.reader;

import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.util.NewsNameComparators;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.GZIPInputStream;

/**
 * A simple collection reader that reads CASes in XMI format from a directory in the filesystem.
 */
public class TimeSortedGzippedXmiCollectionReader extends AbstractCollectionReader {
    private int currentDocIndex;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
     */
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        Collections.sort(this.files, NewsNameComparators.getGigawordDateComparator(extension, "yyyymm"));

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
//        try {
//            if (!StringUtils.isEmpty(inputViewName)) {
//                jCas = jCas.getView(inputViewName);
//            }
//        } catch (Exception e) {
//            throw new CollectionException(e);
//        }

        File currentFile = files.get(currentDocIndex);
        currentDocIndex++;

        GZIPInputStream gzipIn = new GZIPInputStream(new FileInputStream(currentFile));
        try {
            XmiCasDeserializer.deserialize(gzipIn, jCas.getCas(), !failOnUnknownType);
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