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
package org.apache.woden.internal.wsdl20.validation;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.ErrorReporterImpl;
import org.apache.woden.internal.schema.ImportedSchemaImpl;
import org.apache.woden.internal.schema.InlinedSchemaImpl;
import org.apache.woden.internal.wsdl20.Constants;
import org.apache.woden.internal.wsdl20.DescriptionImpl;
import org.apache.woden.schema.ImportedSchema;
import org.apache.woden.schema.InlinedSchema;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.TypesElement;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A test class to test the assertion tests in the WSDLDocumentValidator.
 */
public class WSDLDocumentValidatorTest extends TestCase 
{
  private WSDLDocumentValidator val;
  private ErrorReporter reporter;
  private TestErrorHandler handler;
  
  /**
   * Create a test suite from this test class.
   * 
   * @return A test suite from this test class.
   */
  public static Test suite()
  {
	return new TestSuite(WSDLDocumentValidatorTest.class);
  }
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception 
  {
    val = new WSDLDocumentValidator();
    handler = new TestErrorHandler();
    
    reporter = WSDLFactory.newInstance().newWSDLReader().getErrorReporter();
    reporter.setErrorHandler(handler);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception 
  {
    val = null;
    reporter = null;
    handler = null;
  }
  
  /**
   * Test that the testAssertionDescription1006 method returns
   * true if given a description element with an absolute IRI,
   * false otherwise.
   */
  public void testTestAssertionDescription1006()
  {
	// Test that no error is reported with an absolute IRI.
	handler.reset();
	try
	{
	  DescriptionElement desc = WSDLFactory.newInstance().newDescription();
	  desc.setTargetNamespace(new URI("http://www.sample.org"));
	  
	  if(!val.testAssertionDescription1006(desc, reporter))
	  {
		fail("A message was reported for a description element with an absolute IRI.");
	  }
	}
	catch(Exception e)
	{
	  fail("There was a problem while testing an absolute IRI.");
	}
	
    // Test that error Description-1006 is reported for a relative IRI.
	handler.reset();
	try
	{
	  DescriptionElement desc = WSDLFactory.newInstance().newDescription();
	  desc.setTargetNamespace(new URI("//www.sample.org"));
	  boolean isValid = val.testAssertionDescription1006(desc, reporter);
	  if(isValid)
	  {
		fail("No message was reported for a description element with the relative IRI //www.sample.org.");
	  }
	  else if(handler.warnings.size() > 0)
	  {
		fail("A warning was reported for a description element with the relative IRI //www.sample.org.");
	  }
	  else if(handler.fatalErrors.size() > 0)
	  {
		fail("A fatal error was reported for a description element with the relative IRI //www.sample.org.");
	  }
	  else if(handler.errors.size() != 1)
	  {
		fail("More than one error was reported for a description element with the relative IRI //www.sample.org.");
	  }
	  else if(!handler.errors.containsKey("Description-1006"))
	  {
		fail("The error for Description-1006 was not reported for a description element with the relative IRI //www.sample.org.");
	  }
	}
	catch(Exception e)
	{
	  fail("There was a problem while testing an absolute IRI.");
	}
	  
  }
  
  /**
   * Test assertion Schema-1070. An imported schema must contain the
   * same target namespace as the import element.
   */
  public void testTestAssertionSchema1070()
  {
    // Test that no error is reported for an imported schema that has
	// a null schema. This error should be caught elseware.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	  if(!val.testAssertionSchema1070(importedSchema, reporter))
	  {
		fail("The testAssertionSchema1070 method returned false for a null schema.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
	// Test that no error is reported for an imported schema that has
	// the same target namespace as the imported element.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema("http://www.sample.org", null);
	  importedSchema.setSchemaDefinition(schema);
	  if(!val.testAssertionSchema1070(importedSchema, reporter))
	  {
		fail("The testAssertionSchema1070 method returned false for a schema with the same target namespace as the import element.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
	// Test that an error is reported for an imported schema that has
	// a different defined target namespace than the import element.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema("http://differentnamespace.sample.org", null);
	  importedSchema.setSchemaDefinition(schema);
	  if(val.testAssertionSchema1070(importedSchema, reporter))
	  {
		fail("There was no error reported for a schema with a different target namespace than the import element.");
	  }
	  else if(handler.errors.size() > 1)
	  {
		fail("More than one error was reported for a schema with a different target namespace than the import element.");
	  }
	  else if(!handler.errors.containsKey("Schema-1070"))
	  {
		fail("The error Schema-1070 was not reported for a schema with a different target namespace than the import element.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that an error is reported for an imported schema that has
	// no defined target namespace.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema(null, null);
	  importedSchema.setSchemaDefinition(schema);
	  if(val.testAssertionSchema1070(importedSchema, reporter))
	  {
		fail("There was no error reported for a schema with a null target namespace.");
	  }
	  else if(handler.errors.size() > 1)
	  {
		fail("More than one error was reported for a schema with a null target namespace.");
	  }
	  else if(!handler.errors.containsKey("Schema-1070"))
	  {
		fail("The error Schema-1070 was not reported for a schema with a null target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that an error is reported for an imported schema that has
	// an empty defined target namespace.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema("", null);
	  importedSchema.setSchemaDefinition(schema);
	  if(val.testAssertionSchema1070(importedSchema, reporter))
	  {
		fail("There was no error reported for a schema with an empty target namespace.");
	  }
	  else if(handler.errors.size() > 1)
	  {
		fail("More than one error was reported for a schema with an empty target namespace.");
	  }
	  else if(!handler.errors.containsKey("Schema-1070"))
	  {
		fail("The error Schema-1070 was not reported for a schema with an empty target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
  }
  
  /**
   * Test assertion Schema-1069. An imported schema must contain a
   * target namespace.
   */
  public void testTestAssertionSchema1069()
  {
    // Test that no error is reported for an imported schema that has
	// a null schema. This error should be caught elseware.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	  if(!val.testAssertionSchema1069(importedSchema, reporter))
	  {
		fail("The testAssertionSchema0017 method returned false for a null schema.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
	// Test that no error is reported for an imported schema that has
	// defined a target namespace.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	  XmlSchema schema = new XmlSchema("http://www.sample.org", null);
	  importedSchema.setSchemaDefinition(schema);
	  if(!val.testAssertionSchema1069(importedSchema, reporter))
	  {
		fail("The testAssertionSchema1069 method returned false for a schema with a target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that an error is reported for an imported schema that has
	// no defined target namespace.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema(null, null);
	  importedSchema.setSchemaDefinition(schema);
	  if(val.testAssertionSchema1069(importedSchema, reporter))
	  {
		fail("There was no error reported for a schema with a null target namespace.");
	  }
	  else if(handler.errors.size() > 1)
	  {
		fail("More than one error was reported for a schema with a null target namespace.");
	  }
	  else if(!handler.errors.containsKey("Schema-1069"))
	  {
		fail("The error Schema-1069 was not reported for a schema with a null target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that an error is reported for an imported schema that has
	// an empty defined target namespace.
	handler.reset();
	try
	{
	  ImportedSchemaImpl importedSchema = new ImportedSchemaImpl();
	
	  importedSchema.setNamespace(new URI("http://www.sample.org"));
	
	
	  XmlSchema schema = new XmlSchema("", null);
	  importedSchema.setSchemaDefinition(schema);
	  if(val.testAssertionSchema1069(importedSchema, reporter))
	  {
		fail("There was no error reported for a schema with an empty target namespace.");
	  }
	  else if(handler.errors.size() > 1)
	  {
		fail("More than one error was reported for a schema with an empty target namespace.");
	  }
	  else if(!handler.errors.containsKey("Schema-1069"))
	  {
		fail("The error Schema-1069 was not reported for a schema with an empty target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
  }
  
  /**
   * Test assertion Schema-1073. Inline schemas must not define an element with a name
   * of an element that has already been defined in another inline schema with the same target namespace.
   */
  public void testTestAssertionSchema1073()
  {
	String schemaString = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample.org\">"
		  	+ "<element name=\"myElement\" type=\"string\"/></schema>";
    String schemaStringNS2 = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample2.org\">"
		  	+ "<element name=\"myElement\" type=\"string\"/></schema>";
	// Test that the method returns true for an empty list of inline schemas.
	handler.reset();
	try
	{
      InlinedSchema[] emptySchemaList = new InlinedSchema[0];
      if(!val.testAssertionSchema1073(emptySchemaList, reporter))
	  {
        fail("The testAssertionSchema1073 method returned false for an empty inline schema list.");
	  }
    }
    catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
    
	// Test that two inline schemas with the same target namespace
	// that define the same element produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[2];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	  inlinedSchemas[1] = new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  inlinedSchemas[1].setNamespace(new URI("http://www.sample.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
	  DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);      
      Document schemaDoc1 = builder.parse(is);
      reader = new StringReader(schemaString);
      is = new InputSource(reader);      
      Document schemaDoc2 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
      XmlSchemaCollection xsc2 = new XmlSchemaCollection();
      XmlSchema xs2 = xsc2.read(schemaDoc2.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  inlinedSchemas[1].setSchemaDefinition(xs2);
	  if(val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was no error reported for an inline schema that declares the same element as another inline schema with the same namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
	  
    } catch (ParserConfigurationException e) {
        
      fail("There was a problem parsing the test inline schema document");
      
    } catch (SAXException e) {
        
        fail("There was a problem parsing the test inline schema document");
    }
	

	// Test that two inline schemas with the different target namespaces
	// that define the same element do not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[2];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	  inlinedSchemas[1] = new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  inlinedSchemas[1].setNamespace(new URI("http://www.sample2.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);      
      Document schemaDoc1 = builder.parse(is);
      reader = new StringReader(schemaStringNS2);
      is = new InputSource(reader);
      Document schemaDoc2 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
      XmlSchema xs2 = xsc.read(schemaDoc2.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  inlinedSchemas[1].setSchemaDefinition(xs2);
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema that declares the same element as another inline schema but has a different target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	}
	catch(SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
    }
	
    // Test that only one inline schema does not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);      
      Document schemaDoc1 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema list that contains only one inline schema.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	 }
	catch (SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	}
	
	// Test that an inline schema that can not be read (that's null) does not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));

	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema list that contains a schema that couldn't be read (is null).");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
	//	 Test that only one inline schema does not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);
      Document schemaDoc1 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema list that contains only one inline schema.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	 catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	} 
	catch (SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	}
	
	
	// Test that an inline schema with no defined target namespace doesn't produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema that contains a null namespace.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
  }
  
  /**
   * Test assertion Schema-1073b. Inline schemas must not define a type with a name
   * of an type that has already been defined in another inline schema with the same target namespace.
   */
  public void testTestAssertionSchema1073b()
  {
	String schemaString = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample.org\">"
		  + "<complexType name=\"myType\">"     
		  + "<sequence>"     
          + "<element  name=\"element\" type=\"string\"/>"      
          + "</sequence>"     
          + "</complexType>"
          + "</schema>";
	String schemaStringNS2 = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample2.org\">"
		  + "<complexType name=\"myType\">"     
		  + "<sequence>"     
        + "<element  name=\"element\" type=\"string\"/>"      
        + "</sequence>"     
        + "</complexType>"
        + "</schema>";
	// Test that the method returns true for an empty list of inline schemas.
	handler.reset();
	try
	{
      InlinedSchema[] emptySchemaList = new InlinedSchema[0];
      if(!val.testAssertionSchema1073(emptySchemaList, reporter))
	  {
        fail("The testAssertionSchema1073b method returned false for an empty inline schema list.");
	  }
    }
    catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
    
	// Test that two inline schemas with the same target namespace
	// that define the same type produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[2];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	  inlinedSchemas[1] = new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  inlinedSchemas[1].setNamespace(new URI("http://www.sample.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);     
      Document schemaDoc1 = builder.parse(is);
      reader = new StringReader(schemaString);
      is = new InputSource(reader);
      Document schemaDoc2 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
      XmlSchemaCollection xsc2 = new XmlSchemaCollection();
      XmlSchema xs2 = xsc2.read(schemaDoc2.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  inlinedSchemas[1].setSchemaDefinition(xs2);
	  if(val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was no error reported for an inline schema that declares the same type as another inline schema with the same namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	}
	catch (SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	}
	
	

	// Test that two inline schemas with different target namespaces
	// that define the same type do not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[2];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	  inlinedSchemas[1] = new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  inlinedSchemas[1].setNamespace(new URI("http://www.sample2.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);     
      Document schemaDoc1 = builder.parse(is);
      reader = new StringReader(schemaStringNS2);
      is = new InputSource(reader);
      Document schemaDoc2 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
      XmlSchema xs2 = xsc.read(schemaDoc2.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  inlinedSchemas[1].setSchemaDefinition(xs2);
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema that declares the same element as another inline schema but has a different target namespace.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	}
	catch (SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	}
	
    // Test that only one inline schema does not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));
	  
	  // Create DOM representation of schema, have XmlSchema parse it.
	  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	  factory.setNamespaceAware(true);
      DocumentBuilder builder=factory.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);
      Document schemaDoc1 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      XmlSchema xs1 = xsc.read(schemaDoc1.getDocumentElement());
	  inlinedSchemas[0].setSchemaDefinition(xs1);
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema list that contains only one inline schema.");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(IOException e)
	{
	  fail("There was a problem parsing the test inline schema document");
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	catch (ParserConfigurationException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
	      
	} 
	catch (SAXException e) {
	        
	  fail("There was a problem parsing the test inline schema document");
    }
	
	// Test that an inline schema that can not be read (that's null) does not produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  inlinedSchemas[0].setNamespace(new URI("http://www.sample.org"));

	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema list that contains a schema that couldn't be read (is null).");
	  }
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem setting the namespace of the imported schema: " + e);
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
	// Test that an inline schema with no defined target namespace doesn't produce an error.
	handler.reset();
	try
	{
	  InlinedSchema[] inlinedSchemas = new InlinedSchema[1];
	  inlinedSchemas[0]= new InlinedSchemaImpl();
	
	  if(!val.testAssertionSchema1073(inlinedSchemas, reporter))
	  {
		fail("There was an error reported for an inline schema that contains a null namespace.");
	  }
	}
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
  }
  
  /**
   * Test assertion Interface-1012. Style defaults specified on interface elements
   * must all be absolute.
   */
  public void testTestAssertionInterface1012()
  {
	URI relativeURI = null;
	URI relativeURI2 = null;
	URI absoluteURI = null;
	try
	{
	  relativeURI = new URI("relativeuri");
	  relativeURI2 = new URI("relativeuri2");
	  absoluteURI = new URI("http://absoluteuri");
	}
	catch(URISyntaxException e)
	{
	  fail("There was a problem creating the test URIs: " + e);
	}

    WSDLFactory factory = null;
    try {
        factory = WSDLFactory.newInstance();
    } catch (WSDLException e) {
        fail("Can't instantiate the WSDLFactory object.");
    }
    
	// Test that a schema without any style defaults is valid.
	handler.reset();
    try
	{
      DescriptionElement desc = factory.newDescription();
      InterfaceElement interfaceElem = desc.addInterfaceElement();
	  if(!val.testAssertionInterface1012(interfaceElem, reporter))
	  {
	    fail("The testAssertionInterface1012 method returned false for an interface that specifies no style defaults.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that a schema with an absolute style default is valid.
	handler.reset();
	try
	{
          DescriptionElement desc = factory.newDescription();
      InterfaceElement interfaceElem = desc.addInterfaceElement();
	  interfaceElem.addStyleDefaultURI(absoluteURI);
	  if(!val.testAssertionInterface1012(interfaceElem, reporter))
	  {
	    fail("The testAssertionInterface1012 method returned false for an interface that specifies one absolute style default.");
	  }
    }
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that a schema with a relative style default is not valid.
	handler.reset();
	try
	{
      DescriptionElement desc = factory.newDescription();
	  InterfaceElement interfaceElem = desc.addInterfaceElement();
	  interfaceElem.addStyleDefaultURI(relativeURI);
	  if(val.testAssertionInterface1012(interfaceElem, reporter))
	  {
	    fail("The testAssertionInterface1012 method returned true for an interface that specifies one relative style default.");
	  }
    }
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that a schema with an absolute style default and a relative sytle default is not valid.
	handler.reset();
	try
	{
      DescriptionElement desc = factory.newDescription();
      InterfaceElement interfaceElem = desc.addInterfaceElement();
	  interfaceElem.addStyleDefaultURI(absoluteURI);
	  interfaceElem.addStyleDefaultURI(relativeURI);
	  if(val.testAssertionInterface1012(interfaceElem, reporter))
	  {
	    fail("The testAssertionInterface1012 method returned true for an interface that specifies an absolute style default and a relative style default.");
	  }
    }
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
	
    // Test that a schema with two relative style defaults returns two error messages
	handler.reset();
	try
	{
      DescriptionElement desc = factory.newDescription();
      InterfaceElement interfaceElem = desc.addInterfaceElement();
	  interfaceElem.addStyleDefaultURI(relativeURI);
	  interfaceElem.addStyleDefaultURI(relativeURI2);
	  val.testAssertionInterface1012(interfaceElem, reporter);
	  if(handler.numErrors != 2)
	  {
	    fail("The testAssertionInterface1012 method only reported one error for an interface that specifies two relative style defaults.");
	  }
    }
	catch(WSDLException e)
	{
	  fail("There was a problem running the test assertion method " + e);
    }
  }
  
  /**
   * Test assertion Schema-1066. References to XML schema components must only refer
   * to elements and types in namespaces that have been imported or inlined or that
   * are part of the XML schema namespace.
   * 
   * TODO: Implement tests for specific elements that contain invalid references.
   *       These tests should probably be contained in a test method for validateInterfaces, validateBindings, etc.
   */
  public void testTestAssertionSchema1066()
  {
    WSDLFactory factory = null;
    try {
        factory = WSDLFactory.newInstance();
    } catch (WSDLException e) {
        fail("Can't instantiate the WSDLFactory object.");
    }

	// Create a schema for use in the tests and add it to a types section.
    InlinedSchema schema = new InlinedSchemaImpl();
    XmlSchema xs1 = null;
    URI schemaNS = null;
	try
	{
      String schemaString = "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.sample.org\">"
              + "<complexType name=\"myType\">"     
              + "<sequence>"     
              + "<element  name=\"element\" type=\"string\"/>"      
              + "</sequence>"     
              + "</complexType>" 
              + "<element name=\"myElement\" type=\"string\"/>"
              + "</schema>";
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder builder=dbf.newDocumentBuilder();
	  Reader reader = new StringReader(schemaString);
      InputSource is = new InputSource(reader);
      Document schemaDoc1 = builder.parse(is);
      XmlSchemaCollection xsc = new XmlSchemaCollection();
      xs1 = xsc.read(schemaDoc1.getDocumentElement());
      schemaNS = new URI("http://www.sample.org");
      schema.setSchemaDefinition(xs1);
      schema.setNamespace(schemaNS);
      TypesElement types = factory.newDescription().addTypesElement();
      types.addSchema(schema);
	}
	catch(Exception e)
	{
	  fail("An error occurred while creating the sample types section.");
	}
    
    // Test that a null namespace returns true.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      TypesElement types = descElem.addTypesElement();
      types.addSchema(schema);
      
	  if(!val.testAssertionSchema1066(descElem, null, reporter))
	  {
	    fail("The testAssertionSchema1066 method returned false for a null namespace.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that a reference to a namespace that is defined inline
	// does not return an error.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      TypesElement types = descElem.addTypesElement();
      types.addSchema(schema);
      
	  if(!val.testAssertionSchema1066(descElem, new QName("http://www.sample.org", "myElement"), reporter))
	  {
	    fail("The testAssertionSchema1066 method returned false for a namespace that has been defined inline.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that a reference to a namespace that is imported
	// does not return an error.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      TypesElement typesImported = descElem.addTypesElement();
      ImportedSchema importedSchema = new ImportedSchemaImpl();
      importedSchema.setSchemaDefinition(xs1);
      importedSchema.setNamespace(schemaNS);
      typesImported.addSchema(importedSchema);
      
	  if(!val.testAssertionSchema1066(descElem, new QName("http://www.sample.org", "myElement"), reporter))
	  {
	    fail("The testAssertionSchema1066 method returned false for a namespace that has been imported.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that a reference to the XML Schema namespace does not return an error.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      //descElem.setTypesElement(types);
      
	  if(!val.testAssertionSchema1066(descElem, new QName(Constants.TYPE_XSD_2001, "myElement"), reporter))
	  {
	    fail("The testAssertionSchema1066 method returned false for the XML Schema namespace.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
    // Test that a reference to a namespace that has not been defined inline or imported returns an error.
	// This test also checks that the method functions correctly with no defined types element.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      //descElem.setTypesElement(types);
      
	  if(val.testAssertionSchema1066(descElem, new QName("http://www.sample2.org", "myElement"), reporter))
	  {
	    fail("The testAssertionSchema1066 method returned true for a namespace that is not available..");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
	// Test that a reference to an inline schema that does not define a 
	// target namespace (the targetNamespace is null) does not return an
	// error.
	handler.reset();
    try
	{
      DescriptionElement descElem = factory.newDescription();
      TypesElement typesImported = descElem.addTypesElement();
      InlinedSchema inlinedSchema = new InlinedSchemaImpl();
      typesImported.addSchema(inlinedSchema);
      InlinedSchema inlinedSchema2 = new InlinedSchemaImpl();
      inlinedSchema2.setNamespace(schemaNS);
      typesImported.addSchema(inlinedSchema2);
      
	  if(!val.testAssertionSchema1066(descElem, new QName("http://www.sample.org", "myElement"), reporter))
	  {
	    fail("The testAssertionSchema1066 method returned false for a namespace that has been imported.");
	  }
	}
	catch(WSDLException e)
    {
	  fail("There was a problem running the test assertion method " + e);
	}
	
  }
  
  /**
   * Test the validateTypes method.
   */
  public void testValidateTypes()
  {
	// Test that the method returns true when the type is null.
	try
	{
	  if(!val.validateTypes(null, reporter))
	  {
		fail("The validateTypes method returned false for a null types element.");
	  }
	}
	catch(WSDLException e)
	{
		fail("There was a problem running the test assertion method " + e);
	}
  }
}
