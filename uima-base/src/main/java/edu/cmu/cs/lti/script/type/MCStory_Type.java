
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
public class MCStory_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (MCStory_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = MCStory_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new MCStory(addr, MCStory_Type.this);
  			   MCStory_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new MCStory(addr, MCStory_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = MCStory.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.MCStory");
 
  /** @generated */
  final Feature casFeat_mcQuestions;
  /** @generated */
  final int     casFeatCode_mcQuestions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMcQuestions(int addr) {
        if (featOkTst && casFeat_mcQuestions == null)
      jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    return ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMcQuestions(int addr, int v) {
        if (featOkTst && casFeat_mcQuestions == null)
      jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    ll_cas.ll_setRefValue(addr, casFeatCode_mcQuestions, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getMcQuestions(int addr, int i) {
        if (featOkTst && casFeat_mcQuestions == null)
      jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMcQuestions(int addr, int i, int v) {
        if (featOkTst && casFeat_mcQuestions == null)
      jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_mcQuestions), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public MCStory_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_mcQuestions = jcas.getRequiredFeatureDE(casType, "mcQuestions", "uima.cas.FSArray", featOkTst);
    casFeatCode_mcQuestions  = (null == casFeat_mcQuestions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_mcQuestions).getCode();

  }
}



    