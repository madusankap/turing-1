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
package org.apache.woden.wsdl20.extensions.http;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Functional verification test of HTTPLocationTemplate.
 * 
 * @author John Kaputin (jkaputin@apache.org)
 */
public class HTTPLocationTemplateTest extends TestCase 
{
    public static Test suite()
    {
        return new TestSuite(HTTPLocationTemplateTest.class);
    }
    
    public void testCtor() throws Exception
    {
        HTTPLocationTemplate template;
        
        //foo, encoded, in query
        template = new HTTPLocationTemplate("foo", true, true);
        assertNotNull(template);
        
        //foo, encoded, in path
        template = new HTTPLocationTemplate("foo", true, false);
        assertNotNull(template);
        
        //foo, raw, in path
        template = new HTTPLocationTemplate("foo", false, false);
        assertNotNull(template);
        
        //foo, raw, in query
        template = new HTTPLocationTemplate("foo", false, true);
        assertNotNull(template);
        
        //TODO tests for null or invalid names
    }
    
    public void testGetName() {
        
        HTTPLocationTemplate template;
        template = new HTTPLocationTemplate("foo", true, true);
        assertNotNull(template);
        assertEquals("Incorrect template name", "foo", template.getName());
        
    }
    
    public void testSetGetValue() {
        
        HTTPLocationTemplate template;
        template = new HTTPLocationTemplate("foo", true, true);
        
        String actual = template.getValue();
        assertNull(actual);
        
        template.setValue("bar");
        actual = template.getValue();
        assertNotNull(actual);
        assertEquals("Unexpected value", "bar", actual);
        
        template.setValue(null);
        actual = template.getValue();
        assertNull(actual);
    }
    
    public void testIsEncoded() {
        
        HTTPLocationTemplate template;
        
        template = new HTTPLocationTemplate("foo", true, true);
        assertTrue(template.isEncoded());
        
        template = new HTTPLocationTemplate("foo", false, true);
        assertFalse(template.isEncoded());
    }
        
    public void testIsQuery() {
        
        HTTPLocationTemplate template;
        
        template = new HTTPLocationTemplate("foo", true, true);
        assertTrue(template.isQuery());
        
        template = new HTTPLocationTemplate("foo", true, false);
        assertFalse(template.isQuery());
    }
    
}
