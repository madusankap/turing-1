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
package org.apache.woden.wsdl20.extensions.soap;

import java.net.URI;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorHandler;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.Binding;
import org.apache.woden.wsdl20.BindingOperation;
import org.apache.woden.wsdl20.Description;

/**
 * Functional verification test of SoapBindingOperationExtensions.
 * Checks that the expected API behaviour is supported by the implementation.
 * 
 * @author jkaputin@apache.org
 */
public class SOAPBindingOperationExtensionsTest extends TestCase 
{
    private WSDLFactory fFactory = null;
    private WSDLReader fReader = null;
    private ErrorHandler fHandler = null;
    private BindingOperation fBindOper = null;
    private BindingOperation fBind2Oper = null;
    
    private String fWsdlPath = "org/apache/woden/wsdl20/extensions/soap/resources/SOAPBindingOperationExtensions.wsdl";
    
    public static Test suite()
    {
        return new TestSuite(SOAPBindingOperationExtensionsTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        fFactory = WSDLFactory.newInstance();
        fReader = fFactory.newWSDLReader();
        fHandler = new TestErrorHandler();
        fReader.getErrorReporter().setErrorHandler(fHandler);
        
        URL wsdlURL = getClass().getClassLoader().getResource(fWsdlPath);
        assertNotNull("Failed to find the WSDL document on the classpath using the path: " + fWsdlPath + ".", 
                wsdlURL);
        
        Description descComp = fReader.readWSDL(wsdlURL.toString());
        assertNotNull("The reader did not return a WSDL description.", descComp);
        
        Binding binding = descComp.getBindings()[0];
        assertNotNull("The Description does not contain a Binding.", binding);
        fBindOper = binding.getBindingOperations()[0];
        assertNotNull("The Binding does not a BindingOperation.", fBindOper);
        
        Binding binding2 = descComp.getBindings()[1];
        assertNotNull("The Description does not contain a second Binding.", binding2);
        
        fBind2Oper = binding2.getBindingOperations()[0];
        assertNotNull("The second Binding does not contain a BindingOperation.");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Test that the <code>wsoap:mep</code> extension attribute is represented 
     * in the Component model extensions by the expected URI. 
     */
    public void testGetSoapMep()
    {
        SOAPBindingOperationExtensions soapBindOperExts = 
            (SOAPBindingOperationExtensions) fBindOper.getComponentExtensionContext(SOAPConstants.NS_URI_SOAP);
        URI soapMep = soapBindOperExts.getSoapMep();
        
        assertNotNull("The SOAPBindingOperationExtensions did not return a value for {soap mep}.", soapMep);
        assertEquals("Unexpected soap mep URI.", URI.create("urn:mep"), soapMep);
    }
        
    /**
     * Test that the <code>wsoap:action</code> extension attribute is represented 
     * in the Component model extensions by the expected URI. 
     */
    public void testGetSoapAction()
    {
        SOAPBindingOperationExtensions soapBindOperExts = 
            (SOAPBindingOperationExtensions) fBindOper.getComponentExtensionContext(SOAPConstants.NS_URI_SOAP);
        URI soapAction = soapBindOperExts.getSoapAction();
        
        assertNotNull("The SOAPBindingOperationExtensions did not return a value for {soap action}.", soapAction);
        assertEquals("Unexpected soap action URI.", URI.create("urn:action"), soapAction);
    }
        
    /**
     * Test that the {soap modules} property returned by the <code>getSoapModules</code> method 
     * contains the expected number of SOAPModule objects parsed from the WSDL.
     */
    public void testGetSoapModules()
    {
        SOAPBindingOperationExtensions soapBindOperExts = 
            (SOAPBindingOperationExtensions) fBindOper.getComponentExtensionContext(SOAPConstants.NS_URI_SOAP);
        SOAPModule[] actual = soapBindOperExts.getSoapModules();
        assertEquals("Unexpected number of SOAPModule objects.", 1, actual.length);
    }

    /**
     * Test that the {http query parameter separator} property returned by the 
     * <code>getHttpParameterSeparator</code> method method matches the value
     * parsed from the WSDL or that it defaults to null if omitted from the WSDL.
     */
    public void testGetHttpQueryParameterSeparator()
    {
        SOAPBindingOperationExtensions soapBindOperExts = 
            (SOAPBindingOperationExtensions) fBindOper.getComponentExtensionContext(SOAPConstants.NS_URI_SOAP);
        String actual = soapBindOperExts.getHttpQueryParameterSeparator();
        
        assertNotNull("The SOAPBindingOperationExtensions did not return a value for {http query parameter separator}.", actual);
        assertEquals("Unexpected value for http query parameter separator.", "$", actual);
        
        SOAPBindingOperationExtensions soapBind2OperExts = 
            (SOAPBindingOperationExtensions) fBind2Oper.getComponentExtensionContext(SOAPConstants.NS_URI_SOAP);
        String actual2 = soapBind2OperExts.getHttpQueryParameterSeparator();
        
        assertNull("Non-null value for http query parameter separator.", actual2);
        
    }
}
