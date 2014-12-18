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
package org.apache.woden.wsdl20;

import java.net.URI;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.internal.wsdl20.ElementDeclarationImpl;

/**
 * Unit tests for the implementation of ElementDeclaration interface.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class ElementDeclarationTest extends TestCase {

	private ElementDeclaration fElementDeclaration = null;
	private URI fTypeSystem = null;
	
	public static Test suite()
	{
	   return new TestSuite(ElementDeclarationTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
 
        fElementDeclaration = new ElementDeclarationImpl();
		fTypeSystem = new URI("http://www.w3.org/2001/XMLSchema"); 
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
    
	/*
	 * Test that getContent() correctly returns the assigned ElementDeclaration content object
	 */
	public void testGetContent()
	{
		String content = "edContentObject";
		((ElementDeclarationImpl) fElementDeclaration).setContent(content);
		assertEquals("The ElementDeclaration content Object differs from that set -", content, fElementDeclaration.getContent());
	}
	
	/*
	 * Test that getContentModel() correctly returns the assigned ElementDeclaration content model reference
	 */
	public void testGetContentModel()
	{
		String contentModel = ElementDeclaration.API_APACHE_WS_XS; // one of the presets available
		((ElementDeclarationImpl) fElementDeclaration).setContentModel(contentModel);
		assertEquals("The ElementDeclaration content model String differs from that set -", contentModel, fElementDeclaration.getContentModel());
	}
	
	/*
	 * Test that getName() correctly returns the assigned ElementDeclaration name
	 */
	public void testGetName()
	{	
		((ElementDeclarationImpl)fElementDeclaration).setName(new QName("edName"));
		assertEquals("The ElementDeclaration name QName differs from that set-", "edName", fElementDeclaration.getName().toString());
	}
	
	/*
	 * Test that getSystem() correctly returns the assigned ElementDeclaration type system
	 */
	public void testGetSystem()
	{
		((ElementDeclarationImpl) fElementDeclaration).setSystem(fTypeSystem);
		assertEquals("The ElementDeclaration type system URI differs from that set-", fTypeSystem, fElementDeclaration.getSystem());
	}
	

}
