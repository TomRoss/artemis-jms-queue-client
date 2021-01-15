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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

public class ObjectStoreManager {
   private static final Logger LOG = Logger.getLogger(ObjectStoreManager.class);
   private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
   private static final Lock lock = new ReentrantLock();
   private static InitialContext ctx = null;
   private Hashtable<String,String> env = null;


   public ObjectStoreManager(){

      /* env = new Hashtable<String,String>();

      env.put(Context.INITIAL_CONTEXT_FACTORY, ObjectStoreManager.INITIAL_CONTEXT_FACTORY);
      env.put(Context.PROVIDER_URL, url);
      env.put(Context.SECURITY_PRINCIPAL, username);
      env.put(Context.SECURITY_CREDENTIALS, password); */

   }

   public <T> T getObject(String url) throws NamingException {
      Object obj = null;

      LOG.debug("Looking up JNDI name '" + url + "'.");

      try {

         lock.lock();

         ctx = new InitialContext();

         if (ctx != null){

            obj = ctx.lookup(url);

         }

      } finally{

         if ( ctx != null){

            ctx.close();

         }

         lock.unlock();

      }

      return (T) obj;
   }
}
