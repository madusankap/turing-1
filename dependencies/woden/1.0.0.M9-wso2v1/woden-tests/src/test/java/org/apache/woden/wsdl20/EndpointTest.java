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

import java.net.URI;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.EndpointImpl;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.BindingElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.EndpointElement;
import org.apache.woden.wsdl20.xml.ServiceElement;

/**
 * Unit tests for the ImportElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class EndpointTest extends TestCase {

	private EndpointImpl fEndpointImpl = null;
	private Endpoint fEndpoint = null;
	private URI fURI = null;
	private QName fBindingQName = null;
	private NCName fBindingNCName = null;
	private NCName fName = null;
	
	public static Test suite()
	{
	   return new TestSuite(EndpointTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        fURI = new URI("http://apache.org/endpointURI");
    	fEndpointImpl = new EndpointImpl();
    	fEndpoint = fEndpointImpl;
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown(); 
        fURI = null;
    	fEndpointImpl = null;
    	fEndpoint = null;
    	fBindingQName = null;
    	fBindingNCName = null;
    	fName = null;
    }
	
    /*
     * Test getAddress() when the (optional) address attribute is set (specified)
     */
    public void testGetAddress()
	{
        fEndpointImpl.setAddress(fURI);
    	assertEquals(fEndpoint.getAddress(), fURI); // java.net.URI overrides Object.equals()   	
	}
    
    /*
     * Test getAddress() when the (optional) address attribute is unspecified
     */
    public void testDefaultGetAddress()
	{
    	assertNull(fEndpoint.getAddress());  	
	}
 
    /*
     * The Binding with fBindingName is obtained from the Description element associated with the Endpoint's Service
     *
     */
    public void testGetBinding()
	{	
    	/* 
    	 *  Test setup :  work with the Infoset view for the creation of the 
    	 *  necessary prerequisite partial hierarchy 
    	 *  Note create the minimum necessary wsdl20 objects
    	 */
        
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
    	
    	// Description...
    	DescriptionElement fDescElement = factory.newDescription();
    	
    	// Binding...
        fBindingQName = new QName("binding1");
    	BindingElement fBindingElement = fDescElement.addBindingElement();
        fBindingNCName = new NCName("binding1");
        NCName fServiceNCName = new NCName("service1");
        QName fServiceQName = new QName("service1");
        NCName fEndpointNCname = new NCName("endpoint1");
    	fBindingElement.setName(fBindingNCName);
    	
    	// Service... (attach our Endpoint to a Service parented by our Description)
    	ServiceElement fServiceElement = fDescElement.addServiceElement();
    	fServiceElement.setName(fServiceNCName);
    	
    	// Endpoint...
    	EndpointElement fEndpointElement = fServiceElement.addEndpointElement();
    	fEndpointElement.setName(fEndpointNCname);
        fEndpointElement.setBindingName(fBindingQName);
    	
    	// Component model creation
        Description fDesc = fDescElement.toComponent();
        Endpoint fDerivedEndpoint = fDesc.getService(fServiceQName).getEndpoint(fEndpointNCname);
        
    	assertEquals(fDerivedEndpoint.getBinding().getName(), fBindingQName); // just compare QNames
	}
    
    public void testGetName()
	{
        fName = new NCName("endpoint_name");  
    	fEndpointImpl.setName(fName);
    	assertEquals(fEndpoint.getName(), fName);
	}
    
    /*
     * Tests that the returned class is precisely an EndpointElement (ie not a subclass)
     *
     */
    public void testToElement()
	{
    	assertTrue(fEndpoint.toElement() instanceof EndpointElement);
	}
    
}