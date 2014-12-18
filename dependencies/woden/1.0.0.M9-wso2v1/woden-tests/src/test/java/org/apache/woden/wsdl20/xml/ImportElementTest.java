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
import java.net.URISyntaxException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.internal.wsdl20.ImportImpl;

/**
 * Unit tests for the ImportElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class ImportElementTest extends TestCase {

	private ImportElement fImport = new ImportImpl();
	private DescriptionElement fDescriptionElement = null;
	private URI fURI = null;	
    private WSDLFactory fFactory = null;
	
	public static Test suite()
	{
	   return new TestSuite(ImportElementTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        
        try {
            fFactory = WSDLFactory.newInstance();
        } catch (WSDLException e) {
            fail("Can't instantiate the WSDLFactory object.");
        }
        
        fDescriptionElement = fFactory.newDescription();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
	/**
	 * Test that a Description Element can be successfully set and retrieved from an ImportElement
	 */
	public void testSetGetDescriptionElement()
	{
		fImport.setDescriptionElement(fDescriptionElement);
		assertEquals("The retrieved Description Element object is not that which was set", 
                fDescriptionElement, fImport.getDescriptionElement());
	}

	/**
	 * Test that a Location URI can be successfully set and retrieved from an ImportElement
	 */
	public void testSetGetLocation()
	{
		try 
		{
			fURI = new URI("http://apache.org/test");
		} catch (URISyntaxException e) {
			
		}
		fImport.setLocation(fURI);
		assertEquals("The retrieved Location URI object is not that which was set", fURI, fImport.getLocation());
	}
	
	public void testSetGetNamespace()
	{
		try 
		{
			fURI = new URI("http://apache.org/test");
		} catch (URISyntaxException e) {
			
		}
		fImport.setNamespace(fURI);
		assertEquals("The retrieved Location URI object is not that which was set", fURI, fImport.getNamespace());
	}
	
	/**
	 * Test that an ImportElement without its optional Namespace attribute set, returns null from its getter method
	 */
	public void testGetNamespaceDefault()
	{	
		assertNull("The namespace was unset but appears set.", fImport.getNamespace());
	}
	
    public void testGetParentElement() {
        DescriptionElement desc = fFactory.newDescription();
        ImportElement importEl = desc.addImportElement();
        DescriptionElement parent = (DescriptionElement)importEl.getParentElement();
        assertSame("The import's parent should be the description that created it", desc, parent);
                
    }
}
