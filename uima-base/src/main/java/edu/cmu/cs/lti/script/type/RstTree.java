

/* First created by JCasGen Thu Oct 02 09:11:20 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Oct 02 09:18:06 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class RstTree extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(RstTree.class);
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
  protected RstTree() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public RstTree(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public RstTree(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public RstTree(JCas jcas, int begin, int end) {
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
  //* Feature: relationLabel

  /** getter for relationLabel - gets 
   * @generated
   * @return value of the feature 
   */
  public String getRelationLabel() {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_relationLabel == null)
      jcasType.jcas.throwFeatMissing("relationLabel", "edu.cmu.cs.lti.script.type.RstTree");
    return jcasType.ll_cas.ll_getStringValue(addr, ((RstTree_Type)jcasType).casFeatCode_relationLabel);}
    
  /** setter for relationLabel - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRelationLabel(String v) {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_relationLabel == null)
      jcasType.jcas.throwFeatMissing("relationLabel", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.ll_cas.ll_setStringValue(addr, ((RstTree_Type)jcasType).casFeatCode_relationLabel, v);}    
   
    
  //*--------------*
  //* Feature: relationDirection

  /** getter for relationDirection - gets 
   * @generated
   * @return value of the feature 
   */
  public String getRelationDirection() {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_relationDirection == null)
      jcasType.jcas.throwFeatMissing("relationDirection", "edu.cmu.cs.lti.script.type.RstTree");
    return jcasType.ll_cas.ll_getStringValue(addr, ((RstTree_Type)jcasType).casFeatCode_relationDirection);}
    
  /** setter for relationDirection - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRelationDirection(String v) {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_relationDirection == null)
      jcasType.jcas.throwFeatMissing("relationDirection", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.ll_cas.ll_setStringValue(addr, ((RstTree_Type)jcasType).casFeatCode_relationDirection, v);}    
   
    
  //*--------------*
  //* Feature: children

  /** getter for children - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getChildren() {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children)));}
    
  /** setter for children - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setChildren(FSArray v) {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.ll_cas.ll_setRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for children - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public RstTree getChildren(int i) {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children), i);
    return (RstTree)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children), i)));}

  /** indexed setter for children - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setChildren(int i, RstTree v) { 
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_children == null)
      jcasType.jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((RstTree_Type)jcasType).casFeatCode_children), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: isTerminal

  /** getter for isTerminal - gets 
   * @generated
   * @return value of the feature 
   */
  public boolean getIsTerminal() {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_isTerminal == null)
      jcasType.jcas.throwFeatMissing("isTerminal", "edu.cmu.cs.lti.script.type.RstTree");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((RstTree_Type)jcasType).casFeatCode_isTerminal);}
    
  /** setter for isTerminal - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setIsTerminal(boolean v) {
    if (RstTree_Type.featOkTst && ((RstTree_Type)jcasType).casFeat_isTerminal == null)
      jcasType.jcas.throwFeatMissing("isTerminal", "edu.cmu.cs.lti.script.type.RstTree");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((RstTree_Type)jcasType).casFeatCode_isTerminal, v);}    
  }

    