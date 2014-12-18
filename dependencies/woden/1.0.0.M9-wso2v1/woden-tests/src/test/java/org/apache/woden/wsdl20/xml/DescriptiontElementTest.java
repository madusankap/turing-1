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
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.extensions.ExtensionRegistry;

/**
 * Unit tests for the DescriptionElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class DescriptiontElementTest extends TestCase {

	private DescriptionElement fDescriptionElement = null;
    private String fPrefix1 = "prefix1";
    private String fPrefix2 = "prefix2";
	private URI fNamespace1 = null;	
	private URI fNamespace2 = null;	
	private URI fDocBaseUri = null;
    private WSDLFactory fFactory = null;
	
	public static Test suite()
	{
	   return new TestSuite(DescriptiontElementTest.class);
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
		fNamespace1 = new URI("http://apache.org/namespaceURIa");
		fNamespace2 = new URI("http://apache.org/namespaceURIb");
		fDocBaseUri = new URI("http://apache.org/documentbaseURI");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	public void testSetGetDocumentBaseURI() {
		fDescriptionElement.setDocumentBaseURI(fDocBaseUri);
		URI retrievedUri = fDescriptionElement.getDocumentBaseURI();
		assertEquals("Retrieved document base URI differs from that set", fDocBaseUri, retrievedUri);

	}

	public void testSetGetTargetNamespace() {
		fDescriptionElement.setTargetNamespace(fNamespace1);
		URI retrievedTNS = fDescriptionElement.getTargetNamespace();
		assertEquals("Retrieved target Namespace URI differs from that set", fNamespace1, retrievedTNS);
	}

	public void testAddGetImportElements() {
		ImportElement importElement1 = fDescriptionElement.addImportElement();
		ImportElement importElement2 = fDescriptionElement.addImportElement();
		ImportElement[] imports = fDescriptionElement.getImportElements();
		assertEquals("Expected 2 import elements", 2, imports.length);
	}

	public void testAddGetIncludeElements() {
		IncludeElement includeElement1 = fDescriptionElement.addIncludeElement();
		IncludeElement includeElement2 = fDescriptionElement.addIncludeElement();
		IncludeElement[] includes = fDescriptionElement.getIncludeElements();
		assertEquals("Expected 2 include elements", 2, includes.length);
	}

	public void testAddGetInterfaceElements() {
		InterfaceElement interfaceElement1 = fDescriptionElement.addInterfaceElement();
		InterfaceElement interfaceElement2 = fDescriptionElement.addInterfaceElement();
		InterfaceElement[] interfaces = fDescriptionElement.getInterfaceElements();
		assertEquals("Expected 2 interface elements", 2, interfaces.length);
	}

	public void testAddGetBindingElements() {
		BindingElement bindingElement1 = fDescriptionElement.addBindingElement();
		BindingElement bindingElement2 = fDescriptionElement.addBindingElement();
		BindingElement[] bindings = fDescriptionElement.getBindingElements();
		assertEquals("Expected 2 binding elements", 2, bindings.length);
	}

	public void testAddGetServiceElements() {
		ServiceElement serviceElement1 = fDescriptionElement.addServiceElement();
		ServiceElement serviceElement2 = fDescriptionElement.addServiceElement();
		ServiceElement[] services = fDescriptionElement.getServiceElements();
		assertEquals("Expected 2 service elements", 2, services.length);
	}

	/*
	 * Call the method once to create a new TypesElement and check that its parent is the
	 * DescriptionElement under test.
	 * Call the method again and ensure the same TypesElement object is returned.
	 */
	public void testGetTypesElement() {
		// check first getTypesElement invocation...
		TypesElement typesElement = fDescriptionElement.getTypesElement();
		assertNull("Method returned TypesElement but expected null", typesElement);
		
		// now create a new TypesElement
		try {
		    typesElement = fDescriptionElement.addTypesElement();
		} catch (WSDLException e) {
		    fail("Method could not create a new TypesElement as one already exists.");
		}
		assertNotNull("Method returned null but expected a TypesElement", typesElement);
		
		if (typesElement != null) {
			assertSame("Expected DescriptionElement to be parent of the TypesElement",
					typesElement.getParentElement(), fDescriptionElement);
		}	
		// check subsequent getTypesElement invocation...
		assertSame(typesElement, fDescriptionElement.getTypesElement());
	}

	public void testToComponent() {
		Description descComponent = fDescriptionElement.toComponent();
		assertNotNull("Null component object model unexpected", descComponent);
	}
}
