

/**
 * APIPublisher.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:21 IST)
 */

    package org.wso2.carbon.dssapi.stub;

    /*
     *  APIPublisher java interface
     */

    public interface APIPublisher {
          

        /**
          * Auto generated method signature
          * 
                    * @param removeApi6
                
         */

         
                     public boolean removeApi(

                        java.lang.String serviceId7)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param removeApi6
            
          */
        public void startremoveApi(

            java.lang.String serviceId7,

            final org.wso2.carbon.dssapi.stub.APIPublisherCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param viewSubscriptions10
                
         */

         
                     public long viewSubscriptions(

                        java.lang.String serviceName11)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param viewSubscriptions10
            
          */
        public void startviewSubscriptions(

            java.lang.String serviceName11,

            final org.wso2.carbon.dssapi.stub.APIPublisherCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param listDssServices14
                
             * @throws org.wso2.carbon.dssapi.stub.APIPublisherException : 
         */

         
                     public org.wso2.carbon.service.mgt.xsd.ServiceMetaDataWrapper listDssServices(

                        java.lang.String searchString15,int pageNumber16)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dssapi.stub.APIPublisherException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param listDssServices14
            
          */
        public void startlistDssServices(

            java.lang.String searchString15,int pageNumber16,

            final org.wso2.carbon.dssapi.stub.APIPublisherCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param apiAvailable19
                
         */

         
                     public boolean apiAvailable(

                        java.lang.String serviceName20)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param apiAvailable19
            
          */
        public void startapiAvailable(

            java.lang.String serviceName20,

            final org.wso2.carbon.dssapi.stub.APIPublisherCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param addApi23
                
         */

         
                     public boolean addApi(

                        java.lang.String serviceId24)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param addApi23
            
          */
        public void startaddApi(

            java.lang.String serviceId24,

            final org.wso2.carbon.dssapi.stub.APIPublisherCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        
       //
       }
    