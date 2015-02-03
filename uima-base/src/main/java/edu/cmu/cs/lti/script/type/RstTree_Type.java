
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
public class RstTree_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (RstTree_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = RstTree_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new RstTree(addr, RstTree_Type.this);
  			   RstTree_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new RstTree(addr, RstTree_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = RstTree.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.RstTree");
 
  /** @generated */
  final Feature casFeat_relationLabel;
  /** @generated */
  final int     casFeatCode_relationLabel;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRelationLabel(int addr) {
        if (featOkTst && casFeat_relationLabel == null)
      jcas.throwFeatMissing("relationLabel", "edu.cmu.cs.lti.script.type.RstTree");
    return ll_cas.ll_getStringValue(addr, casFeatCode_relationLabel);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRelationLabel(int addr, String v) {
        if (featOkTst && casFeat_relationLabel == null)
      jcas.throwFeatMissing("relationLabel", "edu.cmu.cs.lti.script.type.RstTree");
    ll_cas.ll_setStringValue(addr, casFeatCode_relationLabel, v);}
    
  
 
  /** @generated */
  final Feature casFeat_relationDirection;
  /** @generated */
  final int     casFeatCode_relationDirection;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRelationDirection(int addr) {
        if (featOkTst && casFeat_relationDirection == null)
      jcas.throwFeatMissing("relationDirection", "edu.cmu.cs.lti.script.type.RstTree");
    return ll_cas.ll_getStringValue(addr, casFeatCode_relationDirection);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRelationDirection(int addr, String v) {
        if (featOkTst && casFeat_relationDirection == null)
      jcas.throwFeatMissing("relationDirection", "edu.cmu.cs.lti.script.type.RstTree");
    ll_cas.ll_setStringValue(addr, casFeatCode_relationDirection, v);}
    
  
 
  /** @generated */
  final Feature casFeat_children;
  /** @generated */
  final int     casFeatCode_children;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildren(int addr) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    return ll_cas.ll_getRefValue(addr, casFeatCode_children);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildren(int addr, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    ll_cas.ll_setRefValue(addr, casFeatCode_children, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getChildren(int addr, int i) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setChildren(int addr, int i, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_isTerminal;
  /** @generated */
  final int     casFeatCode_isTerminal;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsTerminal(int addr) {
        if (featOkTst && casFeat_isTerminal == null)
      jcas.throwFeatMissing("isTerminal", "edu.cmu.cs.lti.script.type.RstTree");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isTerminal);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsTerminal(int addr, boolean v) {
        if (featOkTst && casFeat_isTerminal == null)
      jcas.throwFeatMissing("isTerminal", "edu.cmu.cs.lti.script.type.RstTree");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isTerminal, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public RstTree_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_relationLabel = jcas.getRequiredFeatureDE(casType, "relationLabel", "uima.cas.String", featOkTst);
    casFeatCode_relationLabel  = (null == casFeat_relationLabel) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relationLabel).getCode();

 
    casFeat_relationDirection = jcas.getRequiredFeatureDE(casType, "relationDirection", "uima.cas.String", featOkTst);
    casFeatCode_relationDirection  = (null == casFeat_relationDirection) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relationDirection).getCode();

 
    casFeat_children = jcas.getRequiredFeatureDE(casType, "children", "uima.cas.FSArray", featOkTst);
    casFeatCode_children  = (null == casFeat_children) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_children).getCode();

 
    casFeat_isTerminal = jcas.getRequiredFeatureDE(casType, "isTerminal", "uima.cas.Boolean", featOkTst);
    casFeatCode_isTerminal  = (null == casFeat_isTerminal) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isTerminal).getCode();

  }
}



    