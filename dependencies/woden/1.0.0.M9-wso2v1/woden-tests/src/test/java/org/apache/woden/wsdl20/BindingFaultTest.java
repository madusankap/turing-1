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
import org.apache.woden.internal.wsdl20.BindingFaultImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.BindingFaultElement;
import org.apache.woden.wsdl20.xml.BindingOperationElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultElement;

/**
 * Unit tests for the BindingFault class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingFaultTest extends TestCase {

	private BindingFaultElement fFaultElement = null;
	private BindingFault fFault = null;	

	public static Test suite()
	{
	   return new TestSuite(BindingFaultTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fFaultElement = new BindingFaultImpl();
    	fFault = (BindingFault) fFaultElement;
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/* 
	 * Test that the (Mandatory) InterfaceFault can be successfully retrieved.
	 */
	public void testGetInterfaceFault()
	{
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
		DescriptionElement descriptionElement = factory.newDescription();

		// Create the BindingElement<->InterfaceElement->InterfaceFaultElement hierarchy
		BindingElement bindingElement = descriptionElement.addBindingElement();
		bindingElement.setInterfaceName(new QName("interface1"));
		bindingElement.setName(new NCName("binding1"));
		
		InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		InterfaceFaultElement iffElement = interfaceElement.addInterfaceFaultElement();
		iffElement.setName(new NCName("fault1"));

		// Create the BindingOperationElement->BindingFaultReferenceElement hierarchy
		BindingOperationElement bopElement = bindingElement.addBindingOperationElement();
		bopElement.setRef(new QName("operation1"));
		fFaultElement = bindingElement.addBindingFaultElement();
		fFaultElement.setRef(new QName("fault1"));
		
		Description descComp = descriptionElement.toComponent();
		descComp.getBindings(); // this triggers setting the link to description in the binding

		BindingFault bf = (BindingFault)fFaultElement;
	
		InterfaceFault retrievedFault = bf.getInterfaceFault();
		assertEquals("The retrieved InterfaceFaultElement is not that which was set", 
				iffElement, retrievedFault);
	}
	
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fFaultElement, fFault.toElement());
	}

}
