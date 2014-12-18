
/**
 * ServiceMetaDataWrapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:23 IST)
 */

            
                package org.wso2.carbon.service.mgt.xsd;
            

            /**
            *  ServiceMetaDataWrapper bean class
            */
            @SuppressWarnings({"unchecked","unused"})
        
        public  class ServiceMetaDataWrapper
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = ServiceMetaDataWrapper
                Namespace URI = http://mgt.service.carbon.wso2.org/xsd
                Namespace Prefix = ns1
                */
            

                        /**
                        * field for NumberOfActiveServices
                        */

                        
                                    protected int localNumberOfActiveServices ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNumberOfActiveServicesTracker = false ;

                           public boolean isNumberOfActiveServicesSpecified(){
                               return localNumberOfActiveServicesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getNumberOfActiveServices(){
                               return localNumberOfActiveServices;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NumberOfActiveServices
                               */
                               public void setNumberOfActiveServices(int param){
                            
                                       // setting primitive attribute tracker to true
                                       localNumberOfActiveServicesTracker =
                                       param != java.lang.Integer.MIN_VALUE;
                                   
                                            this.localNumberOfActiveServices=param;
                                    

                               }
                            

                        /**
                        * field for NumberOfCorrectServiceGroups
                        */

                        
                                    protected int localNumberOfCorrectServiceGroups ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNumberOfCorrectServiceGroupsTracker = false ;

                           public boolean isNumberOfCorrectServiceGroupsSpecified(){
                               return localNumberOfCorrectServiceGroupsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getNumberOfCorrectServiceGroups(){
                               return localNumberOfCorrectServiceGroups;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NumberOfCorrectServiceGroups
                               */
                               public void setNumberOfCorrectServiceGroups(int param){
                            
                                       // setting primitive attribute tracker to true
                                       localNumberOfCorrectServiceGroupsTracker =
                                       param != java.lang.Integer.MIN_VALUE;
                                   
                                            this.localNumberOfCorrectServiceGroups=param;
                                    

                               }
                            

                        /**
                        * field for NumberOfFaultyServiceGroups
                        */

                        
                                    protected int localNumberOfFaultyServiceGroups ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNumberOfFaultyServiceGroupsTracker = false ;

                           public boolean isNumberOfFaultyServiceGroupsSpecified(){
                               return localNumberOfFaultyServiceGroupsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getNumberOfFaultyServiceGroups(){
                               return localNumberOfFaultyServiceGroups;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NumberOfFaultyServiceGroups
                               */
                               public void setNumberOfFaultyServiceGroups(int param){
                            
                                       // setting primitive attribute tracker to true
                                       localNumberOfFaultyServiceGroupsTracker =
                                       param != java.lang.Integer.MIN_VALUE;
                                   
                                            this.localNumberOfFaultyServiceGroups=param;
                                    

                               }
                            

                        /**
                        * field for NumberOfPages
                        */

                        
                                    protected int localNumberOfPages ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNumberOfPagesTracker = false ;

                           public boolean isNumberOfPagesSpecified(){
                               return localNumberOfPagesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return int
                           */
                           public  int getNumberOfPages(){
                               return localNumberOfPages;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param NumberOfPages
                               */
                               public void setNumberOfPages(int param){
                            
                                       // setting primitive attribute tracker to true
                                       localNumberOfPagesTracker =
                                       param != java.lang.Integer.MIN_VALUE;
                                   
                                            this.localNumberOfPages=param;
                                    

                               }
                            

                        /**
                        * field for ServiceTypes
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localServiceTypes ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceTypesTracker = false ;

                           public boolean isServiceTypesSpecified(){
                               return localServiceTypesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getServiceTypes(){
                               return localServiceTypes;
                           }

                           
                        


                               
                              /**
                               * validate the array for ServiceTypes
                               */
                              protected void validateServiceTypes(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param ServiceTypes
                              */
                              public void setServiceTypes(java.lang.String[] param){
                              
                                   validateServiceTypes(param);

                               localServiceTypesTracker = true;
                                      
                                      this.localServiceTypes=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addServiceTypes(java.lang.String param){
                                   if (localServiceTypes == null){
                                   localServiceTypes = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localServiceTypesTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localServiceTypes);
                               list.add(param);
                               this.localServiceTypes =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for Services
                        * This was an Array!
                        */

                        
                                    protected org.wso2.carbon.service.mgt.xsd.ServiceMetaData[] localServices ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServicesTracker = false ;

                           public boolean isServicesSpecified(){
                               return localServicesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return org.wso2.carbon.service.mgt.xsd.ServiceMetaData[]
                           */
                           public  org.wso2.carbon.service.mgt.xsd.ServiceMetaData[] getServices(){
                               return localServices;
                           }

                           
                        


                               
                              /**
                               * validate the array for Services
                               */
                              protected void validateServices(org.wso2.carbon.service.mgt.xsd.ServiceMetaData[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param Services
                              */
                              public void setServices(org.wso2.carbon.service.mgt.xsd.ServiceMetaData[] param){
                              
                                   validateServices(param);

                               localServicesTracker = true;
                                      
                                      this.localServices=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param org.wso2.carbon.service.mgt.xsd.ServiceMetaData
                             */
                             public void addServices(org.wso2.carbon.service.mgt.xsd.ServiceMetaData param){
                                   if (localServices == null){
                                   localServices = new org.wso2.carbon.service.mgt.xsd.ServiceMetaData[]{};
                                   }

                            
                                 //update the setting tracker
                                localServicesTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localServices);
                               list.add(param);
                               this.localServices =
                             (org.wso2.carbon.service.mgt.xsd.ServiceMetaData[])list.toArray(
                            new org.wso2.carbon.service.mgt.xsd.ServiceMetaData[list.size()]);

                             }
                             

     
     
        /**
        *
        * @param parentQName
        * @param factory
        * @return org.apache.axiom.om.OMElement
        */
       public org.apache.axiom.om.OMElement getOMElement (
               final javax.xml.namespace.QName parentQName,
               final org.apache.axiom.om.OMFactory factory) throws org.apache.axis2.databinding.ADBException{


        
               org.apache.axiom.om.OMDataSource dataSource =
                       new org.apache.axis2.databinding.ADBDataSource(this,parentQName);
               return factory.createOMElement(dataSource,parentQName);
            
        }

         public void serialize(final javax.xml.namespace.QName parentQName,
                                       javax.xml.stream.XMLStreamWriter xmlWriter)
                                throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
                           serialize(parentQName,xmlWriter,false);
         }

         public void serialize(final javax.xml.namespace.QName parentQName,
                               javax.xml.stream.XMLStreamWriter xmlWriter,
                               boolean serializeType)
            throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
            
                


                java.lang.String prefix = null;
                java.lang.String namespace = null;
                

                    prefix = parentQName.getPrefix();
                    namespace = parentQName.getNamespaceURI();
                    writeStartElement(prefix, namespace, parentQName.getLocalPart(), xmlWriter);
                
                  if (serializeType){
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://mgt.service.carbon.wso2.org/xsd");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":ServiceMetaDataWrapper",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "ServiceMetaDataWrapper",
                           xmlWriter);
                   }

               
                   }
                if (localNumberOfActiveServicesTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "numberOfActiveServices", xmlWriter);
                             
                                               if (localNumberOfActiveServices==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("numberOfActiveServices cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfActiveServices));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localNumberOfCorrectServiceGroupsTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "numberOfCorrectServiceGroups", xmlWriter);
                             
                                               if (localNumberOfCorrectServiceGroups==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("numberOfCorrectServiceGroups cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfCorrectServiceGroups));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localNumberOfFaultyServiceGroupsTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "numberOfFaultyServiceGroups", xmlWriter);
                             
                                               if (localNumberOfFaultyServiceGroups==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("numberOfFaultyServiceGroups cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfFaultyServiceGroups));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localNumberOfPagesTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "numberOfPages", xmlWriter);
                             
                                               if (localNumberOfPages==java.lang.Integer.MIN_VALUE) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("numberOfPages cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfPages));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceTypesTracker){
                             if (localServiceTypes!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localServiceTypes.length;i++){
                                        
                                            if (localServiceTypes[i] != null){
                                        
                                                writeStartElement(null, namespace, "serviceTypes", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceTypes[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "serviceTypes", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "serviceTypes", xmlWriter);

                                           // write the nil attribute
                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                           xmlWriter.writeEndElement();
                                    
                             }

                        } if (localServicesTracker){
                                       if (localServices!=null){
                                            for (int i = 0;i < localServices.length;i++){
                                                if (localServices[i] != null){
                                                 localServices[i].serialize(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","services"),
                                                           xmlWriter);
                                                } else {
                                                   
                                                            writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "services", xmlWriter);

                                                           // write the nil attribute
                                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                           xmlWriter.writeEndElement();
                                                    
                                                }

                                            }
                                     } else {
                                        
                                                writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "services", xmlWriter);

                                               // write the nil attribute
                                               writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                               xmlWriter.writeEndElement();
                                        
                                    }
                                 }
                    xmlWriter.writeEndElement();
               

        }

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://mgt.service.carbon.wso2.org/xsd")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        /**
         * Utility method to write an element start tag.
         */
        private void writeStartElement(java.lang.String prefix, java.lang.String namespace, java.lang.String localPart,
                                       javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
            java.lang.String writerPrefix = xmlWriter.getPrefix(namespace);
            if (writerPrefix != null) {
                xmlWriter.writeStartElement(namespace, localPart);
            } else {
                if (namespace.length() == 0) {
                    prefix = "";
                } else if (prefix == null) {
                    prefix = generatePrefix(namespace);
                }

                xmlWriter.writeStartElement(prefix, localPart, namespace);
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);
            }
        }
        
        /**
         * Util method to write an attribute with the ns prefix
         */
        private void writeAttribute(java.lang.String prefix,java.lang.String namespace,java.lang.String attName,
                                    java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
            if (xmlWriter.getPrefix(namespace) == null) {
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);
            }
            xmlWriter.writeAttribute(namespace,attName,attValue);
        }

        /**
         * Util method to write an attribute without the ns prefix
         */
        private void writeAttribute(java.lang.String namespace,java.lang.String attName,
                                    java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
            if (namespace.equals("")) {
                xmlWriter.writeAttribute(attName,attValue);
            } else {
                registerPrefix(xmlWriter, namespace);
                xmlWriter.writeAttribute(namespace,attName,attValue);
            }
        }


           /**
             * Util method to write an attribute without the ns prefix
             */
            private void writeQNameAttribute(java.lang.String namespace, java.lang.String attName,
                                             javax.xml.namespace.QName qname, javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

                java.lang.String attributeNamespace = qname.getNamespaceURI();
                java.lang.String attributePrefix = xmlWriter.getPrefix(attributeNamespace);
                if (attributePrefix == null) {
                    attributePrefix = registerPrefix(xmlWriter, attributeNamespace);
                }
                java.lang.String attributeValue;
                if (attributePrefix.trim().length() > 0) {
                    attributeValue = attributePrefix + ":" + qname.getLocalPart();
                } else {
                    attributeValue = qname.getLocalPart();
                }

                if (namespace.equals("")) {
                    xmlWriter.writeAttribute(attName, attributeValue);
                } else {
                    registerPrefix(xmlWriter, namespace);
                    xmlWriter.writeAttribute(namespace, attName, attributeValue);
                }
            }
        /**
         *  method to handle Qnames
         */

        private void writeQName(javax.xml.namespace.QName qname,
                                javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
            java.lang.String namespaceURI = qname.getNamespaceURI();
            if (namespaceURI != null) {
                java.lang.String prefix = xmlWriter.getPrefix(namespaceURI);
                if (prefix == null) {
                    prefix = generatePrefix(namespaceURI);
                    xmlWriter.writeNamespace(prefix, namespaceURI);
                    xmlWriter.setPrefix(prefix,namespaceURI);
                }

                if (prefix.trim().length() > 0){
                    xmlWriter.writeCharacters(prefix + ":" + org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                } else {
                    // i.e this is the default namespace
                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                }

            } else {
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
            }
        }

        private void writeQNames(javax.xml.namespace.QName[] qnames,
                                 javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

            if (qnames != null) {
                // we have to store this data until last moment since it is not possible to write any
                // namespace data after writing the charactor data
                java.lang.StringBuffer stringToWrite = new java.lang.StringBuffer();
                java.lang.String namespaceURI = null;
                java.lang.String prefix = null;

                for (int i = 0; i < qnames.length; i++) {
                    if (i > 0) {
                        stringToWrite.append(" ");
                    }
                    namespaceURI = qnames[i].getNamespaceURI();
                    if (namespaceURI != null) {
                        prefix = xmlWriter.getPrefix(namespaceURI);
                        if ((prefix == null) || (prefix.length() == 0)) {
                            prefix = generatePrefix(namespaceURI);
                            xmlWriter.writeNamespace(prefix, namespaceURI);
                            xmlWriter.setPrefix(prefix,namespaceURI);
                        }

                        if (prefix.trim().length() > 0){
                            stringToWrite.append(prefix).append(":").append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                        } else {
                            stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                        }
                    } else {
                        stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                    }
                }
                xmlWriter.writeCharacters(stringToWrite.toString());
            }

        }


        /**
         * Register a namespace prefix
         */
        private java.lang.String registerPrefix(javax.xml.stream.XMLStreamWriter xmlWriter, java.lang.String namespace) throws javax.xml.stream.XMLStreamException {
            java.lang.String prefix = xmlWriter.getPrefix(namespace);
            if (prefix == null) {
                prefix = generatePrefix(namespace);
                while (xmlWriter.getNamespaceContext().getNamespaceURI(prefix) != null) {
                    prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
                }
                xmlWriter.writeNamespace(prefix, namespace);
                xmlWriter.setPrefix(prefix, namespace);
            }
            return prefix;
        }


  
        /**
        * databinding method to get an XML representation of this object
        *
        */
        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName)
                    throws org.apache.axis2.databinding.ADBException{


        
                 java.util.ArrayList elementList = new java.util.ArrayList();
                 java.util.ArrayList attribList = new java.util.ArrayList();

                 if (localNumberOfActiveServicesTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "numberOfActiveServices"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfActiveServices));
                            } if (localNumberOfCorrectServiceGroupsTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "numberOfCorrectServiceGroups"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfCorrectServiceGroups));
                            } if (localNumberOfFaultyServiceGroupsTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "numberOfFaultyServiceGroups"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfFaultyServiceGroups));
                            } if (localNumberOfPagesTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "numberOfPages"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localNumberOfPages));
                            } if (localServiceTypesTracker){
                            if (localServiceTypes!=null){
                                  for (int i = 0;i < localServiceTypes.length;i++){
                                      
                                         if (localServiceTypes[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "serviceTypes"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceTypes[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "serviceTypes"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "serviceTypes"));
                                    elementList.add(null);
                                
                            }

                        } if (localServicesTracker){
                             if (localServices!=null) {
                                 for (int i = 0;i < localServices.length;i++){

                                    if (localServices[i] != null){
                                         elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                          "services"));
                                         elementList.add(localServices[i]);
                                    } else {
                                        
                                                elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                          "services"));
                                                elementList.add(null);
                                            
                                    }

                                 }
                             } else {
                                 
                                        elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                          "services"));
                                        elementList.add(localServices);
                                    
                             }

                        }

                return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(), attribList.toArray());
            
            

        }

  

     /**
      *  Factory class that keeps the parse method
      */
    public static class Factory{

        
        

        /**
        * static method to create the object
        * Precondition:  If this object is an element, the current or next start element starts this object and any intervening reader events are ignorable
        *                If this object is not an element, it is a complex type and the reader is at the event just after the outer start element
        * Postcondition: If this object is an element, the reader is positioned at its end element
        *                If this object is a complex type, the reader is positioned at the end element of its outer element
        */
        public static ServiceMetaDataWrapper parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ServiceMetaDataWrapper object =
                new ServiceMetaDataWrapper();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix ="";
            java.lang.String namespaceuri ="";
            try {
                
                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                
                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","type")!=null){
                  java.lang.String fullTypeName = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance",
                        "type");
                  if (fullTypeName!=null){
                    java.lang.String nsPrefix = null;
                    if (fullTypeName.indexOf(":") > -1){
                        nsPrefix = fullTypeName.substring(0,fullTypeName.indexOf(":"));
                    }
                    nsPrefix = nsPrefix==null?"":nsPrefix;

                    java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":")+1);
                    
                            if (!"ServiceMetaDataWrapper".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (ServiceMetaDataWrapper)org.wso2.carbon.dataservices.core.admin.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                
                    
                    reader.next();
                
                        java.util.ArrayList list5 = new java.util.ArrayList();
                    
                        java.util.ArrayList list6 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","numberOfActiveServices").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"numberOfActiveServices" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNumberOfActiveServices(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setNumberOfActiveServices(java.lang.Integer.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","numberOfCorrectServiceGroups").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"numberOfCorrectServiceGroups" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNumberOfCorrectServiceGroups(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setNumberOfCorrectServiceGroups(java.lang.Integer.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","numberOfFaultyServiceGroups").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"numberOfFaultyServiceGroups" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNumberOfFaultyServiceGroups(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setNumberOfFaultyServiceGroups(java.lang.Integer.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","numberOfPages").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"numberOfPages" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setNumberOfPages(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToInt(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                               object.setNumberOfPages(java.lang.Integer.MIN_VALUE);
                                           
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceTypes").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                              nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                              if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                  list5.add(null);
                                                       
                                                  reader.next();
                                              } else {
                                            list5.add(reader.getElementText());
                                            }
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone5 = false;
                                            while(!loopDone5){
                                                // Ensure we are at the EndElement
                                                while (!reader.isEndElement()){
                                                    reader.next();
                                                }
                                                // Step out of this element
                                                reader.next();
                                                // Step to next element event.
                                                while (!reader.isStartElement() && !reader.isEndElement())
                                                    reader.next();
                                                if (reader.isEndElement()){
                                                    //two continuous end elements means we are exiting the xml structure
                                                    loopDone5 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceTypes").equals(reader.getName())){
                                                         
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list5.add(null);
                                                                   
                                                              reader.next();
                                                          } else {
                                                        list5.add(reader.getElementText());
                                                        }
                                                    }else{
                                                        loopDone5 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setServiceTypes((java.lang.String[])
                                                        list5.toArray(new java.lang.String[list5.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","services").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list6.add(null);
                                                              reader.next();
                                                          } else {
                                                        list6.add(org.wso2.carbon.service.mgt.xsd.ServiceMetaData.Factory.parse(reader));
                                                                }
                                                        //loop until we find a start element that is not part of this array
                                                        boolean loopDone6 = false;
                                                        while(!loopDone6){
                                                            // We should be at the end element, but make sure
                                                            while (!reader.isEndElement())
                                                                reader.next();
                                                            // Step out of this element
                                                            reader.next();
                                                            // Step to next element event.
                                                            while (!reader.isStartElement() && !reader.isEndElement())
                                                                reader.next();
                                                            if (reader.isEndElement()){
                                                                //two continuous end elements means we are exiting the xml structure
                                                                loopDone6 = true;
                                                            } else {
                                                                if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","services").equals(reader.getName())){
                                                                    
                                                                      nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                                      if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                                          list6.add(null);
                                                                          reader.next();
                                                                      } else {
                                                                    list6.add(org.wso2.carbon.service.mgt.xsd.ServiceMetaData.Factory.parse(reader));
                                                                        }
                                                                }else{
                                                                    loopDone6 = true;
                                                                }
                                                            }
                                                        }
                                                        // call the converter utility  to convert and set the array
                                                        
                                                        object.setServices((org.wso2.carbon.service.mgt.xsd.ServiceMetaData[])
                                                            org.apache.axis2.databinding.utils.ConverterUtil.convertToArray(
                                                                org.wso2.carbon.service.mgt.xsd.ServiceMetaData.class,
                                                                list6));
                                                            
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                  
                            while (!reader.isStartElement() && !reader.isEndElement())
                                reader.next();
                            
                                if (reader.isStartElement())
                                // A start element we are not expecting indicates a trailing invalid property
                                throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getName());
                            



            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

        }//end of factory class

        

        }
           
    