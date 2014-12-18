
/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Nov 12, 2014 (10:31:03 IST)
 */

        
            package org.wso2.carbon.dataservices.ui.stub.admin;
        
            /**
            *  ExtensionMapper class
            */
            @SuppressWarnings({"unchecked","unused"})
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://script.core.dataservices.carbon.wso2.org/xsd".equals(namespaceURI) &&
                  "PaginatedTableInfo".equals(typeName)){
                   
                            return  org.wso2.carbon.dataservices.ui.stub.admin.core.xsd.PaginatedTableInfo.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://admin.core.dataservices.carbon.wso2.org".equals(namespaceURI) &&
                  "Exception".equals(typeName)){
                   
                            return  org.wso2.carbon.dataservices.ui.stub.admin.Exception.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    