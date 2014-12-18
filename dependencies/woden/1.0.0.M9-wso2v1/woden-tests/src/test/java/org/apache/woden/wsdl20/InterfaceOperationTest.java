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
import org.apache.woden.internal.ErrorReporterImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.extensions.ExtensionRegistry;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceMessageReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;

/**
 * Unit tests for the InterfaceOperation class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceOperationTest extends TestCase {

	// create a parent Description to hang the Interfaces off
	private DescriptionElement fDescriptionElement = null;
	private Description fDescription = null;
	private InterfaceElement fInterfaceElement = null;
	private InterfaceOperationElement fInterfaceOperationElement = null;
	private InterfaceOperation fInterfaceOperation = null;
	private final String INTF_NAME = "interfaceOperationName";
	private final String INTOP_NAME = "faultName";
	private URI fPattern = null;
	private URI fStyleURI1 = null;
	private URI fStyleURI2 = null;

	public static Test suite()
	{
	   return new TestSuite(InterfaceOperationTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        super.setUp();
        fDescriptionElement = factory.newDescription();
        fInterfaceElement = fDescriptionElement.addInterfaceElement();
        fInterfaceElement.setName(new NCName(INTF_NAME));
        fInterfaceOperationElement = fInterfaceElement.addInterfaceOperationElement();
        fInterfaceOperationElement.setName(new NCName(INTOP_NAME));

        fPattern = new URI("http://www.w3.org/0000/00/wsdl/in-out");       
        fInterfaceOperationElement.setPattern(fPattern);
        
        fDescription = fDescriptionElement.toComponent();

		Interface iface = fDescription.getInterface(new QName(INTF_NAME));
		fInterfaceOperation = iface.getInterfaceOperation(new QName(INTOP_NAME));
		
        fStyleURI1 = new URI("http://www.w3.org/0000/00/apacheStyle");
        fStyleURI2 = new URI("http://www.w3.org/0000/00/anotherApacheStyle");      
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
     * - getName() 
     */
	public void testGetName() 
	{	
		QName retrievedName = fInterfaceOperation.getName();
		assertEquals("Retrieved InterfaceOperation name does not match that set -", INTOP_NAME, retrievedName.toString());
	}

	/*
     * Mandatory attribute ("pattern") (message exchange pattern)
     * - getMessageExchangePattern() 
     */
	public void testGetMessageExchangePattern() 
	{	
		URI uri = fInterfaceOperation.getMessageExchangePattern();
		assertEquals("Retrieved InterfaceOperation mep does not match that set -", fPattern, uri);
	}
	
	/*
     * Optional attribute ("style")
     * style comprises a list of URIs
     * - getStyle() returns the list
     */
	public void testGetStyle() 
	{		
		// check the default:
		URI[] style = fInterfaceOperation.getStyle();
		assertNotNull(style);
		assertEquals("Retrieved InterfaceOperation style should be empty if none set -", 0, style.length);
		
		// add some uri's a couple of times
		fInterfaceOperationElement.addStyleURI(fStyleURI1);
		fInterfaceOperationElement.addStyleURI(fStyleURI2);
		fDescription = fDescriptionElement.toComponent();
		
		// getStyle()
		style = fInterfaceOperation.getStyle();
		assertNotNull(style);
		assertEquals("Unexpected number of URIs in the style -", 2, style.length);
		// check that all added URIs appear in the style
		List sdL = Arrays.asList(style);
		assertTrue(sdL.contains(fStyleURI1));
		assertTrue(sdL.contains(fStyleURI2));
	}   
	
	/* 
     * References to Optional child elements "infault" and "outfault"
     * - getInterfaceFaultReferences()
     */
	public void testGetInterfaceFaultReferences() 
	{	
		// check the default:
		InterfaceFaultReference[] ifrArray = fInterfaceOperation.getInterfaceFaultReferences();
		assertNotNull("Expected an array of InterfaceFaultReference.", ifrArray);
		assertEquals("Retrieved InterfaceFaultReferenceElement group should be empty if none set -", 0, ifrArray.length);

		// create some InterfaceFaultReferenceElements
		InterfaceFaultReferenceElement ifr1 = fInterfaceOperationElement.addInterfaceFaultReferenceElement();
		InterfaceFaultReferenceElement ifr2 = fInterfaceOperationElement.addInterfaceFaultReferenceElement();
		fDescription = fDescriptionElement.toComponent();
		
		// getInterfaceFaultReferenceElements()
		ifrArray = fInterfaceOperation.getInterfaceFaultReferences();
		assertNotNull("Expected an array of InterfaceFaultReference.", ifrArray);
		assertEquals("Retrieved InterfaceFaultReference group should be same number as those set -", 2, ifrArray.length);
		
		// verify all fault references returned
		List ifreL = Arrays.asList(ifrArray);
		assertTrue(ifreL.contains(ifr1));
		assertTrue(ifreL.contains(ifr2));
	}
	
	/* 
     * References to (Optional) child elements "input" and "output"
     * - getInterfaceMessageReferences()
     */
	public void testGetInterfaceMessageReferences() 
	{
		// check the default:
		InterfaceMessageReference[] imrArray = fInterfaceOperation.getInterfaceMessageReferences();
		assertNotNull("Expected an array of InterfaceMessageReference.", imrArray);
		assertEquals("Retrieved InterfaceFaultReference group should be empty if none set -", 0, imrArray.length);

		// create some InterfaceMessageReferenceElements
		InterfaceMessageReferenceElement imre1 = fInterfaceOperationElement.addInterfaceMessageReferenceElement();
		InterfaceMessageReferenceElement imre2 = fInterfaceOperationElement.addInterfaceMessageReferenceElement();
		fDescription = fDescriptionElement.toComponent();
		
		// getInterfaceMessageReferences()
		imrArray = fInterfaceOperation.getInterfaceMessageReferences();
		assertNotNull("Expected an array of InterfaceMessageReference.", imrArray);
		assertEquals("Retrieved InterfaceMessageReference group should be same number as those set -", 2, imrArray.length);
		
		// verify all fault references returned
		List imreL = Arrays.asList(imrArray);
		assertTrue(imreL.contains(imre1));
		assertTrue(imreL.contains(imre2));
	}
	
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fInterfaceOperationElement, fInterfaceOperation.toElement());
	}

}
