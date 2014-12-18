
/**
 * DataServiceAdminExceptionException.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.6.1-wso2v10  Built on : Nov 12, 2014 (10:31:02 IST)
 */

package org.wso2.carbon.dataservices.ui.stub;

public class DataServiceAdminExceptionException extends java.lang.Exception{

    private static final long serialVersionUID = 1416397269359L;
    
    private org.wso2.carbon.dataservices.ui.stub.admin.DataServiceAdminException faultMessage;

    
        public DataServiceAdminExceptionException() {
            super("DataServiceAdminExceptionException");
        }

        public DataServiceAdminExceptionException(java.lang.String s) {
           super(s);
        }

        public DataServiceAdminExceptionException(java.lang.String s, java.lang.Throwable ex) {
          super(s, ex);
        }

        public DataServiceAdminExceptionException(java.lang.Throwable cause) {
            super(cause);
        }
    

    public void setFaultMessage(org.wso2.carbon.dataservices.ui.stub.admin.DataServiceAdminException msg){
       faultMessage = msg;
    }
    
    public org.wso2.carbon.dataservices.ui.stub.admin.DataServiceAdminException getFaultMessage(){
       return faultMessage;
    }
}
    