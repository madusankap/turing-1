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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.types.NCName;

public class NameAttributeTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(NameAttributeTest.class);
    }

    public static Test suite()
    {
        return new TestSuite(NameAttributeTest.class);
    }
    
    /**
     * Create an InterfaceElement but don't add it to a DescriptionElement
     * and ensure that getQName() returns a QName with a namespace value
     * of the emptystring.
     */
    public void testNamespaceOfGetNameReturnValue() {
        WSDLFactory factory = null;
        try {
            factory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        DescriptionElement de = factory.newDescription();
        InterfaceElement ie = de.addInterfaceElement();
        ie.setName(new NCName("foo"));
        String namespace = ie.getName().getNamespaceURI();
        assertTrue("namespace value should be \"\" but is " + namespace, namespace.equals(""));
    }
}
