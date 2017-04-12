package edu.cmu.cs.lti.caevo;

import caevo.SieveDocument;
import caevo.SieveDocuments;
import caevo.SieveSentence;
import caevo.TextEvent;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Adding gold standard events to Caevo.
 *
 * @author Zhengzhong Liu
 */
public class GoldStandardEventClassifier {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private SieveDocuments docs;
    private JCas goldView;

    public GoldStandardEventClassifier(SieveDocuments docs, JCas goldView) {
        this.docs = docs;
        this.goldView = goldView;
    }


    public void extractEvents(){
        for (SieveDocument doc : docs.getDocuments()){
            logger.info("Processing doc " + doc.getDocname());
            List<SieveSentence> sentences = doc.getSentences();


            for (SieveSentence sent : sentences){
                List<TextEvent> events = new ArrayList<>();



            }
        }
    }

}
