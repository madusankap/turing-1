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
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.BindingFaultElement;
import org.apache.woden.wsdl20.xml.BindingOperationElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;

/**
 * Unit tests for the Binding class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingTest extends TestCase {

	// create a parent Description to hang the Bindings off
	private DescriptionElement fDescriptionElement = null;
	private BindingElement fBindingElement = null;
	private Binding fBinding = null;
	private URI fTypeURI = null;
	
	public static Test suite()
	{
	   return new TestSuite(BindingTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

        fDescriptionElement = factory.newDescription();
        fBindingElement = fDescriptionElement.addBindingElement();
        fBindingElement.setName(new NCName("binding"));
        //fBinding = (Binding) fBindingElement;
        fTypeURI = new URI("http://www.w3.org/0000/00/apacheType");
        fBinding = fDescriptionElement.toComponent().getBinding(new QName("binding"));
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	/*  (optional) "interface" attribute 
     * - getInterfaceElement() 
     */
	public void testGetInterface() 
	{		
		// check the default:
		Interface retrievedInterface = fBinding.getInterface();
		assertNull("Retrieved Interface should be null if none set -", retrievedInterface);

        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

		// Create and name an Interface Element
		DescriptionElement desc = factory.newDescription();
		InterfaceElement interfaceElement = desc.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		// Create a binding from the description  
		fBindingElement = desc.addBindingElement();
		fBindingElement.setName(new NCName("binding1"));
		desc.toComponent();
		Binding binding = desc.toComponent().getBinding(new QName("binding1"));
	
		// getInterface() - interface attribute unspecified, but hierarchy in place:
		retrievedInterface = binding.getInterface();
		assertNull("Retrieved Interface should be null if interface attribute unspecified -", retrievedInterface);
		
		// getInterface() - interface attribute specified, and hierarchy in place:
		// Set the "interface" attribute to reference the new Interface Element
		// (have to recreate whole desc hierarchy as toComponent() will not rerun if already run!)
		desc = factory.newDescription();
		interfaceElement = desc.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		fBindingElement = desc.addBindingElement();
		fBindingElement.setName(new NCName("binding1"));
		fBindingElement.setInterfaceName(new QName("interface1"));
		
		//fDescriptionElement.toComponent();
		
        binding = desc.toComponent().getBinding(new QName("binding1"));
		//fBinding = (Binding)fBindingElement;
		
		retrievedInterface = binding.getInterface();
		assertEquals("Retrieved Interface was not that expected -", interfaceElement, retrievedInterface);
	}   
	
    /*
     * Mandatory attribute ("name")
     * - getName() 
     */
	public void testGetName() 
	{	
		fBindingElement.setName(new NCName("BindingName"));
		QName uri = fBinding.getName();
		assertNotNull(uri);
		assertEquals("Retrieved Binding name does not match that set -", "BindingName", uri.toString());
	}

    /*
     * Mandatory attribute ("type")
     * - getType() 
     */
	public void testGetType() 
	{	
		fBindingElement.setType(fTypeURI);
		URI uri = fBinding.getType();
		assertEquals("Retrieved Binding type attribute does not match that set -", fTypeURI, uri);
	}
	
	/*
     * Optional element ("fault")
     * - getBindingFaults() 
     */
	public void testGetBindingFaults() 
	{		
		// check the default:
		BindingFault[] bfArray = fBinding.getBindingFaults();
		assertNotNull("Expected an array of BindingFaults -", bfArray);
		assertEquals("Retrieved BindingFaultElement group should be empty if none set -", 0, bfArray.length);

		// addBindingFaultElement()
		BindingFaultElement bfe1 = fBindingElement.addBindingFaultElement();
		BindingFaultElement bfe2 = fBindingElement.addBindingFaultElement();

		// getBindingFaultElements()
		bfArray = fBinding.getBindingFaults();
		assertNotNull("Expected an array of BindingFaults -", bfArray);
		assertEquals("Incorrect number of retrieved BindingFaults -", 2, bfArray.length);

		// verify all Fault objects returned
		List bfeL = Arrays.asList(bfArray);
		assertTrue(bfeL.contains(bfe1));
		assertTrue(bfeL.contains(bfe2));
	}    
    
	/*
     * Optional element ("operation") 
     * - getBindingOperations() 
     */
	public void testGetBindingOperations() 
	{		
		// check the default:
		BindingOperation[] bopArray = fBinding.getBindingOperations();
		assertNotNull("Expected an array of BindingOperations -", bopArray);
		assertEquals("Retrieved BindingOperation group should be empty if none set -", 0, bopArray.length);

		// addBindingOperationElement()
		BindingOperationElement bop1 = fBindingElement.addBindingOperationElement();
		BindingOperationElement bop2 = fBindingElement.addBindingOperationElement();

		// getBindingOperations()
		bopArray = fBinding.getBindingOperations();
		assertNotNull("Expected an array of BindingOperation -", bopArray);
		assertEquals("Incorrect number of retrieved BindingOperations -", 2, bopArray.length);

		// verify all Operation objects returned
		List ifopL = Arrays.asList(bopArray);
		assertTrue(ifopL.contains(bop1));
		assertTrue(ifopL.contains(bop2));
	} 
	
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fBindingElement, fBinding.toElement());
	}

}
