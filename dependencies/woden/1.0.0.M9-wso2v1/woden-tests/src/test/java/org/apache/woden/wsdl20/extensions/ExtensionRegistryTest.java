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
package org.apache.woden.wsdl20.extensions;

import java.util.MissingResourceException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;

public class ExtensionRegistryTest extends TestCase {

    private ExtensionRegistry fExtReg;
    
    public static Test suite()
    {
        return new TestSuite(ExtensionRegistryTest.class);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        fExtReg = reader.getExtensionRegistry();
    }
    
    //Test for each API method.

    /*TODO remainder of ExtenionRegistryTest methods
    public void testExtensionRegistry() {
        fail("Not yet implemented");
    }

    public void testGetErrorReporter() {
        fail("Not yet implemented");
    }

    public void testSetDefaultSerializer() {
        fail("Not yet implemented");
    }

    public void testGetDefaultSerializer() {
        fail("Not yet implemented");
    }

    public void testSetDefaultDeserializer() {
        fail("Not yet implemented");
    }

    public void testGetDefaultDeserializer() {
        fail("Not yet implemented");
    }

    public void testRegisterSerializer() {
        fail("Not yet implemented");
    }

    public void testRegisterDeserializer() {
        fail("Not yet implemented");
    }

    public void testQuerySerializer() {
        fail("Not yet implemented");
    }

    public void testQueryDeserializer() {
        fail("Not yet implemented");
    }

    public void testQueryExtElementType() {
        fail("Not yet implemented");
    }

    public void testGetAllowableExtensions() {
        fail("Not yet implemented");
    }

    public void testRegisterExtElementType() {
        fail("Not yet implemented");
    }

    public void testCreateExtElement() {
        fail("Not yet implemented");
    }

    public void testRegisterExtAttributeType() {
        fail("Not yet implemented");
    }

    public void testQueryExtAttributeType() {
        fail("Not yet implemented");
    }

    public void testCreateExtAttribute() {
        fail("Not yet implemented");
    }

    public void testRegisterComponentExtension() {
        fail("Not yet implemented");
    }

    public void testQueryComponentExtension() {
        fail("Not yet implemented");
    }

    public void testQueryComponentExtensionNamespaces() {
        fail("Not yet implemented");
    }

    public void testCreateComponentExtension() {
        fail("Not yet implemented");
    }
    */

    public void testRegisterQueryResourceBundleNames() {
        
        //Test that the core resource bundle is pre-registered.
        String[] names = fExtReg.queryResourceBundleNames();
        assertEquals("Unexpected number of registered resource bundles", 1, names.length);
        assertEquals("Unexpected default resource bundle", "org.apache.woden.internal.Messages", names[0]);
        
        //Test that all registered resource bundles can be queried and that the core resourece
        //bundle is the first one and that extension resource bundles follow.
        String resourceBundleName = "org.apache.woden.wsdl20.extensions.resources.ExtensionMessages";
        fExtReg.registerResourceBundle(resourceBundleName);
        names = fExtReg.queryResourceBundleNames();
        assertEquals("Unexpected number of registered resource bundles", 2, names.length);
        assertEquals("Unexpected default resource bundle", "org.apache.woden.internal.Messages", names[0]);
        assertEquals("Unexpected extension resource bundle", resourceBundleName, names[1]);
    }

    /**
     * Test the formatting of messages.
     * <ul>
     * <li>the message format key is in the core resource bundle,</li>
     * <li>the key is in the extension resource bundle,</li>
     * <li>the key is in the resource bundles for the core and the extension.</li>
     * <li>the key is not found in any resource bundle.</li>
     * <ul> 
     */
    public void testMessageExtension() {
        fExtReg.registerResourceBundle("org.apache.woden.wsdl20.extensions.resources.ExtensionMessages");
        ErrorReporter errorReporter = fExtReg.getErrorReporter();

        // verify formatting of a message in core resource bundle
        assertEquals("Unexpected core message", 
                "The feature name \"foo\" is not recognized.", 
                errorReporter.getFormattedMessage("WSDL006", new Object[]{"foo"}));

        // verify formatting of a message in extension resource bundle
        assertEquals("Unexpected extension message",
                "Attribute \"count\" in element \"ext\" must contain \"int\".",
                errorReporter.getFormattedMessage("EXT001", new Object[]{"count", "ext", "int"}));

        // verify core format is not overridden by extension resource bundle
        assertEquals("Unexpected core message", 
                "The property name must not be null when attempting to get or set a named property.",
                errorReporter.getFormattedMessage("WSDL007", null));

        // verify exception on unknown format key
        String unknownKey = "_UNKNOWN_KEY_";
        try {
            errorReporter.getFormattedMessage(unknownKey, new Object[]{});
            fail("Expected MissingResourceException for "+unknownKey);
        } catch (MissingResourceException mre) {
            assertEquals("Unexpected MissingResourceException key", unknownKey, mre.getKey());
        }
    }
}
