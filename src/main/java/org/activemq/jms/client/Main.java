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

package org.activemq.jms.client;

import org.jboss.logging.Logger;

public class Main {
   private static final Logger LOG = Logger.getLogger(Main.class);


   public Main() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @param args
    * @throws InterruptedException
    */
   public static void main(String[] args) throws InterruptedException {

      LOG.info("<<< Starting Simple JMS Queue Producer >>>");

      if (Boolean.parseBoolean(System.getProperty("help", "false")))
      {
         printHelp();
         System.exit(0);
      }

      Client client = new Client();

      client.runClient(Settings.getInstance().getClientCnt());

      LOG.info("<<< shutting down Simple JMS Queue Producer >>>");

      System.exit(Settings.exitStatus);
   }

   public static void printHelp(){

      LOG.info("************************************");
      LOG.info("mvn exec:java -D[property=value]");
      LOG.info("List of properties with [default values]:");
      LOG.info("\tconnect.url - message connect string. [tcp://localhost:5445]");
      LOG.info("\tusername - user name [quickuser]");
      LOG.info("\tpassword - user password [quick123+]");
      LOG.info("\tconnection.name - connection factory name. [jms/RemoteConnectionFactory]");
      LOG.info("\tqueue.name - queue name. [jms/queue/testQueue]");
      LOG.info("\tmessage.number - number of message to send. [1]");
      LOG.info("\tmessage.send.delay - delay between each message send [0] (in milliseconds)");
      LOG.info("\tmessage.consume.delay - delay between message consumption [0] (in milliseconds)");
      LOG.info("\tmessage.scheduled.delay - scheduled message delay (in milliseconds)");
      LOG.info("\tmessage.throw.exception - throw exception when consuming message [false]");
      LOG.info("\tmessage.expire - expire message after x millisends [0] (in milliseconds)");
      LOG.info("\tmessage.group - sets the value of JMSXGroupID");
      LOG.info("\tmessage.priority - message priority [Message.DEFALUT_PRIORITY]");
      LOG.info("\tmessage.size - message size in KB");
      LOG.info("\tclient.number - number of JMS clients. [1]");
      LOG.info("\tsession.transacted - is the JMS session transacted. [false]");
      LOG.info("\tbatch.size - transaction batch size. [1]");
      LOG.info("\tlog.batch.size - logging batch size. [10]");
      LOG.info("\tdup.detect - duplicate message detection. [false]");

      LOG.info("************************************");
   }

}