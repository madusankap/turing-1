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
package org.apache.woden.wsdl20;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.woden.WSDLFactory;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.apache.woden.wsdl20.xml.InterfaceElement;
import org.apache.woden.wsdl20.xml.InterfaceFaultElement;
import org.apache.woden.wsdl20.xml.InterfaceOperationElement;

/**
 * Unit tests for the Interface class.
 * 
 * @author Graham Turrell (gturrell@apache.org)
 */
public class InterfaceTest extends TestCase {

	// create a parent Description to hang the Interfaces off
	private DescriptionElement fDescriptionElement = null;
	private Description fDescription = null;
	private InterfaceElement fInterfaceElement = null;
	private Interface fInterface = null;
	private final String INTF_NAME = "interfaceName";
	
	public static Test suite()
	{
	   return new TestSuite(InterfaceTest.class);
	}
	   
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception 
    {
        super.setUp();
        //fDescriptionElement = new DescriptionImpl();
        fDescriptionElement = WSDLFactory.newInstance().newDescription();
        fInterfaceElement = fDescriptionElement.addInterfaceElement();
		fInterfaceElement.setName(new NCName(INTF_NAME));
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception 
    {
        super.tearDown();
    }
	
    /*
     * Mandatory attribute ("name")
     * - getName() 
     */
	public void testGetName() 
	{	
		QName uri = fInterface.getName();
		assertEquals("Retrieved InterfaceElement name does not match that set -", INTF_NAME, uri.toString());
	}
	
	/*
	 * Gets Interfaces referenced by the "extends" attribute (optional)
	 */
	public void testGetExtendedInterfaceInterfaces() 
	{
		/*
		 * create some InterfaceElements to extend, add them to parent,
		 * access them via :
		 * - getExtendedInterface()
		 * - getExtendedInterfaces()
		 */
		
		// check the default:
		Interface[] ifArray = fInterface.getExtendedInterfaces();
		assertEquals("Retrieved Extended Interface group should be empty if none set -", 0, ifArray.length);
			
		// create further InterfaceElements and name them
		InterfaceElement xife1 = fDescriptionElement.addInterfaceElement();
		InterfaceElement xife2 = fDescriptionElement.addInterfaceElement();
		xife1.setName(new NCName("extendedI1"));
		xife2.setName(new NCName("extendedI2"));
		fInterfaceElement.addExtendedInterfaceName(new QName("extendedI1"));
		fInterfaceElement.addExtendedInterfaceName(new QName("extendedI2"));
		
		// getExtendedInterfaces()
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
		ifArray = fInterface.getExtendedInterfaces();
		assertEquals("Incorrect number of retrieved Extended Interfaces -", 2, ifArray.length);
		// verify object equivalence
		List ifL = Arrays.asList(ifArray);
		assertTrue(ifL.contains(xife1));
		assertTrue(ifL.contains(xife2));
		
		// getExtendedInterface()
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
		Interface retrievedIf = fInterface.getExtendedInterface(new QName("extendedI1"));
		assertEquals("Retrieved Extended Interface unexpected -", (Interface)xife1, retrievedIf);
		retrievedIf = fInterface.getExtendedInterface(new QName("randomUnset"));
		assertNull("The name of a non-existent Interface should not return one -", retrievedIf);
	}
	
	/*
     * Optional element ("fault")
     * - getInterfaceFault() 
     * - getInterfaceFaults() 
     */
	public void testGetInterfaceFaultFaults() 
	{		
		// check the default:
		InterfaceFault[] iffArray = fInterface.getInterfaceFaults();
		assertNotNull("Expected an array of InterfaceFaults -", iffArray);
		assertEquals("Retrieved InterfaceFault group should be empty if none set -", 0, iffArray.length);

		// create some InterfaceFaults
		InterfaceFaultElement iffe1 = fInterfaceElement.addInterfaceFaultElement();
		InterfaceFaultElement iffe2 = fInterfaceElement.addInterfaceFaultElement();

		// getInterfaceFaults()
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
		iffArray = fInterface.getInterfaceFaults();
		assertNotNull("Expected an array of InterfaceFaults -", iffArray);
		assertEquals("Incorrect number of retrieved InterfaceFaults -", 2, iffArray.length);

		// verify all Fault objects returned
		List iffeL = Arrays.asList(iffArray);
		assertTrue(iffeL.contains(iffe1));
		assertTrue(iffeL.contains(iffe2));
	
		// getInterfaceFault()
		// name one of them
		iffe1.setName(new NCName("FaultName"));
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
		InterfaceFault retrievedIff = fInterface.getInterfaceFault(new QName("FaultName"));
		assertNotNull(retrievedIff);
		assertEquals("Retrieved InterfaceFault differs from that expected", (InterfaceFault)iffe1, retrievedIff);
		// try a non-existent fault - should return null
		retrievedIff = fInterface.getInterfaceFault(new QName("nonExistentFault"));
		assertNull(retrievedIff);
	}    
    
	/*
     * Optional element ("operation")
     * - getInterfaceOperation() 
     * - getInterfaceOperations() 
     */
	public void testGetInterfaceOperationOperations() 
	{		
		// check the default:
		InterfaceOperation[] ifopArray = fInterface.getInterfaceOperations();
		assertNotNull("Expected an array of InterfaceOperations -", ifopArray);
		assertEquals("Retrieved InterfaceOperation group should be empty if none set -", 0, ifopArray.length);

		// create some InterfaceOperationElement() to test
		InterfaceOperationElement ifop1 = fInterfaceElement.addInterfaceOperationElement();
		InterfaceOperationElement ifop2 = fInterfaceElement.addInterfaceOperationElement();

		// getInterfaceOperations()
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));

		ifopArray = fInterface.getInterfaceOperations();
		assertNotNull("Expected an array of InterfaceOperationElements -", ifopArray);
		assertEquals("Incorrect number of retrieved InterfaceOperationElements -", 2, ifopArray.length);

		// verify all Operation objects returned
		List ifopL = Arrays.asList(ifopArray);
		assertTrue(ifopL.contains(ifop1));
		assertTrue(ifopL.contains(ifop2));
	
		// getInterfaceOperation()
		// name one of them
		ifop1.setName(new NCName("OperationName"));
		fDescription = fDescriptionElement.toComponent();
		fInterface = fDescription.getInterface(new QName(INTF_NAME));
		InterfaceOperation retrievedIfop = fInterface.getInterfaceOperation(new QName("OperationName"));
		assertNotNull(retrievedIfop);
		assertEquals("Retrieved InterfaceOperationElement differs from that expected", ifop1, retrievedIfop);
		// try a non-existent operation - should return null
		retrievedIfop = fInterface.getInterfaceOperation(new QName("nonExistentOperation"));
		assertNull(retrievedIfop);
	}   
	
    /*
     * Gets Interface Faults declared and inherited.
     */
    public void testGetAllInterfaceFaults() 
    {
        /*
         * create some InterfaceElements to extend, add them to parent,
         * add some named faults and access them via:
         * - getAllInterfaceFaults()
         * - getFromAllInterfaceFaults(QName)
         */
        
        // create InterfaceElements and name them
        InterfaceElement ife = fDescriptionElement.addInterfaceElement();
        InterfaceElement xife1 = fDescriptionElement.addInterfaceElement();
        InterfaceElement xife2 = fDescriptionElement.addInterfaceElement();
        ife.setName(new NCName("Interface"));
        xife1.setName(new NCName("extendedI1"));
        xife2.setName(new NCName("extendedI2"));
        ife.addExtendedInterfaceName(new QName("extendedI1"));
        ife.addExtendedInterfaceName(new QName("extendedI2"));
        
        InterfaceFaultElement fault = ife.addInterfaceFaultElement();
        InterfaceFaultElement fault1 = xife1.addInterfaceFaultElement();
        InterfaceFaultElement fault2 = xife2.addInterfaceFaultElement();
        InterfaceFaultElement fault3 = xife2.addInterfaceFaultElement();
        
        fault.setName(new NCName("fault"));
        fault1.setName(new NCName("fault1"));
        fault2.setName(new NCName("fault2"));
        fault3.setName(new NCName("fault3"));
        
        
        fDescription = fDescriptionElement.toComponent();
        Interface intface = fDescription.getInterface(new QName("Interface"));
        
        //test getAllInterfaceFaults()
        InterfaceFault[] allFaults = intface.getAllInterfaceFaults();
        assertEquals("Incorrect number of interface faults", 4, allFaults.length);
        
        //test getFromAllInterfaceFaults(QName)
        InterfaceFault intFault1 = intface.getFromAllInterfaceFaults(new QName("fault1"));
        assertEquals(fault1, intFault1);
        
        InterfaceFault intFault3 = intface.getFromAllInterfaceFaults(new QName("fault3"));
        assertEquals(fault3, intFault3);
    }
        
    /*
     * Gets Interface Operations declared and inherited.
     */
    public void testGetAllInterfaceOperations() 
    {
        /*
         * create some InterfaceElements to extend, add them to parent,
         * add some named operations and access them via:
         * - getAllInterfaceOperations()
         * - getFromAllInterfaceOperations(QName)
         */
        
        // create InterfaceElements and name them
        InterfaceElement ife = fDescriptionElement.addInterfaceElement();
        InterfaceElement xife1 = fDescriptionElement.addInterfaceElement();
        InterfaceElement xife2 = fDescriptionElement.addInterfaceElement();
        ife.setName(new NCName("Interface"));
        xife1.setName(new NCName("extendedI1"));
        xife2.setName(new NCName("extendedI2"));
        ife.addExtendedInterfaceName(new QName("extendedI1"));
        ife.addExtendedInterfaceName(new QName("extendedI2"));
        
        InterfaceOperationElement oper = ife.addInterfaceOperationElement();
        InterfaceOperationElement oper1 = xife1.addInterfaceOperationElement();
        InterfaceOperationElement oper2 = xife1.addInterfaceOperationElement();
        InterfaceOperationElement oper3 = xife2.addInterfaceOperationElement();
        
        oper.setName(new NCName("oper"));
        oper1.setName(new NCName("oper1"));
        oper2.setName(new NCName("oper2"));
        oper3.setName(new NCName("oper3"));
        
        fDescription = fDescriptionElement.toComponent();
        Interface intface = fDescription.getInterface(new QName("Interface"));
        
        //test getAllInterfaceOperations()
        InterfaceOperation[] allOperations = intface.getAllInterfaceOperations();
        assertEquals("Incorrect number of interface operations", 4, allOperations.length);
        
        //test getFromAllInterfaceOperations(QName)
        InterfaceOperation intOper2 = intface.getFromAllInterfaceOperations(new QName("oper2"));
        assertEquals(oper2, intOper2);
        
        InterfaceOperation intOper3 = intface.getFromAllInterfaceOperations(new QName("oper3"));
        assertEquals(oper3, intOper3);
    }
        
	/*
     * toElement()
     */
	public void testToElement() 
	{	
		assertEquals(fInterfaceElement, fInterface.toElement());
	}

}
