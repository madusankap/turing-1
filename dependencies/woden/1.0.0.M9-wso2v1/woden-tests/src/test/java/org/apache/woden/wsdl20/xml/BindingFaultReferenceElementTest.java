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
package org.apache.woden.wsdl20.xml;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.BindingFaultReferenceImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;

/**
 * Unit tests for the BindingFaultReferenceElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingFaultReferenceElementTest extends TestCase {

	private BindingFaultReferenceElement fFaultReference = null;

	public static Test suite()
	{
	   return new TestSuite(BindingFaultReferenceElementTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fFaultReference = new BindingFaultReferenceImpl();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/*
	 * Test that the (Mandatory) message label attribute ("messageLabel") can be successfully set and retrieved
	 */
	public void testSetGetMessageLabel()
	{
		NCName faultRefNCName = new NCName("faultRefName");
		fFaultReference.setMessageLabel(faultRefNCName);
		assertEquals("The retrieved Element name is not that which was set", 
				faultRefNCName, fFaultReference.getMessageLabel());
	}

	/* 
	 * Test that the (Mandatory) BindingFault reference attribute ("ref") can be successfully set and retrieved 
	 */
	public void testSetGetRef()
	{
		QName faultRefName = new QName("faultRefName");
		fFaultReference.setRef(faultRefName);
		assertEquals("The retrieved Element name is not that which was set", 
				faultRefName, fFaultReference.getRef());
	}
	
	/* 
	 * Test that the (Mandatory) InterfaceFaultReference can be successfully retrieved.
	 * The fault reference is to an Interface Fault associated with the grandparent BindingElement.
	 */
	public void testGetInterfaceFaultReferenceElement()
	{
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

		DescriptionElement descriptionElement = factory.newDescription();
		
		// Create the BindingElement<->InterfaceElement->InterfaceOperationElement->InterfaceFaultReferenceElement hierarchy
		BindingElement bindingElement = descriptionElement.addBindingElement();
		bindingElement.setInterfaceName(new QName("interface1"));
		
		InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		InterfaceOperationElement ifopElement = interfaceElement.addInterfaceOperationElement();
		ifopElement.setName(new NCName("operation1"));
		InterfaceFaultReferenceElement iffrElement = ifopElement.addInterfaceFaultReferenceElement();
		iffrElement.setMessageLabel(new NCName("Fault1MessageLabel"));
		iffrElement.setRef(new QName("Fault1Ref"));
				
		// Create the BindingOperationElement->BindingFaultReferenceElement hierarchy
		BindingOperationElement bopElement = bindingElement.addBindingOperationElement();
		bopElement.setRef(new QName("operation1"));
		fFaultReference = bopElement.addBindingFaultReferenceElement();
		fFaultReference.setMessageLabel(new NCName("Fault1MessageLabel"));
		fFaultReference.setRef(new QName("Fault1Ref"));

		InterfaceFaultReferenceElement retrievedFault = fFaultReference.getInterfaceFaultReferenceElement();
		assertEquals("The retrieved InterfaceFaultReferenceElement is not that which was set", 
				iffrElement, retrievedFault);
	}
}
