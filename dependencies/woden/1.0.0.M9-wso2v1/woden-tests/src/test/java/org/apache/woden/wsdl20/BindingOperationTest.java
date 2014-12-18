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
import org.apache.woden.wsdl20.xml.BindingFaultReferenceElement;
import org.apache.woden.wsdl20.xml.BindingMessageReferenceElement;
import org.apache.woden.wsdl20.xml.BindingOperationElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;

/**
 * Unit tests for the BindingOperation class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */

public class BindingOperationTest extends TestCase {

	// create a parent Description to hang the Binding hierarchy off
	private DescriptionElement fDescriptionElement = null;
	private BindingElement fBindingElement = null;
	private BindingOperationElement fBindingOperationElement = null;
	private BindingOperation fBindingOperation = null;

	public static Test suite()
	{
	   return new TestSuite(BindingOperationTest.class);
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
        fBindingOperationElement = fBindingElement.addBindingOperationElement();
        fBindingOperation = (BindingOperation) fBindingOperationElement;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/* 
     * References to Optional child elements "infault" and "outfault" in the infoset
     * - getBindingFaultReferences()
     */
	public void testGetBindingFaultReferences() 
	{	
		// create some BindingFaultReferenceElements
		BindingFaultReferenceElement bfre1 = fBindingOperationElement.addBindingFaultReferenceElement();
		BindingFaultReferenceElement bfre2 = fBindingOperationElement.addBindingFaultReferenceElement();
	
		fDescriptionElement.toComponent();
		
		// getBindingFaultReferences()
		BindingFaultReference[] bfrArray = fBindingOperation.getBindingFaultReferences();
		assertNotNull("Expected an array of BindingFaultReference.", bfrArray);
		assertEquals("Retrieved BindingFaultReference group should be same number as those set -", 2, bfrArray.length);
		
		// verify all fault references returned
		List bfreL = Arrays.asList(bfrArray);
		assertTrue(bfreL.contains(bfre1));
		assertTrue(bfreL.contains(bfre2));
	}
	
	/* 
     * References to Optional child elements "input" and "output" int the infoset
     * - getBindingMessageReferences()
     */
	public void testGetBindingMessageReferences() 
	{
		// create some BindingMessageReferenceElements
		BindingMessageReferenceElement bmre1 = fBindingOperationElement.addBindingMessageReferenceElement();
		BindingMessageReferenceElement bmre2 = fBindingOperationElement.addBindingMessageReferenceElement();
		
		fDescriptionElement.toComponent();
		
		// getBindingMessageReferences()
		BindingMessageReference[] bmrArray = fBindingOperation.getBindingMessageReferences();
		assertNotNull("Expected an array of BindingMessageReference.", bmrArray);
		assertEquals("Retrieved BindingMessageReference group should be same number as those set -", 2, bmrArray.length);
		
		// verify all fault references returned
		List bmreL = Arrays.asList(bmrArray);
		assertTrue(bmreL.contains(bmre1));
		assertTrue(bmreL.contains(bmre2));
	}
		
	/* 
     * Utility method to find the InterfaceOperation referenced by the (Mandatory) "ref" attribute in the infoset
     * - getInterfaceOperation() 
     */
	public void testGetInterfaceOperation() 
	{		
		// Create and name an Interface Element	
		InterfaceElement interfaceElement = fDescriptionElement.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));

		// Create a binding from the description 
		fBindingElement = fDescriptionElement.addBindingElement();
		fBindingElement.setInterfaceName(new QName("interface1"));
		
		//Create and name an Interface Operation Element
		InterfaceOperationElement intOpElement = interfaceElement.addInterfaceOperationElement();
		intOpElement.setName(new NCName("interfaceOperation1"));
		
		fBindingOperationElement = fBindingElement.addBindingOperationElement();
		fBindingOperationElement.setRef(new QName("interfaceOperation1"));
		
		Description descComp = fDescriptionElement.toComponent();
		descComp.getBindings(); // this triggers setting the link to description in the binding
		
        fBindingOperation = (BindingOperation) fBindingOperationElement;
		
        InterfaceOperation retrievedIntOp = fBindingOperation.getInterfaceOperation();
		assertEquals("Retrieved Interface Element was not that expected -", intOpElement, retrievedIntOp);
	}
	
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fBindingOperationElement, fBindingOperation.toElement());
	}
}
