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
package org.apache.woden.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.XMLElement;
import org.apache.woden.internal.DOMXMLElement;
import org.apache.woden.internal.ErrorReporterImpl;
import org.apache.woden.internal.schema.ImportedSchemaImpl;
import org.apache.woden.internal.schema.InlinedSchemaImpl;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SchemaTest extends TestCase {

    public static Test suite()
    {
       return new TestSuite(SchemaTest.class);
    }
       
    public void testSetGetNamespace() {
        Schema schema = new InlinedSchemaImpl();
        URI expectedNS = URI.create("http://example.com");
        schema.setNamespace(expectedNS);
        URI actualNS = schema.getNamespace();
        assertEquals("Unexpected namespace URI", expectedNS, actualNS);
    }

    public void testSetGetSchemaDefinition() throws Exception {
        String schemaString = 
            "<schema targetNamespace=\"urn:abc\" />";
        byte[] schemaBytes = schemaString.getBytes();
        InputStream iStream = new ByteArrayInputStream(schemaBytes);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(iStream);
        Element elem = doc.getDocumentElement();
        
        XmlSchemaCollection xsc = new XmlSchemaCollection();
        XmlSchema expectedSchemaDef = xsc.read(elem);
        
        Schema schema = new InlinedSchemaImpl();
        schema.setSchemaDefinition(expectedSchemaDef);
        XmlSchema actualSchemaDef = schema.getSchemaDefinition();
        assertEquals("Unexpected XmlSchema", expectedSchemaDef, actualSchemaDef);
    }

    public void testSetGetXMLElement() throws Exception {
        XMLElement expectedEl = null;
        expectedEl = new DOMXMLElement(new ErrorReporterImpl());
        Schema schema = new ImportedSchemaImpl();
        schema.setXMLElement(expectedEl);
        XMLElement actualEl = schema.getXMLElement();
        assertEquals("Unexpected XMLElement", expectedEl, actualEl);
    }

}
