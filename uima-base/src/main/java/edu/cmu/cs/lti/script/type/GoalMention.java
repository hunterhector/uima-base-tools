

/* First created by JCasGen Sat Jan 31 03:33:55 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** annotate a goal mention in text,  of a certain event mention. For example, "He work hard to get higher grade.", the mention "get higher grade" is the goal.
 * Updated by JCasGen Tue Feb 03 16:25:10 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TaskMCTestTypeSystem.xml
 * @generated */
public class GoalMention extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(GoalMention.class);
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
  protected GoalMention() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public GoalMention(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public GoalMention(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public GoalMention(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
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
  //* Feature: eventMentionLinks

  /** getter for eventMentionLinks - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getEventMentionLinks() {
    if (GoalMention_Type.featOkTst && ((GoalMention_Type)jcasType).casFeat_eventMentionLinks == null)
      jcasType.jcas.throwFeatMissing("eventMentionLinks", "edu.cmu.cs.lti.script.type.GoalMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((GoalMention_Type)jcasType).casFeatCode_eventMentionLinks)));}
    
  /** setter for eventMentionLinks - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionLinks(FSList v) {
    if (GoalMention_Type.featOkTst && ((GoalMention_Type)jcasType).casFeat_eventMentionLinks == null)
      jcasType.jcas.throwFeatMissing("eventMentionLinks", "edu.cmu.cs.lti.script.type.GoalMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((GoalMention_Type)jcasType).casFeatCode_eventMentionLinks, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: head

  /** getter for head - gets Head word of this goal
   * @generated
   * @return value of the feature 
   */
  public Word getHead() {
    if (GoalMention_Type.featOkTst && ((GoalMention_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.GoalMention");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((GoalMention_Type)jcasType).casFeatCode_head)));}
    
  /** setter for head - sets Head word of this goal 
   * @generated
   * @param v value to set into the feature 
   */
  public void setHead(Word v) {
    if (GoalMention_Type.featOkTst && ((GoalMention_Type)jcasType).casFeat_head == null)
      jcasType.jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.GoalMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((GoalMention_Type)jcasType).casFeatCode_head, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    