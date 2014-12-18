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
package org.apache.woden.wsdl20;

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
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;

/**
 * Unit tests for the InterfaceFaultReference class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceFaultReferenceTest extends TestCase {

	private InterfaceFaultReferenceElement fFaultReferenceElement = null;
	private InterfaceFaultReference fFaultReferenceComp = null;

	public static Test suite()
	{
	   return new TestSuite(InterfaceFaultReferenceTest.class);	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fFaultReferenceElement = new InterfaceFaultReferenceImpl();
		fFaultReferenceElement.setDirection(Direction.OUT);
		fFaultReferenceComp = (InterfaceFaultReference)fFaultReferenceElement;
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
		assertEquals("The retrieved FaultReference direction is not that which was set", 
				Direction.OUT, fFaultReferenceComp.getDirection());
	}

	/*
	 * Test that the (Mandatory) message label attribute ("messageLabel") can be successfully set and retrieved
	 */
	public void testGetMessageLabel()
	{
		NCName faultRefNCName = new NCName("faultRefName");
		fFaultReferenceElement.setMessageLabel(faultRefNCName);
		assertEquals("The retrieved Fault Reference name is not that which was set", 
				faultRefNCName, fFaultReferenceComp.getMessageLabel());
	}

	/* 
	 * Test that the (Mandatory) InterfaceFault can be successfully retrieved.
	 * The fault reference is to an Interface Fault associated with the grandparent InterfaceElement.
	 */
	public void testGetInterfaceFault()
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
		
		// An alternative here would be to create a Description element on with to hang the
		// hierarchy, call toComponent() on this and extract the InterfaceFaultReference from this.
		// However this inadvertently tests the Description class which is out of scope for this unit test.
		InterfaceFault retrievedFault = ((InterfaceFaultReference)faultReference).getInterfaceFault();
		
		assertEquals("The retrieved InterfaceFault is not that which was set", 
				(InterfaceFault)faultElement, retrievedFault);
	}

	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fFaultReferenceElement, fFaultReferenceComp.toElement());
	}
}
