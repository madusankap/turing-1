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
 * Unit tests for the BindingOperationElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */

public class BindingOperationElementTest extends TestCase {

	// create a parent Description to hang the Bindings off
	private DescriptionElement fDescriptionElement = null;
	private BindingElement fBindingElement = null;
	private BindingOperationElement fBindingOperationElement = null;
	private final String BOP_NAME = "BindingOperationName";
    private WSDLFactory fFactory = null;

	public static Test suite()
	{
	   return new TestSuite(BindingOperationElementTest.class);
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
        fBindingOperationElement = fBindingElement.addBindingOperationElement();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
    /*
     * Mandatory attribute ("ref")
     * - setRef() 
     * - getRef() 
     */
	public void testGetSetRef() 
	{	
		fBindingOperationElement.setRef(new QName(BOP_NAME));
		QName retrievedName = fBindingOperationElement.getRef();
		assertEquals("Retrieved BindingOperationElement name does not match that set -", BOP_NAME, retrievedName.toString());
	}
	
	/* 
     * References to Optional child elements "infault" and "outfault"
     * - addBindingFaultReferenceElement() 
     * - getBindingFaultReferenceElements()
     * - removeBindingFaultReferenceElement() 
     */
	public void testAddGetRemoveBindingFaultReferenceElements() 
	{	
		// check the default:
		BindingFaultReferenceElement[] bfreArray = fBindingOperationElement.getBindingFaultReferenceElements();
		assertNotNull("Expected an array of BindingFaultReferenceElement.", bfreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be empty if none set -", 0, bfreArray.length);

		// addBindingFaultReferenceElement() -  create some BindingFaultReferenceElements
		BindingFaultReferenceElement bfre1 = fBindingOperationElement.addBindingFaultReferenceElement();
		BindingFaultReferenceElement bfre2 = fBindingOperationElement.addBindingFaultReferenceElement();
		
		// getBindingFaultReferenceElements()
		bfreArray = fBindingOperationElement.getBindingFaultReferenceElements();
		assertNotNull("Expected an array of BindingFaultReferenceElement.", bfreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be same number as those set -", 2, bfreArray.length);
		
		// verify all fault references returned
		List bfreL = Arrays.asList(bfreArray);
		assertTrue(bfreL.contains(bfre1));
		assertTrue(bfreL.contains(bfre2));

		// removeBindingFaultReferenceElement() 
		// 1 - attempt to remove an unadded BFRE
		BindingFaultReferenceElement bfre3 = null;
		fBindingOperationElement.removeBindingFaultReferenceElement(bfre3);
		bfreArray = fBindingOperationElement.getBindingFaultReferenceElements();
		assertNotNull("Expected an array of BindingFaultReferenceElement.", bfreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be same number as those set -", 2, bfreArray.length);
		
		// 2- remove all added 
		fBindingOperationElement.removeBindingFaultReferenceElement(bfre1);
		fBindingOperationElement.removeBindingFaultReferenceElement(bfre2);
		bfreArray = fBindingOperationElement.getBindingFaultReferenceElements();
		assertNotNull("Expected an array of BindingFaultReferenceElement.", bfreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be empty if all removed -", 0, bfreArray.length);
		
		//3 - attempt to remove previously removed from empty list
		fBindingOperationElement.removeBindingFaultReferenceElement(bfre2);
		bfreArray = fBindingOperationElement.getBindingFaultReferenceElements();
		assertNotNull("Expected an array of BindingFaultReferenceElement.", bfreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be empty if all removed -", 0, bfreArray.length);
	}
	
	/* 
     * References to Optional child elements "input" and "output"
     * - addBindingMessageReferenceElement() 
     * - getBindingMessageReferenceElements()
     * - removeBindingMessageReferenceElement() 
     */
	public void testAddGetRemoveBindingMessageReferenceElements() 
	{
		// check the default:
		BindingMessageReferenceElement[] bmreArray = fBindingOperationElement.getBindingMessageReferenceElements();
		assertNotNull("Expected an array of BindingMessageReferenceElement.", bmreArray);
		assertEquals("Retrieved BindingFaultReferenceElement group should be empty if none set -", 0, bmreArray.length);

		// addBindingMessageReferenceElement() -  create some addBindingMessageReferenceElements
		BindingMessageReferenceElement bmre1 = fBindingOperationElement.addBindingMessageReferenceElement();
		BindingMessageReferenceElement bmre2 = fBindingOperationElement.addBindingMessageReferenceElement();
		
		// getBindingMessageReferenceElements()
		bmreArray = fBindingOperationElement.getBindingMessageReferenceElements();
		assertNotNull("Expected an array of BindingMessageReferenceElement.", bmreArray);
		assertEquals("Retrieved BindingMessageReferenceElement group should be same number as those set -", 2, bmreArray.length);
		
		// verify all fault references returned
		List bmreL = Arrays.asList(bmreArray);
		assertTrue(bmreL.contains(bmre1));
		assertTrue(bmreL.contains(bmre2));

		// removeBindingMessageReferenceElement() 
		// 1 - attempt to remove an unadded BMRE
		BindingMessageReferenceElement bmre3 = null;
		fBindingOperationElement.removeBindingMessageReferenceElement(bmre3);
		bmreArray = fBindingOperationElement.getBindingMessageReferenceElements();
		assertNotNull("Expected an array of BindingMessageReferenceElement.", bmreArray);
		assertEquals("Retrieved BindingMessageReferenceElement group should be same number as those set -", 2, bmreArray.length);
		
		// 2- remove all added 
		fBindingOperationElement.removeBindingMessageReferenceElement(bmre1);
		fBindingOperationElement.removeBindingMessageReferenceElement(bmre2);
		bmreArray = fBindingOperationElement.getBindingMessageReferenceElements();
		assertNotNull("Expected an array of BindingMessageReferenceElement.", bmreArray);
		assertEquals("Retrieved BindingMessageReferenceElement group should be empty if all removed -", 0, bmreArray.length);
		
		//3 - attempt to remove previously removed from empty list
		fBindingOperationElement.removeBindingMessageReferenceElement(bmre2);
		bmreArray = fBindingOperationElement.getBindingMessageReferenceElements();
		assertNotNull("Expected an array of BindingMessageReferenceElement.", bmreArray);
		assertEquals("Retrieved BindingMessageReferenceElement group should be empty if all removed -", 0, bmreArray.length);
	}
	
	/* 
     * Utility method to find the InterfaceOperation referenced by the (Mandatory) "ref" attribute
     * - getInterfaceOperationElement() 
     */
	public void testGetInterfaceOperationElement() 
	{		
		// check the default:
		InterfaceOperationElement retrievedIntOpElement = fBindingOperationElement.getInterfaceOperationElement();
		assertNull("Retrieved Interface Operation Element should be null if none set -", retrievedIntOpElement);

		// Create and name an Interface Element
		DescriptionElement desc = fFactory.newDescription();
		InterfaceElement interfaceElement = desc.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		// Create a binding from the description 
		fBindingElement = desc.addBindingElement();
		fBindingElement.setInterfaceName(new QName("interface1"));
		
		//Create and name an Interface Operation Element
		InterfaceOperationElement intOpElement = interfaceElement.addInterfaceOperationElement();
		intOpElement.setName(new NCName("interfaceOperation1"));
		
		fBindingOperationElement = fBindingElement.addBindingOperationElement();
		fBindingOperationElement.setRef(new QName("interfaceOperation1"));

		retrievedIntOpElement = fBindingOperationElement.getInterfaceOperationElement();
		assertEquals("Retrieved Interface Element was not that expected -", intOpElement, retrievedIntOpElement);
	}  
}
