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
import org.apache.woden.internal.wsdl20.IncludeImpl;

/**
 * Unit tests for the IncludeElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class IncludeElementTest extends TestCase {

	private IncludeElement fInclude = new IncludeImpl();
	private DescriptionElement fDescriptionElement = null;
	private URI fURI = null;
    private WSDLFactory fFactory = null;
	
	public static Test suite()
	{
	   return new TestSuite(IncludeElementTest.class);
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

	/**
	 * Test that a Description Element can be successfully set and retrieved from an IncludeElement
	 */
	public void testSetGetDescriptionElement()
	{
		fInclude.setDescriptionElement(fDescriptionElement);
		assertEquals("The retrieved Description Element object is not that which was set", 
                fDescriptionElement, fInclude.getDescriptionElement());
	}

	/**
	 * Test that a Location URI can be successfully set and retrieved from an IncludeElement
	 */
	public void testSetGetLocation()
	{
		try 
		{
			fURI = new URI("http://apache.org/test");
		} catch (URISyntaxException e) {
			
		}
		fInclude.setLocation(fURI);
		assertEquals("The retrieved Location URI object is not that which was set", fURI, fInclude.getLocation());
	}

}
