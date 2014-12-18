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

import java.net.URI;
import java.net.URL;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorHandler;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.ServiceImpl;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.Description;

/**
 * Functional verification test of org.apache.woden.wsdl20.xml.ServiceElement.
 * Checks that the expected API behaviour is supported by the implementation.
 * 
 * @author jkaputin@apache.org
 */
public class ServiceElementTest extends TestCase 
{
    private WSDLFactory fFactory = null;
    private WSDLReader fReader = null;
    private ErrorHandler fHandler = null;
    private DescriptionElement fParsedDesc = null;
    private ServiceElement fParsedService = null;
    
    private String fTargetNS = "http://ws.apache.woden/service";
    private QName fQName = new QName("urn:woden","dummy");

    public static Test suite()
    {
        return new TestSuite(ServiceElementTest.class);
    }

    protected void setUp() throws Exception 
    {
        fFactory = WSDLFactory.newInstance();
        fReader = fFactory.newWSDLReader();
        fHandler = new TestErrorHandler();
        fReader.getErrorReporter().setErrorHandler(fHandler);
        
        URL wsdlURL = getClass().getClassLoader().getResource(
            "org/apache/woden/wsdl20/xml/resources/ServiceElementTest.wsdl");
        assertNotNull("Failed to find the WSDL document on the classpath.", wsdlURL);
        
        Description descComp = fReader.readWSDL(wsdlURL.toString());
        assertNotNull("The reader did not return a description.", descComp);
        fParsedDesc = descComp.toElement();
        
        fParsedService = fParsedDesc.getServiceElements()[0];
        assertNotNull("The description does not contain a service.", fParsedService);
    }

    protected void tearDown() throws Exception 
    {
        fFactory = null;
        fReader = null;
        fHandler = null;
        fParsedDesc = null;
        fParsedService = null;
    }
    
    /**
     * Test that the getName method returns the expected QName parsed from a WSDL document.
     */
    public void testGetNameParsed()
    {
        QName qname = fParsedService.getName();
        assertNotNull("ServiceElement.getName() returned null, but a QName was expected.",
                   qname);
        
        QName expectedQN = new QName(fTargetNS, "service1");
        assertTrue("QName returned by ServiceElement.getName() was not the one expected.",
                   expectedQN.equals(qname));
    }
    
    /**
     * Test that the QName specified on the setName method is returned by getName.
     */
    public void testSetAndGetName() throws Exception
    {
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        DescriptionElement descElem = factory.newDescription();
        descElem.setTargetNamespace(new URI("urn:woden"));
        ServiceElement service = descElem.addServiceElement();
        service.setName(new NCName(fQName.getLocalPart()));
        assertTrue("QName returned by ServiceElement.getName() was not the one set by setName().",
                   fQName.equals(service.getName()));
    }
    
    /**
     * Test that the getInterfaceName method returns the QName of the interface
     * associated with this service, as specified by the "interface" attribute
     * of the &lt;service&gt; element in a parsed WSDL document.
     */
    public void testGetInterfaceNameParsed()
    {
        QName qname = fParsedService.getInterfaceName();
        assertNotNull("ServiceElement.getInterfaceName() returned null, but a QName was expected.",
                      qname);
        
        QName expectedQN = new QName(fTargetNS, "interface1");
        assertTrue("QName returned by ServiceElement.getInterfaceName() was not the one expected.",
                   expectedQN.equals(fParsedService.getInterfaceName()));
    }

    /**
     * Test that the QName specified on the setInterfaceName method is returned by 
     * the getInterfaceName method.
     */
    public void testSetAndGetInterfaceName()
    {
        ServiceElement service = new ServiceImpl();
        service.setInterfaceName(fQName);
        QName returnedQN = service.getInterfaceName();
        assertTrue("QName returned by ServiceElement.getInterfaceName() was not the one set by setInterfaceName().",
                   returnedQN.equals(fQName));
    }
    
    /**
     * Test that the getInterfaceElement method returns an InterfaceElement 
     * defined within the description, that is referred to by QName in the 
     * "interface" attribute of the &lt;service&gt; element of a parsed WSDL 
     * document. This tests that the QName is correctly dereferenced to an object.
     */
    public void testGetInterfaceElementParsed()
    {
        InterfaceElement interfaceDefined = fParsedDesc.getInterfaceElements()[0];
        InterfaceElement interfaceReferred = fParsedService.getInterfaceElement();
        assertNotNull("ServiceElement.getInterfaceElement() returned null, but an InterfaceElement was expected.",
                interfaceReferred);
        
        assertTrue("The InterfaceElement returned by ServiceElement.getInterfaceElement() was not the one expected.",
                interfaceReferred == interfaceDefined);
    }

    /**
     * Test that the getEndpointElements method returns an array of the EndpointElements
     * defined as &lt;endpoint&gt; child elements of the &lt;service&gt; element in
     * a parsed WSDL document.
     */
    public void testGetEndpointElementsParsed()
    {
        EndpointElement[] endpoints = fParsedService.getEndpointElements();
        assertTrue("ServiceElement.getEndpointElements() did not return 3 endpoints, as expected.",
                endpoints.length == 3);
    }
}
