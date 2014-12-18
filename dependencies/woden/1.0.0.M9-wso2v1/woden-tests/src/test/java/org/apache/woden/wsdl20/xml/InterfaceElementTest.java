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
import org.apache.woden.types.NCName;

/**
 * Unit tests for the InterfaceElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceElementTest extends TestCase {

	// create a parent Description to hang the Interfaces off
	private DescriptionElement fDescriptionElement = null;
	private InterfaceElement fInterfaceElement = null;
	private URI fStyleDefaultURI1 = null;
	private URI fStyleDefaultURI2 = null;
	
	public static Test suite()
	{
	   return new TestSuite(InterfaceElementTest.class);
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
        fStyleDefaultURI1 = new URI("http://www.w3.org/0000/00/apacheStyle");
        fStyleDefaultURI2 = new URI("http://www.w3.org/0000/00/anotherApacheStyle");
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
		fInterfaceElement.setName(new NCName("interfaceName"));
		QName uri = fInterfaceElement.getName();
		assertEquals("Retrieved InterfaceElement name does not match that set -", "interfaceName", uri.toString());
	}
	
	/*
	 * Gets InterfaceElements referenced by the "extends" attribute (optional)
	 */
	public void testGetExtendedInterfaceElementElements() 
	{
		/*
		 * create some InterfaceElements to extend, add them to parent,
		 * access them via :
		 * - getExtendedInterfaceElement()
		 * - getExtendedInterfaceElements()
		 */
		
		// check the default:
		InterfaceElement[] ifeArray = fInterfaceElement.getExtendedInterfaceElements();
		assertEquals("Retrieved Extended InterfaceElement group should be empty if none set -", 0, ifeArray.length);
	
		// create further InterfaceElements and name them
		InterfaceElement xife1 = fDescriptionElement.addInterfaceElement();
		InterfaceElement xife2 = fDescriptionElement.addInterfaceElement();
		xife1.setName(new NCName("extendedIE1"));
		xife2.setName(new NCName("extendedIE2"));
		fInterfaceElement.addExtendedInterfaceName(new QName("extendedIE1"));
		fInterfaceElement.addExtendedInterfaceName(new QName("extendedIE2"));
		
		// getExtendedInterfaceElements()
		ifeArray = fInterfaceElement.getExtendedInterfaceElements();
		assertEquals("Incorrect number of retrieved Extended InterfaceElements -", 2, ifeArray.length);
		// verify object equivalence
		List ifeL = Arrays.asList(ifeArray);
		assertTrue(ifeL.contains(xife1));
		assertTrue(ifeL.contains(xife2));
		
		// getExtendedInterfaceElement()
		InterfaceElement retrievedIfe = fInterfaceElement.getExtendedInterfaceElement(new QName("extendedIE1"));
		assertEquals("Retrieved Extended InterfaceElement unexpected -", xife1, retrievedIfe);
		retrievedIfe = fInterfaceElement.getExtendedInterfaceElement(new QName("randomUnset"));
		assertNull("The name of a non-existent InterfaceElement should not return one -", retrievedIfe);
	}
	
    /*
     * Optional attribute ("extends")
     * - addExtendedInterfaceName() 
     * - getExtendedInterfaceNames()
     * - removeExtendedInterfaceName() 
     */
	public void testAddGetRemoveExtendedInterfaceNames() 
	{	
		// check the default:
		QName[] ifeQnameArray = fInterfaceElement.getExtendedInterfaceNames();
		assertEquals("Retrieved Extended InterfaceElement QName group should be empty if none set -", 0, ifeQnameArray.length);

		// create further InterfaceElements and name them
		InterfaceElement xife1 = fDescriptionElement.addInterfaceElement();
		InterfaceElement xife2 = fDescriptionElement.addInterfaceElement();
		xife1.setName(new NCName("extendedIE1"));
		xife2.setName(new NCName("extendedIE2"));
		
		// addExtendedInterfaceName()
		QName xifeQname1 = new QName("extendedIE1");
		fInterfaceElement.addExtendedInterfaceName(xifeQname1);
		QName xifeQname2 = new QName("extendedIE2");
		fInterfaceElement.addExtendedInterfaceName(xifeQname2);
		assertNotNull("Retrieved Extended InterfaceElement from a valid name expected.", 
				fInterfaceElement.getExtendedInterfaceElement(xifeQname1));
		// Add a qname on a non-existent InterfaceElement
		QName xifeQname3 = new QName("nonExistentExtendedIE");
		fInterfaceElement.addExtendedInterfaceName(xifeQname3);		
		
		// getExtendedInterfaceNames()
		QName[] ifeNames = fInterfaceElement.getExtendedInterfaceNames();
		assertNotNull("Expected an array of QNames.", ifeNames);
		assertEquals("Incorrect number of retrieved Extended InterfaceElement names -", 3, ifeNames.length);
		// verify all names returned
		List ifeL = Arrays.asList(ifeNames);
		assertTrue(ifeL.contains(xifeQname1));
		assertTrue(ifeL.contains(xifeQname2));
		assertTrue(ifeL.contains(xifeQname3));
		// verify that xifeQname3 does not refer to any InterfaceElement
		assertNull("Extended InterfaceElement for name " + xifeQname3 + " unexpected.", 
				fInterfaceElement.getExtendedInterfaceElement(xifeQname3));
		
		// removeExtendedInterfaceName() 
		fInterfaceElement.removeExtendedInterfaceName(xifeQname1);
		fInterfaceElement.removeExtendedInterfaceName(xifeQname3);
		fInterfaceElement.removeExtendedInterfaceName(xifeQname2);
		ifeNames = fInterfaceElement.getExtendedInterfaceNames();
		assertNotNull("Expected an (empty) array of QNames.", ifeNames);
		assertEquals("Incorrect number of retrieved Extended InterfaceElement names -", 0, ifeNames.length);
	}
	
	/*
     * Optional attribute ("styleDefault")
     * styleDefault comprises a list of URIs (IRIs in the spec)
     * - getStyleDefault() returns the list
     * - addStyleDefaultURI() adds to the list
     */
	public void testAddGetStyleDefault() 
	{		
		// check the default:
		URI[] styleDefault = fInterfaceElement.getStyleDefault();
		assertNotNull(styleDefault);
		assertEquals("Retrieved InterfaceElement style default should be empty if none set -", 0, styleDefault.length);
		
		// addStyleDefaultURI() a couple of times
		fInterfaceElement.addStyleDefaultURI(fStyleDefaultURI1);
		fInterfaceElement.addStyleDefaultURI(fStyleDefaultURI2);
		
		// getStyleDefault()
		styleDefault = fInterfaceElement.getStyleDefault();
		assertNotNull(styleDefault);
		assertEquals("Unexpected number of URIs in the styleDefault -", 2, styleDefault.length);
		// check that all added URIs appear in the styleDefault
		List sdL = Arrays.asList(styleDefault);
		assertTrue(sdL.contains(fStyleDefaultURI1));
		assertTrue(sdL.contains(fStyleDefaultURI2));
	}    
	
	/*
     * Optional element ("fault")
     * - addInterfaceFaultElement() 
     * - getInterfaceFaultElement() 
     * - getInterfaceFaultElements() 
     */
	public void testAddGetInterfaceFaultElements() 
	{		
		// check the default:
		InterfaceFaultElement[] iffeArray = fInterfaceElement.getInterfaceFaultElements();
		assertNotNull("Expected an array of InterfaceFaultElements -", iffeArray);
		assertEquals("Retrieved InterfaceFaultElement group should be empty if none set -", 0, iffeArray.length);

		// addInterfaceFaultElement()
		InterfaceFaultElement iffe1 = fInterfaceElement.addInterfaceFaultElement();
		InterfaceFaultElement iffe2 = fInterfaceElement.addInterfaceFaultElement();
		assertNotNull(iffe1);
		assertNotNull(iffe2);

		// getInterfaceFaultElements()
		iffeArray = fInterfaceElement.getInterfaceFaultElements();
		assertNotNull("Expected an array of InterfaceFaultElements -", iffeArray);
		assertEquals("Incorrect number of retrieved InterfaceFaultElements -", 2, iffeArray.length);

		// verify all Fault objects returned
		List iffeL = Arrays.asList(iffeArray);
		assertTrue(iffeL.contains(iffe1));
		assertTrue(iffeL.contains(iffe2));
	
		// getInterfaceFaultElement()
		// name one of them
		iffe1.setName(new NCName("FaultName"));
		InterfaceFaultElement retrievedIffe = fInterfaceElement.getInterfaceFaultElement(new QName("FaultName"));
		assertNotNull(retrievedIffe);
		assertEquals("Retrieved InterfaceFaultElement differs from that expected", iffe1, retrievedIffe);
		// try a non-existent fault - should return null
		retrievedIffe = fInterfaceElement.getInterfaceFaultElement(new QName("nonExistentFault"));
		assertNull(retrievedIffe);
	}    
    
	/*
     * Optional element ("operation")
     * - addInterfaceOperationElement() 
     * - getInterfaceOperationElement() 
     * - getInterfaceOperationElements() 
     */
	public void testAddGetInterfaceOperationElements() 
	{		
		// check the default:
		InterfaceOperationElement[] ifopArray = fInterfaceElement.getInterfaceOperationElements();
		assertNotNull("Expected an array of InterfaceOperationElements -", ifopArray);
		assertEquals("Retrieved InterfaceOperationElement group should be empty if none set -", 0, ifopArray.length);

		// addInterfaceOperationElement()
		InterfaceOperationElement ifop1 = fInterfaceElement.addInterfaceOperationElement();
		InterfaceOperationElement ifop2 = fInterfaceElement.addInterfaceOperationElement();
		assertNotNull(ifop1);
		assertNotNull(ifop2);

		// getInterfaceOperationElements()
		ifopArray = fInterfaceElement.getInterfaceOperationElements();
		assertNotNull("Expected an array of InterfaceOperationElements -", ifopArray);
		assertEquals("Incorrect number of retrieved InterfaceOperationElements -", 2, ifopArray.length);

		// verify all Operation objects returned
		List ifopL = Arrays.asList(ifopArray);
		assertTrue(ifopL.contains(ifop1));
		assertTrue(ifopL.contains(ifop2));
	
		// getInterfaceOperationElement()
		// name one of them
		ifop1.setName(new NCName("OperationName"));
		InterfaceOperationElement retrievedIfop = fInterfaceElement.getInterfaceOperationElement(new QName("OperationName"));
		assertNotNull(retrievedIfop);
		assertEquals("Retrieved InterfaceOperationElement differs from that expected", ifop1, retrievedIfop);
		// try a non-existent operation - should return null
		retrievedIfop = fInterfaceElement.getInterfaceOperationElement(new QName("nonExistentOperation"));
		assertNull(retrievedIfop);
	}   
}
