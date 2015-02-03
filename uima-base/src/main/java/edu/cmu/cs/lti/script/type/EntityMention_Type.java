
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
 * Updated by JCasGen Tue Feb 03 16:25:09 EST 2015
 * @generated */
public class EntityMention_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityMention_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityMention_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityMention(addr, EntityMention_Type.this);
  			   EntityMention_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityMention(addr, EntityMention_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityMention.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityMention");
 
  /** @generated */
  final Feature casFeat_entityType;
  /** @generated */
  final int     casFeatCode_entityType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEntityType(int addr) {
        if (featOkTst && casFeat_entityType == null)
      jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.EntityMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_entityType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityType(int addr, String v) {
        if (featOkTst && casFeat_entityType == null)
      jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.EntityMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_entityType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_head;
  /** @generated */
  final int     casFeatCode_head;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHead(int addr) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EntityMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_head);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHead(int addr, int v) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.EntityMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_head, v);}
    
  
 
  /** @generated */
  final Feature casFeat_referingEntity;
  /** @generated */
  final int     casFeatCode_referingEntity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getReferingEntity(int addr) {
        if (featOkTst && casFeat_referingEntity == null)
      jcas.throwFeatMissing("referingEntity", "edu.cmu.cs.lti.script.type.EntityMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_referingEntity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setReferingEntity(int addr, int v) {
        if (featOkTst && casFeat_referingEntity == null)
      jcas.throwFeatMissing("referingEntity", "edu.cmu.cs.lti.script.type.EntityMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_referingEntity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_argumentLinks;
  /** @generated */
  final int     casFeatCode_argumentLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArgumentLinks(int addr) {
        if (featOkTst && casFeat_argumentLinks == null)
      jcas.throwFeatMissing("argumentLinks", "edu.cmu.cs.lti.script.type.EntityMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_argumentLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgumentLinks(int addr, int v) {
        if (featOkTst && casFeat_argumentLinks == null)
      jcas.throwFeatMissing("argumentLinks", "edu.cmu.cs.lti.script.type.EntityMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_argumentLinks, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headAnnotation;
  /** @generated */
  final int     casFeatCode_headAnnotation;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadAnnotation(int addr) {
        if (featOkTst && casFeat_headAnnotation == null)
      jcas.throwFeatMissing("headAnnotation", "edu.cmu.cs.lti.script.type.EntityMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headAnnotation);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadAnnotation(int addr, int v) {
        if (featOkTst && casFeat_headAnnotation == null)
      jcas.throwFeatMissing("headAnnotation", "edu.cmu.cs.lti.script.type.EntityMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_headAnnotation, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityMention_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_entityType = jcas.getRequiredFeatureDE(casType, "entityType", "uima.cas.String", featOkTst);
    casFeatCode_entityType  = (null == casFeat_entityType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityType).getCode();

 
    casFeat_head = jcas.getRequiredFeatureDE(casType, "head", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_head  = (null == casFeat_head) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_head).getCode();

 
    casFeat_referingEntity = jcas.getRequiredFeatureDE(casType, "referingEntity", "edu.cmu.cs.lti.script.type.Entity", featOkTst);
    casFeatCode_referingEntity  = (null == casFeat_referingEntity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_referingEntity).getCode();

 
    casFeat_argumentLinks = jcas.getRequiredFeatureDE(casType, "argumentLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_argumentLinks  = (null == casFeat_argumentLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argumentLinks).getCode();

 
    casFeat_headAnnotation = jcas.getRequiredFeatureDE(casType, "headAnnotation", "edu.cmu.cs.lti.script.type.ComponentAnnotation", featOkTst);
    casFeatCode_headAnnotation  = (null == casFeat_headAnnotation) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headAnnotation).getCode();

  }
}



    