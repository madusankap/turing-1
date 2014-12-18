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
package org.apache.woden.wsdl20.fragids;

import java.net.URL;

import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.wsdl20.Description;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FragmentIdentificationTest extends TestCase {
    private WSDLFactory factory;
    private WSDLReader reader;
    
    public static Test suite()
    {
        return new TestSuite(FragmentIdentificationTest.class);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        factory = WSDLFactory.newInstance();
        reader = factory.newWSDLReader();  
    }
    
    public void testSerialisation(){
        Description desc = null;
        
        //Load in a WSDL 2.0 file
        URL wsdlURL2 = getClass().getClassLoader().getResource("org/apache/woden/wsdl20/fragids/greatH.wsdl");
        
        try {
           desc = reader.readWSDL(wsdlURL2.toString()); 
        } catch(Exception e) {
           fail("Failed with unexpected exception: " + e);
        }
        
        assertEquals("wsdl.description()", desc.toString());
        //Elements
        assertEquals("xmlns(ns1=http://greath.example.com/2004/schemas/resSvc)wsdl.elementDeclaration(ns1:checkAvailability)",
                desc.getElementDeclaration(new QName("http://greath.example.com/2004/schemas/resSvc","checkAvailability")).toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/schemas/resSvc)wsdl.elementDeclaration(ns1:checkAvailabilityResponse)",
                desc.getElementDeclaration(new QName("http://greath.example.com/2004/schemas/resSvc","checkAvailabilityResponse")).toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/schemas/resSvc)wsdl.elementDeclaration(ns1:invalidDataError)",
                desc.getElementDeclaration(new QName("http://greath.example.com/2004/schemas/resSvc","invalidDataError")).toString());
        
        //Types
        assertEquals("xmlns(ns1=http://greath.example.com/2004/schemas/resSvc)wsdl.typeDefinition(ns1:tCheckAvailability)",
                desc.getTypeDefinition(new QName("http://greath.example.com/2004/schemas/resSvc","tCheckAvailability")).toString());
        
        //Interface
        assertEquals("wsdl.interface(reservationInterface)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).toString());
        
        assertEquals("wsdl.interfaceFault(reservationInterface/invalidDataFault)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).getInterfaceFault(new QName("http://greath.example.com/2004/wsdl/resSvc", "invalidDataFault")).toString());
        
        assertEquals("wsdl.interfaceOperation(reservationInterface/opCheckAvailability)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).getInterfaceOperation(new QName("http://greath.example.com/2004/wsdl/resSvc", "opCheckAvailability")).toString());
        
        assertEquals("wsdl.interfaceMessageReference(reservationInterface/opCheckAvailability/In)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).getInterfaceOperation(new QName("http://greath.example.com/2004/wsdl/resSvc", "opCheckAvailability")).getInterfaceMessageReferences()[0].toString());
        
        assertEquals("wsdl.interfaceMessageReference(reservationInterface/opCheckAvailability/Out)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).getInterfaceOperation(new QName("http://greath.example.com/2004/wsdl/resSvc", "opCheckAvailability")).getInterfaceMessageReferences()[1].toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.interfaceFaultReference(reservationInterface/opCheckAvailability/Out/ns1:invalidDataFault)",
                desc.getInterface(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationInterface")).getInterfaceOperation(new QName("http://greath.example.com/2004/wsdl/resSvc", "opCheckAvailability", "tns")).getInterfaceFaultReferences()[0].toString());

        //Binding
        assertEquals("wsdl.binding(reservationSOAPBinding)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.bindingFault(reservationSOAPBinding/ns1:invalidDataFault)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).getBindingFaults()[0].toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.bindingOperation(reservationSOAPBinding/ns1:opCheckAvailability)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).getBindingOperations()[0].toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.bindingMessageReference(reservationSOAPBinding/ns1:opCheckAvailability/In)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).getBindingOperations()[0].getBindingMessageReferences()[0].toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.bindingMessageReference(reservationSOAPBinding/ns1:opCheckAvailability/Out)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).getBindingOperations()[0].getBindingMessageReferences()[1].toString());
        
        assertEquals("xmlns(ns1=http://greath.example.com/2004/wsdl/resSvc)wsdl.bindingFaultReference(reservationSOAPBinding/ns1:opCheckAvailability/Out/ns1:invalidDataFault)",
                desc.getBinding(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationSOAPBinding")).getBindingOperations()[0].getBindingFaultReferences()[0].toString());
        
        //Service
        assertEquals("wsdl.service(reservationService)",
                desc.getService(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationService")).toString());
        
        assertEquals("wsdl.endpoint(reservationService/reservationEndpoint)",
                desc.getService(new QName("http://greath.example.com/2004/wsdl/resSvc", "reservationService")).getEndpoints()[0].toString());
        
        //TODO add tests for Extensions.
    }
}
