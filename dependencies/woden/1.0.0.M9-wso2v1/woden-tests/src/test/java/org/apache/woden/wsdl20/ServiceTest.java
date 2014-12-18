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
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.ServiceImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.EndpointElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.ServiceElement;

/**
 * Unit tests for the implementation of Service interface.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class ServiceTest extends TestCase {

	private Service fEmptyService = null;
	
	public static Test suite()
	{
	   return new TestSuite(ServiceTest.class);
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

        DescriptionElement desc = factory.newDescription();
        desc.addServiceElement();
    	fEmptyService = desc.toComponent().getServices()[0];
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }

	/*
	 * Test that endpoints associated with a service are correctly returned with
	 * getEndpoint() and getEndpoints().
	 * 
	 * Note that a service must have at least one endpoint associated.
	 */
	
	public void testGetEndpointGetEndpoints()
	{
		/* Set up prereqs:
		 * - Service with > 1 endpoints created.
		 */
		ServiceElement fServiceElement = new ServiceImpl();
		EndpointElement fEndpointElement1 = fServiceElement.addEndpointElement();
		fEndpointElement1.setName(new NCName("endpoint1"));
		EndpointElement fEndpointElement2 = fServiceElement.addEndpointElement();
		fEndpointElement2.setName(new NCName("endpoint2"));		
		Service fService = (Service) fServiceElement;
		
		// test getEndpoint()
		Endpoint e1 = fService.getEndpoint(new NCName("endpoint1"));
		assertEquals("The retrieved Endpoint object is not that which was set", e1, fEndpointElement1);
		
		// test getEndpoints()
		Endpoint[] e = fService.getEndpoints();
		assertEquals("The incorrect number of endpoints were returned", e.length, 2);
		assertEquals("First endpoint is not endpoint1", e[0], fEndpointElement1);
		assertEquals("Second endpoint is not endpoint2", e[1], fEndpointElement2);
	}
	
	/*
	 * Test that a service returns its (required) associated interface.
	 * 
	 */
	public void testGetInterface()
	{	
		/* Set up prereqs:
		 * - Description containing one named Interface;
		 * - Service using that same Interface.
		 */
		
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }

        // Description...
    	DescriptionElement fDescElement = factory.newDescription();
    	
    	// Interface
    	InterfaceElement fInterfaceElement = fDescElement.addInterfaceElement();
		fInterfaceElement.setName(new NCName("interface1"));
		
	   	// Service... 
    	ServiceElement fServiceElement = fDescElement.addServiceElement();
    	fServiceElement.setName(new NCName("service1"));
    	fServiceElement.setInterfaceName(new QName("interface1"));
    	
    	// "create" the component model to complete the woden object hierachy references
    	Description fDesc = fDescElement.toComponent();
    	fDesc.getServices(); // necessary to set the reference to Description in Service

    	/* Test assertions:
    	 * (Object equivalence is fine here - we check both refer to same Object)
    	 */
		assertEquals(((Service)fServiceElement).getInterface(), (Interface) fInterfaceElement);
	}
	
	/*
	 * Test that getName() correctly returns the assigned service name
	 */
	public void testGetName()
	{	
		// for simplicity, the default namespace is used throughout such that the string representation of
		// QName from the getter will be identical to the NCName of the setter, for the test to succeed.
		((ServiceImpl)fEmptyService).setName(new NCName("service1"));
		assertEquals("The service name from getName() differs from that set.", fEmptyService.getName().toString(), "service1");
	}
	
	/*
     * Tests that the returned class is a ServiceElement
     *
     */
    public void testToElement()
	{
    	assertTrue(fEmptyService.toElement() instanceof ServiceElement);
	}

}
