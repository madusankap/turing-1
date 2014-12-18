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

import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.schema.InlinedSchemaImpl;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.types.NCName;
import org.apache.woden.types.QNameTokenUnion;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Unit tests for the InterfaceFaultElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceFaultElementTest extends TestCase {

    private DescriptionElement fDescriptionElement = null;
    private InterfaceElement fInterfaceElement = null;
	private InterfaceFaultElement fFaultElement;
    private final String TNS = "http://example.org";
    private final String INTF_NAME = "interfaceName";
    private final String FAULT_NAME = "faultName";

	public static Test suite()
	{
	   return new TestSuite(InterfaceFaultElementTest.class);
	}
    
    public void setUp() {

        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        fDescriptionElement = factory.newDescription();
        fDescriptionElement.setTargetNamespace(URI.create(TNS));
        fInterfaceElement = fDescriptionElement.addInterfaceElement();
        fInterfaceElement.setName(new NCName(INTF_NAME));
        fFaultElement = fInterfaceElement.addInterfaceFaultElement();
    }
	
	/*
	 * Test that a (Mandatory) Name QName can be successfully set and retrieved
	 */
	public void testSetGetName()
	{
		QName faultName = new QName(TNS, FAULT_NAME);
		NCName faultNCName = new NCName(FAULT_NAME);
		fFaultElement.setName(faultNCName);
		assertEquals("The retrieved fault name is not that which was set", 
				faultName, fFaultElement.getName());
	}
    
    /*
     * Test that the optional attribute ("element") can be successfully set and retrieved
     */
    public void testSetGetElement()
    {
        //test with type qname.
        QNameTokenUnion element = new QNameTokenUnion(new QName("ElementName"));
        fFaultElement.setElement(element);
        assertEquals("The retrieved 'element' attribute is not that which was set", 
                element, fFaultElement.getElement());
        
        //test with type token.
        QNameTokenUnion token = QNameTokenUnion.ANY;
        fFaultElement.setElement(token);
        assertEquals("The retrieved 'element' attribute is not that which was set", 
                token, fFaultElement.getElement());
    }
    
    /* 
     * Test that the optional schema element declaration can be successfully retrieved if 
     * the QNameTokenUnion is of type qname and if it is of type token, that there is no
     * element declaration returned.
     */
    public void testGetXmlSchemaElement() throws Exception
    {
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        // Create the DescriptionElement->InterfaceElement->InterfaceOperationElement->InterfaceMessageReferenceElement hierarchy
        DescriptionElement descriptionElement = factory.newDescription();
        InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
        InterfaceFaultElement faultElement = interfaceElement.addInterfaceFaultElement();
        
        // Default case:
        XmlSchemaElement retrievedElement = faultElement.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is not set", retrievedElement);

        // Case 1 - (with 'element' set to #any)
        faultElement.setElement(QNameTokenUnion.ANY);
        retrievedElement = faultElement.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is #any", retrievedElement);

        // Case 2 - (with 'element' set to #none)
        faultElement.setElement(QNameTokenUnion.NONE);
        retrievedElement = faultElement.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is #none", retrievedElement);

        // Case 3 - (with 'element' set to #other)
        faultElement.setElement(QNameTokenUnion.OTHER);
        retrievedElement = faultElement.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is #other", retrievedElement);

        // Case 4 - (with 'element' set to the qname of a schema element declaration)
        TypesElement typesElement = descriptionElement.addTypesElement();  //throws WSDLException
        InlinedSchema schema = new InlinedSchemaImpl();
        String schemaString = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample.org\">"
                  + "<complexType name=\"myType\">"     
                  + "<sequence>"     
                  + "<element  name=\"element\" type=\"string\"/>"      
                  + "</sequence>"     
                  + "</complexType>" 
                  + "<element name=\"myElement\" type=\"string\"/>"
                  + "</schema>";
        DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Reader reader = new StringReader(schemaString);
        InputSource is = new InputSource(reader);
        Document schemaDoc1 = builder.parse(is);
        XmlSchemaCollection xsc = new XmlSchemaCollection();
        XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
        schema.setSchemaDefinition(xs1);
        URI schemaNS = URI.create("http://www.sample.org");
        schema.setNamespace(schemaNS);
        typesElement.addSchema(schema);
        
        QName elemQN = new QName("http://www.sample.org","myElement");
        XmlSchemaElement expectedElement = xs1.getElementByName(elemQN);
        
        faultElement.setElement(new QNameTokenUnion(elemQN));
        retrievedElement = faultElement.getXmlSchemaElement();
        assertEquals("The 'element' qname should resolve to the expected XML Schema element declaration", 
                expectedElement, retrievedElement);
    }

}
