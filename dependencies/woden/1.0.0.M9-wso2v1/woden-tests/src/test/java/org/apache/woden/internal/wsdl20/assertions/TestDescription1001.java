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
package org.apache.woden.internal.wsdl20.assertions;

import java.net.URI;

import junit.framework.TestCase;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.internal.resolver.SimpleURIResolver;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.validation.WodenContext;
import org.apache.woden.wsdl20.xml.DescriptionElement;

/**
 * Test class for assertion class Description1001.
 * 
 * @author Lawrence Mandel (lmandel@apache.org)
 */
public class TestDescription1001 extends TestCase {

	private WSDLFactory factory = null;
	private Description1001 assertion = new Description1001();
	private ErrorReporter reporter;
	private TestErrorHandler handler;
	private WodenContext wodenContext;
	
	protected void setUp() throws Exception {
	    try {
	        factory = WSDLFactory.newInstance();
	    } catch (WSDLException e) {
	        fail("Can't instantiate the WSDLFactory object.");
	    }
	    
	    WSDLReader reader = factory.newWSDLReader();
        reader.setFeature(WSDLReader.FEATURE_VALIDATION, true);
	    reporter = reader.getErrorReporter();
        handler = new TestErrorHandler();
	    reporter.setErrorHandler(handler);
		wodenContext = new WodenContextImpl(reporter, new SimpleURIResolver());
	}
	
	protected void tearDown() throws Exception {
		factory = null;
		reporter = null;
		handler = null;
		wodenContext = null;
	}

	/**
	 * Test that the assertion reports a warning for a namespace
	 * with the urn scheme.
	 */
	public void testTargetNamespaceSchemeurn() {
		DescriptionElement descEl = factory.newDescription();
		descEl.setTargetNamespace(URI.create("urn:sample"));

		try {
			assertion.validate(descEl, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Description1001 threw the following WSDLException for target namespace urn:sample: " + e.toString());
		}
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Description1001 reports an error instead of a warning for an target namespace with the scheme urn.");
		}
		else if(!handler.messageHasBeenReported()) {
			fail("Assertion Description1001 did not report a warning for a target namespace with the scheme urn.");
		}
	}
	
	/**
	 * Test that the assertion reports a warning for a namespace
	 * with an unknown host http://example.sample. 
	 */
	public void testTargetNamespaceDoesNotResolveUnknownHost() {
		DescriptionElement descEl = factory.newDescription();
		descEl.setTargetNamespace(URI.create("http://example.sample"));

		try {
			assertion.validate(descEl, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Description1001 threw the following WSDLException for target namespace http://example.sample, which doesn't resolve: " + e.toString());
		}
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Description1001 reports an error instead of a warning for an target namespace that doesn't resolve.");
		}
		else if(!handler.messageHasBeenReported()) {
			fail("Assertion Description1001 did not report a warning for a target namespace that doesn't resolve.");
		}
	}
	
	/**
	 * Test that the assertion reports no warning for a namespace
	 * that resolves such as http://apache.org.
	 */
	public void testTargetNamespaceResolves() {
		DescriptionElement descEl = factory.newDescription();
		descEl.setTargetNamespace(URI.create("http://ws.apache.org"));

		try {
			assertion.validate(descEl, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Description1001 threw the following WSDLException for target namespace http://apache.org, which should resolve: " + e.toString());
		}
		if(handler.messageHasBeenReported()) {
			fail("Assertion Description1001 reports an error or warning for an target namespace that should resolve. Note that this test will fail without network connectivity.");
		}
	}
}
