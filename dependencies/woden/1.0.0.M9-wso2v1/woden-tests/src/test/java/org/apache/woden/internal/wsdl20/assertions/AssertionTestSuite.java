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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The assertion test suite contains all of the assertion tests.
 * 
 * @author Lawrence Mandel (lmandel@apache.org)
 */
public class AssertionTestSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for org.apache.woden.internal.wsdl20.assertions");
		//$JUnit-BEGIN$
		suite.addTestSuite(TestDescription1001.class);
		suite.addTestSuite(TestInterface1009.class);
		suite.addTestSuite(TestInterface1010.class);
		//$JUnit-END$
		return suite;
	}

}
