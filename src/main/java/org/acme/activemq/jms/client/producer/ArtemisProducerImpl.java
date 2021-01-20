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

package org.acme.activemq.jms.client.producer;

import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.Session;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.NamingException;


import org.acme.activemq.jms.client.ArtemisProducer;
import org.acme.activemq.jms.client.Settings;
import org.acme.activemq.jms.client.utils.Result;
import org.acme.activemq.jms.client.utils.Results;
import org.acme.activemq.jms.client.utils.CountDownLatchWrapper;
import org.acme.activemq.jms.client.utils.ObjectStoreManager;
import org.acme.activemq.jms.client.utils.ConnectionManager;
import org.acme.activemq.jms.client.utils.JMSClientException;
import org.acme.activemq.jms.client.utils.ConnectionMangerImpl;
import org.acme.activemq.jms.client.utils.JMSMessageProperties;
import org.acme.activemq.jms.client.utils.Helper;

import org.jboss.logging.Logger;

public class ArtemisProducerImpl implements ArtemisProducer {
    private static final Logger LOG = Logger.getLogger(ArtemisProducerImpl.class);
    private static final String messageText = "This is text message '%d' out of '%d'. Sent from host '%s'.";

    private CountDownLatchWrapper latch = null;
    private ObjectStoreManager objectStoreManager = null;
    private ConnectionManager connectionManager = null;
    private Results results = null;
    private Result result = null;
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
    private int messagePriority = Message.DEFAULT_PRIORITY;
    private int messageSize = 0;
    private boolean dupDetect = false;
    private boolean clientDone = false;
    private boolean queueAutoCreate = false;


    private String messageGroupName = null;
    private String hostName = null;
    private String queueName = null;
    private String threadName = null;
    private int reconnectAttempts = 0;
    private long reconnectDelay = 0;


    // JMS section
    private JMSContext jmsContext = null;
    private JMSProducer jmsProducer = null;
    private Message message = null;
    private Queue queue = null;
    private QueueConnectionFactory qcf = null;
    private QueueConnection queueConnection = null;
    private QueueSender queueSender = null;
    private QueueSession queueSession = null;
    private TextMessage textMessage = null;
    private final ExceptionListener exceptionListener = new ConnectionErrorHandle();

    private boolean firsTime = true;
    private boolean reinitialiseFactory = false;

    public ArtemisProducerImpl() {

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
        messagePriority = Settings.getMessagePriority();
        receiveTimeout = Settings.getReceiveTimeout();
        messageCount = Settings.getMessageCount();
        hostName = Settings.getLocalHostName();
        queueName = Settings.getQueueName();
        reconnectAttempts = Settings.getReconnectAttempts();
        reconnectDelay = Settings.getReconnectDelay();
        queueAutoCreate = Settings.getQueueAutoCreate();
        reinitialiseFactory = Settings.getReInitiliseFactory();
        threadName = Thread.currentThread().getName();

        result = new Result();

    }

    public ArtemisProducerImpl(ObjectStoreManager objectStoreManager, CountDownLatchWrapper latch, Results results) {

        this();

        this.latch = latch;

        this.objectStoreManager = objectStoreManager;

        this.connectionManager = new ConnectionMangerImpl(objectStoreManager);

        this.results = results;

        LOG.debug("ArtemisClient created.");

    }

    public boolean init() throws Exception {
        int i = 1;
         do {

            if (createJMSObjects()){

                return true;

            } else {

                LOG.warnf("[%s] Failed to connect, retrying %d time. Total retry attempts %d, reconnect delay %d milliseconds",threadName,i,Settings.getReconnectAttempts(),Settings.getReconnectDelay());

                Helper.doDelay(Settings.getReconnectDelay());

            }

            i++;
        } while( i <= reconnectAttempts);

        return false;
    }

    public void processMessages() throws JMSClientException {
        int i = 1;

        LOG.infof("[%s] <<< Starting producer thread >>>", threadName);

        int reconn = 0;

        try {

            while (reconn <= reconnectAttempts) {

                try {
                    queueSender = queueSession.createSender(queue);

                    textMessage = queueSession.createTextMessage();

                    startTime = System.currentTimeMillis();

                    while (true) {

                        if (messageSize == 0) {

                            textMessage.setText(String.format(messageText, i, this.messageCount, this.hostName));

                        } else {

                            textMessage.setText(getMessagePayLoad(messageSize));

                        }

                        textMessage.setIntProperty(JMSMessageProperties.TOTAL_MESSAGE_COUNT, this.messageCount);

                        textMessage.setLongProperty(JMSMessageProperties.UNIQUE_VALUE, System.currentTimeMillis());

                        textMessage.setStringProperty(JMSMessageProperties.PRODUCER_HOST, this.hostName);

                        textMessage.setStringProperty(JMSMessageProperties.PRODUCER_NAME, threadName);

                        textMessage.setLongProperty(JMSMessageProperties.MESSAGE_CONSUMER_DELAY, this.messageConsumerDelay);

                        textMessage.setBooleanProperty(JMSMessageProperties.MESSAGE_THROW_EXCEPTION, this.throwException);


                        if (this.messageScheduledDelay != 0) {

                            long timeToDeliver = System.currentTimeMillis() + messageScheduledDelay;

                            textMessage.setLongProperty("_AMQ_SCHED_DELIVERY", timeToDeliver);
                        }


                        if (this.messageGroupName != null) {

                            long time = System.currentTimeMillis();

                            textMessage.setStringProperty("JMSXGroupID", this.messageGroupName);

                        }

                        if (i < 30){

                            textMessage.setStringProperty("queue_command","BA");

                        } else if ( i > 30 && i < 60){

                            textMessage.setStringProperty("queue_command","BB");

                        } else if ( i > 60){

                            textMessage.setStringProperty("queue_command","BC");
                        }


                        if (dupDetect) {

                            //textMessage.setStringProperty(org.apache.activemq.artemis.api.core.Message.HDR_DUPLICATE_DETECTION_ID.toString(),Long.toString(System.currentTimeMillis()));

                        }
                        
                        queueSender.send(textMessage, DeliveryMode.PERSISTENT, messagePriority, messageExpiration);

                        if (this.sessionTransacted && ((i % this.txBatchSize) == 0)) {

                            queueSession.commit();

                        }

                        if ((i % logBatchSize) == 0) {

                            LOG.infof("[%s] Message '%d' sent.", threadName,i);

                        }

                        if (i == messageCount) {

                            clientDone = true;

                            break;

                        }

                        i++;

                        if (this.messageSendDelay != 0) {
                            doDelay(this.messageSendDelay);
                        }

                    } // end of while loop

                    finishTime = System.currentTimeMillis();

                    totalTime = finishTime - startTime;

                    if (clientDone) {

                        break;
                    }

                } catch (JMSException jmsEx) {

                    Exception ex = jmsEx.getLinkedException();

                    LOG.errorf(jmsEx, "[%s] Got JMS Exception - ", threadName);

                    doDelay(reconnectDelay);

                } catch (Exception exception) {

                    LOG.errorf(exception, "[%s] Got Exception - ", threadName);

                    break;

                } finally {

                    reconn++;

                }
            } // end of while reconnect loop

        } finally {

            try {

                cleanUp();

                LOG.infof("[%s] Producer finished.", threadName);

                latch.countDown();

            } catch (JMSException jmsEx) {

                LOG.errorf(jmsEx, "[%s] Got JMS Exception - ", threadName);

            }
        }
    }

    public String getMessagePayLoad(int size) {
        int _size = size * 1024;

        StringBuffer buffer = new StringBuffer(_size);

        for (int i = 0; i < _size; i++) {

            buffer.append('A');
        }

        return buffer.toString();

    }


    public void cleanUp() throws JMSException {

        if (LOG.isInfoEnabled()) {

            LOG.infof("[%s] Cleaning up JMS resources", threadName);

        }

        if (queueSender != null) {

            queueSender.close();
            if (LOG.isDebugEnabled())
                LOG.debug("Sender closed.");
        }

        if (queueSession != null) {

            queueSession.close();
            if (LOG.isDebugEnabled())
                LOG.debug("Session closed.");
        }

        if (queueConnection != null) {

            queueConnection.close();
            if (LOG.isDebugEnabled())
                LOG.debug("Connection closed.");
        }
    }

    public void run() {

        try {

            processMessages();

            if (totalTime == 0) {

                totalTime = System.currentTimeMillis() - startTime;

            }

            result.setTotalTime(totalTime);

            result.setMessagecount(messageCount);

            results.setResult(threadName, result);

        } catch (JMSClientException exitError) {

            LOG.errorf(exitError, "ERROR");

        }
    }

    private void doDelay(long delay) {

        if (LOG.isTraceEnabled()) {

            LOG.tracef("Going to sleep %d.", delay);

        }

        try {

            Thread.sleep(delay);

        } catch (InterruptedException interp) {

            LOG.error("This thread has been interruped.", interp);

        }
    }

    private boolean createJMSObjects() {

        try {

            LOG.infof("[%s] Creating JMS resources", threadName);

            if (firsTime) {

                qcf = connectionManager.getConnection(Settings.getConnectionFactoryName());

                if (!Settings.getReInitiliseFactory()){
                    firsTime = false;
                }

            }

            queueConnection = qcf.createQueueConnection(Settings.getUserName(), Settings.getPassword());

            queueConnection.setExceptionListener(exceptionListener);

            LOG.infof("[%s] Connection started. Starting receiving messages.", threadName);

            if (this.sessionTransacted) {

                queueSession = queueConnection.createQueueSession(true, Session.SESSION_TRANSACTED);

            } else {

                queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            }

            LOG.infof("[%s] Created queue session '%s'.", threadName, Helper.sessionTypeToString(queueSession.getAcknowledgeMode()));



            queue = connectionManager.getDestination(this.queueName);

            queueSender = queueSession.createSender(queue);

            LOG.infof("[%s] Queue sender for queue '%s' created.", threadName, queueSender.getQueue().getQueueName());

            return true;

        } catch (JMSException jmsException) {

            LOG.errorf(jmsException, "JMS Error");

            return false;

        } catch (NamingException namingException) {

            LOG.errorf(namingException, "Naming Error");

            return false;
        }
    }

    private void disconnect() {

        LOG.infof("[%s] Disconnect method called", threadName);

        try {
            if (queueSender != null) {

                queueSender.close();
                if (LOG.isDebugEnabled())
                    LOG.debugf("[%s] Sender closed.", threadName);
            }

            if (queueSession != null) {

                queueSession.close();
                if (LOG.isDebugEnabled())
                    LOG.debugf("[%s] Session closed.", threadName);
            }

            if (queueConnection != null) {

                if (LOG.isDebugEnabled()) {

                    LOG.debugf("[%s] Removing exception listener", threadName);
                }

                if (queueConnection.getExceptionListener() != null) {

                    queueConnection.setExceptionListener(null);

                }

                queueConnection.close();

                if (LOG.isDebugEnabled()) {
                    LOG.debugf("[%s] Connection closed.", threadName);
                }
            }
        } catch (JMSException jmsException) {

            LOG.errorf(jmsException,"[%s] Got JMSException while disconnecting",threadName);


        } finally {
            //queueSender = null;
            //queueSession = null;
            //queueConnection = null;
        }
    }

    class ConnectionErrorHandle implements ExceptionListener {
        private Logger LOG = Logger.getLogger(ArtemisProducerImpl.ConnectionErrorHandle.class);

        @Override
        public void onException(JMSException exception) {
            LOG.warnf(exception, "[%s] * * * * Exception handler called on connection * * * *", threadName);

            disconnect();

            createJMSObjects();

        }
    }
}
