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
import org.apache.woden.internal.wsdl20.BindingFaultImpl;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;

/**
 * Unit tests for the BindingFaultElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class BindingFaultElementTest extends TestCase {

	private BindingFaultElement fFault = null;

	public static Test suite()
	{
	   return new TestSuite(BindingFaultElementTest.class);
	   
	}
	   /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
    	super.setUp();
    	fFault = new BindingFaultImpl();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/* 
	 * Test that the (Mandatory) BindingFault reference attribute ("ref") can be successfully set and retrieved 
	 */
	public void testSetGetRef()
	{
		QName faultRefName = new QName("faultRefName");
		fFault.setRef(faultRefName);
		assertEquals("The retrieved BindingFault reference is not that which was set", 
				faultRefName, fFault.getRef());
	}
	
	/* 
	 * Test that the (Mandatory) InterfaceFault can be successfully retrieved.
	 */
	public void testGetInterfaceFaultElement()
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
		
		InterfaceElement interfaceElement = descriptionElement.addInterfaceElement();
		interfaceElement.setName(new NCName("interface1"));
		
		InterfaceFaultElement iffElement = interfaceElement.addInterfaceFaultElement();
		iffElement.setName(new NCName("fault1"));

		// Create the BindingOperationElement->BindingFaultReferenceElement hierarchy
		BindingOperationElement bopElement = bindingElement.addBindingOperationElement();
		bopElement.setRef(new QName("operation1"));
		fFault = bindingElement.addBindingFaultElement();
		fFault.setRef(new QName("fault1"));
	
		InterfaceFaultElement retrievedFault = fFault.getInterfaceFaultElement();
		assertEquals("The retrieved InterfaceFaultElement is not that which was set", 
				iffElement, retrievedFault);
	}
}
