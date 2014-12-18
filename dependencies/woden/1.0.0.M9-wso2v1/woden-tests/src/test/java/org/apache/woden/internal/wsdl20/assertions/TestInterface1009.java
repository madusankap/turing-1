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
public class TestInterface1009 extends TestCase {

	private WSDLFactory factory = null;
	private Interface1009 assertion = new Interface1009();
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
		wodenContext = new WodenContextImpl(reporter, null);
	}
	
	protected void tearDown() throws Exception {
		factory = null;
		reporter = null;
		handler = null;
		wodenContext = null;
	}

	/**
	 * Test that the assertion passes for an interface that 
	 * extends no other interfaces.
	 */
	public void testNoInterfaceExtension() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
		descEl.setTargetNamespace(URI.create("http://testnamespace"));
		
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name"));
		
		// init Interface's ref to its Description, needed for interface extension.
		descComp.getInterfaces();
		try {
			assertion.validate(interfac, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Interface1009 threw a WSDLException.");
		}
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1009 fails incorrectly for an interface that extends no other interfaces.");
		}
	}
	
	/**
	 * Test that the assertion fails if the interface is in the direct list.
	 */
	public void testInterfaceExtendsItselfDirectly() throws Exception {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
        descEl.setTargetNamespace(URI.create("http://testnamespace"));
		  
        InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name1"));
		
	    interfac.addExtendedInterfaceName(interfac.getName());
	      
	    // init Interface's ref to its Description, needed for interface extension.
	    descComp.getInterfaces(); 
		  
	    try {
	    	assertion.validate(interfac, wodenContext);
	    } catch(WSDLException e) {
	    	fail("Assertion Interface1009 threw a WSDLException.");
	    }
	    if(!handler.errorMessageHasBeenReported()) {
	    	fail("Assertion Interface1009 passes incorrectly for an interface that directly extends itself.");
	    }
	}
	
	/**
	 * Test that the assertion fails if the interface is in the indirect list.
	 */
	public void testInterfaceExtendsItselfIndirectly() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
        descEl.setTargetNamespace(URI.create("http://testnamespace"));
        
        InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name1"));
		InterfaceImpl interfac2 = (InterfaceImpl)descEl.addInterfaceElement();
		interfac2.setName(new NCName("name2"));
	      
		interfac.addExtendedInterfaceName(interfac2.getName());
		interfac2.addExtendedInterfaceName(interfac.getName());
	      
		// init Interface's ref to its Description, needed for interface extension.
		descComp.getInterfaces(); 
		  
		try {
			assertion.validate(interfac, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Interface1009 threw a WSDLException.");
		}
		if(!handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1009 passes incorrectly for an interface that indirectly extends itself.");
		}
	}
	
	/**
	 * Test that the assertion passes if the interface is not 
	 * in the direct or indirect list of extended interfaces.
	 */
	public void testInterfaceExtendsOtherInterfaces() {
		DescriptionElement descEl = factory.newDescription();
		Description descComp = descEl.toComponent();
	      
        descEl.setTargetNamespace(URI.create("http://testnamespace"));
	      
		// Create an interface, set it to extend to other interfaces and have
		// one of those interfaces extend a fourth interface.
		InterfaceElement interfac = descEl.addInterfaceElement();
		interfac.setName(new NCName("name1"));
	      
		InterfaceElement interfac2 = descEl.addInterfaceElement();
		interfac2.setName(new NCName("name2"));

		interfac.addExtendedInterfaceName(interfac2.getName());
	      
		InterfaceElement interfac3 = descEl.addInterfaceElement();
		interfac3.setName(new NCName("name3"));
	      
		interfac.addExtendedInterfaceName(interfac3.getName());
	      
		InterfaceElement interfac4 = descEl.addInterfaceElement();
		interfac4.setName(new NCName("name4"));
	      
		interfac2.addExtendedInterfaceName(interfac4.getName());
	        
		// init Interface's ref to its Description, needed for interface extension.
		descComp.getInterfaces(); 
		try {
			assertion.validate(interfac, wodenContext);
		} catch(WSDLException e){
			fail("Assertion Interface1009 threw a WSDLException.");
		}
		
		if(handler.errorMessageHasBeenReported()) {
			fail("Assertion Interface1009 fails incorrectly for an interface that is not in the list of exteneded interfaces.");
		}
	}
}
