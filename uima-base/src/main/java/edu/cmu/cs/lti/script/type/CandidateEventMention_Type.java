
/* First created by JCasGen Fri Jan 30 12:07:08 EST 2015 */
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
 * Updated by JCasGen Fri Jan 30 12:07:08 EST 2015
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
  final Feature casFeat_relatedFrame;
  /** @generated */
  final int     casFeatCode_relatedFrame;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRelatedFrame(int addr) {
        if (featOkTst && casFeat_relatedFrame == null)
      jcas.throwFeatMissing("relatedFrame", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_relatedFrame);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRelatedFrame(int addr, int v) {
        if (featOkTst && casFeat_relatedFrame == null)
      jcas.throwFeatMissing("relatedFrame", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_relatedFrame, v);}
    
  
 
  /** @generated */
  final Feature casFeat_argument;
  /** @generated */
  final int     casFeatCode_argument;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArgument(int addr) {
        if (featOkTst && casFeat_argument == null)
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_argument);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgument(int addr, int v) {
        if (featOkTst && casFeat_argument == null)
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_argument, v);}
    
  
 
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
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public CandidateEventMention_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_relatedFrame = jcas.getRequiredFeatureDE(casType, "relatedFrame", "uima.cas.StringList", featOkTst);
    casFeatCode_relatedFrame  = (null == casFeat_relatedFrame) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relatedFrame).getCode();

 
    casFeat_argument = jcas.getRequiredFeatureDE(casType, "argument", "uima.cas.FSList", featOkTst);
    casFeatCode_argument  = (null == casFeat_argument) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argument).getCode();

 
    casFeat_headWord = jcas.getRequiredFeatureDE(casType, "headWord", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_headWord  = (null == casFeat_headWord) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headWord).getCode();

  }
}



    