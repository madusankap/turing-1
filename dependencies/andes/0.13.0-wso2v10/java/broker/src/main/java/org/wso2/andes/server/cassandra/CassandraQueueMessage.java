/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.andes.server.cassandra;


import org.wso2.andes.server.message.AMQMessage;

/**
 * <code>CassandraQueueMessage</code> holds the message meta data that are transferred between
 * Global Queues and User queues.
 */
public class CassandraQueueMessage {

    private long messageId;
    private String destinationQueueName;
    private byte[] message;
    private AMQMessage amqMessage;
    private String nodeQueueName;


    public CassandraQueueMessage(long messageId , String destinationQueueName , byte[] data,AMQMessage amqMessage ) {
        this.messageId = messageId;
        this.destinationQueueName = destinationQueueName;
        this.message = data;
        this.amqMessage = amqMessage;
    }

    /**
     * Get the Queue Name for the the of the message
     * @return userQueueName name
     */
    public String getNodeQueue() {
        return nodeQueueName;
    }

    /**
     * Get Meta data content of the message
     * @return Meta data content as byte[]
     */
    public byte[] getMessage() {
        return message;
    }


    /**
     * Get qpid message id of the message
     * @return  message id
     */
    public long getMessageId() {
        return messageId;
    }

    /*Need to reset the message bytes for DLC*/
    public void setMessage(byte[] message) { this.message = message; }

    /**
     * set user queue name
     * @param queue
     */
    public void setNodeQueue(String queue) {
        this.nodeQueueName = queue;
    }

    /**
     * get amq Message inside
     * @return  amqMessage
     */
    public AMQMessage getAmqMessage() {
        return amqMessage;
    }

    /**
     * Set AMQ message
     * @param amqMessage
     */
    public void setAmqMessage(AMQMessage amqMessage) {
        this.amqMessage = amqMessage;
    }

    public String getDestinationQueueName() {
        return destinationQueueName;
    }

    public void setDestinationQueueName(String destinationQueueName) {
        this.destinationQueueName = destinationQueueName;
    }
}
