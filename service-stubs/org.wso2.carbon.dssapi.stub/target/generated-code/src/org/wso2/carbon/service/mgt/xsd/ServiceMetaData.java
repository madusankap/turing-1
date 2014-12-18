
/**
 * ServiceMetaData.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:23 IST)
 */

            
                package org.wso2.carbon.service.mgt.xsd;
            

            /**
            *  ServiceMetaData bean class
            */
            @SuppressWarnings({"unchecked","unused"})
        
        public  class ServiceMetaData
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = ServiceMetaData
                Namespace URI = http://mgt.service.carbon.wso2.org/xsd
                Namespace Prefix = ns1
                */
            

                        /**
                        * field for Active
                        */

                        
                                    protected boolean localActive ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localActiveTracker = false ;

                           public boolean isActiveSpecified(){
                               return localActiveTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getActive(){
                               return localActive;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Active
                               */
                               public void setActive(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       localActiveTracker =
                                       true;
                                   
                                            this.localActive=param;
                                    

                               }
                            

                        /**
                        * field for Description
                        */

                        
                                    protected java.lang.String localDescription ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDescriptionTracker = false ;

                           public boolean isDescriptionSpecified(){
                               return localDescriptionTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDescription(){
                               return localDescription;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Description
                               */
                               public void setDescription(java.lang.String param){
                            localDescriptionTracker = true;
                                   
                                            this.localDescription=param;
                                    

                               }
                            

                        /**
                        * field for DisableDeletion
                        */

                        
                                    protected boolean localDisableDeletion ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDisableDeletionTracker = false ;

                           public boolean isDisableDeletionSpecified(){
                               return localDisableDeletionTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getDisableDeletion(){
                               return localDisableDeletion;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DisableDeletion
                               */
                               public void setDisableDeletion(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       localDisableDeletionTracker =
                                       true;
                                   
                                            this.localDisableDeletion=param;
                                    

                               }
                            

                        /**
                        * field for DisableTryit
                        */

                        
                                    protected boolean localDisableTryit ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDisableTryitTracker = false ;

                           public boolean isDisableTryitSpecified(){
                               return localDisableTryitTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getDisableTryit(){
                               return localDisableTryit;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DisableTryit
                               */
                               public void setDisableTryit(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       localDisableTryitTracker =
                                       true;
                                   
                                            this.localDisableTryit=param;
                                    

                               }
                            

                        /**
                        * field for Eprs
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localEprs ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localEprsTracker = false ;

                           public boolean isEprsSpecified(){
                               return localEprsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getEprs(){
                               return localEprs;
                           }

                           
                        


                               
                              /**
                               * validate the array for Eprs
                               */
                              protected void validateEprs(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param Eprs
                              */
                              public void setEprs(java.lang.String[] param){
                              
                                   validateEprs(param);

                               localEprsTracker = true;
                                      
                                      this.localEprs=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addEprs(java.lang.String param){
                                   if (localEprs == null){
                                   localEprs = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localEprsTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localEprs);
                               list.add(param);
                               this.localEprs =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for FoundWebResources
                        */

                        
                                    protected boolean localFoundWebResources ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localFoundWebResourcesTracker = false ;

                           public boolean isFoundWebResourcesSpecified(){
                               return localFoundWebResourcesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return boolean
                           */
                           public  boolean getFoundWebResources(){
                               return localFoundWebResources;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param FoundWebResources
                               */
                               public void setFoundWebResources(boolean param){
                            
                                       // setting primitive attribute tracker to true
                                       localFoundWebResourcesTracker =
                                       true;
                                   
                                            this.localFoundWebResources=param;
                                    

                               }
                            

                        /**
                        * field for MtomStatus
                        */

                        
                                    protected java.lang.String localMtomStatus ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localMtomStatusTracker = false ;

                           public boolean isMtomStatusSpecified(){
                               return localMtomStatusTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getMtomStatus(){
                               return localMtomStatus;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param MtomStatus
                               */
                               public void setMtomStatus(java.lang.String param){
                            localMtomStatusTracker = true;
                                   
                                            this.localMtomStatus=param;
                                    

                               }
                            

                        /**
                        * field for Name
                        */

                        
                                    protected java.lang.String localName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localNameTracker = false ;

                           public boolean isNameSpecified(){
                               return localNameTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getName(){
                               return localName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Name
                               */
                               public void setName(java.lang.String param){
                            localNameTracker = true;
                                   
                                            this.localName=param;
                                    

                               }
                            

                        /**
                        * field for Operations
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localOperations ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localOperationsTracker = false ;

                           public boolean isOperationsSpecified(){
                               return localOperationsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getOperations(){
                               return localOperations;
                           }

                           
                        


                               
                              /**
                               * validate the array for Operations
                               */
                              protected void validateOperations(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param Operations
                              */
                              public void setOperations(java.lang.String[] param){
                              
                                   validateOperations(param);

                               localOperationsTracker = true;
                                      
                                      this.localOperations=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addOperations(java.lang.String param){
                                   if (localOperations == null){
                                   localOperations = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localOperationsTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localOperations);
                               list.add(param);
                               this.localOperations =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for Scope
                        */

                        
                                    protected java.lang.String localScope ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localScopeTracker = false ;

                           public boolean isScopeSpecified(){
                               return localScopeTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getScope(){
                               return localScope;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Scope
                               */
                               public void setScope(java.lang.String param){
                            localScopeTracker = true;
                                   
                                            this.localScope=param;
                                    

                               }
                            

                        /**
                        * field for SecurityScenarioId
                        */

                        
                                    protected java.lang.String localSecurityScenarioId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSecurityScenarioIdTracker = false ;

                           public boolean isSecurityScenarioIdSpecified(){
                               return localSecurityScenarioIdTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSecurityScenarioId(){
                               return localSecurityScenarioId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SecurityScenarioId
                               */
                               public void setSecurityScenarioId(java.lang.String param){
                            localSecurityScenarioIdTracker = true;
                                   
                                            this.localSecurityScenarioId=param;
                                    

                               }
                            

                        /**
                        * field for ServiceDeployedTime
                        */

                        
                                    protected java.lang.String localServiceDeployedTime ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceDeployedTimeTracker = false ;

                           public boolean isServiceDeployedTimeSpecified(){
                               return localServiceDeployedTimeTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceDeployedTime(){
                               return localServiceDeployedTime;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceDeployedTime
                               */
                               public void setServiceDeployedTime(java.lang.String param){
                            localServiceDeployedTimeTracker = true;
                                   
                                            this.localServiceDeployedTime=param;
                                    

                               }
                            

                        /**
                        * field for ServiceGroupName
                        */

                        
                                    protected java.lang.String localServiceGroupName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceGroupNameTracker = false ;

                           public boolean isServiceGroupNameSpecified(){
                               return localServiceGroupNameTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceGroupName(){
                               return localServiceGroupName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceGroupName
                               */
                               public void setServiceGroupName(java.lang.String param){
                            localServiceGroupNameTracker = true;
                                   
                                            this.localServiceGroupName=param;
                                    

                               }
                            

                        /**
                        * field for ServiceId
                        */

                        
                                    protected java.lang.String localServiceId ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceIdTracker = false ;

                           public boolean isServiceIdSpecified(){
                               return localServiceIdTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceId(){
                               return localServiceId;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceId
                               */
                               public void setServiceId(java.lang.String param){
                            localServiceIdTracker = true;
                                   
                                            this.localServiceId=param;
                                    

                               }
                            

                        /**
                        * field for ServiceType
                        */

                        
                                    protected java.lang.String localServiceType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceTypeTracker = false ;

                           public boolean isServiceTypeSpecified(){
                               return localServiceTypeTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceType(){
                               return localServiceType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceType
                               */
                               public void setServiceType(java.lang.String param){
                            localServiceTypeTracker = true;
                                   
                                            this.localServiceType=param;
                                    

                               }
                            

                        /**
                        * field for ServiceUpTime
                        */

                        
                                    protected java.lang.String localServiceUpTime ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceUpTimeTracker = false ;

                           public boolean isServiceUpTimeSpecified(){
                               return localServiceUpTimeTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceUpTime(){
                               return localServiceUpTime;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceUpTime
                               */
                               public void setServiceUpTime(java.lang.String param){
                            localServiceUpTimeTracker = true;
                                   
                                            this.localServiceUpTime=param;
                                    

                               }
                            

                        /**
                        * field for ServiceVersion
                        */

                        
                                    protected java.lang.String localServiceVersion ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localServiceVersionTracker = false ;

                           public boolean isServiceVersionSpecified(){
                               return localServiceVersionTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getServiceVersion(){
                               return localServiceVersion;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ServiceVersion
                               */
                               public void setServiceVersion(java.lang.String param){
                            localServiceVersionTracker = true;
                                   
                                            this.localServiceVersion=param;
                                    

                               }
                            

                        /**
                        * field for TryitURL
                        */

                        
                                    protected java.lang.String localTryitURL ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localTryitURLTracker = false ;

                           public boolean isTryitURLSpecified(){
                               return localTryitURLTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getTryitURL(){
                               return localTryitURL;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param TryitURL
                               */
                               public void setTryitURL(java.lang.String param){
                            localTryitURLTracker = true;
                                   
                                            this.localTryitURL=param;
                                    

                               }
                            

                        /**
                        * field for WsdlPortTypes
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localWsdlPortTypes ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localWsdlPortTypesTracker = false ;

                           public boolean isWsdlPortTypesSpecified(){
                               return localWsdlPortTypesTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getWsdlPortTypes(){
                               return localWsdlPortTypes;
                           }

                           
                        


                               
                              /**
                               * validate the array for WsdlPortTypes
                               */
                              protected void validateWsdlPortTypes(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param WsdlPortTypes
                              */
                              public void setWsdlPortTypes(java.lang.String[] param){
                              
                                   validateWsdlPortTypes(param);

                               localWsdlPortTypesTracker = true;
                                      
                                      this.localWsdlPortTypes=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addWsdlPortTypes(java.lang.String param){
                                   if (localWsdlPortTypes == null){
                                   localWsdlPortTypes = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localWsdlPortTypesTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localWsdlPortTypes);
                               list.add(param);
                               this.localWsdlPortTypes =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for WsdlPorts
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localWsdlPorts ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localWsdlPortsTracker = false ;

                           public boolean isWsdlPortsSpecified(){
                               return localWsdlPortsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getWsdlPorts(){
                               return localWsdlPorts;
                           }

                           
                        


                               
                              /**
                               * validate the array for WsdlPorts
                               */
                              protected void validateWsdlPorts(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param WsdlPorts
                              */
                              public void setWsdlPorts(java.lang.String[] param){
                              
                                   validateWsdlPorts(param);

                               localWsdlPortsTracker = true;
                                      
                                      this.localWsdlPorts=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addWsdlPorts(java.lang.String param){
                                   if (localWsdlPorts == null){
                                   localWsdlPorts = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localWsdlPortsTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localWsdlPorts);
                               list.add(param);
                               this.localWsdlPorts =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

                             }
                             

                        /**
                        * field for WsdlURLs
                        * This was an Array!
                        */

                        
                                    protected java.lang.String[] localWsdlURLs ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localWsdlURLsTracker = false ;

                           public boolean isWsdlURLsSpecified(){
                               return localWsdlURLsTracker;
                           }

                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String[]
                           */
                           public  java.lang.String[] getWsdlURLs(){
                               return localWsdlURLs;
                           }

                           
                        


                               
                              /**
                               * validate the array for WsdlURLs
                               */
                              protected void validateWsdlURLs(java.lang.String[] param){
                             
                              }


                             /**
                              * Auto generated setter method
                              * @param param WsdlURLs
                              */
                              public void setWsdlURLs(java.lang.String[] param){
                              
                                   validateWsdlURLs(param);

                               localWsdlURLsTracker = true;
                                      
                                      this.localWsdlURLs=param;
                              }

                               
                             
                             /**
                             * Auto generated add method for the array for convenience
                             * @param param java.lang.String
                             */
                             public void addWsdlURLs(java.lang.String param){
                                   if (localWsdlURLs == null){
                                   localWsdlURLs = new java.lang.String[]{};
                                   }

                            
                                 //update the setting tracker
                                localWsdlURLsTracker = true;
                            

                               java.util.List list =
                            org.apache.axis2.databinding.utils.ConverterUtil.toList(localWsdlURLs);
                               list.add(param);
                               this.localWsdlURLs =
                             (java.lang.String[])list.toArray(
                            new java.lang.String[list.size()]);

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
                           namespacePrefix+":ServiceMetaData",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "ServiceMetaData",
                           xmlWriter);
                   }

               
                   }
                if (localActiveTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "active", xmlWriter);
                             
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("active cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localActive));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localDescriptionTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "description", xmlWriter);
                             

                                          if (localDescription==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDescription);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localDisableDeletionTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "disableDeletion", xmlWriter);
                             
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("disableDeletion cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableDeletion));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localDisableTryitTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "disableTryit", xmlWriter);
                             
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("disableTryit cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableTryit));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localEprsTracker){
                             if (localEprs!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localEprs.length;i++){
                                        
                                            if (localEprs[i] != null){
                                        
                                                writeStartElement(null, namespace, "eprs", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEprs[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "eprs", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "eprs", xmlWriter);

                                           // write the nil attribute
                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                           xmlWriter.writeEndElement();
                                    
                             }

                        } if (localFoundWebResourcesTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "foundWebResources", xmlWriter);
                             
                                               if (false) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("foundWebResources cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localFoundWebResources));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localMtomStatusTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "mtomStatus", xmlWriter);
                             

                                          if (localMtomStatus==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localMtomStatus);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localNameTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "name", xmlWriter);
                             

                                          if (localName==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localOperationsTracker){
                             if (localOperations!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localOperations.length;i++){
                                        
                                            if (localOperations[i] != null){
                                        
                                                writeStartElement(null, namespace, "operations", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localOperations[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "operations", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "operations", xmlWriter);

                                           // write the nil attribute
                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                           xmlWriter.writeEndElement();
                                    
                             }

                        } if (localScopeTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "scope", xmlWriter);
                             

                                          if (localScope==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localScope);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localSecurityScenarioIdTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "securityScenarioId", xmlWriter);
                             

                                          if (localSecurityScenarioId==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSecurityScenarioId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceDeployedTimeTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceDeployedTime", xmlWriter);
                             

                                          if (localServiceDeployedTime==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceDeployedTime);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceGroupNameTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceGroupName", xmlWriter);
                             

                                          if (localServiceGroupName==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceGroupName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceIdTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceId", xmlWriter);
                             

                                          if (localServiceId==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceId);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceTypeTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceType", xmlWriter);
                             

                                          if (localServiceType==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceUpTimeTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceUpTime", xmlWriter);
                             

                                          if (localServiceUpTime==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceUpTime);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localServiceVersionTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "serviceVersion", xmlWriter);
                             

                                          if (localServiceVersion==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localServiceVersion);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localTryitURLTracker){
                                    namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                    writeStartElement(null, namespace, "tryitURL", xmlWriter);
                             

                                          if (localTryitURL==null){
                                              // write the nil attribute
                                              
                                                     writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localTryitURL);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localWsdlPortTypesTracker){
                             if (localWsdlPortTypes!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localWsdlPortTypes.length;i++){
                                        
                                            if (localWsdlPortTypes[i] != null){
                                        
                                                writeStartElement(null, namespace, "wsdlPortTypes", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlPortTypes[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "wsdlPortTypes", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "wsdlPortTypes", xmlWriter);

                                           // write the nil attribute
                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                           xmlWriter.writeEndElement();
                                    
                             }

                        } if (localWsdlPortsTracker){
                             if (localWsdlPorts!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localWsdlPorts.length;i++){
                                        
                                            if (localWsdlPorts[i] != null){
                                        
                                                writeStartElement(null, namespace, "wsdlPorts", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlPorts[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "wsdlPorts", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "wsdlPorts", xmlWriter);

                                           // write the nil attribute
                                           writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                           xmlWriter.writeEndElement();
                                    
                             }

                        } if (localWsdlURLsTracker){
                             if (localWsdlURLs!=null) {
                                   namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                   for (int i = 0;i < localWsdlURLs.length;i++){
                                        
                                            if (localWsdlURLs[i] != null){
                                        
                                                writeStartElement(null, namespace, "wsdlURLs", xmlWriter);

                                            
                                                        xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlURLs[i]));
                                                    
                                                xmlWriter.writeEndElement();
                                              
                                                } else {
                                                   
                                                           // write null attribute
                                                            namespace = "http://mgt.service.carbon.wso2.org/xsd";
                                                            writeStartElement(null, namespace, "wsdlURLs", xmlWriter);
                                                            writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","nil","1",xmlWriter);
                                                            xmlWriter.writeEndElement();
                                                       
                                                }

                                   }
                             } else {
                                 
                                         // write the null attribute
                                        // write null attribute
                                           writeStartElement(null, "http://mgt.service.carbon.wso2.org/xsd", "wsdlURLs", xmlWriter);

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

                 if (localActiveTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "active"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localActive));
                            } if (localDescriptionTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "description"));
                                 
                                         elementList.add(localDescription==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDescription));
                                    } if (localDisableDeletionTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "disableDeletion"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableDeletion));
                            } if (localDisableTryitTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "disableTryit"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDisableTryit));
                            } if (localEprsTracker){
                            if (localEprs!=null){
                                  for (int i = 0;i < localEprs.length;i++){
                                      
                                         if (localEprs[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "eprs"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEprs[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "eprs"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "eprs"));
                                    elementList.add(null);
                                
                            }

                        } if (localFoundWebResourcesTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "foundWebResources"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localFoundWebResources));
                            } if (localMtomStatusTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "mtomStatus"));
                                 
                                         elementList.add(localMtomStatus==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localMtomStatus));
                                    } if (localNameTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "name"));
                                 
                                         elementList.add(localName==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localName));
                                    } if (localOperationsTracker){
                            if (localOperations!=null){
                                  for (int i = 0;i < localOperations.length;i++){
                                      
                                         if (localOperations[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "operations"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localOperations[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "operations"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "operations"));
                                    elementList.add(null);
                                
                            }

                        } if (localScopeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "scope"));
                                 
                                         elementList.add(localScope==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localScope));
                                    } if (localSecurityScenarioIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "securityScenarioId"));
                                 
                                         elementList.add(localSecurityScenarioId==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSecurityScenarioId));
                                    } if (localServiceDeployedTimeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceDeployedTime"));
                                 
                                         elementList.add(localServiceDeployedTime==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceDeployedTime));
                                    } if (localServiceGroupNameTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceGroupName"));
                                 
                                         elementList.add(localServiceGroupName==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceGroupName));
                                    } if (localServiceIdTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceId"));
                                 
                                         elementList.add(localServiceId==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceId));
                                    } if (localServiceTypeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceType"));
                                 
                                         elementList.add(localServiceType==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceType));
                                    } if (localServiceUpTimeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceUpTime"));
                                 
                                         elementList.add(localServiceUpTime==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceUpTime));
                                    } if (localServiceVersionTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "serviceVersion"));
                                 
                                         elementList.add(localServiceVersion==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localServiceVersion));
                                    } if (localTryitURLTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                      "tryitURL"));
                                 
                                         elementList.add(localTryitURL==null?null:
                                         org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localTryitURL));
                                    } if (localWsdlPortTypesTracker){
                            if (localWsdlPortTypes!=null){
                                  for (int i = 0;i < localWsdlPortTypes.length;i++){
                                      
                                         if (localWsdlPortTypes[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPortTypes"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlPortTypes[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPortTypes"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPortTypes"));
                                    elementList.add(null);
                                
                            }

                        } if (localWsdlPortsTracker){
                            if (localWsdlPorts!=null){
                                  for (int i = 0;i < localWsdlPorts.length;i++){
                                      
                                         if (localWsdlPorts[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPorts"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlPorts[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPorts"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlPorts"));
                                    elementList.add(null);
                                
                            }

                        } if (localWsdlURLsTracker){
                            if (localWsdlURLs!=null){
                                  for (int i = 0;i < localWsdlURLs.length;i++){
                                      
                                         if (localWsdlURLs[i] != null){
                                          elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlURLs"));
                                          elementList.add(
                                          org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWsdlURLs[i]));
                                          } else {
                                             
                                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlURLs"));
                                                    elementList.add(null);
                                                
                                          }
                                      

                                  }
                            } else {
                              
                                    elementList.add(new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd",
                                                                              "wsdlURLs"));
                                    elementList.add(null);
                                
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
        public static ServiceMetaData parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            ServiceMetaData object =
                new ServiceMetaData();

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
                    
                            if (!"ServiceMetaData".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (ServiceMetaData)org.wso2.carbon.dataservices.core.admin.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                
                    
                    reader.next();
                
                        java.util.ArrayList list5 = new java.util.ArrayList();
                    
                        java.util.ArrayList list9 = new java.util.ArrayList();
                    
                        java.util.ArrayList list19 = new java.util.ArrayList();
                    
                        java.util.ArrayList list20 = new java.util.ArrayList();
                    
                        java.util.ArrayList list21 = new java.util.ArrayList();
                    
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","active").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"active" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setActive(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","description").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDescription(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","disableDeletion").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"disableDeletion" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDisableDeletion(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","disableTryit").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"disableTryit" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDisableTryit(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","eprs").equals(reader.getName())){
                                
                                    
                                    
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
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","eprs").equals(reader.getName())){
                                                         
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
                                            
                                                    object.setEprs((java.lang.String[])
                                                        list5.toArray(new java.lang.String[list5.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","foundWebResources").equals(reader.getName())){
                                
                                    nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                    if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                        throw new org.apache.axis2.databinding.ADBException("The element: "+"foundWebResources" +"  cannot be null");
                                    }
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setFoundWebResources(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToBoolean(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","mtomStatus").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setMtomStatus(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","name").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","operations").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                              nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                              if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                  list9.add(null);
                                                       
                                                  reader.next();
                                              } else {
                                            list9.add(reader.getElementText());
                                            }
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone9 = false;
                                            while(!loopDone9){
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
                                                    loopDone9 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","operations").equals(reader.getName())){
                                                         
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list9.add(null);
                                                                   
                                                              reader.next();
                                                          } else {
                                                        list9.add(reader.getElementText());
                                                        }
                                                    }else{
                                                        loopDone9 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setOperations((java.lang.String[])
                                                        list9.toArray(new java.lang.String[list9.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","scope").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setScope(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","securityScenarioId").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSecurityScenarioId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceDeployedTime").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceDeployedTime(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceGroupName").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceGroupName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceId").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceId(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceType").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceUpTime").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceUpTime(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","serviceVersion").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setServiceVersion(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","tryitURL").equals(reader.getName())){
                                
                                       nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                       if (!"true".equals(nillableValue) && !"1".equals(nillableValue)){
                                    

                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setTryitURL(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                            
                                       } else {
                                           
                                           
                                           reader.getElementText(); // throw away text nodes if any.
                                       }
                                      
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlPortTypes").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                              nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                              if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                  list19.add(null);
                                                       
                                                  reader.next();
                                              } else {
                                            list19.add(reader.getElementText());
                                            }
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone19 = false;
                                            while(!loopDone19){
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
                                                    loopDone19 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlPortTypes").equals(reader.getName())){
                                                         
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list19.add(null);
                                                                   
                                                              reader.next();
                                                          } else {
                                                        list19.add(reader.getElementText());
                                                        }
                                                    }else{
                                                        loopDone19 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setWsdlPortTypes((java.lang.String[])
                                                        list19.toArray(new java.lang.String[list19.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlPorts").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                              nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                              if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                  list20.add(null);
                                                       
                                                  reader.next();
                                              } else {
                                            list20.add(reader.getElementText());
                                            }
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone20 = false;
                                            while(!loopDone20){
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
                                                    loopDone20 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlPorts").equals(reader.getName())){
                                                         
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list20.add(null);
                                                                   
                                                              reader.next();
                                                          } else {
                                                        list20.add(reader.getElementText());
                                                        }
                                                    }else{
                                                        loopDone20 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setWsdlPorts((java.lang.String[])
                                                        list20.toArray(new java.lang.String[list20.size()]));
                                                
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlURLs").equals(reader.getName())){
                                
                                    
                                    
                                    // Process the array and step past its final element's end.
                                    
                                              nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                              if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                  list21.add(null);
                                                       
                                                  reader.next();
                                              } else {
                                            list21.add(reader.getElementText());
                                            }
                                            //loop until we find a start element that is not part of this array
                                            boolean loopDone21 = false;
                                            while(!loopDone21){
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
                                                    loopDone21 = true;
                                                } else {
                                                    if (new javax.xml.namespace.QName("http://mgt.service.carbon.wso2.org/xsd","wsdlURLs").equals(reader.getName())){
                                                         
                                                          nillableValue = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","nil");
                                                          if ("true".equals(nillableValue) || "1".equals(nillableValue)){
                                                              list21.add(null);
                                                                   
                                                              reader.next();
                                                          } else {
                                                        list21.add(reader.getElementText());
                                                        }
                                                    }else{
                                                        loopDone21 = true;
                                                    }
                                                }
                                            }
                                            // call the converter utility  to convert and set the array
                                            
                                                    object.setWsdlURLs((java.lang.String[])
                                                        list21.toArray(new java.lang.String[list21.size()]));
                                                
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
           
    