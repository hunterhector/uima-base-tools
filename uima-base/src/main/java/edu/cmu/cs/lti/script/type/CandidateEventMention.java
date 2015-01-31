

/* First created by JCasGen Sat Jan 31 03:33:55 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.StringArray;


import org.apache.uima.jcas.cas.StringList;


/** 
 * Updated by JCasGen Sat Jan 31 13:44:09 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
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
  //* Feature: potentialFrames

  /** getter for potentialFrames - gets possible frame that this event mention could invoke
   * @generated
   * @return value of the feature 
   */
  public StringList getPotentialFrames() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_potentialFrames == null)
      jcasType.jcas.throwFeatMissing("potentialFrames", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_potentialFrames)));}
    
  /** setter for potentialFrames - sets possible frame that this event mention could invoke 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPotentialFrames(StringList v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_potentialFrames == null)
      jcasType.jcas.throwFeatMissing("potentialFrames", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_potentialFrames, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: arguments

  /** getter for arguments - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getArguments() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_arguments)));}
    
  /** setter for arguments - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArguments(FSList v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_arguments == null)
      jcasType.jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_arguments, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: headWord

  /** getter for headWord - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordCorenlpToken getHeadWord() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return (StanfordCorenlpToken)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_headWord)));}
    
  /** setter for headWord - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadWord(StanfordCorenlpToken v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_headWord, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: goldStandardMentionType

  /** getter for goldStandardMentionType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getGoldStandardMentionType() {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_goldStandardMentionType == null)
      jcasType.jcas.throwFeatMissing("goldStandardMentionType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    return jcasType.ll_cas.ll_getStringValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_goldStandardMentionType);}
    
  /** setter for goldStandardMentionType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setGoldStandardMentionType(String v) {
    if (CandidateEventMention_Type.featOkTst && ((CandidateEventMention_Type)jcasType).casFeat_goldStandardMentionType == null)
      jcasType.jcas.throwFeatMissing("goldStandardMentionType", "edu.cmu.cs.lti.script.type.CandidateEventMention");
    jcasType.ll_cas.ll_setStringValue(addr, ((CandidateEventMention_Type)jcasType).casFeatCode_goldStandardMentionType, v);}    
  }

    