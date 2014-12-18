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
package testcase.extensions.foo;

import java.net.URI;

import javax.xml.namespace.QName;

public class FooConstants {
    
    // Namespace URIs.
    public static final String NS_STRING_FOO =
        "http://www.w3.org/ns/wsdl/foo";
    
    public static final URI NS_URI_FOO = URI.create(NS_STRING_FOO);

    // Prefixes
    public static final String PFX_WFOO = "wfoo";

    // Attribute names
    public static final String ATTR_BAR = "bar";
    public static final String ATTR_BAZ = "baz";

    // Qualified attribute names.
    public static final QName Q_ATTR_BAR=
        new QName(NS_STRING_FOO, ATTR_BAR, PFX_WFOO);
    
    public static final QName Q_ATTR_BAZ=
        new QName(NS_STRING_FOO, ATTR_BAZ, PFX_WFOO);
    
    // Property names
    public static final String PROP_FOO_BAR = "foo bar";
    public static final String PROP_FOO_BAZ = "foo baz";

}
