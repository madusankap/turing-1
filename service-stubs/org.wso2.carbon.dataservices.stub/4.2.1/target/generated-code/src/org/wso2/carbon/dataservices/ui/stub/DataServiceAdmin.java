

/**
 * DataServiceAdmin.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Nov 12, 2014 (10:31:02 IST)
 */

    package org.wso2.carbon.dataservices.ui.stub;

    /*
     *  DataServiceAdmin java interface
     */

    public interface DataServiceAdmin {
          

        /**
          * Auto generated method signature
          * 
                    * @param getDataServiceContentAsString33
                
         */

         
                     public java.lang.String getDataServiceContentAsString(

                        java.lang.String serviceId34)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getDataServiceContentAsString33
            
          */
        public void startgetDataServiceContentAsString(

            java.lang.String serviceId34,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getInputMappingNames37
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public java.lang.String[] getInputMappingNames(

                        java.lang.String sql38)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getInputMappingNames37
            
          */
        public void startgetInputMappingNames(

            java.lang.String sql38,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getCarbonDataSourceNamesForTypes41
                
         */

         
                     public java.lang.String[] getCarbonDataSourceNamesForTypes(

                        java.lang.String[] types42)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getCarbonDataSourceNamesForTypes41
            
          */
        public void startgetCarbonDataSourceNamesForTypes(

            java.lang.String[] types42,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param validateJSONMapping45
                
         */

         
                     public java.lang.String validateJSONMapping(

                        java.lang.String jsonMapping46)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param validateJSONMapping45
            
          */
        public void startvalidateJSONMapping(

            java.lang.String jsonMapping46,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     
       /**
         * Auto generated method signature for Asynchronous Invocations
         * 
         */
        public void  saveDataService(
         java.lang.String serviceName50,java.lang.String serviceHierarchy51,java.lang.String serviceContents52

        ) throws java.rmi.RemoteException
        
        ;

        

        /**
          * Auto generated method signature
          * 
                    * @param getPaginatedSchemaInfo53
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public org.wso2.carbon.dataservices.ui.stub.admin.core.xsd.PaginatedTableInfo getPaginatedSchemaInfo(

                        int pageNumber54,java.lang.String datasourceId55)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getPaginatedSchemaInfo53
            
          */
        public void startgetPaginatedSchemaInfo(

            int pageNumber54,java.lang.String datasourceId55,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getTableList58
                
         */

         
                     public java.lang.String[] getTableList(

                        java.lang.String datasourceId59,java.lang.String dbName60,java.lang.String[] schemas61)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getTableList58
            
          */
        public void startgetTableList(

            java.lang.String datasourceId59,java.lang.String dbName60,java.lang.String[] schemas61,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getAvailableDS64
                
         */

         
                     public java.lang.String[] getAvailableDS(

                        )
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getAvailableDS64
            
          */
        public void startgetAvailableDS(

            

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getDSServiceList67
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public java.lang.String[] getDSServiceList(

                        java.lang.String dataSourceId68,java.lang.String dbName69,java.lang.String[] schemas70,java.lang.String[] tableNames71,boolean singleService72,java.lang.String serviceNamespace73)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getDSServiceList67
            
          */
        public void startgetDSServiceList(

            java.lang.String dataSourceId68,java.lang.String dbName69,java.lang.String[] schemas70,java.lang.String[] tableNames71,boolean singleService72,java.lang.String serviceNamespace73,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getCarbonDataSourceNames76
                
         */

         
                     public java.lang.String[] getCarbonDataSourceNames(

                        )
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getCarbonDataSourceNames76
            
          */
        public void startgetCarbonDataSourceNames(

            

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getDSService79
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public java.lang.String getDSService(

                        java.lang.String dataSourceId80,java.lang.String dbName81,java.lang.String[] schemas82,java.lang.String[] tableNames83,boolean singleService84,java.lang.String serviceName85,java.lang.String serviceNamespace86)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getDSService79
            
          */
        public void startgetDSService(

            java.lang.String dataSourceId80,java.lang.String dbName81,java.lang.String[] schemas82,java.lang.String[] tableNames83,boolean singleService84,java.lang.String serviceName85,java.lang.String serviceNamespace86,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getCarbonDataSourceType89
                
         */

         
                     public java.lang.String getCarbonDataSourceType(

                        java.lang.String dsName90)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getCarbonDataSourceType89
            
          */
        public void startgetCarbonDataSourceType(

            java.lang.String dsName90,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param testGSpreadConnection93
                
         */

         
                     public java.lang.String testGSpreadConnection(

                        java.lang.String user94,java.lang.String password95,java.lang.String visibility96,java.lang.String documentURL97,java.lang.String passwordAlias98)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param testGSpreadConnection93
            
          */
        public void starttestGSpreadConnection(

            java.lang.String user94,java.lang.String password95,java.lang.String visibility96,java.lang.String documentURL97,java.lang.String passwordAlias98,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getPaginatedTableInfo101
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public org.wso2.carbon.dataservices.ui.stub.admin.core.xsd.PaginatedTableInfo getPaginatedTableInfo(

                        int pageNumber102,java.lang.String datasourceId103,java.lang.String dbName104,java.lang.String[] schemas105)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getPaginatedTableInfo101
            
          */
        public void startgetPaginatedTableInfo(

            int pageNumber102,java.lang.String datasourceId103,java.lang.String dbName104,java.lang.String[] schemas105,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getOutputColumnNames108
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public java.lang.String[] getOutputColumnNames(

                        java.lang.String sql109)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getOutputColumnNames108
            
          */
        public void startgetOutputColumnNames(

            java.lang.String sql109,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param getdbSchemaList112
                
             * @throws org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException : 
         */

         
                     public java.lang.String[] getdbSchemaList(

                        java.lang.String datasourceId113)
                        throws java.rmi.RemoteException
             
          ,org.wso2.carbon.dataservices.ui.stub.DataServiceAdminExceptionException;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param getdbSchemaList112
            
          */
        public void startgetdbSchemaList(

            java.lang.String datasourceId113,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        /**
          * Auto generated method signature
          * 
                    * @param testJDBCConnection116
                
         */

         
                     public java.lang.String testJDBCConnection(

                        java.lang.String driverClass117,java.lang.String jdbcURL118,java.lang.String username119,java.lang.String password120,java.lang.String passwordAlias121)
                        throws java.rmi.RemoteException
             ;

        
         /**
            * Auto generated method signature for Asynchronous Invocations
            * 
                * @param testJDBCConnection116
            
          */
        public void starttestJDBCConnection(

            java.lang.String driverClass117,java.lang.String jdbcURL118,java.lang.String username119,java.lang.String password120,java.lang.String passwordAlias121,

            final org.wso2.carbon.dataservices.ui.stub.DataServiceAdminCallbackHandler callback)

            throws java.rmi.RemoteException;

     

        
       //
       }
    