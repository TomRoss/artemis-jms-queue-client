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

package org.acme.activemq.jms.client;


import javax.jms.Message;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.jboss.logging.Logger;


public class Settings {

   private static final Logger LOG = Logger.getLogger(Settings.class);

   private static final Object lock = Settings.class;

   private static Settings settings = null;

   private static final String RECEIVE_TIME_OUT_PROP = "receive.timeout";

   private static final String CONNECT_URL = "connection.url";

   private static final String USE_JNDI = "use.jndi";

   private static final String QUEUE_NAME_PROP = "queue.name";

   private static final String QUEUE_AUTO_CREATE_PROP = "queue.auto-create";

   private static final String CONNECTION_NAME_PROP = "connection.name";

   private static final String MESSAGE_COUNT_PROP = "message.count";

   private static final String MESSAGE_THROW_EXCEPTION_PROP = "message.throw.exception";

   private static final String MESSAGE_SEND_DELAY_PROP = "message.send.delay";

   private static final String CONSUMER_DELAY_PROP ="message.consume.delay";

   private static final String MESSAGE_SCHEDULED_PROP ="message.scheduled.delay";

   private static final String MESSAGE_EXPIRE_PROP = "message.expire";

   private static final String MESSAGE_GROUP_PROP = "message.group";

   private static final String MESSAGE_PRIORITY_PROP = "message.priority";

   private static final String MESSAGE_SELECTOR_PROP = "message.selector";

   private static final String MESSAGE_SIZE_PROP = "message.size";

   private static final String NUMBER_OF_CLIENTS_PROP = "client.count";

   private static final String SESSION_TRANSACTED_PROP = "session.transacted";

   private static final String DUP_DETECY_PROP = "dup.detect";

   private static final String TX_BATCH_SIZE_PROP = "tx.batch.size";

   private static final String LOG_BATCH_SIZE_PROP = "log.batch.size";

   private static final String USER_NAME_PROP = "username";

   private static final String USER_PASSWORD_PROP = "password";

   private static final String RECONNECT_ATTEMPTS = "reconnect.attempts";

   private static final String RECONNECT_DELAY = "reconnect.delay";

   private static String localHostName = null;

   public static int exitStatus = 0;



   public Settings() {


      try {

         localHostName = InetAddress.getLocalHost().getHostName();

         LOG.debug("Client running on host '" + localHostName + "'.");

      }  catch (UnknownHostException ex){

         LOG.warn("Can't obtain host name.");
      }
   }

   public static Settings getInstance() {
      if (settings == null) {
         synchronized(lock) {
            if (settings == null)
               settings = new Settings();
         }
      }

      return settings;
   }

   public void init(){

   }

   public static String getQueueName(){

      return System.getProperty(QUEUE_NAME_PROP, "jms/queue/testQueue");
   }

   public static String getUserName(){

      return System.getProperty(USER_NAME_PROP,"quickuser");

   }

   public static String getPassword(){

      return System.getProperty(USER_PASSWORD_PROP,"quick123+");

   }

   public static long getConsumerDelay() {

      return Long.parseLong(System.getProperty(CONSUMER_DELAY_PROP, "0"));
   }

   public static long getMsgExpire() {

      return Long.parseLong(System.getProperty(MESSAGE_EXPIRE_PROP, "0"));
   }

   public static int getTotalMsg() {

      return Integer.parseInt(System.getProperty(MESSAGE_COUNT_PROP, "1"));
   }

   public static int getClientCnt() {

      return Integer.parseInt(System.getProperty(NUMBER_OF_CLIENTS_PROP, "1"));
   }

   public static int getTxBatchSize() {

      return Integer.parseInt(System.getProperty(TX_BATCH_SIZE_PROP,"1"));
   }

   public static int getMessageCount() {

      return Integer.parseInt(System.getProperty(MESSAGE_COUNT_PROP,"1"));
   }

   public static String getConnectionFactoryName() {

      return System.getProperty(CONNECTION_NAME_PROP, "ConnectionFactory");
   }

   public static boolean getMessageThrowException(){

      return Boolean.parseBoolean(System.getProperty(MESSAGE_THROW_EXCEPTION_PROP, "false"));
   }

   public static String getMessageGroup(){

      return System.getProperty(Settings.MESSAGE_GROUP_PROP, null);
   }

   public static long getMessageSendDelay(){

      return Long.parseLong(System.getProperty(Settings.MESSAGE_SEND_DELAY_PROP, "0"));
   }

   public static long getMessageConsumerDelay(){

      return Long.parseLong(System.getProperty(Settings.CONSUMER_DELAY_PROP, "0"));
   }

   public static int getLogBatchSize(){

      return Integer.parseInt(System.getProperty(Settings.LOG_BATCH_SIZE_PROP, "1"));
   }

   public static long getReceiveTimeout(){

      return Long.parseLong(System.getProperty(Settings.RECEIVE_TIME_OUT_PROP, "30000"));

   }

   public static String getLocalHostName()
   {
      try
      {
         localHostName = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException ex)
      {
         LOG.error("ERROR", ex);
         System.exit(-1);
      }

      return localHostName;
   }

   public static boolean getSessionTransacted()
   {
      return Boolean.parseBoolean(System.getProperty(SESSION_TRANSACTED_PROP, "false"));
   }

   public static int getMessagePriority(){

      return Integer.parseInt(System.getProperty(MESSAGE_PRIORITY_PROP, Integer.toString(Message.DEFAULT_PRIORITY)));

   }

   public static String getConnectUrl(){

      return System.getProperty(Settings.CONNECT_URL, "tcp://localhost:61616");
   }

   public static String getMessageSelector(){

      return System.getProperty(Settings.MESSAGE_SELECTOR_PROP);
   }

   public static boolean getDupDetect(){

      return Boolean.parseBoolean(System.getProperty(Settings.DUP_DETECY_PROP,"false"));

   }

   public static int getMessageSize(){

      return Integer.parseInt(System.getProperty(MESSAGE_SIZE_PROP, "0"));
   }

   public static long getMessageScheduledDelay(){

      return Long.parseLong(System.getProperty(MESSAGE_SCHEDULED_PROP, "0"));
   }

   public static boolean getUseJNDI(){

      return Boolean.parseBoolean(System.getProperty(USE_JNDI,"true"));

   }

   public static int getReconnectAttempts(){
      return Integer.parseInt(System.getProperty(RECONNECT_ATTEMPTS,"15"));
   }

   public static long getReconnectDelay(){
      return Long.parseLong(System.getProperty(RECONNECT_DELAY,"10000"));
   }

   public static boolean getQueueAutoCreate(){

      return Boolean.parseBoolean(System.getProperty(QUEUE_AUTO_CREATE_PROP,"false"));
   }

}
