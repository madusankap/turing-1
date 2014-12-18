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

import org.apache.woden.internal.wsdl20.TypeDefinitionImpl;

/**
 * Unit tests for the implementation of TypeDefinition interface.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class TypeDefinitionTest extends TestCase {

	private TypeDefinition fTypeDefinition = null;
	private URI fTypeSystem = null;
	
	public static Test suite()
	{
	   return new TestSuite(TypeDefinitionTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
 
        fTypeDefinition = new TypeDefinitionImpl();
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
	 * Test that getContent() correctly returns the assigned TypeDefinition content object
	 */
	public void testGetContent()
	{
		String content = "tdContentObject";
		((TypeDefinitionImpl) fTypeDefinition).setContent(content);
		assertEquals("The TypeDefinition content Object differs from that set -", content, fTypeDefinition.getContent());
	}
	
	/*
	 * Test that getContentModel() correctly returns the assigned TypeDefinition content model reference
	 */
	public void testGetContentModel()
	{
		String contentModel = TypeDefinition.API_W3C_DOM; // one of the presets available
		((TypeDefinitionImpl) fTypeDefinition).setContentModel(contentModel);
		assertEquals("The TypeDefinition content model String differs from that set -", contentModel, fTypeDefinition.getContentModel());
	}
	
	/*
	 * Test that getName() correctly returns the assigned TypeDefinition name
	 */
	public void testGetName()
	{	
		((TypeDefinitionImpl)fTypeDefinition).setName(new QName("tdName"));
		assertEquals("The TypeDefinition name QName differs from that set-", "tdName", fTypeDefinition.getName().toString());
	}
	
	/*
	 * Test that getSystem() correctly returns the assigned TypeDefinition type system
	 */
	public void testGetSystem()
	{
		((TypeDefinitionImpl) fTypeDefinition).setSystem(fTypeSystem);
		assertEquals("The TypeDefinition type system URI differs from that set-", fTypeSystem, fTypeDefinition.getSystem());
	}
	

}
