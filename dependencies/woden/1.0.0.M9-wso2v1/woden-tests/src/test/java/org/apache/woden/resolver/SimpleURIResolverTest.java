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
package org.apache.woden.resolver;


import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorHandler;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.internal.resolver.SimpleURIResolver;
import org.apache.woden.schema.ImportedSchema;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.schema.Schema;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.Binding;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.Interface;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.TypesElement;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaInclude;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;

/**
 * Functional verification test of SimpleURIResolver.
 * 
 * @author Graham Turrell
 */
public class SimpleURIResolverTest extends TestCase 
{
    private Binding[] fBindings = null;
    private Interface[] fInterfaces = null;
    private Schema[] fSchemas = null;
	private ImportedSchema[] fImportedSchemas = null;
	private InlinedSchema[] fInlinedSchemas = null;
    private String fCatalogPropValue = null;
    private String fHttpProxyHostValue = null;
    private String fHttpProxyHostProperty = "http.proxyHost";
    private String fCatalogProperty = "org.apache.woden.resolver.simpleresolver.catalog";
    private String fWsdlPath = "org/apache/woden/resolver/resources/woden14.wsdl";
    private String fCatalogPath = "org/apache/woden/resolver/resources/simpleresolver.catalog";
    
    public static Test suite()
    {
        return new TestSuite(SimpleURIResolverTest.class);
    }
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        // disable external http connections to ensure that test resolves only to local files
        // specify a fantasy proxy host server fo use by HttpURLConnection ...
        fHttpProxyHostValue = System.setProperty(fHttpProxyHostProperty, "proxy.thedomainforthisproxydoesnotexist.com");
        
        // ensure any external (http) connection now fails
    	URLConnection testConnection = null;
    	try
        {
        	testConnection = new URL("www.apache.org").openConnection();
        } 
    	catch (IOException e){
        	
        }
        assertNull("Expected no connection IOException due to non-existent HTTP Proxy.", testConnection);
        
        // locate catalog file and set org.apache.woden.resolver.simpleresolver.catalog
        // property to be that file.
        
        URL catalogURL = getClass().getClassLoader().getResource(fCatalogPath);
        assertNotNull("Failed to find the Resolver Catalog document on the classpath using the path: " + fCatalogPath + ".", 
        		catalogURL);
        // simulate the standard compiler "-D" option ...
        fCatalogPropValue = System.setProperty(fCatalogProperty, catalogURL.toString()); 
        
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        ErrorHandler handler = new TestErrorHandler();
        reader.setFeature(WSDLReader.FEATURE_VALIDATION, true);
        reader.getErrorReporter().setErrorHandler(handler);
        
        URL wsdlURL = getClass().getClassLoader().getResource(fWsdlPath);
        assertNotNull("Failed to find the WSDL document on the classpath using the path: " + fWsdlPath + ".", 
                wsdlURL);
        

		// set the resolver explicitly, using the root as a base URL 
		// (SimpleURIResolver() default constructor is the default behaviour)
        
        // Base/root URL property no longer required in this test - now uses classpath
        //URL rootURL = getClass().getClassLoader().getResource(fResources);
        //assertNotNull("Failed to find the Base URL document on the classpath using the path: " + fResources + ".", 
        //        wsdlURL);
        //System.setProperty(fCatalogBaseProperty, rootURL.toString());
		reader.setURIResolver(new SimpleURIResolver());
		
		Description descComp = reader.readWSDL(wsdlURL.toString());
        assertNotNull("The reader did not return a WSDL description.", descComp);
        
        fBindings = descComp.getBindings();
        fInterfaces = descComp.getInterfaces();
        
        DescriptionElement descElem = descComp.toElement();
        TypesElement types = descElem.getTypesElement();	
		fSchemas = types.getSchemas();
		fImportedSchemas = types.getImportedSchemas();
		fInlinedSchemas = types.getInlinedSchemas();
        
        assertEquals("The Description should contain 1 Binding component.", 1, fBindings.length);
        assertEquals("The Description should contain 1 Interface component.", 1, fInterfaces.length);
        assertEquals("The Description should contain 1 imported schema.", 1, fImportedSchemas.length); //the schema for W3C XML Schema imported by the DOM impl
        assertEquals("The Description should contain 1 included (inlined) schema.", 1, fInlinedSchemas.length);
        assertEquals("The Description should contain total of 2 imported & inlined schema.", 2, fSchemas.length);
    }
    
  /**
  * Test that the included binding matches the expected value parsed from the WSDL.
  */
 public void testCheckWSDLInclude()
 {
     Binding binding1 = fBindings[0];
    
     assertEquals("Unexpected binding name.", "{http://example.com/bank}BankSOAPBinding", binding1.getName().toString());
 }
 
 /**
  * Test that the imported interface matches the expected value parsed from the WSDL.
  */
 public void testCheckWSDLImport()
 {
     Interface interface1 = fInterfaces[0];
    
     assertEquals("Unexpected interface name.", "{http://example.com/bank}Bank", interface1.getName().toString());
 }
 
 /**
  * Test that the imported schema document matches the expected value parsed from the WSDL.
  */
 public void testCheckSchemaImport()
 {
	 Schema schema1 = fSchemas[0];
     XmlSchema xmlSchema = schema1.getSchemaDefinition();
     assertEquals("Unexpected targetNamespace.", "http://example.org/getAccountDetails/", schema1.getNamespace().toString());
     
     XmlSchemaObjectCollection schemaIncludes = xmlSchema.getIncludes(); // returns both includes and imports
	 Iterator schemaIterator = schemaIncludes.getIterator();
	 while (schemaIterator.hasNext()) 
	 {
		XmlSchemaExternal xso = (XmlSchemaExternal) schemaIterator.next();
		if (xso instanceof XmlSchemaImport) 
		{
			assertEquals("Unexpected namespace.", "http://example.org/getOverdraft", ((XmlSchemaImport)xso).getNamespace());
			assertEquals("Unexpected schemaLocation.", "http://test.com/getOverdraft.xsd", xso.getSchemaLocation());
		}
	 }
 }
 
 /**
  * Test that the included schema document matches the expected value parsed from the WSDL.
  */
 public void testCheckSchemaInclude()
 {
	 InlinedSchema schema1 = fInlinedSchemas[0];
	 XmlSchema xmlSchema = schema1.getSchemaDefinition();
	 assertEquals("Unexpected targetNamespace.", "http://example.org/getAccountDetails/", schema1.getNamespace().toString());
	     
	 XmlSchemaObjectCollection schemaIncludes = xmlSchema.getIncludes(); // returns both includes and imports
     Iterator schemaIterator = schemaIncludes.getIterator();
	 while (schemaIterator.hasNext()) 
	 {
		XmlSchemaExternal xso = (XmlSchemaExternal) schemaIterator.next();
		if (xso instanceof XmlSchemaInclude) 
		{
			assertEquals("Unexpected schemaLocation.", "http://test.com/getBalance.xsd", xso.getSchemaLocation());
		}
	 }
 }
 
 
 protected void tearDown() throws Exception 
 {
	 Properties p = System.getProperties();
	 if (fCatalogPropValue == null)
	 {
		 p.remove(fCatalogProperty);
	 }
	 else 
	 {
		 p.setProperty(fCatalogProperty, fCatalogPropValue);
	 }
	 
	 if (fHttpProxyHostValue == null)
	 {
		 p.remove(fHttpProxyHostProperty);
	 }
	 else 
	 {
		 p.setProperty(fHttpProxyHostProperty, fHttpProxyHostValue);
	 }

	 // above required as following throws NPE when arg1 == null (contrary to spec)
	 //System.setProperty(fCatalogProperty, fCatalogPropValue);
	 //System.setProperty("http.proxyHost", fHttpProxyHostValue);
	 
	 System.setProperties(p);
 }
   
}
