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
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.InterfaceFaultReferenceImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.enumeration.Direction;

/**
 * Unit tests for the InterfaceFaultReferenceElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceFaultReferenceElementTest extends TestCase {

	private InterfaceFaultReferenceElement fFaultReference = null;

	public static Test suite()
	{
	   return new TestSuite(InterfaceFaultReferenceElementTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fFaultReference = new InterfaceFaultReferenceImpl();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	/*
	 * Test that a (mandatory) direction can be successfully set and retrieved
	 */
	public void testSetGetDirection()
	{
		// Default case
		assertNull("The retrieved Element name when unset should be null", fFaultReference.getDirection());
		
		fFaultReference.setDirection(Direction.IN);
		assertEquals("The retrieved FaultReference direction is not that which was set", 
				Direction.IN, fFaultReference.getDirection());
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
	 * Test that the(Mandatory) InterfaceFault reference attribute ("ref") can be successfully set and retrieved 
	 */
	public void testSetGetRef()
	{
		QName faultRefName = new QName("faultRefName");
		fFaultReference.setRef(faultRefName);
		assertEquals("The retrieved Element name is not that which was set", 
				faultRefName, fFaultReference.getRef());
	}
	
	/* 
	 * Test that the (Mandatory) InterfaceFault can be successfully retrieved.
	 * The fault reference is to an Interface Fault associated with the grandparent InterfaceElement.
	 */
	public void testGetInterfaceFaultElement()
	{
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
		// Create the DescriptionElement->InterfaceElement->InterfaceOperationElement->InterfaceFaultReference hierarchy
        DescriptionElement desc = factory.newDescription();
		InterfaceElement interfaceElement = desc.addInterfaceElement();
		InterfaceOperationElement interfaceOperationElement = interfaceElement.addInterfaceOperationElement();

		// Add an InterfaceFault to the InterfaceElement
		InterfaceFaultElement faultElement = interfaceElement.addInterfaceFaultElement();
		faultElement.setName(new NCName("Fault1"));
		
		// create the InterfaceFaultReference to test
		InterfaceFaultReferenceElement faultReference = interfaceOperationElement.addInterfaceFaultReferenceElement();
		faultReference.setRef(new QName("Fault1"));
		InterfaceFaultElement retrievedFault = faultReference.getInterfaceFaultElement();
		assertEquals("The retrieved InterfaceFaultElement is not that which was set", 
				faultElement, retrievedFault);
	}
}
