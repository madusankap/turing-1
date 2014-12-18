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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.schema.InlinedSchemaImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.ElementDeclarationImpl;
import org.apache.woden.internal.wsdl20.TypeDefinitionImpl;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.ServiceElement;
import org.apache.woden.wsdl20.xml.TypesElement;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Unit tests for the Description component API.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class DescriptionTest extends TestCase {

	private Description fDescription = null;
	private DescriptionElement fDescriptionElement = null;
	private BindingElement fBindingElement = null;
	private InterfaceElement fInterfaceElement = null;
	private ServiceElement fServiceElement = null;
	
	public static Test suite()
	{
	   return new TestSuite(DescriptionTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        WSDLFactory factory = WSDLFactory.newInstance();
    	fDescriptionElement = factory.newDescription();
    	fBindingElement = fDescriptionElement.addBindingElement();
    	fBindingElement.setName(new NCName("bindingName"));
    	fInterfaceElement = fDescriptionElement.addInterfaceElement();
    	fInterfaceElement.setName(new NCName("interfaceName"));
    	fServiceElement = fDescriptionElement.addServiceElement();
    	fServiceElement.setName(new NCName("serviceName"));

        //Create a test schema
        InlinedSchema schema = new InlinedSchemaImpl();
        XmlSchema xs1 = null;
        URI schemaNS = null;

        String schemaString = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\">"
                    + "<element  name=\"myEd\" type=\"string\"/>"
                    + "<complexType name=\"myTypeDef\">"     
                    + "<sequence>"     
                    + "<element name=\"element\" type=\"string\"/>"      
                    + "</sequence>"     
                    + "</complexType>" 
                    + "</schema>";
        
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Reader reader = new StringReader(schemaString);
        InputSource is = new InputSource(reader);
        Document schemaDoc1 = null;
        try {
            schemaDoc1 = builder.parse(is);
        } catch(IOException e) {
            fail("There was an IOException whilst trying to parse the sample schema.");
        }
        
        XmlSchemaCollection xsc = new XmlSchemaCollection();
        xs1 = xsc.read(schemaDoc1.getDocumentElement());
        schema.setSchemaDefinition(xs1);
    	
        //Add it to the typesElement.
        TypesElement typesEl = fDescriptionElement.addTypesElement();
        typesEl.addSchema(schema);
        
    	fDescription = fDescriptionElement.toComponent();
    
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	
	public void testGetBinding()
	{
		Binding retrievedBinding = fDescription.getBinding(new QName("bindingName"));
		assertNotNull(retrievedBinding);
		assertEquals(fBindingElement, retrievedBinding.toElement());
		
		Binding noBinding = fDescription.getBinding(new QName("noBinding"));
		assertNull(noBinding);
		
		QName nullQName = null;
		Binding nullBinding = fDescription.getBinding(nullQName);
		assertNull(nullBinding);
	}
	
	public void testGetBindings()
	{
		Binding[] retrievedBindings = fDescription.getBindings();
		assertNotNull(retrievedBindings);
		assertEquals(1, retrievedBindings.length);
	}
	
	public void testGetElementDeclaration()
	{
		ElementDeclaration ed = fDescription.getElementDeclaration(new QName("myEd")); 
		assertNotNull(ed);
		
		// non-existent ElementDeclaration...
		ed = fDescription.getElementDeclaration(new QName("doesNotExist"));
		assertNull(ed);	
	}
	
	
	public void testGetElementDeclarations()
	{
		ElementDeclaration[] eds = fDescription.getElementDeclarations();
		assertNotNull(eds);
		assertEquals(1, eds.length);
		
		// TODO - test for when no Element Declarations ?
	}
	
	/*
	 * Test that a service returns its (required) associated interface.
	 * 
	 */
	public void testGetInterface()
	{	
		Interface retrievedIf = fDescription.getInterface(new QName("interfaceName"));
		assertNotNull(retrievedIf);
		assertEquals(fInterfaceElement, retrievedIf.toElement()); // TODO - a fair test or a rash assumption?!
		
		// non-existent interface
		retrievedIf = fDescription.getInterface(new QName("noSuchInterface"));
		assertNull(retrievedIf);
		
		// null interface name
		QName nullQName = null;
		retrievedIf = fDescription.getInterface(nullQName);
		assertNull(retrievedIf);
	}
	
	public void testGetInterfaces()
	{
		Interface[] retrievedIfs = fDescription.getInterfaces();
		assertEquals(1, retrievedIfs.length);
	}
	
	public void testGetService()
	{
		Service retrievedService = fDescription.getService(new QName("serviceName"));
		assertNotNull(retrievedService);
		
		// non-existent service
		retrievedService = fDescription.getService(new QName("noSuchService"));
		assertNull(retrievedService);
		
		// null service name
		QName nullQName = null;
		retrievedService = fDescription.getService(nullQName);
		assertNull(retrievedService);
	}
	
	public void testGetServices()
	{
		Service[] services = fDescription.getServices();
		assertEquals(1, services.length);
	}
	
	public void testGetTypeDefinition()
	{
		TypeDefinition td = fDescription.getTypeDefinition(new QName("myTypeDef"));
		assertNotNull(td);
		
		// non-existent TypeDefinition...
		td = fDescription.getTypeDefinition(new QName("doesNotExist"));
		assertNull(td);
	}
	
	public void testGetTypeDefinitions()
	{
		TypeDefinition[] tds = fDescription.getTypeDefinitions();
		assertNotNull(tds);
		assertEquals(1, tds.length);
	}

	/*
     * Tests that the returned class is a DescriptionElement
     *
     */
    public void testToElement()
	{
    	assertTrue(fDescription.toElement() instanceof DescriptionElement);
	}

}
