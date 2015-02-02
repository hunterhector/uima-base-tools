
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
 * Updated by JCasGen Sun Feb 01 19:19:41 EST 2015
 * @generated */
public class PairwiseEventFeature_Type extends AbstractFeature_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (PairwiseEventFeature_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = PairwiseEventFeature_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new PairwiseEventFeature(addr, PairwiseEventFeature_Type.this);
  			   PairwiseEventFeature_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new PairwiseEventFeature(addr, PairwiseEventFeature_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = PairwiseEventFeature.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.PairwiseEventFeature");
 
  /** @generated */
  final Feature casFeat_featureType;
  /** @generated */
  final int     casFeatCode_featureType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFeatureType(int addr) {
        if (featOkTst && casFeat_featureType == null)
      jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    return ll_cas.ll_getStringValue(addr, casFeatCode_featureType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeatureType(int addr, String v) {
        if (featOkTst && casFeat_featureType == null)
      jcas.throwFeatMissing("featureType", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    ll_cas.ll_setStringValue(addr, casFeatCode_featureType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_defaultZero;
  /** @generated */
  final int     casFeatCode_defaultZero;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getDefaultZero(int addr) {
        if (featOkTst && casFeat_defaultZero == null)
      jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_defaultZero);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDefaultZero(int addr, boolean v) {
        if (featOkTst && casFeat_defaultZero == null)
      jcas.throwFeatMissing("defaultZero", "edu.cmu.cs.lti.script.type.PairwiseEventFeature");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_defaultZero, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public PairwiseEventFeature_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_featureType = jcas.getRequiredFeatureDE(casType, "featureType", "uima.cas.String", featOkTst);
    casFeatCode_featureType  = (null == casFeat_featureType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_featureType).getCode();

 
    casFeat_defaultZero = jcas.getRequiredFeatureDE(casType, "defaultZero", "uima.cas.Boolean", featOkTst);
    casFeatCode_defaultZero  = (null == casFeat_defaultZero) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_defaultZero).getCode();

  }
}



    