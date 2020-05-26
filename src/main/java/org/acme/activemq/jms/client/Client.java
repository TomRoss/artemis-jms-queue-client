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

import javax.jms.JMSException;
import javax.naming.NamingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acme.activemq.jms.client.producer.JMSClient;
import org.acme.activemq.jms.client.utils.CountDownLatchWrapper;
import org.acme.activemq.jms.client.producer.JMSClientImpl;
import org.acme.activemq.jms.client.utils.JMSClientException;
import org.acme.activemq.jms.client.utils.ObjectStoreManager;

import org.acme.activemq.jms.client.utils.Results;
import org.jboss.logging.Logger;

public class Client {
   private static final Logger LOG = Logger.getLogger(Client.class);
   private ExecutorService executor = null;
   private JMSClient queueProducer = null;
   private CountDownLatchWrapper cLatch = null;
   private ObjectStoreManager objectStoreManager = null;
   private Results results = new Results();

   public Client()
   {

      executor = Executors.newFixedThreadPool(Settings.getClientCnt());
      objectStoreManager = new ObjectStoreManager(Settings.getConnectUrl(), Settings.getUserName(), Settings.getPassword());
      cLatch = new CountDownLatchWrapper(Settings.getClientCnt());

      LOG.debug("Client created.");

   }

   public void runClient(int clientCnt) {

      LOG.info("<<< Starting client threads >>>");

      try {

         for (int i = 0; i < clientCnt; i++) {

            queueProducer = new JMSClientImpl(objectStoreManager, cLatch,results);

            if (queueProducer.init()) {

               executor.execute(queueProducer);

            } else {

               cLatch.shutDown();

               break;

            }
         }

      } catch (NamingException namingException) {

         LOG.errorf(namingException,"[%s]Got NaimingException: ",Thread.currentThread().getName());

         cLatch.shutDown();

      } catch (JMSClientException jmsclientException){

         LOG.errorf(jmsclientException,"[%s] Exiting because of ",Thread.currentThread().getName());

         cLatch.shutDown();

      } catch (JMSException jmsException){

         LOG.errorf(jmsException,"[%s]Got JMSException: ", Thread.currentThread().getName());

         cLatch.shutDown();

      } catch (Exception exception){

         LOG.errorf(exception,"");

         cLatch.shutDown();

      }


      LOG.info("All producers started.");

      cLatch.waitTillDone();

      this.executor.shutdown();


      if (!this.executor.isShutdown())
      {

         while(true){

            if (!this.executor.isShutdown())
            {
               break;
            }
            else
            {

               try
               {
                  Thread.sleep(2000);
               }
               catch (InterruptedException e)
               {

                  LOG.warn("Thread interrupted.",e);

               }
            }
         }
      }

      LOG.info(" === Clients finished === ");

      results.printResults();

   }
}
