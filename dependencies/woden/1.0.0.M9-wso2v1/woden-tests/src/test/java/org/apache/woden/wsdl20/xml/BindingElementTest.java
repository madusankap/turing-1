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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;

/**
 * Unit tests for the BindingElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingElementTest extends TestCase {

	// create a parent Description to hang the Bindings off
	private DescriptionElement fDescriptionElement = null;
	private BindingElement fBindingElement = null;
	private URI fTypeURI = null;
    private WSDLFactory fFactory = null;
	
	public static Test suite()
	{
	   return new TestSuite(BindingElementTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();

        try {
            fFactory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        fDescriptionElement = fFactory.newDescription();
        fBindingElement = fDescriptionElement.addBindingElement();
        fTypeURI = new URI("http://www.w3.org/0000/00/apacheType");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
    /*
     * Optional attribute ("interface")
     * - setInterfaceName() 
     * - getInterfaceName() 
     */
	public void testGetSetInterfaceName() 
	{	
		// check the default
		QName uri = fBindingElement.getInterfaceName();
		assertNull("Retrieved BindingElement interface name should be null if none set -", uri);
		
		fBindingElement.setInterfaceName(new QName("InterfaceName"));
		uri = fBindingElement.getInterfaceName();
		assertNotNull(uri);
		assertEquals("Retrieved BindingElement interface name does not match that set -", "InterfaceName", uri.toString());
	}
	
	/* Utility method to obtain the InterfaceElement referenced by the (optional) "interface" attribute 
     * - getInterfaceElement() 
     */
	public void testGetInterfaceElement() 
	{		
		// check the default:
		InterfaceElement retrievedInterfaceElement = fBindingElement.getInterfaceElement();
		assertNull("Retrieved Interface Element should be null if none set -", retrievedInterfaceElement);

		// Create and name an Interface Element
		DescriptionElement desc = fFactory.newDescription();
		InterfaceElement interfaceElement = desc.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		// Create a binding from the description 
		fBindingElement = desc.addBindingElement();
	
		// getInterfaceElement() - interface attribute unspecified, but hierarchy in place:
		retrievedInterfaceElement = fBindingElement.getInterfaceElement();
		assertNull("Retrieved Interface Element should be null if interface attribute unspecified -", retrievedInterfaceElement);
		
		// getInterfaceElement() - interface attribute specified, and hierarchy in place:
		// Set the "interface" attribute to reference the new Interface Element
		fBindingElement.setInterfaceName(new QName("interface1"));
		retrievedInterfaceElement = fBindingElement.getInterfaceElement();
		assertEquals("Retrieved Interface Element was not that expected -", interfaceElement, retrievedInterfaceElement);
	}   
	
    /*
     * Mandatory attribute ("name")
     * - setName() 
     * - getName() 
     */
	public void testGetSetName() 
	{	
		fBindingElement.setName(new NCName("BindingName"));
		QName uri = fBindingElement.getName();
		assertNotNull(uri);
		assertEquals("Retrieved BindingElement name does not match that set -", "BindingName", uri.toString());
	}

    /*
     * Mandatory attribute ("type")
     * - setType() 
     * - getType() 
     */
	public void testGetSetType() 
	{	
		fBindingElement.setType(fTypeURI);
		URI uri = fBindingElement.getType();
		assertEquals("Retrieved BindingElement type attribute does not match that set -", fTypeURI, uri);
	}
	
	
	/*
     * Optional element ("fault")
     * - addBindingFaultElement() 
     * - getBindingFaultElements() 
     */
	public void testAddGetBindingFaultElements() 
	{		
		// check the default:
		BindingFaultElement[] bfeArray = fBindingElement.getBindingFaultElements();
		assertNotNull("Expected an array of BindingFaultElements -", bfeArray);
		assertEquals("Retrieved BindingFaultElement group should be empty if none set -", 0, bfeArray.length);

		// addBindingFaultElement()
		BindingFaultElement bfe1 = fBindingElement.addBindingFaultElement();
		BindingFaultElement bfe2 = fBindingElement.addBindingFaultElement();
		assertNotNull(bfe1);
		assertNotNull(bfe2);

		// getBindingFaultElements()
		bfeArray = fBindingElement.getBindingFaultElements();
		assertNotNull("Expected an array of BindingFaultElements -", bfeArray);
		assertEquals("Incorrect number of retrieved BindingFaultElements -", 2, bfeArray.length);

		// verify all Fault objects returned
		List bfeL = Arrays.asList(bfeArray);
		assertTrue(bfeL.contains(bfe1));
		assertTrue(bfeL.contains(bfe2));
	}    
    
	/*
     * Optional element ("operation")
     * - addBindingOperationElement() 
     * - getBindingOperationElements() 
     */
	public void testAddGetBindingOperationElements() 
	{		
		// check the default:
		BindingOperationElement[] bopArray = fBindingElement.getBindingOperationElements();
		assertNotNull("Expected an array of BindingOperationElements -", bopArray);
		assertEquals("Retrieved BindingOperationElement group should be empty if none set -", 0, bopArray.length);

		// addBindingOperationElement()
		BindingOperationElement bop1 = fBindingElement.addBindingOperationElement();
		BindingOperationElement bop2 = fBindingElement.addBindingOperationElement();
		assertNotNull(bop1);
		assertNotNull(bop2);

		// getBindingOperationElements()
		bopArray = fBindingElement.getBindingOperationElements();
		assertNotNull("Expected an array of BindingOperationElements -", bopArray);
		assertEquals("Incorrect number of retrieved BindingOperationElements -", 2, bopArray.length);

		// verify all Operation objects returned
		List ifopL = Arrays.asList(bopArray);
		assertTrue(ifopL.contains(bop1));
		assertTrue(ifopL.contains(bop2));
	}   
}
