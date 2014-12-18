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
package org.apache.woden.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.wsdl20.Description;

public class W3CTestSuiteTest extends TestCase 
{
  private WSDLFactory factory = null;
  private WSDLReader reader = null;
  private TestErrorHandler handler;
	  
  public static Test suite()
  {
	return new TestSuite(W3CTestSuiteTest.class);
  }

  protected void setUp() throws Exception 
  {
    try
	{
	  factory = WSDLFactory.newInstance();
	  reader = factory.newWSDLReader();  
	  reader.setFeature(WSDLReader.FEATURE_VALIDATION, true);
	} 
	catch (Exception e) 
    {
	}
	handler = new TestErrorHandler();
	reader.getErrorReporter().setErrorHandler(handler);
  }
 
  protected void tearDown() throws Exception 
  {
    factory = null;
	reader = null;
	handler = null;
  }
  
  /**
   * BAD TEST CASES
   * All of the following test cases should report errors. 
   * TODO: Add in error checks as the WSDL 2.0 validator is developed.
   */
  
  /**
   * Test for the test-suite/documents/bad/Chameleon-1B W3C test.
   */
  public void testChameleon1B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Chameleon-1B/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Chameleon-2B W3C test.
   */
  public void testChameleon2B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Chameleon-2B/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Import-1B W3C test.
   */
  public void testImport1B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Import-1B/XSDImport.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
	  
  /**
   * Test for the test-suite/documents/bad/Import-2B W3C test.
   */
  public void testImport2B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Import-2B/XSDImportInWSDL.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
	  
  /**
   * Test for the test-suite/documents/bad/Import-3B W3C test.
   */
  public void testImport3B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Import-3B/XSDImport2.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Interface-1B W3C test.
   */
  public void testInterface1B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Interface-1B/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Interface-2B W3C test.
   */
  public void testInterface2B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Interface-2B/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Interface-3B W3C test.
   */
  public void testInterface3B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Interface-3B/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Service-1B W3C test.
   */
  public void testService1B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-1B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Service-2B W3C test.
   */
  public void testService2B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-2B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }

  /**
   * Test for the test-suite/documents/bad/Service-12B W3C test.
   */
  public void testService12B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-12B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Service-13B W3C test.
   */
  public void testService13B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-13B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Service-14B W3C test.
   */
  public void testService14B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-14B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/Service-15B W3C test.
   */
  public void testService15B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/Service-15B/Service.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/bad/TicketAgent-1B W3C test.
   */
  public void testTicketAgent1B()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/bad/TicketAgent-1B/TicketAgent-bad.wsdl");
	  assertNotNull("Description is null.", desc);
      // TODO: determine the assertions that should fail for this test.
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * GOOD TEST CASES
   * All of the following test cases shouldn't report errors. 
   * TODO: Add in error checks as the WSDL 2.0 validator is developed.
   */
  
  /**
   * Test for the test-suite/documents/good/Chameleon-1G W3C test.
   */
  public void testChameleon1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Chameleon-1G/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Chameleon-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Chameleon-2G W3C test.
   */
  public void testChameleon2G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Chameleon-2G/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Chameleon-2G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Chameleon-3G W3C test.
   */
  public void testChameleon3G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Chameleon-3G/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Chameleon-3G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Chameleon-4G W3C test.
   */
  public void testChameleon4G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Chameleon-4G/getBalance.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Chameleon-4G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/CreditCardFaults-1G W3C test.
   */
  public void testCreditCardFaults1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/CreditCardFaults-1G/use-credit-card-faults.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good CreditCardFaults-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/GreatH-1G W3C test.
   */
  public void testGreatH1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/GreatH-1G/primer-hotelReservationService.wsdl");
      assertTrue("number of bindings isn't 1", desc.getBindings().length == 1);
      assertTrue("interfacename is null", desc.getBindings()[0].getInterface()!=null);

	  assertNotNull("Description is null.", desc);
	  assertFalse("The good GreatH-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Import-1G W3C test.
   */
  public void testImport1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Import-1G/XSDImport.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Import-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Import-2G W3C test.
   */
  public void testImport2G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Import-2G/XSDImport2.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Import-2G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/ImportedWSDL-1G W3C test.
   */
  public void testImportedWSDL1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/ImportedWSDL-1G/updateDetails.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good ImportedWSDL-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.errorMessageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-1G W3C test.
   */
  public void testInterface1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-1G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-2G W3C test.
   */
  public void testInterface2G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-2G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-2G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-3G W3C test.
   */
  public void testInterface3G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-3G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-3G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-4G W3C test.
   */
  public void testInterface4G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-4G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-4G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-5G W3C test.
   */
  public void testInterface5G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-5G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-5G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-6G W3C test.
   */
  public void testInterface6G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-6G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-6G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Interface-7G W3C test.
   */
  public void testInterface7G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Interface-7G/Interface.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Interface-7G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/MultipleInlineSchemas-1G W3C test.
   */
  public void testMultipleInlineSchemas1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/MultipleInlineSchemas-1G/retrieveItems.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good MultipleInlineSchemas-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/SchemaId-1G W3C test.
   */
  public void testSchemaId1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/SchemaId-1G/schemaIds.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good SchemaId-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/SchemaLocationFragment-1G W3C test.
   */
  public void testSchemaLocationFragment1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/SchemaLocationFragment-1G/Items.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good SchemaLocationFragment-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Service-1G W3C test.
   */
  public void testService1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Service-1G/Service.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Service-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Service-2G W3C test.
   */
  public void testService2G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Service-2G/Service.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Service-2G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/Service-3G W3C test.
   */
  public void testService3G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/Service-3G/Service.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good Service-3G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/ServiceReference-1G W3C test.
   */
  public void testServiceReference1G1()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/ServiceReference-1G/reservationList.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good ServiceReference-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/ServiceReference-1G W3C test.
   */
  public void testServiceReference1G2()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/ServiceReference-1G/reservationDetails.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good ServiceReference-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/TicketAgent-1G W3C test.
   */
  public void testTicketAgent1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/TicketAgent-1G/TicketAgent.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good TicketAgent-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/WeathSvc-1G W3C test.
   */
  public void testWeathSvc1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/WeathSvc-1G/WeathSvc.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good WeathSvc-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/XsImport-1G W3C test.
   */
  public void testXsImport1G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/XsImport-1G/reservation.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good XsImport-1G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/XsImport-2G W3C test.
   */
  public void testXsImport2G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/XsImport-2G/reservationDetails.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good XsImport-2G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
  
  /**
   * Test for the test-suite/documents/good/XsImport-3G W3C test.
   */
  public void testXsImport3G()
  {
	try
	{
	  Description desc = reader.readWSDL("http://dev.w3.org/cvsweb/~checkout~/2002/ws/desc/test-suite/documents/good/XsImport-3G/reservationDetails.wsdl");
	  assertNotNull("Description is null.", desc);
	  assertFalse("The good XsImport-3G test returned errors. " + handler.getSummaryOfMessageKeys(), handler.messageHasBeenReported());
	}
	catch(Exception e)
	{
	  fail("Unable to read WSDL document because of " + e);
	}
  }
}
