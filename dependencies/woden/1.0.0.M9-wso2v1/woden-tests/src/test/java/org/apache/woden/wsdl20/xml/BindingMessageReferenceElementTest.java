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

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.BindingMessageReferenceImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.enumeration.Direction;



/**
 * Unit tests for the BindingMessageReferenceElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingMessageReferenceElementTest extends TestCase {

	private BindingMessageReferenceElement fMessageReference = null;

	public static Test suite()
	{
	   return new TestSuite(BindingMessageReferenceElementTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp(); 	
    	fMessageReference = new BindingMessageReferenceImpl();
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
		assertNull("The retrieved Direction when unset should be null", fMessageReference.getDirection());
		
		fMessageReference.setDirection(Direction.OUT);
		assertEquals("The retrieved MessageReference direction is not that which was set", 
				Direction.OUT, fMessageReference.getDirection());
	}

	/*
	 * Test that the (Mandatory) message label attribute ("messageLabel") can be successfully set and retrieved
	 */
	public void testSetGetMessageLabel()
	{
		NCName messageRefNCName = new NCName("messageRefName");
		fMessageReference.setMessageLabel(messageRefNCName);
		assertEquals("The retrieved messageLabel is not that which was set", 
				messageRefNCName, fMessageReference.getMessageLabel());
	}
	
    /* 
     * Test that the associated InterfaceMessageReferenceElement can be retrieved.
     */
    public void testGetInterfaceMessageReferenceElement()
    {
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

        DescriptionElement descriptionElement = factory.newDescription();
        
        // Create the BindingElement->InterfaceElement->InterfaceOperationElement->InterfaceMessageReferenceElement
        BindingElement bindingElement = descriptionElement.addBindingElement();
        bindingElement.setInterfaceName(new QName("interface1"));
        
        InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
        interfaceElement.setName(new NCName("interface1"));
        
        InterfaceOperationElement ifopElement = interfaceElement.addInterfaceOperationElement();
        ifopElement.setName(new NCName("operation1"));
        InterfaceMessageReferenceElement ifmrElement = ifopElement.addInterfaceMessageReferenceElement();
        ifmrElement.setMessageLabel(new NCName("input1MessageLabel"));
                
        // Create the BindingOperationElement->BindingMessageReferenceElement
        BindingOperationElement bopElement = bindingElement.addBindingOperationElement();
        bopElement.setRef(new QName("operation1"));
        fMessageReference = bopElement.addBindingMessageReferenceElement();
        fMessageReference.setMessageLabel(new NCName("input1MessageLabel"));

        InterfaceMessageReferenceElement retrievedMsgRef = fMessageReference.getInterfaceMessageReferenceElement();
        assertEquals("The InterfaceMessageReferenceElement is not the expected one.", 
                ifmrElement, retrievedMsgRef);
    }
}
