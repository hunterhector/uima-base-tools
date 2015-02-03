
/* First created by JCasGen Tue Feb 03 15:33:56 EST 2015 */
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
 * Updated by JCasGen Tue Feb 03 16:25:10 EST 2015
 * @generated */
public class MCQuestion_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (MCQuestion_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = MCQuestion_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new MCQuestion(addr, MCQuestion_Type.this);
  			   MCQuestion_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new MCQuestion(addr, MCQuestion_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = MCQuestion.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.MCQuestion");
 
  /** @generated */
  final Feature casFeat_mcAnswerChoices;
  /** @generated */
  final int     casFeatCode_mcAnswerChoices;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMcAnswerChoices(int addr) {
        if (featOkTst && casFeat_mcAnswerChoices == null)
      jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    return ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMcAnswerChoices(int addr, int v) {
        if (featOkTst && casFeat_mcAnswerChoices == null)
      jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    ll_cas.ll_setRefValue(addr, casFeatCode_mcAnswerChoices, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getMcAnswerChoices(int addr, int i) {
        if (featOkTst && casFeat_mcAnswerChoices == null)
      jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMcAnswerChoices(int addr, int i, int v) {
        if (featOkTst && casFeat_mcAnswerChoices == null)
      jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcAnswerChoices), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_isMultipleChoice;
  /** @generated */
  final int     casFeatCode_isMultipleChoice;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsMultipleChoice(int addr) {
        if (featOkTst && casFeat_isMultipleChoice == null)
      jcas.throwFeatMissing("isMultipleChoice", "edu.cmu.cs.lti.script.type.MCQuestion");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isMultipleChoice);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsMultipleChoice(int addr, boolean v) {
        if (featOkTst && casFeat_isMultipleChoice == null)
      jcas.throwFeatMissing("isMultipleChoice", "edu.cmu.cs.lti.script.type.MCQuestion");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isMultipleChoice, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public MCQuestion_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_mcAnswerChoices = jcas.getRequiredFeatureDE(casType, "mcAnswerChoices", "uima.cas.FSArray", featOkTst);
    casFeatCode_mcAnswerChoices  = (null == casFeat_mcAnswerChoices) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_mcAnswerChoices).getCode();

 
    casFeat_isMultipleChoice = jcas.getRequiredFeatureDE(casType, "isMultipleChoice", "uima.cas.Boolean", featOkTst);
    casFeatCode_isMultipleChoice  = (null == casFeat_isMultipleChoice) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isMultipleChoice).getCode();

  }
}



    