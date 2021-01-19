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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DuplicateMessageDetector {
    private static final Logger LOG = Logger.getLogger(Results.class.getName());

    private AtomicInteger receiveMessageCount = null;
    private AtomicInteger duplicateMessageCount = null;
    private Set<String> messageIdList = null;
    private List<String> duplicateIdList = null;

    public DuplicateMessageDetector() {
        receiveMessageCount = new AtomicInteger(0);

        duplicateMessageCount = new AtomicInteger(0);

        messageIdList = new HashSet<>();

        duplicateIdList = new ArrayList<>();

    }

    public void addMessage(String messageId){

        if (!messageIdList.contains(messageId)) {

            messageIdList.add(messageId);

            receiveMessageCount.incrementAndGet();

        } else {

            LOG.log(Level.SEVERE,"Duplicate message found. Message id {0}" , messageId);

            duplicateIdList.add(messageId);

            duplicateMessageCount.incrementAndGet();
        }

    }

    public void printDupList(){

        LOG.info(" *** Duplicate Messages ***");

        if (!duplicateIdList.isEmpty()) {

            LOG.info("List of duplicte message IDs");

            for (String item : duplicateIdList) {

                LOG.info(item);

            }

        } else {

            LOG.info("No duplicate messages were found.");
        }

    }

    @Override
    public String toString() {
        return "This client received {" +
                "message count = " + receiveMessageCount +
                ", messageIdList.size = " + messageIdList.size() +
                ", duplicate count = " + duplicateMessageCount +
                ", duplicateIdList.size = " + duplicateIdList.size() +
                '}';
    }
}
