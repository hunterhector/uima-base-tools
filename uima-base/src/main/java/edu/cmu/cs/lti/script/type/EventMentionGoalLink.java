

/* First created by JCasGen Sat Jan 31 03:33:55 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Sun Feb 01 19:19:40 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventMentionGoalLink extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventMentionGoalLink.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected EventMentionGoalLink() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventMentionGoalLink(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventMentionGoalLink(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: eventMention

  /** getter for eventMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMention() {
    if (EventMentionGoalLink_Type.featOkTst && ((EventMentionGoalLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMentionGoalLink_Type)jcasType).casFeatCode_eventMention)));}
    
  /** setter for eventMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMention(EventMention v) {
    if (EventMentionGoalLink_Type.featOkTst && ((EventMentionGoalLink_Type)jcasType).casFeat_eventMention == null)
      jcasType.jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMentionGoalLink_Type)jcasType).casFeatCode_eventMention, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: goalMention

  /** getter for goalMention - gets 
   * @generated
   * @return value of the feature 
   */
  public GoalMention getGoalMention() {
    if (EventMentionGoalLink_Type.featOkTst && ((EventMentionGoalLink_Type)jcasType).casFeat_goalMention == null)
      jcasType.jcas.throwFeatMissing("goalMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    return (GoalMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventMentionGoalLink_Type)jcasType).casFeatCode_goalMention)));}
    
  /** setter for goalMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGoalMention(GoalMention v) {
    if (EventMentionGoalLink_Type.featOkTst && ((EventMentionGoalLink_Type)jcasType).casFeat_goalMention == null)
      jcasType.jcas.throwFeatMissing("goalMention", "edu.cmu.cs.lti.script.type.EventMentionGoalLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventMentionGoalLink_Type)jcasType).casFeatCode_goalMention, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    