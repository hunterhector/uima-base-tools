

/* First created by JCasGen Wed Jan 28 12:02:15 EST 2015 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Wed Jan 28 12:54:28 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventArgumentLink extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventArgumentLink.class);
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
  protected EventArgumentLink() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventArgumentLink(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventArgumentLink(JCas jcas) {
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
  //* Feature: event

  /** getter for event - gets 
   * @generated
   * @return value of the feature 
   */
  public Event getEvent() {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_event == null)
      jcasType.jcas.throwFeatMissing("event", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return (Event)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_event)));}
    
  /** setter for event - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEvent(Event v) {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_event == null)
      jcasType.jcas.throwFeatMissing("event", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_event, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: entity

  /** getter for entity - gets 
   * @generated
   * @return value of the feature 
   */
  public Entity getEntity() {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_entity == null)
      jcasType.jcas.throwFeatMissing("entity", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return (Entity)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_entity)));}
    
  /** setter for entity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntity(Entity v) {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_entity == null)
      jcasType.jcas.throwFeatMissing("entity", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_entity, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: argumentRole

  /** getter for argumentRole - gets 
   * @generated
   * @return value of the feature 
   */
  public String getArgumentRole() {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_argumentRole == null)
      jcasType.jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    return jcasType.ll_cas.ll_getStringValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_argumentRole);}
    
  /** setter for argumentRole - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArgumentRole(String v) {
    if (EventArgumentLink_Type.featOkTst && ((EventArgumentLink_Type)jcasType).casFeat_argumentRole == null)
      jcasType.jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventArgumentLink");
    jcasType.ll_cas.ll_setStringValue(addr, ((EventArgumentLink_Type)jcasType).casFeatCode_argumentRole, v);}    
  }

    