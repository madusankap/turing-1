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

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.BindingMessageReferenceImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.BindingMessageReferenceElement;
import org.apache.woden.wsdl20.xml.BindingOperationElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceMessageReferenceElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;

/**
 * Unit tests for the BindingMessageReference class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingMessageReferenceTest extends TestCase {

	private BindingMessageReferenceElement fBindingMessageRefElement = null;
	private BindingMessageReference fBindingMessageRef = null;	

	public static Test suite()
	{
	   return new TestSuite(BindingMessageReferenceTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fBindingMessageRefElement = new BindingMessageReferenceImpl();
		fBindingMessageRef = (BindingMessageReference) fBindingMessageRefElement;
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/* 
	 * Test that the (Mandatory) InterfaceMessageReference can be successfully retrieved.
	 * - getInterfaceMessageReference()
	 * 
	 */
	public void testGetInterfaceMessageReference()
	{
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

        DescriptionElement descriptionElement = factory.newDescription();

		// Create the BindingElement<->InterfaceElement->InterfaceOperationElement->InterfaceMessageReferenceElement hierarchy
		BindingElement bindingElement = descriptionElement.addBindingElement();
		bindingElement.setInterfaceName(new QName("interface1"));
		
		InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));

		InterfaceOperationElement ifopElement = interfaceElement.addInterfaceOperationElement();
		ifopElement.setName(new NCName("operation1"));
		InterfaceMessageReferenceElement ifmrElement = ifopElement.addInterfaceMessageReferenceElement();
		ifmrElement.setMessageLabel(new NCName("MessageRef1MessageLabel"));
				
		// Create the BindingOperationElement->BindingMessageReferenceElement hierarchy
		BindingOperationElement bopElement = bindingElement.addBindingOperationElement();
		bopElement.setRef(new QName("operation1"));
		fBindingMessageRefElement = bopElement.addBindingMessageReferenceElement();
		fBindingMessageRefElement.setMessageLabel(new NCName("MessageRef1MessageLabel"));

		Description descComp = descriptionElement.toComponent();
		descComp.getBindings(); // this triggers setting the link to description in the binding
		
		fBindingMessageRef = (BindingMessageReference) fBindingMessageRefElement;
		InterfaceMessageReference retrievedMessage = fBindingMessageRef.getInterfaceMessageReference();
		assertEquals("The retrieved InterfaceMessageReference is not that which was set", 
				ifmrElement, retrievedMessage);
	}
	
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fBindingMessageRefElement, fBindingMessageRef.toElement());
	}

}
