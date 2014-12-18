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

import javax.xml.namespace.QName;

import org.apache.woden.ErrorReporter;
import org.apache.woden.WSDLException;
import org.apache.woden.XMLElement;
import org.apache.woden.internal.ErrorLocatorImpl;
import org.apache.woden.internal.xml.XMLAttrImpl;

public class FooOddAttrImpl extends XMLAttrImpl implements FooOddAttr {

    public FooOddAttrImpl(XMLElement ownerEl, QName attrType, 
            String attrValue, ErrorReporter errRpt) throws WSDLException
    {
        super(ownerEl, attrType, attrValue, errRpt);
    }
    
	protected Object convert(XMLElement ownerEl, String attrValue)
			throws WSDLException {
		Integer result = null;
		try {
			int value = Integer.parseInt(attrValue);
			if (value % 2 == 1) {
				result = new Integer(value);
				setValid(true);
				return result;
			}
		} catch (Exception e) {
			// fall through
		}
		// Even numbers and exceptions are treated as errors
		getErrorReporter().reportError(new ErrorLocatorImpl(), // TODO line&col nos.
				"FOO-001", new Object[] { attrValue },
				ErrorReporter.SEVERITY_ERROR);
		return result;
	}

	public int getValue() {
		return isValid() ? ((Integer)getContent()).intValue() : -1;
	}

}
