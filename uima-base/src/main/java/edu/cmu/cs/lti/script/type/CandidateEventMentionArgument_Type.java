
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
 * Updated by JCasGen Sat Jan 31 01:41:24 EST 2015
 * @generated */
public class CandidateEventMentionArgument_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (CandidateEventMentionArgument_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = CandidateEventMentionArgument_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new CandidateEventMentionArgument(addr, CandidateEventMentionArgument_Type.this);
  			   CandidateEventMentionArgument_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new CandidateEventMentionArgument(addr, CandidateEventMentionArgument_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = CandidateEventMentionArgument.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");



  /** @generated */
  final Feature casFeat_roleName;
  /** @generated */
  final int     casFeatCode_roleName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRoleName(int addr) {
        if (featOkTst && casFeat_roleName == null)
      jcas.throwFeatMissing("roleName", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    return ll_cas.ll_getStringValue(addr, casFeatCode_roleName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRoleName(int addr, String v) {
        if (featOkTst && casFeat_roleName == null)
      jcas.throwFeatMissing("roleName", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    ll_cas.ll_setStringValue(addr, casFeatCode_roleName, v);}
    
  
 
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
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headWord);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadWord(int addr, int v) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    ll_cas.ll_setRefValue(addr, casFeatCode_headWord, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public CandidateEventMentionArgument_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_roleName = jcas.getRequiredFeatureDE(casType, "roleName", "uima.cas.String", featOkTst);
    casFeatCode_roleName  = (null == casFeat_roleName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_roleName).getCode();

 
    casFeat_headWord = jcas.getRequiredFeatureDE(casType, "headWord", "edu.cmu.cs.lti.script.type.StanfordCorenlpToken", featOkTst);
    casFeatCode_headWord  = (null == casFeat_headWord) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headWord).getCode();

  }
}



    