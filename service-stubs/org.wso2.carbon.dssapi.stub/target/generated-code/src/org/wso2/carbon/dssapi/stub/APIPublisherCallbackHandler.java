
/**
 * APIPublisherCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:21 IST)
 */

    package org.wso2.carbon.dssapi.stub;

    /**
     *  APIPublisherCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class APIPublisherCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public APIPublisherCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public APIPublisherCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for removeApi method
            * override this method for handling normal response from removeApi operation
            */
           public void receiveResultremoveApi(
                    boolean result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from removeApi operation
           */
            public void receiveErrorremoveApi(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for viewSubscriptions method
            * override this method for handling normal response from viewSubscriptions operation
            */
           public void receiveResultviewSubscriptions(
                    long result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from viewSubscriptions operation
           */
            public void receiveErrorviewSubscriptions(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for listDssServices method
            * override this method for handling normal response from listDssServices operation
            */
           public void receiveResultlistDssServices(
                    org.wso2.carbon.service.mgt.xsd.ServiceMetaDataWrapper result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from listDssServices operation
           */
            public void receiveErrorlistDssServices(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for apiAvailable method
            * override this method for handling normal response from apiAvailable operation
            */
           public void receiveResultapiAvailable(
                    boolean result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from apiAvailable operation
           */
            public void receiveErrorapiAvailable(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for addApi method
            * override this method for handling normal response from addApi operation
            */
           public void receiveResultaddApi(
                    boolean result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from addApi operation
           */
            public void receiveErroraddApi(java.lang.Exception e) {
            }
                


    }
    