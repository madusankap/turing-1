/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.apache.woden.internal.wsdl20.validation;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.ErrorReporterImpl;
import org.apache.woden.internal.wsdl20.BindingFaultImpl;
import org.apache.woden.internal.wsdl20.BindingFaultReferenceImpl;
import org.apache.woden.internal.wsdl20.EndpointImpl;
import org.apache.woden.internal.wsdl20.InterfaceFaultReferenceImpl;
import org.apache.woden.internal.wsdl20.InterfaceImpl;
import org.apache.woden.internal.wsdl20.InterfaceMessageReferenceImpl;
import org.apache.woden.internal.wsdl20.ServiceImpl;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.types.NCName;
import org.apache.woden.types.QNameTokenUnion;
import org.apache.woden.wsdl20.Binding;
import org.apache.woden.wsdl20.BindingFault;
import org.apache.woden.wsdl20.BindingFaultReference;
import org.apache.woden.wsdl20.BindingMessageReference;
import org.apache.woden.wsdl20.BindingOperation;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.Interface;
import org.apache.woden.wsdl20.InterfaceFaultReference;
import org.apache.woden.wsdl20.InterfaceMessageReference;
import org.apache.woden.wsdl20.Service;
import org.apache.woden.wsdl20.enumeration.MessageLabel;
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.BindingFaultElement;
import org.apache.woden.wsdl20.xml.BindingFaultReferenceElement;
import org.apache.woden.wsdl20.xml.BindingMessageReferenceElement;
import org.apache.woden.wsdl20.xml.BindingOperationElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.EndpointElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceMessageReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;
import org.apache.woden.wsdl20.xml.ServiceElement;

/**
 * A test class to test the assertion tests in the WSDLComponentValidator.
 */
public class WSDLComponentValidatorTest extends TestCase 
{
  private WSDLComponentValidator val;
  private ErrorReporter reporter;
  private TestErrorHandler handler;
  // Helper test values
  private final static URI namespace1 = URI.create("http://www.sample.org");
  private final static NCName name1 = new NCName("name1");
  private final static NCName name2 = new NCName("name2");
  private final static NCName name3 = new NCName("name3");
  private final static NCName name4 = new NCName("name4");
  private final static QName name1QN = new QName(namespace1.toString(), name1.toString());
  private final static QName name2QN = new QName(namespace1.toString(), name2.toString());
  private final static QName name3QN = new QName(namespace1.toString(), name3.toString());
  private final static QName name4QN = new QName(namespace1.toString(), name4.toString());
  
  /**
   * Create a test suite from this test class.
   * 
   * @return A test suite from this test class.
   */
  public static Test suite()
  {
    return new TestSuite(WSDLComponentValidatorTest.class);
  }
	  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception 
  {
    val = new WSDLComponentValidator();
	handler = new TestErrorHandler();
    reporter = WSDLFactory.newInstance().newWSDLReader().getErrorReporter();
	reporter.setErrorHandler(handler);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception 
  {
	val = null;
	reporter = null;
	handler = null;
  }
  
  /**
   * Test that the testAssertionInterface1009 method returns
   * true if the interface does not appear in the list of its
   * extended interfaces, false otherwise.
   */
  public void testTestAssertionInterface1009()
  {
    WSDLFactory factory = null;
    try {
        factory = WSDLFactory.newInstance();
    } catch (WSDLException e) {
        fail("Can't instanciate the WSDLFactory object.");
    }
    
    // Test that the assertion returns true for an interace that extends no other interfaces.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      Description descComp = descEl.toComponent();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
	  if(!val.testAssertionInterface1009(interfac, reporter))
	  {
	    fail("The testAssertionInterface1009 method returned false for an interface that extends no other interfaces.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true if the interface is not in the direct or indirect list.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      Description descComp = descEl.toComponent();
      descEl.setTargetNamespace(namespace1);
      
      // Create an interface element, name it and add to the description element
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      
      // Create another interface element, name it and add to the description element
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);

      interfac.addExtendedInterfaceName(interfac2.getName());
      
      InterfaceElement interfac3 = descEl.addInterfaceElement();
      interfac3.setName(name3);
      
      interfac.addExtendedInterfaceName(interfac3.getName());
      
      InterfaceElement interfac4 = descEl.addInterfaceElement();
      interfac4.setName(name4);
      
      interfac2.addExtendedInterfaceName(interfac4.getName());
        
      descComp.getInterfaces(); //init Interface's ref to its Description, needed for interface extension
	  
	  if(!val.testAssertionInterface1009((Interface)interfac, reporter))
	  {
	    fail("The testAssertionInterface1009 method returned false for an interface that is not in the list of exteneded interfaces.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false if the interface is in the direct list.
	try
	{
	  DescriptionElement descEl = factory.newDescription();
	  Description descComp = descEl.toComponent();
      descEl.setTargetNamespace(namespace1);
	  InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac3.setName(name3);
      interfac.addExtendedInterfaceName(interfac.getName());
      interfac.addExtendedInterfaceName(interfac2.getName());
      interfac.addExtendedInterfaceName(interfac3.getName());
      
      descComp.getInterfaces(); //init Interface's ref to its Description, needed for interface extension
	  
	  if(val.testAssertionInterface1009(interfac, reporter))
	  {
	    fail("The testAssertionInterface1009 method returned true for an interface that is in the direct extended interface list.");
	  }
	}
	catch(Exception e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false if the interface is in the indirect list.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      Description descComp = descEl.toComponent();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac3.setName(name3);
      interfac.addExtendedInterfaceName(interfac2.getName());
      interfac.addExtendedInterfaceName(interfac3.getName());
      interfac2.addExtendedInterfaceName(interfac.getName());
      
      descComp.getInterface(interfac.getName()); //to ensure the Interface can reference its containing Description
	  
	  if(val.testAssertionInterface1009(interfac, reporter))
	  {
	    fail("The testAssertionInterface1009 method returned true for an interface that is in the indirect extended interface list.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionInterface1010 method returns
   * true if the interface name is unique in the description,
   * false otherwise.
   */
  public void testTestAssertionInterface1010()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
      
    // Test that the assertion returns true for an empty list of interfaces.
	try
	{
	  if(!val.testAssertionInterface1010(new Interface[]{}, reporter))
	  {
	    fail("The testAssertionInterface1010 method returned false for an empty list of interfaces.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for an interface that is the only interface defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac.setName(name1);
	  if(!val.testAssertionInterface1010(new Interface[]{interfac}, reporter))
	  {
	    fail("The testAssertionInterface1010 method returned false for an list of interfaces that contains only one interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for a list of interfaces that contains no duplicate names.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac2.setName(name2);
      InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac3.setName(name3);
	  
	  Interface[] interfaces = new Interface[]{interfac, interfac2, interfac3};
	  
	  if(!val.testAssertionInterface1010(interfaces, reporter))
	  {
	    fail("The testAssertionInterface1010 method returned false for a list of interfaces that contains no duplicates.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two interfaces that are defined with the same QName object.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac.setName(name1);
      InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac2.setName(name2);
      InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
	  interfac3.setName(name1);
	  
	  Interface[] interfaces = new Interface[]{interfac, interfac2, interfac3};
	  
	  if(val.testAssertionInterface1010(interfaces, reporter))
	  {
	    fail("The testAssertionInterface1010 method returned true for a list of interfaces that contains two interfaces defined with the same QName object.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two interfaces that are defined with the same name and
	// different QName objects.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceImpl interfac = (InterfaceImpl)descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
      interfac3.setName(new NCName("name1"));
          
      Interface[] interfaces = new Interface[]{interfac, interfac2, interfac3};
	  
	  if(val.testAssertionInterface1010(interfaces, reporter))
	  {
	    fail("The testAssertionInterface1010 method returned true for a list of interfaces that contains two interfaces with the same name defined with different QName objects.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionMEP1022 method returns
   * true if the pattern is an absolute IRI, false otherwise.
   */
  public void testTestAssertionMEP1022()
  {
	// Test that an absolute IRI is valid.
	try
	{
	  if(!val.testAssertionMEP1022(new URI("http://www.sample.org"), reporter))
	  {
		  fail("The testAssertionMEP1022 method returned false for an absolute pattern.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the test IRI: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that a relative IRI is not valid.
	try
	{
	  if(val.testAssertionMEP1022(new URI("sample.org"), reporter))
	  {
		  fail("The testAssertionMEP1022 method returned true for a relative pattern.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the test IRI: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionInterfaceMessageReference1028 method returns
   * true if the message content model is #any or #none and the element
   * declaration is empty, false otherwise.
   */
  public void testTestAssertionInterfaceMessageReference1028()
  {
    WSDLFactory factory = null;
    try {
        factory = WSDLFactory.newInstance();
    } catch (WSDLException e) {
        fail("Can't instanciate the WSDLFactory object.");
    }
    
    // Test that the method returns true if the message content model is #any and the element declaration is empty.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
	  InterfaceMessageReferenceElement interfaceMessageReference = oper.addInterfaceMessageReferenceElement();
	  interfaceMessageReference.setElement(QNameTokenUnion.ANY);

      Description descComp = descEl.toComponent(); //initialise Interface's ref to its Description
      InterfaceMessageReference msgRefComp = 
        descComp.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0]; 
      
      if(!val.testAssertionInterfaceMessageReference1028((InterfaceMessageReferenceImpl)msgRefComp, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1028 method returned false for an interface message reference with the message content model #any and an empty element declaration.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
		
    // Test that the method returns true if the message content model is #none and the element declaration is empty.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
      InterfaceMessageReferenceElement interfaceMessageReference = oper.addInterfaceMessageReferenceElement();
      interfaceMessageReference.setElement(QNameTokenUnion.NONE);

      Description descComp = descEl.toComponent(); //initialise Interface's ref to its Description
      InterfaceMessageReference msgRefComp = 
        descComp.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0]; 
          
      if(!val.testAssertionInterfaceMessageReference1028((InterfaceMessageReferenceImpl)msgRefComp, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1028 method returned false for an interface message reference with the message content model #none and an empty element declaration.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}	
  }
  
  /**
   * Test that the testAssertionInterfaceMessageReferencet1029 method returns
   * true if the list of interface message references contains no duplicate
   * message labels, false otherwise.
   */
  public void testTestAssertionInterfaceMessageReference1029()
  {
    // Test that the assertion returns true for an empty list of message references.
	try
	{
	  if(!val.testAssertionInterfaceMessageReference1029(new InterfaceMessageReference[]{}, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1029 method returned false for an empty list of interface message references.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for an interface message reference that is the only interface message reference defined.
	try
	{
	  InterfaceMessageReferenceImpl interfaceMessageReference = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference.setMessageLabel(MessageLabel.IN);
	  if(!val.testAssertionInterfaceMessageReference1029(new InterfaceMessageReference[]{interfaceMessageReference}, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1029 method returned false for an interface message reference that is the only interface message reference defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for a list of interface message references that contains no duplicate message labels.
	try
	{
	  InterfaceMessageReferenceImpl interfaceMessageReference = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference.setMessageLabel(MessageLabel.IN);
	  InterfaceMessageReferenceImpl interfaceMessageReference2 = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference2.setMessageLabel(MessageLabel.OUT);
	  
	  InterfaceMessageReference[] interfaceMessageReferences = new InterfaceMessageReference[]{interfaceMessageReference, interfaceMessageReference2};
	  
	  if(!val.testAssertionInterfaceMessageReference1029(interfaceMessageReferences, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1029 method returned false for a list of interface message references that contains no duplicate message labels.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two interface message references that are defined with the same message label.
	try
	{
	  InterfaceMessageReferenceImpl interfaceMessageReference = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference.setMessageLabel(MessageLabel.IN);
	  InterfaceMessageReferenceImpl interfaceMessageReference2 = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference2.setMessageLabel(MessageLabel.OUT);
	  InterfaceMessageReferenceImpl interfaceMessageReference3 = new InterfaceMessageReferenceImpl();
	  interfaceMessageReference3.setMessageLabel(MessageLabel.IN);
	  
	  InterfaceMessageReference[] interfaceMessageReferences = new InterfaceMessageReference[]{interfaceMessageReference, interfaceMessageReference2, interfaceMessageReference3};
	  
	  if(val.testAssertionInterfaceMessageReference1029(interfaceMessageReferences, reporter))
	  {
	    fail("The testAssertionInterfaceMessageReference1029 method returned true for a list of interface message references that contains two interface message references defined with the same message label.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionInterfaceFaultReference1039 method returns
   * true if the list of interface fault references contains no duplicate
   * fault/message label pairs, false otherwise.
   */
  public void testTestAssertionInterfaceFaultReference1039()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
    // Test that the assertion returns true for an interface fault reference list that is empty.
	try
	{
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for an interface fault reference list that is empty.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for an interface fault reference list with one entry.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
	  InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
      
	  InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
	  faultReference.setRef(name1QN);
	  faultReference.setMessageLabel(MessageLabel.IN);

      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for an interface fault reference that is the only interface fault reference defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for two interface fault references that have both different
	// faults and different message labels.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceFaultElement fault2 = interfac.addInterfaceFaultElement();
      fault2.setName(name2);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
          
	  InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
	  faultReference.setRef(name1QN);
	  faultReference.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference2 = oper.addInterfaceFaultReferenceElement();
	  faultReference2.setRef(name2QN);
	  faultReference2.setMessageLabel(MessageLabel.OUT);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference, (InterfaceFaultReferenceImpl)faultReference2}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for two interface fault references that have different faults and message labels.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for two interface fault references that have the same fault
	// but different message labels
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
          
      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setRef(name1QN);
      faultReference.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference2 = oper.addInterfaceFaultReferenceElement();
      faultReference2.setRef(name1QN);
      faultReference2.setMessageLabel(MessageLabel.OUT);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference, (InterfaceFaultReferenceImpl)faultReference2}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for two interface fault references that have the same fault but different message labels.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for two interface fault references that have the same
	// message label but different faults.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceFaultElement fault2 = interfac.addInterfaceFaultElement();
      fault2.setName(name2);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
              
      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setRef(name1QN);
      faultReference.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference2 = oper.addInterfaceFaultReferenceElement();
      faultReference2.setRef(name2QN);
      faultReference2.setMessageLabel(MessageLabel.IN);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference, (InterfaceFaultReferenceImpl)faultReference2}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for two interface fault references that have different faults but the same message labels.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two interface fault references that have the same
	// fault and message label.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
          
      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setRef(name1QN);
      faultReference.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference2 = oper.addInterfaceFaultReferenceElement();
      faultReference2.setRef(name1QN);
      faultReference2.setMessageLabel(MessageLabel.IN);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference, (InterfaceFaultReferenceImpl)faultReference2}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned true for two interface fault references that have the same fault and message label.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the method returns true for an interface fault reference with a null message label.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();

      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setRef(name1QN);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for an interface fault references with a null message labels.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the method returns true for an interface fault reference with a null fault.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
      
      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setMessageLabel(MessageLabel.IN);
      
      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(!val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned false for ano interface fault reference that has a null fault.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the method returns false for two interface fault references that have the same
	// fault/message label and a third interface fault reference that has a null fault and is 
	// defined second.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement fault = interfac.addInterfaceFaultElement();
      fault.setName(name1);
      InterfaceOperationElement oper = interfac.addInterfaceOperationElement();
          
      InterfaceFaultReferenceElement faultReference = oper.addInterfaceFaultReferenceElement();
      faultReference.setRef(name1QN);
      faultReference.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference2 = oper.addInterfaceFaultReferenceElement();
      faultReference2.setRef(name1QN);
      faultReference2.setMessageLabel(MessageLabel.IN);
      InterfaceFaultReferenceElement faultReference3 = oper.addInterfaceFaultReferenceElement();
      faultReference3.setMessageLabel(MessageLabel.OUT);

      descEl.toComponent().getInterfaces(); //init Interface's ref to its Description
      
	  if(val.testAssertionInterfaceFaultReference1039(new InterfaceFaultReference[]{(InterfaceFaultReferenceImpl)faultReference, (InterfaceFaultReferenceImpl)faultReference3, (InterfaceFaultReferenceImpl)faultReference2}, reporter))
	  {
	    fail("The testAssertionInterfaceFaultReference1039 method returned true for two interface fault references that have the same fault and message label and a third interface fault reference that has a null fault and is defined second.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBinding1044 method returns
   * true if interface is specified when a binding operation 
   * or fault is specified, false otherwise.
   */
  public void testTestAssertionBinding1044()
  {
    NCName name1 = new NCName("name1");
    NCName name2 = new NCName("name2");
    WSDLFactory factory = null;
    try {
        factory = WSDLFactory.newInstance();
    } catch (WSDLException e) {
        fail("Can't instantiate the WSDLFactory object.");
    }

    
    // Test that the assertion returns true when no operation or fault is specified
	// and an interface is not specified.
	try
	{
      DescriptionElement desc = factory.newDescription();
      BindingElement bindingEl = desc.addBindingElement();
      bindingEl.setName(name1);
	  if(!val.testAssertionBinding1044(desc.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned false for a binding with no interface, operation, or faults specified.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when no operation or fault is specified
	// and an interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
	  BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
	  if(!val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned false for a binding with an interface and no operation or faults specified.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an operation is specified
	// and an interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  if(!val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned false for a binding with an interface and an operation specified.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when a fault is specified
	// and an interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
	  if(!val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned false for a binding with an interface and a fault specified.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an operation and a fault are specified
	// and an interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  if(!val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned false for a binding with an interface and an operation and a fault specified.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an operation is specified
	// and no interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name1);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  if(val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned true for a binding with an operation and no interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when a fault is specified
	// and no interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name1);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
	  if(val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned true for a binding with a fault and no interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an operation and a fault are specified
	// and no interface is specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name1);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  if(val.testAssertionBinding1044(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1044 method returned true for a binding with an operation and a fault and no interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
  }
  
  /**
   * Test that the testAssertionBinding1048 method returns
   * true if the binding type is absolute, false otherwise.
   */
  public void testTestAssertionBinding1048()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instantiate the WSDLFactory object.");
      }
    // Test that the assertion returns true for a binding with an absolute type.
	try
	{
      DescriptionElement descEl = factory.newDescription();
	  BindingElement binding = descEl.addBindingElement();
	  binding.setType(new URI("http://www.sample.org"));
	  if(!val.testAssertionBinding1048(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1048 method returned false for a binding with an absolute type.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the type URI for the test method " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns false for a binding with a relative type.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
	  BindingElement binding = descEl.addBindingElement();
	  binding.setType(new URI("sample.org"));
	  if(val.testAssertionBinding1048(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1048 method returned true for a binding with a relative type.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the type URI for the test method " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for a binding with a null type. This will be
	// caught be schema validation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
	  BindingElement binding = descEl.addBindingElement();
	  if(!val.testAssertionBinding1048(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1048 method returned false for a binding with a null type.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBinding1049 method returns
   * true if all the binding names are unique in the description,
   * false otherwise.
   */
  public void testTestAssertionBinding1049()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }

    // Test that the assertion returns true for an empty list of bindings.
	try
	{
	  if(!val.testAssertionBinding1049(new Binding[]{}, reporter))
	  {
	    fail("The testAssertionBinding1049 method returned false for an empty list of bindings.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for a list of bindings that only contains one binding.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
	  BindingElement binding = descEl.addBindingElement();
	  binding.setName(name1);
	  if(!val.testAssertionBinding1049(descEl.toComponent().getBindings(), reporter))
	  {
	    fail("The testAssertionBinding1049 method returned false for an list of bindings that contains only one binding.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for a list of bindings that contains no duplicate names.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
	  BindingElement binding = descEl.addBindingElement();
	  binding.setName(name1);
      BindingElement binding2 = descEl.addBindingElement();
	  binding2.setName(name2);
      BindingElement binding3 = descEl.addBindingElement();
	  binding3.setName(name3);
	  
	  Binding[] bindings = descEl.toComponent().getBindings();
	  
	  if(!val.testAssertionBinding1049(bindings, reporter))
	  {
	    fail("The testAssertionBinding1049 method returned false for a list of bindings that contains no duplicates.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two bindings that are defined with the same QName object.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name1);
      BindingElement binding2 = descEl.addBindingElement();
      binding2.setName(name2);
      BindingElement binding3 = descEl.addBindingElement();
	  binding3.setName(name1);
	  
	  Binding[] bindings = descEl.toComponent().getBindings();
	  
	  if(val.testAssertionBinding1049(bindings, reporter))
	  {
	    fail("The testAssertionBinding1049 method returned true for a list of binginds that contains two bindings defined with the same QName object.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two bindings that are defined with the same name and
	// different QName objects.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      BindingElement binding = descEl.addBindingElement();
	  binding.setName(name1);
      BindingElement binding2 = descEl.addBindingElement();
      binding2.setName(name2);
      BindingElement binding3 = descEl.addBindingElement();
	  binding3.setName(new NCName("name1"));
	  
	  Binding[] bindings = descEl.toComponent().getBindings();
	  
	  if(val.testAssertionBinding1049(bindings, reporter))
	  {
	    fail("The testAssertionBinding1049 method returned true for a list of bindings that contains two bindings with the same name defined with different QName objects.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBinding1045 method returns
   * true if all the interface operations have bindings defined,
   * false otherwise.
   */
  public void testTestAssertionBinding1045()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
    // Test that the assertion returns true when the binding does not specify an interface.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name1);
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with no defined interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true when an interface is specified with no operations and
    // the binding has no operations defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with an interface with no operations.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an interface is specified with no operations and
	// the binding has an operation defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with an interface with no operations and one binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an interface is specified with one operation and
	// the binding defines a binding operation for the interface operation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name2);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  bindingOperation.setRef(name2QN);
      
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with an interface with one operation and one binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an interface is specified with one operation and
	// the binding defines no operations.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name2);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      
	  if(val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned true for a binding with an interface with one operation and no binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an interface is specified with one operation through
	// an extended interface and the binding defines a binding operation for the operation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation = interfac2.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
	  binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  bindingOperation.setRef(name3QN);
      
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with an interface with one extended operation and one binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an interface is specified with one operation through
	// an extended interface and the binding defines no operations.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation = interfac2.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      
	  if(val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned true for a binding with an interface with one extended operation and no binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when an interface is specified with two operations, one
	// explicit and one inherited, and the binding defines operations for both.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceImpl interfac2 = new InterfaceImpl();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation2 = interfac2.addInterfaceOperationElement();
      interfaceOperation2.setName(name4);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      BindingOperationElement bindingOperation2 = binding.addBindingOperationElement();
      bindingOperation2.setRef(name4QN);
      
	  if(!val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned false for a binding with an interface with one defined operation and one extended operation and two binding operations defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an interface is specified with two operations, one
	// explicit and one inherited, and the binding defines an operation only for the explicit operation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation2 = interfac2.addInterfaceOperationElement();
      interfaceOperation2.setName(name4);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      
	  if(val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned true for a binding with an interface with one defined operation and one extended operation and one binding operation defined for the defined operation.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when an interface is specified with two operations, one
	// explicit and one inherited, and the binding defines an operation only for the inherited operation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceImpl interfac2 = new InterfaceImpl();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation2 = interfac2.addInterfaceOperationElement();
      interfaceOperation2.setName(name4);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name4QN);
      
	  if(val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned true for a binding with an interface with one defined operation and one inherited operation and one binding operation defined for the inherited operation.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	handler.reset();
	
	// Test that two messages are returned when an interface with two operations, one explicit and one
	// inherited, is specified and the binding defines no operations.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      interfac.addExtendedInterfaceName(name2QN);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);
      InterfaceOperationElement interfaceOperation2 = interfac2.addInterfaceOperationElement();
      interfaceOperation2.setName(name4);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      
	  if(val.testAssertionBinding1045(descEl.toComponent().getBindings()[0], reporter))
	  {
	    fail("The testAssertionBinding1045 method returned true for a binding with an interface with one defined operation and one inherited operation and no binding operations defined.");
	  }
	  if(handler.numErrors != 2)
	  {
		  fail("The testAssertionBinding1045 method did not report two errors for a binding with an interface with one defined operation and one inherited operation and no binding operations defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBindingOperation1051 method returns
   * true if all the binding operations have unique interface 
   * operations specified, false otherwise.
   */
  public void testTestAssertionBindingOperation1051()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
    // Test that the assertion returns true when there are no binding operations defined.
	try
	{
	  if(!val.testAssertionBindingOperation1051(new BindingOperation[]{}, reporter))
	  {
	    fail("The testAssertionBindingOperation1051 method returned false with no binding operations defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there is one binding operation defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name2);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
	  bindingOperation.setRef(name2QN);
      
	  if(!val.testAssertionBindingOperation1051(descEl.toComponent().getBindings()[0].getBindingOperations(), reporter))
	  {
	    fail("The testAssertionBindingOperation1051 method returned false with one valid binding operation defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there are two binding operations defined with
	// unique interface operations.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceOperationElement interfaceOperation2 = interfac.addInterfaceOperationElement();
      interfaceOperation2.setName(name4);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      BindingOperationElement bindingOperation2 = binding.addBindingOperationElement();
      bindingOperation2.setRef(name4QN);
      
	  if(!val.testAssertionBindingOperation1051(descEl.toComponent().getBindings()[0].getBindingOperations(), reporter))
	  {
	    fail("The testAssertionBindingOperation1051 method returned false with two valid binding operations defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	
	// Test that the assertion returns false when there are two binding operations defined with
	// the same interface operation.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      BindingOperationElement bindingOperation2 = binding.addBindingOperationElement();
      bindingOperation2.setRef(name3QN);
      
	  if(val.testAssertionBindingOperation1051(descEl.toComponent().getBindings()[0].getBindingOperations(), reporter))
	  {
	    fail("The testAssertionBindingOperation1051 method returned true with two binding operations defined with the same interface operation.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBindingMessageReference1052 method returns
   * true if all the binding message references have unique interface 
   * message references specified, false otherwise.
   */
  public void testTestAssertionBindingMessageReference1052()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
    // Test that the assertion returns true when there are no binding message references defined.
	try
	{
	  if(!val.testAssertionBindingMessageReference1052(new BindingMessageReference[]{}, reporter))
	  {
	    fail("The testAssertionBindingMessageReference1052 method returned false with no binding message references defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there is one binding message reference defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceMessageReferenceElement interfaceMessageReference = interfaceOperation.addInterfaceMessageReferenceElement();
      interfaceMessageReference.setMessageLabel(MessageLabel.IN);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);

      BindingMessageReferenceElement bindingMessageReference = bindingOperation.addBindingMessageReferenceElement();
      bindingMessageReference.setMessageLabel(MessageLabel.IN);
          
	  if(!val.testAssertionBindingMessageReference1052(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingMessageReferences(), reporter))
	  {
	    fail("The testAssertionBindingMessageReference1052 method returned false with one valid binding message reference defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there are two binding message references defined with
	// unique interface message references.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceMessageReferenceElement interfaceMessageReference = interfaceOperation.addInterfaceMessageReferenceElement();
      interfaceMessageReference.setMessageLabel(MessageLabel.IN);
      InterfaceMessageReferenceElement interfaceMessageReference2 = interfaceOperation.addInterfaceMessageReferenceElement();
      interfaceMessageReference2.setMessageLabel(MessageLabel.OUT);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      BindingMessageReferenceElement bindingMessageReference = bindingOperation.addBindingMessageReferenceElement();
      bindingMessageReference.setMessageLabel(MessageLabel.IN);
      BindingMessageReferenceElement bindingMessageReference2 = bindingOperation.addBindingMessageReferenceElement();
      bindingMessageReference2.setMessageLabel(MessageLabel.OUT);
          
	  if(!val.testAssertionBindingMessageReference1052(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingMessageReferences(), reporter))
	  {
	    fail("The testAssertionBindingMessageReference1052 method returned false with two valid binding message references defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	
	// Test that the assertion returns false when there are two binding message references define with
	// the same interface message reference.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name3);
      InterfaceMessageReferenceElement interfaceMessageReference = interfaceOperation.addInterfaceMessageReferenceElement();
      interfaceMessageReference.setMessageLabel(MessageLabel.IN);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name3QN);
      BindingMessageReferenceElement bindingMessageReference = bindingOperation.addBindingMessageReferenceElement();
      bindingMessageReference.setMessageLabel(MessageLabel.IN);
      BindingMessageReferenceElement bindingMessageReference2 = bindingOperation.addBindingMessageReferenceElement();
      bindingMessageReference2.setMessageLabel(MessageLabel.IN);
          
	  if(val.testAssertionBindingMessageReference1052(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingMessageReferences(), reporter))
	  {
	    fail("The testAssertionBindingMessageReference1052 method returned true with two binding message references defined with the same interface message reference.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBindingFaultReference1055 method returns
   * true if all the binding fault references have unique interface 
   * fault references specified, false otherwise.
   */
  public void testTestAssertionBindingFaultReference1055()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
      
    // Test that the assertion returns true when there are no binding fault references defined.
	try
	{
	  if(!val.testAssertionBindingFaultReference1055(new BindingFaultReference[]{}, reporter))
	  {
	    fail("The testAssertionBindingFaultReference1055 method returned false with no binding fault references defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there is one binding fault reference defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name3);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name4);
      InterfaceFaultReferenceElement interfaceFaultReference = interfaceOperation.addInterfaceFaultReferenceElement();
      interfaceFaultReference.setMessageLabel(MessageLabel.IN);
      interfaceFaultReference.setRef(name3QN);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name4QN);
      BindingFaultReferenceElement bindingFaultReference = bindingOperation.addBindingFaultReferenceElement();

      bindingFaultReference.setMessageLabel(MessageLabel.IN);
      bindingFaultReference.setRef(name3QN);
          
	  if(!val.testAssertionBindingFaultReference1055(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingFaultReferences(), reporter))
	  {
	    fail("The testAssertionBindingFaultReference1055 method returned false with one valid binding fault reference defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true when there are two binding fault references defined with
	// unique interface fault references.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name3);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name4);
      InterfaceFaultReferenceElement interfaceFaultReference = interfaceOperation.addInterfaceFaultReferenceElement();
      interfaceFaultReference.setMessageLabel(MessageLabel.IN);
      interfaceFaultReference.setRef(name3QN);
      InterfaceFaultReferenceElement interfaceFaultReference2 = interfaceOperation.addInterfaceFaultReferenceElement();
      interfaceFaultReference2.setMessageLabel(MessageLabel.OUT);
      interfaceFaultReference2.setRef(name3QN);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name4QN);
      BindingFaultReferenceElement bindingFaultReference = bindingOperation.addBindingFaultReferenceElement();
      bindingFaultReference.setMessageLabel(MessageLabel.IN);
      bindingFaultReference.setRef(name3QN);
      BindingFaultReferenceElement bindingFaultReference2 = bindingOperation.addBindingFaultReferenceElement();
      bindingFaultReference2.setMessageLabel(MessageLabel.OUT);
      bindingFaultReference2.setRef(name3QN);
          
	  if(!val.testAssertionBindingFaultReference1055(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingFaultReferences(), reporter))
	  {
	    fail("The testAssertionBindingFaultReference1055 method returned false with two valid binding fault references defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	
	// Test that the assertion returns false when there are two binding fault references define with
	// the same interface fault reference.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name3);
      InterfaceOperationElement interfaceOperation = interfac.addInterfaceOperationElement();
      interfaceOperation.setName(name4);
      InterfaceFaultReferenceElement interfaceFaultReference = interfaceOperation.addInterfaceFaultReferenceElement();
      interfaceFaultReference.setMessageLabel(MessageLabel.IN);
      interfaceFaultReference.setRef(name3QN);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name4QN);
      BindingFaultReferenceElement bindingFaultReference = bindingOperation.addBindingFaultReferenceElement();
      bindingFaultReference.setMessageLabel(MessageLabel.IN);
      bindingFaultReference.setRef(name3QN);
      BindingFaultReferenceElement bindingFaultReference2 = bindingOperation.addBindingFaultReferenceElement();
      bindingFaultReference2.setMessageLabel(MessageLabel.IN);
      bindingFaultReference2.setRef(name3QN);
          
	  if(val.testAssertionBindingFaultReference1055(descEl.toComponent().getBindings()[0].getBindingOperations()[0].getBindingFaultReferences(), reporter))
	  {
	    fail("The testAssertionBindingFaultReference1055 method returned true with two binding fault references defined with the same interface fault reference.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionBindingFaultReference1059 method returns
   * true if all the binding fault reference has a valid reference to an
   * interface fault reference, false otherwise.
   */
  public void testTestAssertionBindingFaultReference1059()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
    // Test that the assertion returns true when the binding fault reference defines a valid
	// interface fault reference.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name2);
      InterfaceOperationElement interfaceOper = interfac.addInterfaceOperationElement();
      interfaceOper.setName(name1);
      InterfaceFaultReferenceElement interfaceFaultReference = interfaceOper.addInterfaceFaultReferenceElement();
      interfaceFaultReference.setRef(name2QN);
      interfaceFaultReference.setMessageLabel(MessageLabel.IN);
      
      BindingElement binding = descEl.addBindingElement();
      binding.setInterfaceName(name1QN);
      BindingOperationElement bindingOperation = binding.addBindingOperationElement();
      bindingOperation.setRef(name1QN);
      BindingFaultReferenceElement bindingFaultReference = bindingOperation.addBindingFaultReferenceElement();
	  bindingFaultReference.setRef(name2QN);
      bindingFaultReference.setMessageLabel(MessageLabel.IN);
      
      descEl.toComponent().getBindings(); //init Binding's ref to its Description
      
	  if(!val.testAssertionBindingFaultReference1059((BindingFaultReferenceImpl)bindingFaultReference, reporter))
	  {
	    fail("The testAssertionBindingFaultReference1059 method returned false with a binding fault reference with a valid interface fault reference defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false when the binding fault reference does not define an
	// interface fault reference.
	try
	{
	  BindingFaultReferenceImpl bindingFaultReference = new BindingFaultReferenceImpl();
	  if(val.testAssertionBindingFaultReference1059(bindingFaultReference, reporter))
	  {
	    fail("The testAssertionBindingFaultReference1059 method returned true with a binding fault reference that does not define an interface fault reference.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// TODO: Test with an invalid interface fault reference - how does the model represent this?
  }
  
  /**
   * Test that the testAssertionBindingFault1050 method returns
   * true if all the binding faults have unique references to
   * interface faults, false otherwise.
   */
  public void testTestAssertionBindingFault1050()
  {	
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
      
    // Test that the assertion returns true for an empty list of binding faults.
	try
	{
	  if(!val.testAssertionBindingFault1050(new BindingFault[]{}, reporter))
	  {
	    fail("The testAssertionBindingFault1050 method returned false for an empty list of binding faults.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for a list of binding faults that only contains one binding.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name2);
      
      BindingElement binding = descEl.addBindingElement();
      binding.setInterfaceName(name1QN);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
	  bindingFault.setRef(name2QN);
      
      descEl.toComponent().getBindings(); //init Binding's ref to its Description
      
      if(!val.testAssertionBindingFault1050(new BindingFault[]{(BindingFaultImpl)bindingFault}, reporter))
	  {
	    fail("The testAssertionBindingFault1050 method returned false for an list of binding faults that contains only one binding.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for a list of binding faults that contains no duplicate interface fault references.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name1);
      InterfaceFaultElement interfaceFault2 = interfac.addInterfaceFaultElement();
      interfaceFault2.setName(name2);
      InterfaceFaultElement interfaceFault3 = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name3);
      
      BindingElement binding = descEl.addBindingElement();
      binding.setInterfaceName(name1QN);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
	  bindingFault.setRef(name1QN);
      BindingFaultElement bindingFault2 = binding.addBindingFaultElement();
	  bindingFault2.setRef(name2QN);
      BindingFaultElement bindingFault3 = binding.addBindingFaultElement();
	  bindingFault3.setRef(name3QN);
	  
      descEl.toComponent().getBindings(); //init Binding's ref to its Description
      
	  BindingFault[] bindingFaults = new BindingFault[]{(BindingFaultImpl)bindingFault, (BindingFaultImpl)bindingFault2, (BindingFaultImpl)bindingFault3};
	  
	  if(!val.testAssertionBindingFault1050(bindingFaults, reporter))
	  {
	    fail("The testAssertionBindingFault1050 method returned false for a list of binding faults that contains no duplicate interface fault references.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two binding faults that are defined with the same interface fault reference.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceFaultElement interfaceFault = interfac.addInterfaceFaultElement();
      interfaceFault.setName(name1);
      InterfaceFaultElement interfaceFault2 = interfac.addInterfaceFaultElement();
      interfaceFault2.setName(name2);
      
      BindingElement binding = descEl.addBindingElement();
      binding.setInterfaceName(name1QN);
      BindingFaultElement bindingFault = binding.addBindingFaultElement();
      bindingFault.setRef(name1QN);
      BindingFaultElement bindingFault2 = binding.addBindingFaultElement();
      bindingFault2.setRef(name2QN);
      BindingFaultElement bindingFault3 = binding.addBindingFaultElement();
      bindingFault3.setRef(name1QN);
      
      descEl.toComponent().getBindings(); //init Binding's ref to its Description
      
	  BindingFault[] bindingFaults = new BindingFault[]{(BindingFaultImpl)bindingFault, (BindingFaultImpl)bindingFault2, (BindingFaultImpl)bindingFault3};
	  
	  if(val.testAssertionBindingFault1050(bindingFaults, reporter))
	  {
	    fail("The testAssertionBindingFault1050 method returned true for a list of binging faults that contains two binding faults defined with the same interface fault reference.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionService1060 method returns
   * true if the list of services contains no services
   * with duplicate names, false otherwise.
   */
  public void testTestAssertionService1060()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
	// Test that the assertion returns true for an empty list of services.
	try
	{
	  if(!val.testAssertionService1060(new Service[]{}, reporter))
	  {
	    fail("The testAssertionService1060 method returned false for an empty list of services.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for a service with no name.
	try
	{
	  ServiceImpl service = new ServiceImpl();
	  if(!val.testAssertionService1060(new Service[]{service}, reporter))
	  {
	    fail("The testAssertionService1060 method returned false for a service with no defined name.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for a service that is the only service defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      ServiceImpl service = (ServiceImpl)descEl.addServiceElement();
	  service.setName(name1);
	  if(!val.testAssertionService1060(new Service[]{service}, reporter))
	  {
	    fail("The testAssertionService1060 method returned false for a service that is the only service defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for a list of services that contains no duplicate names.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      ServiceImpl service = (ServiceImpl)descEl.addServiceElement();
	  service.setName(name1);
	  ServiceImpl service2 = (ServiceImpl)descEl.addServiceElement();
      service2.setName(name2);
	  ServiceImpl service3 = (ServiceImpl)descEl.addServiceElement();
      service3.setName(name3);
	  
	  Service[] services = new Service[]{service, service2, service3};
	  
	  if(!val.testAssertionService1060(services, reporter))
	  {
	    fail("The testAssertionService1060 method returned false for a list of services that contains no duplicate names.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two services that are defined with the same QName object.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      ServiceImpl service = (ServiceImpl)descEl.addServiceElement();
      service.setName(name1);
      ServiceImpl service2 = (ServiceImpl)descEl.addServiceElement();
      service2.setName(name2);
      ServiceImpl service3 = (ServiceImpl)descEl.addServiceElement();
	  service3.setName(name1);
	  
	  Service[] services = new Service[]{service, service2, service3};
	  
	  if(val.testAssertionService1060(services, reporter))
	  {
	    fail("The testAssertionService1060 method returned true for a list of services that contains two services defined with the same QName object.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for two services that are defined with the same name and
	// different QName objects.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
      ServiceImpl service = (ServiceImpl)descEl.addServiceElement();
      service.setName(name1);
      ServiceImpl service2 = (ServiceImpl)descEl.addServiceElement();
      service2.setName(name2);
      ServiceImpl service3 = (ServiceImpl)descEl.addServiceElement();
      service3.setName(new NCName("name1"));
	  
	  Service[] services = new Service[]{service, service2, service3};
	  
	  if(val.testAssertionService1060(services, reporter))
	  {
	    fail("The testAssertionService1060 method returned true for a list of services that contains two services with the same name defined with different QName objects.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionEndpoint1061 method returns
   * true if the endpoint address is absolute, false otherwise.
   */
  public void testTestAssertionEndpoint1061()
  {
    // Test that the assertion returns true for an endpoint with an absolute address.
	try
	{
	  EndpointImpl endpoint = new EndpointImpl();
	  endpoint.setAddress(new URI("http://www.sample.org"));
	  if(!val.testAssertionEndpoint1061(endpoint, reporter))
	  {
	    fail("The testAssertionEndpoint1061 method returned false for an endpoint with an absolute address.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the address URI for the test method " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns false for an endpoint with a relative address.
	try
	{
	  EndpointImpl endpoint = new EndpointImpl();
	  endpoint.setAddress(new URI("sample.org"));
	  if(val.testAssertionEndpoint1061(endpoint, reporter))
	  {
	    fail("The testAssertionEndpoint1061 method returned true for an endpoint with a relative address.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the address URI for the test method " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that the assertion returns true for an endpoint with a null address. This will be
	// caught be schema validation.
	try
	{
	  EndpointImpl endpoint = new EndpointImpl();
	  if(!val.testAssertionEndpoint1061(endpoint, reporter))
	  {
	    fail("The testAssertionEndpoint1061 method returned false for an endpoint with a null address.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
  
  /**
   * Test that the testAssertionEndpoint1062 method returns
   * true if the endpoint binding does not specify an interface or
   * specifies the same interface as the endpoint's parent service,
   * false otherwise.
   */
  public void testTestAssertionEndpoint1062()
  {
      WSDLFactory factory = null;
      try {
          factory = WSDLFactory.newInstance();
      } catch (WSDLException e) {
          fail("Can't instanciate the WSDLFactory object.");
      }
      
    /* (jkaputin) By following the Woden programming model, the implementation will initialize 'parent' references.
     * To create an endpoint without a parent, this test case cannot use the correct programming model and a NPE 
     * is thrown on endpoint.getParent(). This test is commented out while the issue is being considered.
     * 
     * TODO either assume that model is schema-validated prior to semantic checking and remove this test case OR
     * change the getParent() behaviour across the whole model to check for nulls. Probably the latter option,
     * to better support programmatic creation of a wsdl model. 
     */
    /*
    // Test that the assertion returns true for an endpoint with no binding or parent defined.
	try
	{
	  EndpointImpl endpoint = new EndpointImpl();
	  if(!val.testAssertionEndpoint0066(endpoint, reporter))
	  {
	    fail("The testAssertionEndpoint0066 method returned false for an endpoint with no binding or parent defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
    */

	// Test that the assertion returns true for an endpoint with no binding defined.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      ServiceElement service = descEl.addServiceElement();
	  EndpointElement endpoint = service.addEndpointElement();
	  if(!val.testAssertionEndpoint1062(descEl.toComponent().getServices()[0].getEndpoints()[0], reporter))
	  {
	    fail("The testAssertionEndpoint1062 method returned false for an endpoint with no binding defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
    /* (jkaputin) ditto the comment above for the first test case (NPE on getParent)
	// Test that the assertion returns true for an endpoint with no parent defined.
	try
	{
	  EndpointImpl endpoint = new EndpointImpl();
	  BindingElement binding = descEl.createBindingElement();
      binding.setName(name1);
	  endpoint.setBindingName(name1);
	  if(!val.testAssertionEndpoint0066(endpoint, reporter))
	  {
	    fail("The testAssertionEndpoint0066 method returned false for an endpoint with no parent defined.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
    */
	
	// Test that the assertion returns true for an endpoint that specifies a binding with no interface specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      ServiceElement service = descEl.addServiceElement();
      service.setInterfaceName(name1QN);
      EndpointElement endpoint = service.addEndpointElement();
      endpoint.setBindingName(name2QN);
	  if(!val.testAssertionEndpoint1062(descEl.toComponent().getServices()[0].getEndpoints()[0], reporter))
	  {
	    fail("The testAssertionEndpoint1062 method returned false for an endpoint that specifies a binding with no specified interface.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns true for an endpoint that specifies a binding with the same interface
	// as the parent service specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name2);
      binding.setInterfaceName(name1QN);
      ServiceElement service = descEl.addServiceElement();
      service.setInterfaceName(name1QN);
      EndpointElement endpoint = service.addEndpointElement();
      endpoint.setBindingName(name2QN);
	  if(!val.testAssertionEndpoint1062(descEl.toComponent().getServices()[0].getEndpoints()[0], reporter))
	  {
	    fail("The testAssertionEndpoint1062 method returned false for an endpoint that specifies a binding with the same interface specified as the parent service specifies.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that the assertion returns false for an endpoint that specifies a binding with a different interface
	// than the parent service specified.
	try
	{
      DescriptionElement descEl = factory.newDescription();
      descEl.setTargetNamespace(namespace1);
	  InterfaceElement interfac = descEl.addInterfaceElement();
      interfac.setName(name1);
      InterfaceElement interfac2 = descEl.addInterfaceElement();
      interfac2.setName(name2);
      BindingElement binding = descEl.addBindingElement();
      binding.setName(name3);
      binding.setInterfaceName(name2QN);
      ServiceElement service = descEl.addServiceElement();
      service.setInterfaceName(name1QN);
      EndpointElement endpoint = service.addEndpointElement();
      endpoint.setBindingName(name3QN);
	  if(val.testAssertionEndpoint1062(descEl.toComponent().getServices()[0].getEndpoints()[0], reporter))
	  {
	    fail("The testAssertionEndpoint1062 method returned true for an endpoint that specifies a binding with a different interface specified than the parent service specifies.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	}
  }
}
