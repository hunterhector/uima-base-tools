
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
 * Updated by JCasGen Sat Jan 31 13:44:09 EST 2015
 * @generated */
public class EventArgumentLink_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventArgumentLink_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventArgumentLink_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventArgumentLink(addr, EventArgumentLink_Type.this);
  			   EventArgumentLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventArgumentLink(addr, EventArgumentLink_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventArgumentLink.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventArgumentLink");
 
  /** @generated */
  final Feature casFeat_event;
  /** @generated */
  final int     casFeatCode_event;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEvent(int addr) {
        if (featOkTst && casFeat_event == null)
      jcas.throwFeatMissing("event", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_event);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEvent(int addr, int v) {
        if (featOkTst && casFeat_event == null)
      jcas.throwFeatMissing("event", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_event, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entity;
  /** @generated */
  final int     casFeatCode_entity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEntity(int addr) {
        if (featOkTst && casFeat_entity == null)
      jcas.throwFeatMissing("entity", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_entity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntity(int addr, int v) {
        if (featOkTst && casFeat_entity == null)
      jcas.throwFeatMissing("entity", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_entity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_argumentRole;
  /** @generated */
  final int     casFeatCode_argumentRole;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getArgumentRole(int addr) {
        if (featOkTst && casFeat_argumentRole == null)
      jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_argumentRole);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgumentRole(int addr, String v) {
        if (featOkTst && casFeat_argumentRole == null)
      jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_argumentRole, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventArgumentLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_event = jcas.getRequiredFeatureDE(casType, "event", "edu.cmu.cs.lti.script.type.Event", featOkTst);
    casFeatCode_event  = (null == casFeat_event) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_event).getCode();

 
    casFeat_entity = jcas.getRequiredFeatureDE(casType, "entity", "edu.cmu.cs.lti.script.type.Entity", featOkTst);
    casFeatCode_entity  = (null == casFeat_entity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entity).getCode();

 
    casFeat_argumentRole = jcas.getRequiredFeatureDE(casType, "argumentRole", "uima.cas.String", featOkTst);
    casFeatCode_argumentRole  = (null == casFeat_argumentRole) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argumentRole).getCode();

  }
}



    