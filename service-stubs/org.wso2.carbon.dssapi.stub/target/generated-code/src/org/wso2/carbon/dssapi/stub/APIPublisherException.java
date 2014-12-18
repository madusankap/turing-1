
/**
 * APIPublisherException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Dec 09, 2014 (12:03:21 IST)
 */

package org.wso2.carbon.dssapi.stub;

public class APIPublisherException extends java.lang.Exception{

    private static final long serialVersionUID = 1418274031069L;
    
    private org.wso2.carbon.dataservices.core.admin.APIPublisherException faultMessage;

    
        public APIPublisherException() {
            super("APIPublisherException");
        }

        public APIPublisherException(java.lang.String s) {
           super(s);
        }

        public APIPublisherException(java.lang.String s, java.lang.Throwable ex) {
          super(s, ex);
        }

        public APIPublisherException(java.lang.Throwable cause) {
            super(cause);
        }
    

    public void setFaultMessage(org.wso2.carbon.dataservices.core.admin.APIPublisherException msg){
       faultMessage = msg;
    }
    
    public org.wso2.carbon.dataservices.core.admin.APIPublisherException getFaultMessage(){
       return faultMessage;
    }
}
    