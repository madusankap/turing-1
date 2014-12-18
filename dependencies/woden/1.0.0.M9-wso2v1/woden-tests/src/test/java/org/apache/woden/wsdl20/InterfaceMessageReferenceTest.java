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
import org.apache.woden.internal.wsdl20.Constants;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.types.NCName;
import org.apache.woden.types.QNameTokenUnion;
import org.apache.woden.wsdl20.enumeration.Direction;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceMessageReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;
import org.apache.woden.wsdl20.xml.TypesElement;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;



/**
 * Unit tests for the InterfaceMessageReference class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceMessageReferenceTest extends TestCase {

    private DescriptionElement fDescriptionElement = null;
    private InterfaceElement fInterfaceElement = null;
    private InterfaceOperationElement fInterfaceOperationElement = null;
	private InterfaceMessageReferenceElement fMessageReferenceElement = null;
    private final String TNS = "http://example.org";
    private final String INTF_NAME = "interfaceName";
    private final String OPER_NAME = "operationName";
	
	public static Test suite()
	{
	   return new TestSuite(InterfaceMessageReferenceTest.class);	   
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
        fDescriptionElement.setTargetNamespace(URI.create(TNS));
        fInterfaceElement = fDescriptionElement.addInterfaceElement();
        fInterfaceElement.setName(new NCName(INTF_NAME));
        fInterfaceOperationElement = fInterfaceElement.addInterfaceOperationElement();
        fInterfaceOperationElement.setName(new NCName(OPER_NAME));
    	fMessageReferenceElement = fInterfaceOperationElement.addInterfaceMessageReferenceElement();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	/*
	 * Test that a (mandatory) direction can be successfully retrieved
	 */
	public void testGetDirection()
	{
        fMessageReferenceElement.setDirection(Direction.OUT);
        Description desc = fDescriptionElement.toComponent();
        InterfaceMessageReference msgRef = desc.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0];
		assertEquals("The retrieved direction is not that which was set", 
				Direction.OUT, msgRef.getDirection());
	}

	/*
	 * Test that the (Mandatory) message label attribute ("messageLabel") can be successfully retrieved
	 */
	public void testGetMessageLabel()
	{
		NCName messageRefNCName = new NCName("messageRefName");
		fMessageReferenceElement.setMessageLabel(messageRefNCName);
        Description desc = fDescriptionElement.toComponent();
        InterfaceMessageReference msgRef = desc.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0];
		assertEquals("The retrieved message label is not that which was set", 
				messageRefNCName, msgRef.getMessageLabel());
	}

	/* 
	 * Test that the (Mandatory) Message Content Model property can be successfully retrieved 
	 */
	public void testGetMessageContentModel()
	{
        Description desc = fDescriptionElement.toComponent();
        InterfaceMessageReference msgRef = desc.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0];

        fMessageReferenceElement.setElement(QNameTokenUnion.ANY);
		assertEquals("The retrieved Message Content Model is not #any", 
				Constants.NMTOKEN_ANY, msgRef.getMessageContentModel());

        fMessageReferenceElement.setElement(QNameTokenUnion.NONE);
        assertEquals("The retrieved Message Content Model is not #none", 
                Constants.NMTOKEN_NONE, msgRef.getMessageContentModel());

        fMessageReferenceElement.setElement(QNameTokenUnion.OTHER);
        assertEquals("The retrieved Message Content Model is not #other", 
                Constants.NMTOKEN_OTHER, msgRef.getMessageContentModel());

        fMessageReferenceElement.setElement(new QNameTokenUnion(new QName("elementName")));
        assertEquals("The retrieved Message Content Model is not #element", 
                Constants.NMTOKEN_ELEMENT, msgRef.getMessageContentModel());
	}
    
    /*
     * Test that an (optional) ElementDecalaration can be successfully retrieved
     */
    public void testGetElementDeclaration() throws Exception
    {
        Description desc = fDescriptionElement.toComponent();
        Interface iface = desc.getInterface(new QName(TNS,INTF_NAME));
        InterfaceOperation oper = iface.getInterfaceOperation(new QName(TNS,OPER_NAME));
        InterfaceMessageReference msgRef = oper.getInterfaceMessageReferences()[0];
        assertNotNull(msgRef);
        
        // Default case
        assertNull("When 'element' attribute is omitted, no ElementDeclaration should be present", msgRef.getElementDeclaration());
        
        // Test that the expected ElementDeclaration is returned
        TypesElement typesElement = fDescriptionElement.addTypesElement();  //throws WSDLException
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
        
        fMessageReferenceElement.setElement(new QNameTokenUnion(elemQN));
        ElementDeclaration elemDecl = msgRef.getElementDeclaration();
        
        assertEquals("The ElementDeclaration did not match the qname in the 'element' attribute",
                elemQN, elemDecl.getName());
    }

	
	/*
     * toElement() - test that the infoset element view of this component can be retreived
     */
	public void testToElement() 
	{	
        Description desc = fDescriptionElement.toComponent();
        InterfaceMessageReference msgRef = desc.getInterfaces()[0].getInterfaceOperations()[0].getInterfaceMessageReferences()[0];
		assertEquals(fMessageReferenceElement, msgRef.toElement());
	}
}
