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

import org.apache.woden.ErrorReporter;
import org.apache.woden.wsdl20.WSDLComponent;
import org.apache.woden.wsdl20.extensions.BaseComponentExtensionContext;
import org.apache.woden.wsdl20.extensions.ExtensionProperty;
import org.apache.woden.wsdl20.xml.WSDLElement;
import org.apache.woden.xml.StringAttr;

public class FooBindingExtensionsImpl extends BaseComponentExtensionContext 
        implements FooBindingExtensions {

    public FooBindingExtensionsImpl(WSDLComponent parent, 
            URI extNamespace, ErrorReporter errReporter) {
        
        super(parent, extNamespace, errReporter);
    }
    
    /* ************************************************************
     *  Methods declared by ComponentExtensionContext
     *  
     *  These are the abstract methods inherited from BaseComponentExtensionContext,
     *  to be implemented by this subclass.
     * ************************************************************/

    /*
     * (non-Javadoc)
     * @see org.apache.woden.wsdl20.extensions.ComponentExtensionContext#getProperties()
     */
    public ExtensionProperty[] getProperties() {
        
        return new ExtensionProperty[] {
                getProperty(FooConstants.PROP_FOO_BAR),
                getProperty(FooConstants.PROP_FOO_BAZ)};
    }

    /*
     * (non-Javadoc)
     * @see org.apache.woden.wsdl20.extensions.ComponentExtensionContext#getProperty(java.lang.String)
     */
    public ExtensionProperty getProperty(String propertyName) {
        
        if(FooConstants.PROP_FOO_BAR.equals(propertyName)) {
            return newExtensionProperty(FooConstants.PROP_FOO_BAR, getFooBar());
            
        } else if(FooConstants.PROP_FOO_BAZ.equals(propertyName)) {
            return newExtensionProperty(FooConstants.PROP_FOO_BAZ, getFooBaz());
            
        } else {
            return null; //the specified property name does not exist
        }
        
    }
    
    /* ************************************************************
     *  Additional methods declared by FooBindingExtensions
     * ************************************************************/
    
	public Integer getFooBar() {
        FooOddAttr def = (FooOddAttr) ((WSDLElement) getParent()).getExtensionAttribute(FooConstants.Q_ATTR_BAR);
        return def != null ? new Integer(def.getValue()) : null;
	}

	public String getFooBaz() {
		StringAttr def = (StringAttr) ((WSDLElement) getParent()).getExtensionAttribute(FooConstants.Q_ATTR_BAZ);
		return def != null ? def.getString() : null;
	}

}
