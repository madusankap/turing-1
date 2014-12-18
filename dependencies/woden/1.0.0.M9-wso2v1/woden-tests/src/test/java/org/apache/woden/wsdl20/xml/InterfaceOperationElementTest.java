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
 * Unit tests for the InterfaceOperationElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */

public class InterfaceOperationElementTest extends TestCase {

	// create a parent Description to hang the Interfaces off
	private DescriptionElement fDescriptionElement = null;
	private InterfaceElement fInterfaceElement = null;
	private InterfaceOperationElement fInterfaceOperationElement = null;
	private URI fStyleURI1 = null;
	private URI fStyleURI2 = null;
	private URI fPattern = null;
	
	public static Test suite()
	{
	   return new TestSuite(InterfaceOperationElementTest.class);
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
        fInterfaceElement = fDescriptionElement.addInterfaceElement();
        fInterfaceOperationElement = fInterfaceElement.addInterfaceOperationElement();
        fStyleURI1 = new URI("http://www.w3.org/0000/00/apacheStyle");
        fStyleURI2 = new URI("http://www.w3.org/0000/00/anotherApacheStyle");
        fPattern = new URI("http://www.w3.org/0000/00/wsdl/in-out");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
    /*
     * Mandatory attribute ("name")
     * - setName() 
     * - getName() 
     */
	public void testGetSetName() 
	{	
		fInterfaceOperationElement.setName(new NCName("interfaceOperationName"));
		QName retrievedName = fInterfaceOperationElement.getName();
		assertEquals("Retrieved InterfaceOperationElement name does not match that set -", "interfaceOperationName", retrievedName.toString());
	}
	
	/*
     * Mandatory attribute ("pattern") (message exchange pattern)
     * - setPattern() 
     * - getPattern() 
     */
	public void testGetSetPattern() 
	{	
		fInterfaceOperationElement.setPattern(fPattern);
		URI uri = fInterfaceOperationElement.getPattern();
		assertEquals("Retrieved InterfaceOperationElement mep does not match that set -", fPattern, uri);
	}
	
	/*
     * Optional attribute ("style")
     * style comprises a list of URIs
     * - getStyle() returns the list
     * - addStyleURI() adds to the list
     * - removeStyleURI() removes from the list
     */
	public void testAddGetRemoveStyle() 
	{		
		// check the default:
		URI[] style = fInterfaceOperationElement.getStyle();
		assertNotNull(style);
		assertEquals("Retrieved InterfaceOperationElement style should be empty if none set -", 0, style.length);
		
		// addStyleURI() a couple of times
		fInterfaceOperationElement.addStyleURI(fStyleURI1);
		fInterfaceOperationElement.addStyleURI(fStyleURI2);
		
		// getStyle()
		style = fInterfaceOperationElement.getStyle();
		assertNotNull(style);
		assertEquals("Unexpected number of URIs in the style -", 2, style.length);
		// check that all added URIs appear in the style
		List sdL = Arrays.asList(style);
		assertTrue(sdL.contains(fStyleURI1));
		assertTrue(sdL.contains(fStyleURI2));
		
		// removeStyleURI()
		fInterfaceOperationElement.removeStyleURI(fStyleURI1);
		fInterfaceOperationElement.removeStyleURI(fStyleURI2);
		style = fInterfaceOperationElement.getStyle();
		assertNotNull(style);
		assertEquals("Unexpected number of URIs in the style -", 0, style.length);
	}   
	
	/* 
     * References to Optional child elements "infault" and "outfault"
     * - addInterfaceFaultReferenceElement() 
     * - getInterfaceFaultReferenceElements()
     * - removeInterfaceFaultReferenceElement() 
     */
	public void testAddGetRemoveInterfaceFaultReferenceElements() 
	{	
		// check the default:
		InterfaceFaultReferenceElement[] ifreArray = fInterfaceOperationElement.getInterfaceFaultReferenceElements();
		assertNotNull("Expected an array of InterfaceFaultReferenceElement.", ifreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be empty if none set -", 0, ifreArray.length);

		// addInterfaceFaultReferenceElement() -  create some InterfaceFaultReferenceElements
		InterfaceFaultReferenceElement ifre1 = fInterfaceOperationElement.addInterfaceFaultReferenceElement();
		InterfaceFaultReferenceElement ifre2 = fInterfaceOperationElement.addInterfaceFaultReferenceElement();
		
		// getInterfaceFaultReferenceElements()
		ifreArray = fInterfaceOperationElement.getInterfaceFaultReferenceElements();
		assertNotNull("Expected an array of InterfaceFaultReferenceElement.", ifreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be same number as those set -", 2, ifreArray.length);
		
		// verify all fault references returned
		List ifreL = Arrays.asList(ifreArray);
		assertTrue(ifreL.contains(ifre1));
		assertTrue(ifreL.contains(ifre2));

		// removeInterfaceFaultReferenceElement() 
		// 1 - attempt to remove an unadded IFRE
		InterfaceFaultReferenceElement ifre3 = null;
		fInterfaceOperationElement.removeInterfaceFaultReferenceElement(ifre3);
		ifreArray = fInterfaceOperationElement.getInterfaceFaultReferenceElements();
		assertNotNull("Expected an array of InterfaceFaultReferenceElement.", ifreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be same number as those set -", 2, ifreArray.length);
		
		// 2- remove all added 
		fInterfaceOperationElement.removeInterfaceFaultReferenceElement(ifre1);
		fInterfaceOperationElement.removeInterfaceFaultReferenceElement(ifre2);
		ifreArray = fInterfaceOperationElement.getInterfaceFaultReferenceElements();
		assertNotNull("Expected an array of InterfaceFaultReferenceElement.", ifreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be empty if all removed -", 0, ifreArray.length);
		
		//3 - attempt to remove previously removed from empty list
		fInterfaceOperationElement.removeInterfaceFaultReferenceElement(ifre2);
		ifreArray = fInterfaceOperationElement.getInterfaceFaultReferenceElements();
		assertNotNull("Expected an array of InterfaceFaultReferenceElement.", ifreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be empty if all removed -", 0, ifreArray.length);
	}
	
	/* 
     * References to Optional child elements "input" and "output"
     * - addInterfaceMessageReferenceElement() 
     * - getInterfaceMessageReferenceElements()
     * - removeInterfaceMessageReferenceElement() 
     */
	public void testAddGetRemoveInterfaceMessageReferenceElements() 
	{
		// check the default:
		InterfaceMessageReferenceElement[] imreArray = fInterfaceOperationElement.getInterfaceMessageReferenceElements();
		assertNotNull("Expected an array of InterfaceMessageReferenceElement.", imreArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be empty if none set -", 0, imreArray.length);

		// addInterfaceMessageReferenceElement() -  create some addInterfaceMessageReferenceElements
		InterfaceMessageReferenceElement imre1 = fInterfaceOperationElement.addInterfaceMessageReferenceElement();
		InterfaceMessageReferenceElement imre2 = fInterfaceOperationElement.addInterfaceMessageReferenceElement();
		
		// getInterfaceMessageReferenceElements()
		imreArray = fInterfaceOperationElement.getInterfaceMessageReferenceElements();
		assertNotNull("Expected an array of InterfaceMessageReferenceElement.", imreArray);
		assertEquals("Retrieved InterfaceMessageReferenceElement group should be same number as those set -", 2, imreArray.length);
		
		// verify all fault references returned
		List imreL = Arrays.asList(imreArray);
		assertTrue(imreL.contains(imre1));
		assertTrue(imreL.contains(imre2));

		// removeInterfaceMessageReferenceElement() 
		// 1 - attempt to remove an unadded IMRE
		InterfaceMessageReferenceElement imre3 = null;
		fInterfaceOperationElement.removeInterfaceMessageReferenceElement(imre3);
		imreArray = fInterfaceOperationElement.getInterfaceMessageReferenceElements();
		assertNotNull("Expected an array of InterfaceMessageReferenceElement.", imreArray);
		assertEquals("Retrieved InterfaceMessageReferenceElement group should be same number as those set -", 2, imreArray.length);
		
		// 2- remove all added 
		fInterfaceOperationElement.removeInterfaceMessageReferenceElement(imre1);
		fInterfaceOperationElement.removeInterfaceMessageReferenceElement(imre2);
		imreArray = fInterfaceOperationElement.getInterfaceMessageReferenceElements();
		assertNotNull("Expected an array of InterfaceMessageReferenceElement.", imreArray);
		assertEquals("Retrieved InterfaceMessageReferenceElement group should be empty if all removed -", 0, imreArray.length);
		
		//3 - attempt to remove previously removed from empty list
		fInterfaceOperationElement.removeInterfaceMessageReferenceElement(imre2);
		imreArray = fInterfaceOperationElement.getInterfaceMessageReferenceElements();
		assertNotNull("Expected an array of InterfaceMessageReferenceElement.", imreArray);
		assertEquals("Retrieved InterfaceMessageReferenceElement group should be empty if all removed -", 0, imreArray.length);
	}
}
