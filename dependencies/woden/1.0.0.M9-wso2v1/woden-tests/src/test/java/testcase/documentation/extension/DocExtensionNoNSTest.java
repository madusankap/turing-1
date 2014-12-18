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
package testcase.documentation.extension;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorHandler;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.resolver.SimpleURIResolverTest;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.extensions.UnknownExtensionElement;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.DocumentationElement;

/**
 * Test that when no default namespace is declared in the WSDL,
 * that documentation extension elements with no namespace are
 * represented correctly - they should return a null NS uri.
 */
public class DocExtensionNoNSTest extends TestCase {

    public static Test suite()
    {
        return new TestSuite(DocExtensionNoNSTest.class);
    }
    
    
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    public void testNSUriIsNull() throws Exception {
        URL wsdlURL = getClass().getClassLoader().getResource("testcase/documentation/extension/resources/nonamespace.wsdl");
        assertNotNull("Failed to find nonamespace.wsdl on the classpath",wsdlURL);

        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        ErrorHandler handler = new TestErrorHandler();
        reader.setFeature(WSDLReader.FEATURE_VALIDATION, true);
        reader.getErrorReporter().setErrorHandler(handler);
        Description descComp = reader.readWSDL(wsdlURL.toString());
        assertNotNull("The reader did not return a WSDL description.", descComp);
        DescriptionElement desc = descComp.toElement();
        
        DocumentationElement doc = null;
        UnknownExtensionElement ee = null;
        
        //wsdl:description
        doc = desc.getDocumentationElements()[0];
        ee = (UnknownExtensionElement)doc.getExtensionElements()[0];
        assertNull(ee.getElement().getNamespaceURI());
        
        //wsdl:types
        doc=desc.getTypesElement().getDocumentationElements()[0];
        ee = (UnknownExtensionElement)doc.getExtensionElements()[0];
        assertNull(ee.getElement().getNamespaceURI());
        
        //wsdl:interface
        doc=desc.getInterfaceElements()[0].getDocumentationElements()[0];
        ee = (UnknownExtensionElement)doc.getExtensionElements()[0];
        assertNull(ee.getElement().getNamespaceURI());
        
        //wsdl:binding
        doc=desc.getBindingElements()[0].getDocumentationElements()[0];
        ee = (UnknownExtensionElement)doc.getExtensionElements()[0];
        assertNull(ee.getElement().getNamespaceURI());
        
        //wsdl:service
        doc=desc.getServiceElements()[0].getDocumentationElements()[0];
        ee = (UnknownExtensionElement)doc.getExtensionElements()[0];
        assertNull(ee.getElement().getNamespaceURI());
    }

}
