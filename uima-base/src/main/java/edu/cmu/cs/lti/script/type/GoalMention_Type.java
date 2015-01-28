
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

/** annotate a goal mention in text,  of a certain event mention. For example, "He work hard to get higher grade.", the mention "get higher grade" is the goal.
 * Updated by JCasGen Wed Jan 28 12:54:28 EST 2015
 * @generated */
public class GoalMention_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (GoalMention_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = GoalMention_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new GoalMention(addr, GoalMention_Type.this);
  			   GoalMention_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new GoalMention(addr, GoalMention_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = GoalMention.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.GoalMention");
 
  /** @generated */
  final Feature casFeat_eventMentionLinks;
  /** @generated */
  final int     casFeatCode_eventMentionLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMentionLinks(int addr) {
        if (featOkTst && casFeat_eventMentionLinks == null)
      jcas.throwFeatMissing("eventMentionLinks", "edu.cmu.cs.lti.script.type.GoalMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentionLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionLinks(int addr, int v) {
        if (featOkTst && casFeat_eventMentionLinks == null)
      jcas.throwFeatMissing("eventMentionLinks", "edu.cmu.cs.lti.script.type.GoalMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMentionLinks, v);}
    
  
 
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
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.GoalMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_head);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHead(int addr, int v) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.GoalMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_head, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public GoalMention_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventMentionLinks = jcas.getRequiredFeatureDE(casType, "eventMentionLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_eventMentionLinks  = (null == casFeat_eventMentionLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionLinks).getCode();

 
    casFeat_head = jcas.getRequiredFeatureDE(casType, "head", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_head  = (null == casFeat_head) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_head).getCode();

  }
}



    