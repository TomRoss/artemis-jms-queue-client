/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activemq.jms.client.producer;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.activemq.jms.client.Settings;
import org.activemq.jms.client.utils.ConnectionManager;
import org.activemq.jms.client.utils.CountDownLatchWrapper;
import org.activemq.jms.client.utils.JMSClientException;
import org.activemq.jms.client.utils.JMSMessageProperties;
import org.activemq.jms.client.utils.ObjectStoreManager;

import org.jboss.logging.Logger;

import org.activemq.jms.client.utils.ConnectionMangerImpl;

public class JMSClientImpl implements JMSClient {
   private static final Logger LOG = Logger.getLogger(JMSClientImpl.class);
   private static final String messageText = "This is text message '%d' out of '%d'. Sent from host '%s'.";

   private CountDownLatchWrapper latch = null;
   private ObjectStoreManager objectStoreManager = null;
   private ConnectionManager connectionManager = null;
   private boolean sessionTransacted = false;
   private boolean throwException = false;

   private long startTime = 0;
   private long receiveTimeout = 0;
   private long messageConsumerDelay = 0;
   private long messageSendDelay = 0;
   private long messageScheduledDelay = 0;
   private long messageExpiration = 0;
   private long finishTime = 0;
   private long totalTime = 0;

   private int txBatchSize = 0;
   private int logBatchSize = 0;
   private int messageCount = 0;
   private int messagesReceivedCnt = 0;
   private int messagePriority = Message.DEFAULT_PRIORITY;
   private int messageSize = 0;
   private boolean dupDetect = false;

   private String messageGroupName = null;
   private String hostName = null;
   private String queueName = null;
   private String threadName = null;


   // JMS section
   private Message message = null;
   private Queue queue = null;
   private QueueConnectionFactory qcf = null;
   private QueueConnection queueConnection = null;
   private QueueSender queueSender = null;
   private QueueSession queueSession = null;
   private TextMessage textMessage = null;

   public JMSClientImpl(){

      throwException = Settings.getMessageThrowException();
      messageGroupName = Settings.getMessageGroup();
      sessionTransacted = Settings.getSessionTransacted();
      txBatchSize = Settings.getTxBatchSize();
      logBatchSize = Settings.getLogBatchSize();
      messageSendDelay = Settings.getMessageSendDelay();
      messageGroupName = Settings.getMessageGroup();
      messageConsumerDelay = Settings.getMessageConsumerDelay();
      messageExpiration = Settings.getMsgExpire();
      messageSize = Settings.getMessageSize();
      receiveTimeout = Settings.getReceiveTimeout();
      messageCount = Settings.getMessageCount();
      hostName = Settings.getLocalHostName();
      queueName = Settings.getQueueName();

   }

   public JMSClientImpl(ObjectStoreManager objectStoreManager, CountDownLatchWrapper latch){

      this();

      this.latch = latch;

      this.objectStoreManager = objectStoreManager;

      this.connectionManager = new ConnectionMangerImpl(objectStoreManager,Settings.useJndi());

      LOG.debug("JMSClient created.");

   }

   public void init() throws JMSClientException, NamingException, JMSException {

      queueConnection = connectionManager.createConnection();

      queue = connectionManager.createDestination(this.queueName);

   }

   public void processMessages()  throws JMSClientException {
      int i = 1;
      threadName = Thread.currentThread().getName();


      LOG.infof("[%s] <<< Starting producer thread >>>",threadName);

      try {

         if (Settings.getSessionTransacted()){

            queueSession = queueConnection.createQueueSession(true,Session.SESSION_TRANSACTED);

         } else {

            queueSession = queueConnection.createQueueSession(false,Session.AUTO_ACKNOWLEDGE);

         }

         queueSender = queueSession.createSender(queue);

         textMessage = queueSession.createTextMessage();

         startTime = System.currentTimeMillis();

         while (true){

            if (messageSize == 0) {

               textMessage.setText(String.format(messageText, i, this.messageCount, this.hostName));

            } else {

               textMessage.setText(getMessagePayLoad(messageSize));

            }

            textMessage.setIntProperty(JMSMessageProperties.TOTAL_MESSAGE_COUNT, this.messageCount);

            textMessage.setLongProperty(JMSMessageProperties.UNIQUE_VALUE,System.currentTimeMillis());

            textMessage.setStringProperty(JMSMessageProperties.PRODUCER_HOST, this.hostName);

            textMessage.setStringProperty(JMSMessageProperties.PRODUCER_NAME, threadName);

            textMessage.setLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY, this.messageConsumerDelay);

            textMessage.setBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION,this.throwException);


            if (this.messageScheduledDelay != 0){

               long timeToDeliver = System.currentTimeMillis() + messageScheduledDelay;

               textMessage.setLongProperty("_AMQ_SCHED_DELIVERY",timeToDeliver);
            }



            if (this.messageGroupName != null){

               long time = System.currentTimeMillis();

               textMessage.setStringProperty("JMSXGroupID", this.messageGroupName);

            }

            if (dupDetect){

               textMessage.setStringProperty(org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID.toString(),Long.toString(System.currentTimeMillis()));

            }

            messagePriority = 8;

            queueSender.send(textMessage, DeliveryMode.PERSISTENT, messagePriority, messageExpiration);

            if ( this.sessionTransacted && ((  i % this.txBatchSize ) == 0)){

               queueSession.commit();

            }

            if ( (i % logBatchSize) == 0){

               LOG.infof("[%s] Message '" + i + "' sent.",threadName);

            }

            if ( i == messageCount){

               break;

            }

            i++;

            if (this.messageSendDelay != 0){

               try {

                  Thread.sleep(this.messageSendDelay);

               } catch (InterruptedException interp){

                  LOG.warnf("[%s]This thread has been interruped. %s",threadName,interp);

               }

            }

         } // end of while loop

         finishTime = System.currentTimeMillis();

         totalTime = finishTime - startTime;

         printResults(Thread.currentThread().getName(), totalTime,messageCount);

      } catch (JMSException jmsEx) {

         LOG.errorf(jmsEx,"[%s] Got JMS Exception - ",threadName);

      } catch (Exception ex){

         LOG.errorf(ex,"[%s] Got Exception - ",threadName);

      } finally {

         try {

            cleanUp();

            LOG.infof("[%s] Producer finished.",threadName);

            latch.countDown();

         } catch (JMSException jmsEx) {

            LOG.errorf(jmsEx,"[%s] Got JMS Exception - ",threadName);

         }
      }
   }

   public void printResults(String threadName, long totalTime, long messageCount){

      LOG.infof("[%s] ******** Results ********",threadName);

      LOG.infof("Total %d messages sent in %d milliseconds.",messageCount,totalTime);
   }

   public String getMessagePayLoad(int size){
      int _size = size * 1024;

      StringBuffer buffer = new StringBuffer(_size);

      for (int i = 0; i < _size; i++){

         buffer.append('A');
      }

      return buffer.toString();

   }


   public String sessionTypeToString(int type)
   {

      switch (type){
         case Session.AUTO_ACKNOWLEDGE:
            return "Auto-Acknowledge";
         case Session.CLIENT_ACKNOWLEDGE:
            return "Client-Acknowledge";
         case Session.DUPS_OK_ACKNOWLEDGE:
            return "Dups-OK_Acknowledge";
         case Session.SESSION_TRANSACTED:
            return "Session-Transacted";
         default:
            return "Unknown";
      }
   }


   public void cleanUp() throws JMSException {

      if (LOG.isDebugEnabled()) {

         LOG.debug("Cleaning up JMS resources");

      }

      if ( queueSender != null){

         queueSender.close();
         if (LOG.isDebugEnabled())
            LOG.debug("Sender closed.");
      }

      if ( queueSession != null){

         queueSession.close();
         if (LOG.isDebugEnabled())
            LOG.debug("Session closed.");
      }

      if ( queueConnection != null){

         queueConnection.close();
         if (LOG.isDebugEnabled())
            LOG.debug("Connection closed.");
      }
   }

   public void run(){

      try {

         processMessages();

      } catch (JMSClientException exitError) {

         LOG.error("ERROR",exitError);

         if (totalTime == 0){
            totalTime = System.currentTimeMillis() - startTime;
         }
         printResults(Thread.currentThread().getName(),totalTime,messageCount);
      }
   }
}
