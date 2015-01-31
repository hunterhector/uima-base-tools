

/* First created by JCasGen Fri Jan 30 12:07:08 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Sat Jan 31 01:41:24 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/EventMentionDetectionTypeSystem.xml
 * @generated */
public class CandidateEventMentionArgument extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(CandidateEventMentionArgument.class);
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
  protected CandidateEventMentionArgument() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public CandidateEventMentionArgument(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public CandidateEventMentionArgument(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public CandidateEventMentionArgument(JCas jcas, int begin, int end) {
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
  //* Feature: roleName

  /** getter for roleName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getRoleName() {
    if (CandidateEventMentionArgument_Type.featOkTst && ((CandidateEventMentionArgument_Type)jcasType).casFeat_roleName == null)
      jcasType.jcas.throwFeatMissing("roleName", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    return jcasType.ll_cas.ll_getStringValue(addr, ((CandidateEventMentionArgument_Type)jcasType).casFeatCode_roleName);}
    
  /** setter for roleName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRoleName(String v) {
    if (CandidateEventMentionArgument_Type.featOkTst && ((CandidateEventMentionArgument_Type)jcasType).casFeat_roleName == null)
      jcasType.jcas.throwFeatMissing("roleName", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    jcasType.ll_cas.ll_setStringValue(addr, ((CandidateEventMentionArgument_Type)jcasType).casFeatCode_roleName, v);}    
   
    
  //*--------------*
  //* Feature: headWord

  /** getter for headWord - gets 
   * @generated
   * @return value of the feature 
   */
  public StanfordCorenlpToken getHeadWord() {
    if (CandidateEventMentionArgument_Type.featOkTst && ((CandidateEventMentionArgument_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    return (StanfordCorenlpToken)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((CandidateEventMentionArgument_Type)jcasType).casFeatCode_headWord)));}
    
  /** setter for headWord - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHeadWord(StanfordCorenlpToken v) {
    if (CandidateEventMentionArgument_Type.featOkTst && ((CandidateEventMentionArgument_Type)jcasType).casFeat_headWord == null)
      jcasType.jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.CandidateEventMentionArgument");
    jcasType.ll_cas.ll_setRefValue(addr, ((CandidateEventMentionArgument_Type)jcasType).casFeatCode_headWord, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    