package edu.cmu.cs.lti.collection_reader;

import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.jcas.JCas;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 2/3/15
 * Time: 6:12 PM
 */
public abstract class AbstractSourceDocumentCollectionReader extends JCasCollectionReader_ImplBase {
    protected void setSourceDocumentInformation(JCas aJCas, String uri, int size, int offsetInSource, boolean isLastSegment) {
        SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(aJCas);
        srcDocInfo.setUri(uri);
        srcDocInfo.setOffsetInSource(offsetInSource);
        srcDocInfo.setDocumentSize(size);
        srcDocInfo.setLastSegment(isLastSegment);
        srcDocInfo.addToIndexes();
    }
}
