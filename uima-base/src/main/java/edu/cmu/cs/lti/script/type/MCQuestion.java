

/* First created by JCasGen Tue Feb 03 15:33:56 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** 
 * Updated by JCasGen Tue Feb 03 16:25:10 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TaskMCTestTypeSystem.xml
 * @generated */
public class MCQuestion extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(MCQuestion.class);
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
  protected MCQuestion() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public MCQuestion(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public MCQuestion(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public MCQuestion(JCas jcas, int begin, int end) {
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
  //* Feature: mcAnswerChoices

  /** getter for mcAnswerChoices - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getMcAnswerChoices() {
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_mcAnswerChoices == null)
      jcasType.jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices)));}
    
  /** setter for mcAnswerChoices - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMcAnswerChoices(FSArray v) {
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_mcAnswerChoices == null)
      jcasType.jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    jcasType.ll_cas.ll_setRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for mcAnswerChoices - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public MCAnswerChoice getMcAnswerChoices(int i) {
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_mcAnswerChoices == null)
      jcasType.jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices), i);
    return (MCAnswerChoice)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices), i)));}

  /** indexed setter for mcAnswerChoices - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setMcAnswerChoices(int i, MCAnswerChoice v) { 
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_mcAnswerChoices == null)
      jcasType.jcas.throwFeatMissing("mcAnswerChoices", "edu.cmu.cs.lti.script.type.MCQuestion");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_mcAnswerChoices), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: isMultipleChoice

  /** getter for isMultipleChoice - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsMultipleChoice() {
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_isMultipleChoice == null)
      jcasType.jcas.throwFeatMissing("isMultipleChoice", "edu.cmu.cs.lti.script.type.MCQuestion");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_isMultipleChoice);}
    
  /** setter for isMultipleChoice - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsMultipleChoice(boolean v) {
    if (MCQuestion_Type.featOkTst && ((MCQuestion_Type)jcasType).casFeat_isMultipleChoice == null)
      jcasType.jcas.throwFeatMissing("isMultipleChoice", "edu.cmu.cs.lti.script.type.MCQuestion");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((MCQuestion_Type)jcasType).casFeatCode_isMultipleChoice, v);}    
  }

    