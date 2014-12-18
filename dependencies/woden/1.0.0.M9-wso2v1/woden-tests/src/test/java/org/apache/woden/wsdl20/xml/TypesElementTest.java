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
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.schema.ImportedSchemaImpl;
import org.apache.woden.internal.schema.InlinedSchemaImpl;
import org.apache.woden.internal.wsdl20.TypesImpl;
import org.apache.woden.schema.ImportedSchema;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.schema.Schema;

/**
 * Unit tests for the TypesElement class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */

public class TypesElementTest extends TestCase {

    private DescriptionElement fDescriptionElement = null;
	private TypesElement fTypesElement = null;
	private Schema fInlinedSchema1 = null;
	private Schema fInlinedSchema2 = null;
	private Schema fImportedSchema1 = null;
	private Schema fImportedSchema2 = null;
    private final String TNS = "http://example.org";
	
	public static Test suite()
	{
	   return new TestSuite(TypesElementTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        WSDLFactory factory = null;
        factory = WSDLFactory.newInstance();
        fDescriptionElement = factory.newDescription();
        fDescriptionElement.setTargetNamespace(URI.create(TNS));
    	fTypesElement = fDescriptionElement.addTypesElement();
    	fInlinedSchema1 = new InlinedSchemaImpl();
    	fInlinedSchema2 = new InlinedSchemaImpl();
    	fImportedSchema1 = new ImportedSchemaImpl();
    	fImportedSchema2 = new ImportedSchemaImpl();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
    /*
     * Test adding both Inlined and Imported schema objects
     */
	public void testAddGetSchemas()
	{
		Schema[] schemas = fTypesElement.getSchemas();
		assertEquals(0, schemas.length);
    	fTypesElement.addSchema(fImportedSchema1);
		fTypesElement.addSchema(fImportedSchema2);
		fTypesElement.addSchema(fInlinedSchema1);
		fTypesElement.addSchema(fInlinedSchema2);
		schemas = fTypesElement.getSchemas();
		assertNotNull(schemas);
		assertEquals(4, schemas.length);
	}

	public void testRemoveSchema()
	{
		Schema[] schemas = null;
		Schema randomSchema = new ImportedSchemaImpl();
		
		// remove from empty list
		fTypesElement.removeSchema(randomSchema);
    	schemas = fTypesElement.getSchemas();
		assertEquals(0, schemas.length);
		
		
		fTypesElement.addSchema(fImportedSchema1);
		fTypesElement.addSchema(fImportedSchema2);
		fTypesElement.addSchema(fInlinedSchema1);
		fTypesElement.addSchema(fInlinedSchema2);
		fTypesElement.removeSchema(fInlinedSchema2);
		schemas = fTypesElement.getSchemas();
		assertEquals(3, schemas.length);
    	fTypesElement.removeSchema(fImportedSchema1);
    	schemas = fTypesElement.getSchemas();
		assertEquals(2, schemas.length);
		
		// attempt to remove an un-added schema
		fTypesElement.removeSchema(randomSchema);
    	schemas = fTypesElement.getSchemas();
		assertEquals(2, schemas.length); // number should be unchanged
		
		fTypesElement.removeSchema(fImportedSchema2);
		fTypesElement.removeSchema(fInlinedSchema1);
    	schemas = fTypesElement.getSchemas();
		assertEquals(0, schemas.length);	
	}

	public void testGetImportedSchemas()
	{
    	fTypesElement.addSchema(fImportedSchema1);
		fTypesElement.addSchema(fImportedSchema2);
		fTypesElement.addSchema(fInlinedSchema1);
		fTypesElement.addSchema(fInlinedSchema2);
		
		ImportedSchema[] schemas = fTypesElement.getImportedSchemas();
		assertEquals(2, schemas.length);
		
		// verify object equivalence
		List schemaL = Arrays.asList(schemas);
		assertTrue(schemaL.contains(fImportedSchema1));
		assertTrue(schemaL.contains(fImportedSchema2));
		
	}
	
	public void testGetInlinedSchemas()
	{
    	fTypesElement.addSchema(fImportedSchema1);
		fTypesElement.addSchema(fImportedSchema2);
		fTypesElement.addSchema(fInlinedSchema1);
		fTypesElement.addSchema(fInlinedSchema2);
		
		InlinedSchema[] schemas = fTypesElement.getInlinedSchemas();
		assertEquals(2, schemas.length);
		
		// verify object equivalence
		List schemaL = Arrays.asList(schemas);
		assertTrue(schemaL.contains(fInlinedSchema1));
		assertTrue(schemaL.contains(fInlinedSchema2));				
	}
	
	public void testSetGetTypeSystem()
	{
		String typeSystem = "http://www.w3.org/2001/XMLSchema";
		fTypesElement.setTypeSystem(typeSystem);
		assertEquals(typeSystem, fTypesElement.getTypeSystem());		
	}
	
}
