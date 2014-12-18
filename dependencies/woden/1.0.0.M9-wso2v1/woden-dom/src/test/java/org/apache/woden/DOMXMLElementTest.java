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

import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.woden.internal.DOMXMLElement;
import org.apache.woden.internal.ErrorReporterImpl;
import org.apache.woden.internal.wsdl20.Constants;
import org.apache.woden.internal.util.dom.DOMQNameUtils;
import org.apache.woden.internal.util.dom.DOMUtils;

public class DOMXMLElementTest extends TestCase {

    private URL wsdlURL = getClass().getClassLoader().getResource("org/apache/woden/primer-hotelReservationService.wsdl");
    private InputSource inputSource = new InputSource(wsdlURL.toString());
    private ErrorReporter errorReporter;
    private Element elem = null;

  public static Test suite(){
    return new TestSuite(DOMXMLElementTest.class);
  }

  protected void setUp() throws Exception{
      try {
          //replaced with JAXP
          /*
          DOMParser parser = new DOMParser();
          parser.parse(inputSource);
          Document doc = parser.getDocument();
          elem = doc.getDocumentElement();
          */
          DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
          factory.setNamespaceAware(true);
          DocumentBuilder builder=factory.newDocumentBuilder();
          Document doc = builder.parse(inputSource);
          elem = doc.getDocumentElement();
          
          
          errorReporter = new ErrorReporterImpl();

      } catch (IOException e) {
          e.printStackTrace();
      } catch (SAXException e) {
          e.printStackTrace();
      }
  }

  protected void tearDown() throws Exception{
      elem = null;
  }

  public void testGetFirstChildElement() throws WSDLException {

      DOMXMLElement domXMLElement = new DOMXMLElement(errorReporter);
      domXMLElement.setSource(elem);
      assertNotNull(domXMLElement.getFirstChildElement());
  }

  /* TODO implement this method only if getAttributes() is added to XMLElement.
   * 
  public void testGetAttributes() throws WSDLException {

      //The <binding> element in the hotelReservation WSDL has many attributes
      //So, let's test with that element
      DOMXMLElement domXMLElement = new DOMXMLElement(errorReporter);
      domXMLElement.setSource(elem);
      Object obj;
      Element tempEl;
      if ((obj = domXMLElement.getFirstChildElement().getSource()) instanceof Element){
          tempEl = (Element)obj;
          while (tempEl != null){
                if (DOMQNameUtils.matches(Constants.Q_ELEM_BINDING, tempEl)){
                    domXMLElement.setSource(tempEl);
                    assertNotNull(domXMLElement.getAttributes());
                }
              tempEl = DOMUtils.getNextSiblingElement(tempEl);
          }
      }
  }
  */

    public void testGetAttributeValue() throws WSDLException {
        //The <binding> element in the hotelReservation WSDL has many attributes
       //So, let's test with that element
       DOMXMLElement domXMLElement = new DOMXMLElement(errorReporter);
       domXMLElement.setSource(elem);
       Object obj;
       Element tempEl;
       if ((obj = domXMLElement.getFirstChildElement().getSource()) instanceof Element){
           tempEl = (Element)obj;
           while (tempEl != null){
                 if (DOMQNameUtils.matches(Constants.Q_ELEM_BINDING, tempEl)){
                     domXMLElement.setSource(tempEl);
                     assertNotNull(domXMLElement.getAttributeValue("name"));
                 }
               tempEl = DOMUtils.getNextSiblingElement(tempEl);
           }
       }
    }

    public void testGetQName() throws WSDLException {
        DOMXMLElement domXMLElement = new DOMXMLElement(errorReporter);
        domXMLElement.setSource(elem);
        Object obj;
        Element tempEl;
        if ((obj = domXMLElement.getSource()) instanceof Element){
            tempEl = (Element)obj;
            while (tempEl != null){
                  if (DOMQNameUtils.matches(Constants.Q_ELEM_BINDING, tempEl)){
                      domXMLElement.setSource(tempEl);
                      assertNotNull(domXMLElement.getQName("wsoap:protocol"));
                  }
                tempEl = DOMUtils.getNextSiblingElement(tempEl);
            }
        }
    }

}
