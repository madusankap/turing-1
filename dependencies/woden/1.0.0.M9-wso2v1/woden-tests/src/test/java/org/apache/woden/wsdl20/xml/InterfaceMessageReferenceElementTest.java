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
import org.apache.woden.internal.wsdl20.InterfaceMessageReferenceImpl;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.types.NCName;
import org.apache.woden.types.QNameTokenUnion;
import org.apache.woden.wsdl20.enumeration.Direction;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;



/**
 * Unit tests for the InterfaceMessageReferenceElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceMessageReferenceElementTest extends TestCase {

	private InterfaceMessageReferenceElement fMessageReferenceElement = null;

	public static Test suite()
	{
	   return new TestSuite(InterfaceMessageReferenceElementTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fMessageReferenceElement = new InterfaceMessageReferenceImpl();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	/*
	 * Test that a (mandatory) direction can be successfully set and retrieved
	 */
	public void testSetGetDirection()
	{
		// Default case
		assertNull("The retrieved Direction when unset should be null", fMessageReferenceElement.getDirection());
		
		fMessageReferenceElement.setDirection(Direction.OUT);
		assertEquals("The retrieved MessageReference direction is not that which was set", 
				Direction.OUT, fMessageReferenceElement.getDirection());
	}

	/*
	 * Test that the (Mandatory) message label attribute ("messageLabel") can be successfully set and retrieved
	 */
	public void testSetGetMessageLabel()
	{
		NCName messageRefNCName = new NCName("messageRefName");
		fMessageReferenceElement.setMessageLabel(messageRefNCName);
		assertEquals("The retrieved messageLabel is not that which was set", 
				messageRefNCName, fMessageReferenceElement.getMessageLabel());
	}

	/*
	 * Test that the optional attribute ("element") can be successfully set and retrieved
	 */
	public void testSetGetElement()
	{
        //test with type qname.
        QNameTokenUnion element = new QNameTokenUnion(new QName("ElementName"));
		fMessageReferenceElement.setElement(element);
		assertEquals("The retrieved 'element' attribute is not that which was set", 
                element, fMessageReferenceElement.getElement());
        
        //test with type token.
        QNameTokenUnion token = QNameTokenUnion.ANY;
        fMessageReferenceElement.setElement(token);
        assertEquals("The retrieved 'element' attribute is not that which was set", 
                token, fMessageReferenceElement.getElement());
	}
	
	/* 
	 * Test that the optional schema element declaration can be successfully retrieved if 
     * the QNameTokenUnion is of type qname and if it is of type token, that there is no
     * element declaration returned.
	 * 
	 * TODO Need to check model structure for XmlSchema
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
		InterfaceOperationElement interfaceOperationElement = interfaceElement.addInterfaceOperationElement();
		InterfaceMessageReferenceElement messageReference = interfaceOperationElement.addInterfaceMessageReferenceElement();
		
		// Default case:
		XmlSchemaElement retrievedElement = messageReference.getXmlSchemaElement();
		assertNull("Should return null if 'element' attribute is not set", retrievedElement);

        // Case 1 - (with 'element' set to #any)
        messageReference.setElement(QNameTokenUnion.ANY);
        retrievedElement = messageReference.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is #any", retrievedElement);

        // Case 2 - (with 'element' set to #none)
        messageReference.setElement(QNameTokenUnion.NONE);
        retrievedElement = messageReference.getXmlSchemaElement();
        assertNull("Should return null if 'element' attribute is #none", retrievedElement);

        // Case 3 - (with 'element' set to #other)
        messageReference.setElement(QNameTokenUnion.OTHER);
        retrievedElement = messageReference.getXmlSchemaElement();
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
        
        messageReference.setElement(new QNameTokenUnion(elemQN));
        retrievedElement = messageReference.getXmlSchemaElement();
        assertEquals("The 'element' qname should resolve to the expected XML Schema element declaration", 
                expectedElement, retrievedElement);
	}
}
