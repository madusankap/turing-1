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

/**
 * 
 */
package testcase.extensions.foo;

import java.net.URL;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.tests.TestErrorHandler;
import org.apache.woden.wsdl20.Binding;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.extensions.ExtensionRegistry;

/**
 * @author Peter Danielsen 
 *
 */
public class FooBindingExtensionsTest extends TestCase {

    private WSDLReader fReader;
    private Binding[] fBindings = null;
    private String fWsdlPath = "testcase/extensions/foo/resources/FooBindingExtensions.wsdl";
    private TestErrorHandler testErrorHandler = null;

    public static Test suite() {
    	return new TestSuite(FooBindingExtensionsTest.class);
    }

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
        System.setProperty(ExtensionRegistry.REGISTRAR_PROPERTY, 
                "testcase.extensions.foo.FooExtensionRegistrar");
        WSDLFactory factory = WSDLFactory.newInstance();
        fReader = factory.newWSDLReader();
        testErrorHandler = new TestErrorHandler();
        //Don't set validation on, as the testcase WSDL is not intended to be a valid WSDL 2.0 doc.
        fReader.getErrorReporter().setErrorHandler(testErrorHandler);
        
        
        URL wsdlURL = getClass().getClassLoader().getResource(fWsdlPath);
        assertNotNull("Failed to find the WSDL document on the classpath using the path: " + fWsdlPath + ".", 
                wsdlURL);
        
        Description descComp = fReader.readWSDL(wsdlURL.toString());
        assertNotNull("The reader did not return a WSDL description.", descComp);
        
        fBindings = descComp.getBindings();
        assertEquals("The Description should contain 2 Binding components.", 2, fBindings.length);       
	}

    protected void tearDown() throws Exception {
        super.tearDown();
        System.getProperties().remove(ExtensionRegistry.REGISTRAR_PROPERTY);
    }

	/**
     * Test that the value for the {bar} property returned by the <code>getFooBar</code> 
     * method matches the expected value parsed from the WSDL.
	 */
	public void testGetFooBar() {
        Binding binding = null;
        FooBindingExtensions exts = null;
        binding = fBindings[0];
        exts = (FooBindingExtensions)binding
    		.getComponentExtensionContext(FooConstants.NS_URI_FOO);
        assertNotNull("The Binding '" + binding.getName() + "' does not contain an FooBindingExtensions object.",
            exts);
        Integer actual = exts.getFooBar();
        assertEquals("Unexpected number of errors", 1, testErrorHandler.numErrors);
        assertEquals("Unexpected error key", "Errors: FOO-001 \n", testErrorHandler.getSummaryOfMessageKeys());

    	binding = fBindings[1];
        exts = (FooBindingExtensions)binding
        	.getComponentExtensionContext(FooConstants.NS_URI_FOO);
        assertNotNull("The Binding '" + binding.getName() + "' does not contain an FooBindingExtensions object.",
                exts);
        
        actual = exts.getFooBar();
        assertNotNull("The value for bar was null", actual);
        assertEquals("Unexpected value for bar.", 3, actual.intValue());
	}

	/**
     * Test that the value for the {baz} property returned by the <code>getFooBaz</code> 
     * method matches the expected value parsed from the WSDL.
	 */
	public void testGetFooBaz() {
        Binding binding1 = fBindings[1];
        FooBindingExtensions exts = (FooBindingExtensions)binding1
        	.getComponentExtensionContext(FooConstants.NS_URI_FOO);
        assertNotNull("The Binding '" + binding1.getName() + "' does not contain an FooBindingExtensions object.",
                exts);
        
        String actual = exts.getFooBaz();
        assertNotNull("The value for bar was null", actual);
        assertEquals("Unexpected value for baz.", "john", actual);
	}

}
