/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.activemq.jms.client.utils;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

public class Results {
    private Logger LOG = Logger.getLogger(Results.class);
    private final ReentrantLock lock = new ReentrantLock();
    private HashMap<String,Result> endResults= null;

    public Results (){

        endResults = new HashMap<>();
    }

    public HashMap<String, Result> getResults(){

        return this.endResults;

    }

    public void setResult(String key,Result value){

        lock.lock();

        try {

            this.endResults.put(key, value);

        } finally {

            lock.unlock();

        }
    }

    public void printResults(){

        LOG.infof("******** Results ********");

        for (String name : endResults.keySet()){

            Result v = endResults.get(name);

            if ((v.getTotalTime()/1000) > 0){

                LOG.infof("Client `%s` sent total of %d messages sent in %d milliseconds. Average messages per seconds %d",name,v.getMessagecount(),v.getTotalTime(),(v.getMessagecount()/(v.getTotalTime()/1000)));

            } else {

                LOG.infof("Client `%s` sent total of %d messages sent in %d milliseconds. Average messages per second %d",name,v.getMessagecount(),v.getTotalTime(),v.getMessagecount());
            }

        }

    }
}
