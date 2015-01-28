
/* First created by JCasGen Fri Oct 17 16:20:36 EDT 2014 */
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
 * Updated by JCasGen Wed Jan 28 12:54:28 EST 2015
 * @generated */
public class EventMentionGoalLink_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventMentionGoalLink_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventMentionGoalLink_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventMentionGoalLink(addr, EventMentionGoalLink_Type.this);
  			   EventMentionGoalLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventMentionGoalLink(addr, EventMentionGoalLink_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventMentionGoalLink.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventMentionGoalLink");
 
  /** @generated */
  final Feature casFeat_eventMention;
  /** @generated */
  final int     casFeatCode_eventMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMention(int addr) {
        if (featOkTst && casFeat_eventMention == null)
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMention(int addr, int v) {
        if (featOkTst && casFeat_eventMention == null)
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMention, v);}
    
  
 
  /** @generated */
  final Feature casFeat_goalMention;
  /** @generated */
  final int     casFeatCode_goalMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getGoalMention(int addr) {
        if (featOkTst && casFeat_goalMention == null)
      jcas.throwFeatMissing("goalMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_goalMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoalMention(int addr, int v) {
        if (featOkTst && casFeat_goalMention == null)
      jcas.throwFeatMissing("goalMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_goalMention, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventMentionGoalLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventMention = jcas.getRequiredFeatureDE(casType, "eventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMention  = (null == casFeat_eventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMention).getCode();

 
    casFeat_goalMention = jcas.getRequiredFeatureDE(casType, "goalMention", "edu.cmu.cs.lti.script.type.GoalMention", featOkTst);
    casFeatCode_goalMention  = (null == casFeat_goalMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goalMention).getCode();

  }
}



    