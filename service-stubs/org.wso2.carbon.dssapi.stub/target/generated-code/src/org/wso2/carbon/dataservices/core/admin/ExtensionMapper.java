
/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:23 IST)
 */

        
            package org.wso2.carbon.dataservices.core.admin;
        
            /**
            *  ExtensionMapper class
            */
            @SuppressWarnings({"unchecked","unused"})
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://mgt.service.carbon.wso2.org/xsd".equals(namespaceURI) &&
                  "ServiceMetaDataWrapper".equals(typeName)){
                   
                            return  org.wso2.carbon.service.mgt.xsd.ServiceMetaDataWrapper.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://mgt.service.carbon.wso2.org/xsd".equals(namespaceURI) &&
                  "ServiceMetaData".equals(typeName)){
                   
                            return  org.wso2.carbon.service.mgt.xsd.ServiceMetaData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://admin.core.dataservices.carbon.wso2.org".equals(namespaceURI) &&
                  "Exception".equals(typeName)){
                   
                            return  org.wso2.carbon.dataservices.core.admin.Exception.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    