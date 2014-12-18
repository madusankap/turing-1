
/**
 * DataServiceAdminCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Nov 12, 2014 (10:31:02 IST)
 */

    package org.wso2.carbon.dataservices.ui.stub;

    /**
     *  DataServiceAdminCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class DataServiceAdminCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public DataServiceAdminCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public DataServiceAdminCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for getDataServiceContentAsString method
            * override this method for handling normal response from getDataServiceContentAsString operation
            */
           public void receiveResultgetDataServiceContentAsString(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getDataServiceContentAsString operation
           */
            public void receiveErrorgetDataServiceContentAsString(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getInputMappingNames method
            * override this method for handling normal response from getInputMappingNames operation
            */
           public void receiveResultgetInputMappingNames(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getInputMappingNames operation
           */
            public void receiveErrorgetInputMappingNames(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getCarbonDataSourceNamesForTypes method
            * override this method for handling normal response from getCarbonDataSourceNamesForTypes operation
            */
           public void receiveResultgetCarbonDataSourceNamesForTypes(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getCarbonDataSourceNamesForTypes operation
           */
            public void receiveErrorgetCarbonDataSourceNamesForTypes(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for validateJSONMapping method
            * override this method for handling normal response from validateJSONMapping operation
            */
           public void receiveResultvalidateJSONMapping(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from validateJSONMapping operation
           */
            public void receiveErrorvalidateJSONMapping(java.lang.Exception e) {
            }
                
               // No methods generated for meps other than in-out
                
           /**
            * auto generated Axis2 call back method for getPaginatedSchemaInfo method
            * override this method for handling normal response from getPaginatedSchemaInfo operation
            */
           public void receiveResultgetPaginatedSchemaInfo(
                    org.wso2.carbon.dataservices.ui.stub.admin.core.xsd.PaginatedTableInfo result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getPaginatedSchemaInfo operation
           */
            public void receiveErrorgetPaginatedSchemaInfo(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getTableList method
            * override this method for handling normal response from getTableList operation
            */
           public void receiveResultgetTableList(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getTableList operation
           */
            public void receiveErrorgetTableList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getAvailableDS method
            * override this method for handling normal response from getAvailableDS operation
            */
           public void receiveResultgetAvailableDS(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getAvailableDS operation
           */
            public void receiveErrorgetAvailableDS(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getDSServiceList method
            * override this method for handling normal response from getDSServiceList operation
            */
           public void receiveResultgetDSServiceList(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getDSServiceList operation
           */
            public void receiveErrorgetDSServiceList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getCarbonDataSourceNames method
            * override this method for handling normal response from getCarbonDataSourceNames operation
            */
           public void receiveResultgetCarbonDataSourceNames(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getCarbonDataSourceNames operation
           */
            public void receiveErrorgetCarbonDataSourceNames(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getDSService method
            * override this method for handling normal response from getDSService operation
            */
           public void receiveResultgetDSService(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getDSService operation
           */
            public void receiveErrorgetDSService(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getCarbonDataSourceType method
            * override this method for handling normal response from getCarbonDataSourceType operation
            */
           public void receiveResultgetCarbonDataSourceType(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getCarbonDataSourceType operation
           */
            public void receiveErrorgetCarbonDataSourceType(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for testGSpreadConnection method
            * override this method for handling normal response from testGSpreadConnection operation
            */
           public void receiveResulttestGSpreadConnection(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from testGSpreadConnection operation
           */
            public void receiveErrortestGSpreadConnection(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getPaginatedTableInfo method
            * override this method for handling normal response from getPaginatedTableInfo operation
            */
           public void receiveResultgetPaginatedTableInfo(
                    org.wso2.carbon.dataservices.ui.stub.admin.core.xsd.PaginatedTableInfo result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getPaginatedTableInfo operation
           */
            public void receiveErrorgetPaginatedTableInfo(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getOutputColumnNames method
            * override this method for handling normal response from getOutputColumnNames operation
            */
           public void receiveResultgetOutputColumnNames(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getOutputColumnNames operation
           */
            public void receiveErrorgetOutputColumnNames(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for getdbSchemaList method
            * override this method for handling normal response from getdbSchemaList operation
            */
           public void receiveResultgetdbSchemaList(
                    java.lang.String[] result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getdbSchemaList operation
           */
            public void receiveErrorgetdbSchemaList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for testJDBCConnection method
            * override this method for handling normal response from testJDBCConnection operation
            */
           public void receiveResulttestJDBCConnection(
                    java.lang.String result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from testJDBCConnection operation
           */
            public void receiveErrortestJDBCConnection(java.lang.Exception e) {
            }
                


    }
    