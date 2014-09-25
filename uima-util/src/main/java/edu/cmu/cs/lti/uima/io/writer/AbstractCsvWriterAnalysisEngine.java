/**
 * 
 */
package edu.cmu.cs.lti.uima.io.writer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.cs.lti.uima.util.CsvFactory;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * @author Zhengzhong Liu, Hector
 * 
 */
public abstract class AbstractCsvWriterAnalysisEngine extends JCasAnnotator_ImplBase {

  public static final String PARAM_PARENT_OUTPUT_DIR = "ParentOutputDirectory";

  public static final String PARAM_BASE_OUTPUT_DIR_NAME = "BaseOutputDirectoryName";

  public static final String PARAM_STEP_NUMBER = "StepNumber";

  public static final String PARAM_OUTPUT_FILE_SUFFIX = "OutputFileSuffix";

  public static final String PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME = "sourceDocumentViewName";

  @ConfigurationParameter(name = PARAM_PARENT_OUTPUT_DIR, mandatory = true)
  private String parentOutputDir;

  @ConfigurationParameter(name = PARAM_BASE_OUTPUT_DIR_NAME, mandatory = true)
  private String baseOutputDirName;

  @ConfigurationParameter(name = PARAM_STEP_NUMBER, mandatory = true)
  private Integer stepNumber;

  @ConfigurationParameter(name = PARAM_OUTPUT_FILE_SUFFIX, mandatory = false)
  private String outputFileSuffix;

  @ConfigurationParameter(name = PARAM_SOURCE_DOCUMENT_INFO_VIEW_NAME, mandatory = false)
  private String sourceDocumentViewName;

  private File outputDir;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  String defaultOutputFileSuffix = ".csv";

  private char seperator = ',';

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    try {
      super.initialize(context);
    } catch (ResourceInitializationException e) {
      throw new ResourceInitializationException(e);
    }

    parentOutputDir = (String) context.getConfigParameterValue(PARAM_PARENT_OUTPUT_DIR);

    baseOutputDirName = (String) context.getConfigParameterValue(PARAM_BASE_OUTPUT_DIR_NAME);

    stepNumber = (Integer) context.getConfigParameterValue(PARAM_STEP_NUMBER);

    outputFileSuffix = (String) context.getConfigParameterValue(PARAM_OUTPUT_FILE_SUFFIX);

    List<Object> partOfDirNames = new ArrayList<Object>();
    if (stepNumber != null) {
      String stepNumberStr = Integer.toString(stepNumber);
      partOfDirNames.add(StringUtils.leftPad(stepNumberStr, 2, '0'));
    }
    partOfDirNames.add(baseOutputDirName);

    outputDir = new File(parentOutputDir + File.separator + StringUtils.join(partOfDirNames, "_"));
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    subinitialize(context);
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    JCas sourceDocumentView = null;

    if (sourceDocumentViewName != null) {
      try {
        sourceDocumentView = aJCas.getView(sourceDocumentViewName);
      } catch (CASException e) {
        sourceDocumentView = aJCas;
      }
    } else {
      sourceDocumentView = aJCas;
    }

    prepare(aJCas);

    // Retrieve the filename of the input file from the CAS.
    File outFile = null;
    SourceDocumentInformation fileLoc = JCasUtil.selectSingle(sourceDocumentView,
            SourceDocumentInformation.class);

    File inFile;
    try {
      inFile = new File(new URL(fileLoc.getUri()).getPath());
      String outFileName = inFile.getName();
      if (fileLoc.getOffsetInSource() > 0) {
        outFileName += ("_" + fileLoc.getOffsetInSource());
      }
      if (outputFileSuffix != null && outputFileSuffix.length() > 0) {
        outFileName += "." + outputFileSuffix;
      } else if (!outFileName.endsWith(defaultOutputFileSuffix)) {
        outFileName += defaultOutputFileSuffix;
      }
      outFile = new File(outputDir, outFileName);

      CSVWriter writer = CsvFactory.getCSVWriter(outFile, seperator);

      String[] header = getHeader();

      if (header != null) {
        writer.writeNext(header);
      }

      while (hasNextRow()) {
        writer.writeNext(getNextCsvRow());
      }
      writer.close();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected abstract String[] getHeader();

  protected abstract void prepare(JCas aJCas);

  protected abstract boolean hasNextRow();

  protected abstract String[] getNextCsvRow();

  /**
   * Subclass can implement this to get more things to done
   * 
   * @param context
   */
  protected void subinitialize(UimaContext context) {

  }

  protected void setSeperator(char sep) {
    seperator = sep;
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {

  }
}
