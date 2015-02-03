

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
public class MCStory extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(MCStory.class);
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
  protected MCStory() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public MCStory(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public MCStory(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public MCStory(JCas jcas, int begin, int end) {
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
  //* Feature: mcQuestions

  /** getter for mcQuestions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getMcQuestions() {
    if (MCStory_Type.featOkTst && ((MCStory_Type)jcasType).casFeat_mcQuestions == null)
      jcasType.jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions)));}
    
  /** setter for mcQuestions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMcQuestions(FSArray v) {
    if (MCStory_Type.featOkTst && ((MCStory_Type)jcasType).casFeat_mcQuestions == null)
      jcasType.jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    jcasType.ll_cas.ll_setRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for mcQuestions - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public MCQuestion getMcQuestions(int i) {
    if (MCStory_Type.featOkTst && ((MCStory_Type)jcasType).casFeat_mcQuestions == null)
      jcasType.jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions), i);
    return (MCQuestion)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions), i)));}

  /** indexed setter for mcQuestions - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setMcQuestions(int i, MCQuestion v) { 
    if (MCStory_Type.featOkTst && ((MCStory_Type)jcasType).casFeat_mcQuestions == null)
      jcasType.jcas.throwFeatMissing("mcQuestions", "edu.cmu.cs.lti.script.type.MCStory");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((MCStory_Type)jcasType).casFeatCode_mcQuestions), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    