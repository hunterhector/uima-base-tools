
/* First created by JCasGen Sat Jan 31 03:33:55 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Tue Feb 03 14:44:05 EST 2015
 * @generated */
public class CandidateEventMention_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (CandidateEventMention_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = CandidateEventMention_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new CandidateEventMention(addr, CandidateEventMention_Type.this);
  			   CandidateEventMention_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new CandidateEventMention(addr, CandidateEventMention_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = CandidateEventMention.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.CandidateEventMention");
 
  /** @generated */
  final Feature casFeat_potentialFrames;
  /** @generated */
  final int     casFeatCode_potentialFrames;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPotentialFrames(int addr) {
        if (featOkTst && casFeat_potentialFrames == null)
      jcas.throwFeatMissing("potentialFrames", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_potentialFrames);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPotentialFrames(int addr, int v) {
        if (featOkTst && casFeat_potentialFrames == null)
      jcas.throwFeatMissing("potentialFrames", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_potentialFrames, v);}
    
  
 
  /** @generated */
  final Feature casFeat_arguments;
  /** @generated */
  final int     casFeatCode_arguments;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArguments(int addr) {
        if (featOkTst && casFeat_arguments == null)
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arguments);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArguments(int addr, int v) {
        if (featOkTst && casFeat_arguments == null)
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_arguments, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headWord;
  /** @generated */
  final int     casFeatCode_headWord;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadWord(int addr) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headWord);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadWord(int addr, int v) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_headWord, v);}
    
  
 
  /** @generated */
  final Feature casFeat_goldStandardMentionType;
  /** @generated */
  final int     casFeatCode_goldStandardMentionType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getGoldStandardMentionType(int addr) {
        if (featOkTst && casFeat_goldStandardMentionType == null)
      jcas.throwFeatMissing("goldStandardMentionType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_goldStandardMentionType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoldStandardMentionType(int addr, String v) {
        if (featOkTst && casFeat_goldStandardMentionType == null)
      jcas.throwFeatMissing("goldStandardMentionType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_goldStandardMentionType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_predictedType;
  /** @generated */
  final int     casFeatCode_predictedType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPredictedType(int addr) {
        if (featOkTst && casFeat_predictedType == null)
      jcas.throwFeatMissing("predictedType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_predictedType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPredictedType(int addr, String v) {
        if (featOkTst && casFeat_predictedType == null)
      jcas.throwFeatMissing("predictedType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_predictedType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_goldRealis;
  /** @generated */
  final int     casFeatCode_goldRealis;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getGoldRealis(int addr) {
        if (featOkTst && casFeat_goldRealis == null)
      jcas.throwFeatMissing("goldRealis", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_goldRealis);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoldRealis(int addr, String v) {
        if (featOkTst && casFeat_goldRealis == null)
      jcas.throwFeatMissing("goldRealis", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_goldRealis, v);}
    
  
 
  /** @generated */
  final Feature casFeat_predictedRealis;
  /** @generated */
  final int     casFeatCode_predictedRealis;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPredictedRealis(int addr) {
        if (featOkTst && casFeat_predictedRealis == null)
      jcas.throwFeatMissing("predictedRealis", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_predictedRealis);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPredictedRealis(int addr, String v) {
        if (featOkTst && casFeat_predictedRealis == null)
      jcas.throwFeatMissing("predictedRealis", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_predictedRealis, v);}
    
  
 
  /** @generated */
  final Feature casFeat_typePredictionConfidence;
  /** @generated */
  final int     casFeatCode_typePredictionConfidence;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getTypePredictionConfidence(int addr) {
        if (featOkTst && casFeat_typePredictionConfidence == null)
      jcas.throwFeatMissing("typePredictionConfidence", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_typePredictionConfidence);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTypePredictionConfidence(int addr, double v) {
        if (featOkTst && casFeat_typePredictionConfidence == null)
      jcas.throwFeatMissing("typePredictionConfidence", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_typePredictionConfidence, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public CandidateEventMention_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_potentialFrames = jcas.getRequiredFeatureDE(casType, "potentialFrames", "uima.cas.StringList", featOkTst);
    casFeatCode_potentialFrames  = (null == casFeat_potentialFrames) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_potentialFrames).getCode();

 
    casFeat_arguments = jcas.getRequiredFeatureDE(casType, "arguments", "uima.cas.FSList", featOkTst);
    casFeatCode_arguments  = (null == casFeat_arguments) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arguments).getCode();

 
    casFeat_headWord = jcas.getRequiredFeatureDE(casType, "headWord", "edu.cmu.cs.lti.script.type.StanfordCorenlpToken", featOkTst);
    casFeatCode_headWord  = (null == casFeat_headWord) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headWord).getCode();

 
    casFeat_goldStandardMentionType = jcas.getRequiredFeatureDE(casType, "goldStandardMentionType", "uima.cas.String", featOkTst);
    casFeatCode_goldStandardMentionType  = (null == casFeat_goldStandardMentionType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goldStandardMentionType).getCode();

 
    casFeat_predictedType = jcas.getRequiredFeatureDE(casType, "predictedType", "uima.cas.String", featOkTst);
    casFeatCode_predictedType  = (null == casFeat_predictedType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_predictedType).getCode();

 
    casFeat_goldRealis = jcas.getRequiredFeatureDE(casType, "goldRealis", "uima.cas.String", featOkTst);
    casFeatCode_goldRealis  = (null == casFeat_goldRealis) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goldRealis).getCode();

 
    casFeat_predictedRealis = jcas.getRequiredFeatureDE(casType, "predictedRealis", "uima.cas.String", featOkTst);
    casFeatCode_predictedRealis  = (null == casFeat_predictedRealis) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_predictedRealis).getCode();

 
    casFeat_typePredictionConfidence = jcas.getRequiredFeatureDE(casType, "typePredictionConfidence", "uima.cas.Double", featOkTst);
    casFeatCode_typePredictionConfidence  = (null == casFeat_typePredictionConfidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_typePredictionConfidence).getCode();

  }
}



    