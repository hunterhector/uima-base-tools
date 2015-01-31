

/* First created by JCasGen Fri Jan 30 12:07:08 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.StringList;


/** 
 * Updated by JCasGen Fri Jan 30 12:07:08 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/EventMentionDetectionTypeSystem.xml
 * @generated */
public class CandidateEventMention extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(CandidateEventMention.class);
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
  protected CandidateEventMention() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public CandidateEventMention(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public CandidateEventMention(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public CandidateEventMention(JCas jcas, int begin, int end) {
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
  //* Feature: relatedFrame

  /** getter for relatedFrame - gets possible frame that this event mention could invoke
   * @generated
   * @return value of the feature 
   */
  public StringList getRelatedFrame() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_relatedFrame == null)
      jcasType.jcas.throwFeatMissing("relatedFrame", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_relatedFrame)));}
    
  /** setter for relatedFrame - sets possible frame that this event mention could invoke 
   * @generated
   * @param v value to set into the feature 
   */
  public void setRelatedFrame(StringList v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_relatedFrame == null)
      jcasType.jcas.throwFeatMissing("relatedFrame", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_relatedFrame, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: argument

  /** getter for argument - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getArgument() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_argument)));}
    
  /** setter for argument - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArgument(FSList v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_argument == null)
      jcasType.jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_argument, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: headWord

  /** getter for headWord - gets 
   * @generated
   * @return value of the feature 
   */
  public Word getHeadWord() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (Word)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_headWord)));}
    
  /** setter for headWord - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadWord(Word v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_headWord, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    