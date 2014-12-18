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
package org.apache.woden;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.xml.DescriptionElement;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestCase to test DOM based WSDLWriter implementation.
 *
 * @author sagara Gunathunga
 *
 */
public class DOMWSDLWriterTest extends TestCase {

	    private String finputWsdlPath = null;
	    private String foutputWsdlPath = null;
	    private Description fDescription = null;
	    private WSDLFactory FWSDLFactory=null;
	    private WSDLReader fReader=null;
	    private WSDLWriter fWriter=null;

	    private final String inputPath="org/apache/woden/primer-hotelReservationService.wsdl";
	    //change this path according to the local file system.
	    private final String outputPath="out.wsdl";
	    
	    public static Test suite(){
            return new TestSuite(DOMWSDLWriterTest.class);
          }

	    protected void setUp() throws Exception {
	        super.setUp();
	        finputWsdlPath =inputPath;
	        foutputWsdlPath = outputPath;
	        //This will ensure that DOM based WSDLFactory creation.
	        FWSDLFactory=WSDLFactory.newInstance("org.apache.woden.internal.DOMWSDLFactory");
	    }


	    protected void tearDown() throws Exception {
	        super.tearDown();
	        finputWsdlPath = null;
	        foutputWsdlPath = null;
	        fDescription = null;
	        FWSDLFactory=null;
	        fReader=null;
	        fWriter=null;
	    }


	    /**
		 * Test method for {@link org.apache.woden.WSDLWriter#writeWSDL(org.apache.woden.wsdl20.xml.DescriptionElement, java.io.Writer)}.
		 */
		public void testWriteWSDLDescriptionElementWriter() {
			try {
	            fReader=FWSDLFactory.newWSDLReader();
	            fWriter=FWSDLFactory.newWSDLWriter();
	            FileWriter fFileWriter=new FileWriter(foutputWsdlPath);
	            URL wsdlInputURL = getClass().getClassLoader().getResource(inputPath);
	            DescriptionElement descElem = (DescriptionElement)fReader.readWSDL(wsdlInputURL.toString());
	            assertNotNull("DescriptionElement can not  be null", descElem);
	            fDescription=descElem.toComponent();
	            fWriter.writeWSDL(descElem, fFileWriter);
	            fFileWriter.flush();
	            fFileWriter.close();
	            File outFile=new File(outputPath);
	            String outFilePath=outFile.toURL().toString();
	            DescriptionElement outDescElem = (DescriptionElement)fReader.readWSDL(outFilePath);
	            assertNotNull("DescriptionElement can not  be null", outDescElem);	            
	            //TODO - complete after the WODEN-209
	            /*
	            assertEquals("Two Description component should be same ",
	            		  outDescElem.toComponent(), fDescription);  
	            		  */
	            
	            //delete the temp file.
	            outFile.delete();

	        } catch (WSDLException e) {
	            fail("Can not instantiate the WSDLReader or WSDLWriter object.");
	        }catch (IOException e) {
	            fail("Can not access the specified file");
	        }
		}

		/**
		 * Test method for {@link org.apache.woden.WSDLWriter#writeWSDL(org.apache.woden.wsdl20.xml.DescriptionElement, java.io.OutputStream)}.
		 */
		public void testWriteWSDLDescriptionElementOutputStream() {
			try {
		        fReader=FWSDLFactory.newWSDLReader();
		        fWriter=FWSDLFactory.newWSDLWriter();
		        FileOutputStream fFileStream=new FileOutputStream(foutputWsdlPath);
		        URL wsdlInputURL = getClass().getClassLoader().getResource(inputPath);
		        DescriptionElement descElem = (DescriptionElement)fReader.readWSDL(wsdlInputURL.toString());
		        assertNotNull("DescriptionElement can not  be null", descElem);
		        fDescription=descElem.toComponent();
		        fWriter.writeWSDL(descElem, fFileStream);
		        fFileStream.flush();
		        fFileStream.close();
		        File outFile=new File(outputPath);
                String outFilePath=outFile.toURL().toString();
		        DescriptionElement outDescElem = (DescriptionElement)fReader.readWSDL(outFilePath);
		        assertNotNull("DescriptionElement can not  be null", outDescElem);
		        //TODO - complete after the WODEN-209
                /*
                assertEquals("Two Description component should be same ",
                          outDescElem.toComponent(), fDescription);  
                          */     
		      
		        //delete the temp file.
                outFile.delete();

		    } catch (WSDLException e) {
		        fail("Can not instantiate the WSDLReader or WSDLWriter object.");
		    }catch (IOException e) {
		        fail("Can not access the specified file");
		    }
		    }


		}






