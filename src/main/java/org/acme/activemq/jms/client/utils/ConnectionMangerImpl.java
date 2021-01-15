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

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.naming.NamingException;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.logging.Logger;

import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.acme.activemq.jms.client.Settings;

public class ConnectionMangerImpl implements ConnectionManager {
    private static final Logger LOG = Logger.getLogger(ConnectionMangerImpl.class);
    private boolean useJndi = true;
    private Hashtable<String, String> env = null;
    private ObjectStoreManager objectStoreManager = null;
    private QueueConnectionFactory qcf = null;
    private TransportConfiguration transportConfiguration = null;
    private ActiveMQConnectionFactory amqCF = null;
    private String threadName = Thread.currentThread().getName();


    public ConnectionMangerImpl(ObjectStoreManager objectStoreManager) {

        this.objectStoreManager = objectStoreManager;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Connection manager created.");
        }
    }

    /* public <T> T createConnection() throws Exception {

         amqCF.setBlockOnDurableSend(false);
         amqCF.setBrokerURL("(tcp://jess:61616,tcp://aza:61616)?QUEUE_CF&ha=true&reconnectAttempts=-1");
         qcf = (QueueConnectionFactory) amqCF;

        amqCF = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.QUEUE_CF, transportConfiguration);

        amqCF.setBlockOnDurableSend(false);
        amqCF.setBrokerURL(Settings.getConnectUrl());
        qcf = (QueueConnectionFactory) amqCF;

        LOG.infof("Creating connection to %s with user:password %s:%s''.", Settings.getConnectUrl(), Settings.getUserName(), Settings.getPassword());

        return (T) qcf.createQueueConnection(Settings.getUserName(), Settings.getPassword());

    } */

    public <T> T createDestination(String destinationName) throws NamingException, JMSException {



            LOG.infof("Creating destination '%s'.", destinationName);

            return (T) objectStoreManager.getObject(destinationName);


    }

    @Override
    public <T> T getConnection(String connectionName) throws JMSException, NamingException {
        LOG.infof("[%s] Fetching destination '%s'.", threadName, connectionName);

        return (T) objectStoreManager.getObject(connectionName);

    }

    @Override
    public <T> T getDestination(String destinationName) throws JMSException, NamingException {
        LOG.infof("[%s] Fetching destination '%s'.", Thread.currentThread().getName(), destinationName);

        return (T) objectStoreManager.getObject(destinationName);
    }

    public String toString() {
        StringBuilder str = new StringBuilder();

        str.append("Connection Manager: URL='");
        //str.append(Settings.getConnectUrl());
        str.append(" connection type='");
        str.append("'.");

        return str.toString();
    }

    private HashMap<String, Object> parseUrl(String url) {
        HashMap<String, Object> map = new HashMap<>();
        String host = null;
        String port = null;
        String[] tokens = url.split(":");

        for (int i = 0; i < tokens.length; i++) {

            if (i == 1) {
                host = tokens[i].substring(2);
            }
            if (i == 2) {

                port = tokens[i].substring(0);
            }
        }

        map.put("host", host);

        map.put("port", port);

        return map;

    }
}
