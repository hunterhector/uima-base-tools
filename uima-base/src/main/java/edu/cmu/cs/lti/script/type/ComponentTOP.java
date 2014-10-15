

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Wed Oct 15 17:18:57 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class ComponentTOP extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ComponentTOP.class);
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
  protected ComponentTOP() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public ComponentTOP(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ComponentTOP(JCas jcas) {
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
  //* Feature: componentId

  /** getter for componentId - gets Indicate which component edit or changes it
   * @generated
   * @return value of the feature 
   */
  public String getComponentId() {
    if (ComponentTOP_Type.featOkTst && ((ComponentTOP_Type)jcasType).casFeat_componentId == null)
      jcasType.jcas.throwFeatMissing("componentId", "edu.cmu.cs.lti.script.type.ComponentTOP");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ComponentTOP_Type)jcasType).casFeatCode_componentId);}
    
  /** setter for componentId - sets Indicate which component edit or changes it 
   * @generated
   * @param v value to set into the feature 
   */
  public void setComponentId(String v) {
    if (ComponentTOP_Type.featOkTst && ((ComponentTOP_Type)jcasType).casFeat_componentId == null)
      jcasType.jcas.throwFeatMissing("componentId", "edu.cmu.cs.lti.script.type.ComponentTOP");
    jcasType.ll_cas.ll_setStringValue(addr, ((ComponentTOP_Type)jcasType).casFeatCode_componentId, v);}    
   
    
  //*--------------*
  //* Feature: id

  /** getter for id - gets 
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (ComponentTOP_Type.featOkTst && ((ComponentTOP_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.cs.lti.script.type.ComponentTOP");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ComponentTOP_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (ComponentTOP_Type.featOkTst && ((ComponentTOP_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "edu.cmu.cs.lti.script.type.ComponentTOP");
    jcasType.ll_cas.ll_setStringValue(addr, ((ComponentTOP_Type)jcasType).casFeatCode_id, v);}    
  }

    