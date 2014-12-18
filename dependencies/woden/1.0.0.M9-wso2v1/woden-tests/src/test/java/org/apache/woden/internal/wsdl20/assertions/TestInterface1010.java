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
import java.net.URISyntaxException;

import junit.framework.TestCase;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.internal.wsdl20.InterfaceImpl;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.validation.WodenContext;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;

/**
 * Test class for assertion class Interface1009.
 * 
 * @author Lawrence Mandel (lmandel@apache.org)
 */
public class TestInterface1010 extends TestCase {

	private WSDLFactory factory = null;
	private Interface1010 assertion = new Interface1010();
	private ErrorReporter reporter;
	private TestErrorHandler handler;
	private WodenContext wodenContext;
	
	protected void setUp() throws Exception {
	    try {
	        factory = WSDLFactory.newInstance();
	    } catch (WSDLException e) {
	        fail("Can't instantiate the WSDLFactory object.");
	    }
	    
	    handler = new TestErrorHandler();
	    reporter = factory.newWSDLReader().getErrorReporter();
		reporter.setErrorHandler(handler);
		wodenContext = new WodenContextImpl(reporter, null);
	}
	
	protected void tearDown() throws Exception {
		factory = null;
		reporter = null;
		handler = null;
		wodenContext = null;
	}

	/**
	 * Test that the assertion passes for an empty list of interfaces.
	 */
	public void testNoInterfaces() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
		try {
			descEl.setTargetNamespace(new URI("http://testnamespace"));
		} catch(URISyntaxException e) {
			// Do nothing.
		}
	      
		try {
			assertion.validate(descComp, wodenContext);
		} catch(WSDLException e) {
			fail("Assertion Interface1010 threw WSDLException.");
		}
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1010 failed incorrectly with no interfaces specified.");
		}
	}
	
	/**
	 * Test that the assertion passes when only one interface is defined.
	 */
	public void testOneInterface() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
		try {
			descEl.setTargetNamespace(new URI("http://testnamespace"));
		} catch(URISyntaxException e) {
			// Do nothing.
		}
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name1"));
		
		try {
			assertion.validate(descComp, wodenContext);
		} catch(WSDLException e) {
			fail("Assertion Interface1010 threw WSDLException.");
		}
		
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1010 failed incorrectly with a single interface defined.");
		}
	}
	
	/**
	 * Test that the assertion passes for a list of interfaces that contains no duplicate names.
	 */
	public void testMultipleInterfaceNoDuplicateNames() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
		try {
			descEl.setTargetNamespace(new URI("http://testnamespace"));
		} catch(URISyntaxException e) {
			// Do nothing.
		}
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name1"));
		InterfaceElement interfac2 = descEl.addInterfaceElement();
		interfac2.setName(new NCName("name2"));
		InterfaceElement interfac3 = descEl.addInterfaceElement();
		interfac3.setName(new NCName("name3"));
		  
		try {
			assertion.validate(descComp, wodenContext);
		} catch(WSDLException e) {
			fail("Assertion Interface1010 threw WSDLException.");
		}
		
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1010 failed incorrectly with multiple interfaces defined with no duplicate names.");
		}
	}
	
	/**
	 * Test that the assertion fails for two interfaces that are defined with the same NCName object.
	 */
	public void testDuplicateInterfaceNCNames() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
		try {
			descEl.setTargetNamespace(new URI("http://testnamespace"));
		} catch(URISyntaxException e) {
			// Do nothing.
		}
		
		NCName name = new NCName("name");
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(name);
		InterfaceElement interfac2 = descEl.addInterfaceElement();
		interfac2.setName(name);
		// Need to create an extended interface otherwise the component model treats the
		// two interfaces as equal and only adds one to the set.
		InterfaceImpl interfac3 = (InterfaceImpl)descEl.addInterfaceElement();
		interfac3.setName(new NCName("name3"));
		interfac2.addExtendedInterfaceName(interfac3.getName());
		  
		try {
			assertion.validate(descComp, wodenContext);
		} catch(WSDLException e) {
			fail("Assertion Interface1010 threw WSDLException.");
		}
		  
		if(!handler.errorMessageHasBeenReported()) {
		    fail("Assertion Interface1010 passed incorrectly for two interfaces defined with the same NCName.");
		}
	}
	
	/**
	 * Test that the assertion fails for two interfaces that are defined with the same name.
	 */
	public void testDuplicateInterfaceNames() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
		try {
			descEl.setTargetNamespace(new URI("http://testnamespace"));
		} catch(URISyntaxException e) {
			// Do nothing.
		}
		
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name"));
		InterfaceElement interfac2 = descEl.addInterfaceElement();
		interfac2.setName(new NCName("name"));
		// Need to create an extended interface otherwise the component model treats the
		// two interfaces as equal and only adds one to the set.
		InterfaceElement interfac3 = descEl.addInterfaceElement();
		interfac3.setName(new NCName("name3"));
		interfac2.addExtendedInterfaceName(interfac3.getName());
		  
		try {
			assertion.validate(descComp, wodenContext);
		} catch(WSDLException e) {
			fail("Assertion Interface1010 threw WSDLException.");
		}
		  
		if(!handler.errorMessageHasBeenReported()) {
		    fail("Assertion Interface1010 passed incorrectly for two interfaces defined with the same name.");
		}
	}
	
}
