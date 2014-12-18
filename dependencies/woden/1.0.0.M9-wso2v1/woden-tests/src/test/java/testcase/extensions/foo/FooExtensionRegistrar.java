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

import org.apache.woden.internal.xml.StringAttrImpl;
import org.apache.woden.wsdl20.Binding;
import org.apache.woden.wsdl20.extensions.ExtensionRegistrar;
import org.apache.woden.wsdl20.extensions.ExtensionRegistry;
import org.apache.woden.wsdl20.xml.BindingElement;

public class FooExtensionRegistrar implements ExtensionRegistrar {

	public void registerExtensions(ExtensionRegistry registry) {

		// Binding attributes
		
		registry.registerExtAttributeType(BindingElement.class, 
				FooConstants.Q_ATTR_BAR, FooOddAttrImpl.class);

		registry.registerExtAttributeType(BindingElement.class, 
				FooConstants.Q_ATTR_BAZ, StringAttrImpl.class);


		// Component Extensions

		registry.registerComponentExtension(Binding.class,
				FooConstants.NS_URI_FOO,
				FooBindingExtensionsImpl.class);

		// Register error message resource bundle
		
		registry.registerResourceBundle("testcase.extensions.foo.Messages");
	}

}
