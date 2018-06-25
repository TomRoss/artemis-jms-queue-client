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

package org.acme.activemq.jms.client.utils;

import java.util.concurrent.CountDownLatch;

import org.jboss.logging.Logger;

public class CountDownLatchWrapper {

   private static final Logger LOG = Logger.getLogger(CountDownLatchWrapper.class);

   private CountDownLatch latch = null;

   public CountDownLatchWrapper(int clients)
   {
      latch = new CountDownLatch(clients);
   }

   public void waitTillDone()
   {
      try
      {
         latch.await();

      }
      catch (InterruptedException e)
      {
         LOG.warn("Got Interrupted exception ", e);

      }
   }

   public void countDown()
   {
      try
      {

         latch.countDown();

      }
      finally
      {
         LOG.debug("Current latch count = " + latch.getCount());
      }
   }

   public void shutDown(){

      try {
         long c = latch.getCount();

         for (long l = 0; l < c; l++) {

            latch.countDown();

         }
      } finally {

         LOG.warnf("[%s] Should never happen. Shutting down the latch.",Thread.currentThread().getName());
      }
   }
}
