

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Wed Jan 28 12:54:28 EST 2015
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class FanseToken extends Word {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(FanseToken.class);
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
  protected FanseToken() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public FanseToken(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public FanseToken(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public FanseToken(JCas jcas, int begin, int end) {
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
  //* Feature: lexicalSense

  /** getter for lexicalSense - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLexicalSense() {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_lexicalSense == null)
      jcasType.jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    return jcasType.ll_cas.ll_getStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_lexicalSense);}
    
  /** setter for lexicalSense - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLexicalSense(String v) {
    if (FanseToken_Type.featOkTst && ((FanseToken_Type)jcasType).casFeat_lexicalSense == null)
      jcasType.jcas.throwFeatMissing("lexicalSense", "edu.cmu.cs.lti.script.type.FanseToken");
    jcasType.ll_cas.ll_setStringValue(addr, ((FanseToken_Type)jcasType).casFeatCode_lexicalSense, v);}    
  }

    