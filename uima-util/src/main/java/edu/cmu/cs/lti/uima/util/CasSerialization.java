package edu.cmu.cs.lti.uima.util;

import com.google.common.base.Joiner;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/15
 * Time: 10:33 AM
 *
 * @author Zhengzhong Liu
 */
public class CasSerialization {
    private static final Logger logger = LoggerFactory.getLogger(CasSerialization.class);


    /**
     * Serialize a CAS to a file in XMI format
     *
     * @param aCas       CAS to serialize
     * @param outputFile output file
     * @throws SAXException
     * @throws Exception
     * @throws ResourceProcessException
     */
    public static void writeAsGzip(CAS aCas, File outputFile) throws IOException, SAXException {
        GZIPOutputStream gzipOut = null;

        try {
            // write gzipped XMI
            gzipOut = new GZIPOutputStream(new FileOutputStream(outputFile));
            XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
            XMLSerializer xmlSer = new XMLSerializer(gzipOut, false);
            ser.serialize(aCas, xmlSer.getContentHandler());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (gzipOut != null) {
                gzipOut.close();
            }
        }
    }

    public static String cleanText(String text) {
        StringBuilder cleanedText = new StringBuilder(text);
        int invalid = XMLUtils.checkForNonXmlCharacters(cleanedText.toString());

        while (invalid > -1) {
            // Replacing invalid characters with spaces.
            cleanedText.replace(invalid, invalid + 1, " ");
            invalid = XMLUtils.checkForNonXmlCharacters(cleanedText.toString());
        }

        return cleanedText.toString();
    }

    /**
     * Serialize a CAS to a file in XMI format
     *
     * @param aCas    CAS to serialize
     * @param xmiFile output file
     * @throws SAXException
     * @throws Exception
     * @throws ResourceProcessException
     */
    public static void writeAsXmi(CAS aCas, File xmiFile) throws IOException, SAXException {
        FileOutputStream out = null;

        try {
            // write XMI
            out = new FileOutputStream(xmiFile);
            XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
            XMLSerializer xmlSer = new XMLSerializer(out, false);
            ser.serialize(aCas, xmlSer.getContentHandler());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Deserialize XMI into JCas
     *
     * @param jCas    The jCas to take the input.
     * @param xmiFile The input xmi file.
     * @throws IOException
     * @throws CollectionException
     */
    public static void readXmi(JCas jCas, File xmiFile) throws IOException, CollectionException {
        readXmi(jCas, xmiFile, false);
    }

    /**
     * Deserialize XMI into JCas
     *
     * @param jCas              The jCas to take the input.
     * @param xmiFile           The input xmi file.
     * @param failOnUnknownType Whether to fail on unknown types.
     * @throws IOException
     * @throws CollectionException
     */
    public static void readXmi(JCas jCas, File xmiFile, boolean failOnUnknownType) throws IOException,
            CollectionException {
        try (FileInputStream inputStream = new FileInputStream(xmiFile)) {
            XmiCasDeserializer.deserialize(inputStream, jCas.getCas(), !failOnUnknownType);
        } catch (SAXException e) {
            throw new CollectionException(e);
        }
    }

    /**
     * Retrieve the input file name from the source document information.
     *
     * @param view             The view that contains the Article or SourceDocumentInformation annotation.
     * @param outputFileSuffix The file suffix to output.
     * @return A filename that based on the input file. Null if input if cannot find input file name.
     * @throws AnalysisEngineProcessException
     */
    public static String getOutputFileNameFromSource(JCas view, String outputFileSuffix) throws
            AnalysisEngineProcessException {
        // Retrieve the filename of the input file from the CAS.
        try {
            return UimaConvenience.getDocumentName(view) + outputFileSuffix;
        } catch (IllegalArgumentException e) {
            logger.info("Cannot find original input for file.");
            e.printStackTrace();
        }
        return null;
    }

    public static String getGigawordDirSegments(String docName) {
        Pattern pattern = Pattern.compile("([A-Za-z]{3})_[A-Za-z]{3}_(\\d{4})(\\d{2})(\\d{2}).*");
//        Pattern pattern = Pattern.compile("([A-Za-z]{3})_[A-Za-z]{3}");
        Matcher matcher = pattern.matcher(docName);
        if (matcher.matches()) {
            String[] segments = new String[]{matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)};
            return Joiner.on("/").join(segments) + "/";
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        System.out.println(getGigawordDirSegments("APW_ENG_19941127.0103.xmi.gz"));
    }

}
