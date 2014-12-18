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

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.XMLElement;

/**
 * Unit tests for DocumentationImpl class.
 * 
 * @author John Kaputin (jkaputin@apache.org)
 *
 */
public class DocumentationElementTest extends TestCase {

    public static Test suite()
    {
       return new TestSuite(DocumentationElementTest.class);
    }
    
    /**
     * This is a Bare minimum test XMLElement to test the DocumentationElement.
     */
    private class TestXMLElement implements XMLElement {
        public String getAttributeValue(String attrName) {
            return null;
        }

        public XMLElement[] getChildElements() {
            return null;
        }

        public XMLElement getFirstChildElement() {
            return null;
        }

        public String getLocalName() {
            return null;
        }

        public URI getNamespaceURI() throws WSDLException {
            return null;
        }

        public XMLElement getNextSiblingElement() {
            return null;
        }

        public QName getQName() {
            return null;
        }

        public QName getQName(String prefixedValue) throws WSDLException {
            return null;
        }

        public Object getSource() {
            return null;
        }

        public void setSource(Object elem) {            
        }
    }

    private WSDLFactory fFactory = null;
    
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        try {
            fFactory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
    }
    
    /**
     * Test method for {@link org.apache.woden.internal.wsdl20.DocumentationImpl#setContent(java.lang.Object)}.
     * Test method for {@link org.apache.woden.internal.wsdl20.DocumentationImpl#setContent(java.lang.Object)}.
     */
    public void testSetGetContent() {
        DescriptionElement descElem = fFactory.newDescription();
        DocumentationElement docElem = descElem.addDocumentationElement();
        
        //check that the doc element is empty when first created
        assertNull(docElem.getContent());
        
        //test the setter and getter methods for doc elem content
        XMLElement xmlel = new TestXMLElement();
        docElem.setContent(xmlel);
        assertEquals(xmlel, docElem.getContent());
    }

    /**
     * Test method for {@link org.apache.woden.internal.wsdl20.DocumentationImpl#getParentElement()}.
     */
    public void testGetParentElement() {
        DescriptionElement descElem1 = fFactory.newDescription();
        DocumentationElement docElem = descElem1.addDocumentationElement();
        
        //check the parent
        assertEquals(descElem1, docElem.getParentElement());
    }

}
