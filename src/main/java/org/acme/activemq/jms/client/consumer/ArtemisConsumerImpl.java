package org.acme.activemq.jms.client.consumer;


import org.acme.activemq.jms.client.ArtemisConsumer;
import org.acme.activemq.jms.client.Settings;
import org.acme.activemq.jms.client.producer.ArtemisProducerImpl;
import org.acme.activemq.jms.client.utils.*;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQMessage;
import org.jboss.logging.Logger;

import javax.jms.*;
import javax.naming.NamingException;


/**
 * Created by tomr on 21/06/15.
 */
public class ArtemisConsumerImpl implements ArtemisConsumer, Runnable {
    private static final Logger LOG = Logger.getLogger(ArtemisConsumerImpl.class.getName());

    private CountDownLatchWrapper latch = null;
    private ObjectStoreManager objectStoreManager = null;
    private ConnectionManager connectionManager = null;
    private Results results = null;
    private Result result = null;
    private DuplicateMessageDetector duplicates = null;
    private boolean sessionTransacted = false;
    private boolean throwException = false;
    private boolean ignoreRemoteMessageCount = false;
    private boolean logMessageText = false;

    private long startTime = 0;
    private long receiveTimeout = 0;
    private long messageConsumerDelay = 0;
    private long messageSendDelay = 0;
    private long finishTime = 0;
    private long totalTime = 0;
    private long messageUniqueValue = 0;

    private int txBatchSize = 0;
    private int logBatchSize = 0;
    private int messageCount = 0;
    private int remoteMessageCount = 0;
    private int localMessageCount = 0;
    private int messagesReceivedCnt = 0;
    private int redelieveryCount = 0;
    private int expectedMessagesCount = 0;

    private String userID = null;
    private String messageGroupName = null;
    private String hostName = null;
    private String queueName = null;
    private String threadName = null;
    private int reconnectAttempts = 0;


    // JMS section
    private Message message = null;
    private Queue queue = null;
    private QueueConnectionFactory qcf = null;
    private QueueConnection queueConnection = null;
    private QueueReceiver queueReceiver = null;
    private QueueSession queueSession = null;
    private TextMessage textMessage = null;
    private final ExceptionListener exceptionListener = new ArtemisConsumerImpl.ConnectionErrorHandle();

    private boolean firsTime = true;
    private boolean reinitialiseFactory = false;

    public ArtemisConsumerImpl() {

        throwException = Settings.getMessageThrowException();
        messageGroupName = Settings.getMessageGroup();
        sessionTransacted = Settings.getSessionTransacted();
        txBatchSize = Settings.getTxBatchSize();
        logBatchSize = Settings.getLogBatchSize();
        messageSendDelay = Settings.getMessageSendDelay();
        messageGroupName = Settings.getMessageGroup();
        messageConsumerDelay = Settings.getMessageConsumerDelay();
        receiveTimeout = Settings.getReceiveTimeout();
        localMessageCount = Settings.getMessageCount();
        hostName = Settings.getLocalHostName();
        queueName = Settings.getQueueName();
        logMessageText = Settings.getLogMessageText();

        ignoreRemoteMessageCount = Settings.getIgnoreRemoteCount();

        threadName = Thread.currentThread().getName();
        reconnectAttempts = Settings.getReconnectAttempts();

        result = new Result();

        duplicates = new DuplicateMessageDetector();

    }

    public ArtemisConsumerImpl(ObjectStoreManager objectStoreManager, CountDownLatchWrapper latch, Results results) {

        this();

        this.latch = latch;

        this.objectStoreManager = objectStoreManager;

        this.connectionManager = new ConnectionMangerImpl(objectStoreManager);

        this.results = results;

        if (LOG.isDebugEnabled()) {

            LOG.debug("ArtemisClient created.");

        }
    }

    @Override
    public boolean init() throws Exception {

        int i = 1;
        do {

            if (createJMSObjects()) {

                return true;

            } else {

                LOG.warnf("[%s] Failed to connect, retrying %d time. Total retry attempts %d, reconnect delay %d milliseconds", threadName, i, Settings.getReconnectAttempts(), Settings.getReconnectDelay());

                Helper.doDelay(Settings.getReconnectDelay());

            }

            i++;
        } while (i <= reconnectAttempts);

        return false;
    }

    @Override
    public void cleanUp() throws JMSException {

        if (LOG.isInfoEnabled()) {

            LOG.infof("Cleaning up JMS resources", threadName);

        }

        if (queueReceiver != null) {

            queueReceiver.close();
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

    @Override
    public void processMessages() throws JMSClientException {
        int i = 1;
        threadName = Thread.currentThread().getName();

        LOG.infof("[%s] <<< Starting consumer thread >>>", threadName);

        int reconn = 0;

        try {

            do {

                try {

                    queueConnection.start();

                    LOG.infof("[%s] Connection started. Starting receiving messages.", threadName);

                    while (true) {

                        message = queueReceiver.receive(receiveTimeout);

                        if (LOG.isTraceEnabled()) {

                            LOG.tracef("Recieved message {%s}.", message.toString());
                        }

                        if (startTime == 0) {
                            // first message received get current time
                            startTime = System.currentTimeMillis();
                        }

                        if (message != null && message instanceof TextMessage) {

                            textMessage = (TextMessage) message;

                            if (sessionTransacted && ((i % txBatchSize) == 0)) {

                                queueSession.commit();

                            }

                            if ((i % logBatchSize) == 0) {

                                if (LOG.isInfoEnabled()) {

                                    LOG.infof("[%s] Message '%d' consumed.", threadName, messagesReceivedCnt);

                                    if (logMessageText) {

                                        LOG.infof("[%s] Text - %s", threadName, textMessage.getText());

                                    }

                                } else if (LOG.isTraceEnabled()) {

                                    if (isLargeMessage(message)) {

                                        LOG.tracef("[%s] Message %d consumed. Message size %d", threadName, messagesReceivedCnt, getBodySize(message));

                                    } else {

                                        LOG.tracef("[%s] Message '%d' consumed. Message text '%s'", threadName, messagesReceivedCnt, textMessage.getText());
                                    }

                                }

                            }

                            if (isDone(i)) {

                                messagesReceivedCnt = i;

                                break;
                            }

                            i++;

                        } else if (message == null) {

                            LOG.infof("[%s] Receive() method timed out after '%d' seconds.", threadName, (receiveTimeout / 1000));

                            break;

                        } else {

                            LOG.warnf("[%s] Received unknown message type. Ignoring.", threadName);

                            break;

                        }
                    } // end of while loop

                    finishTime = System.currentTimeMillis();

                    totalTime = finishTime - startTime;

                    break;

                } catch (JMSException jmsException) {

                    LOG.errorf(jmsException, "Error while sending messages");

                }

            } while (reconn <= reconnectAttempts);

        } finally {


            try {

                cleanUp();

                LOG.info("[" + threadName + "] Consumer finished.");

                latch.countDown();

            } catch (JMSException jmsEx) {

                LOG.errorf(jmsEx, "[%s] Got JMS Exception while cleaning up JMS resources - ", threadName);

            }
        }

    }

    /*@Override
    public void printResults(String threadName, long totalTime, long messageCount) {

        LOG.infof("[%s]************** Results ******************",threadName);


        if ((totalTime/1000) > 0)
        {
            LOG.infof("Thread [%s] processed '%d' messages in '%d' seconds.",threadName,(totalTime/1000));
            LOG.infof("Thread [%s] Average ration per messages/second is '%d'.",threadName,(messagesReceivedCnt/(totalTime/1000) ));
        } else {
            LOG.infof("Thread [%s] processed '%d' messages in '%x' microseconds.",threadName,messageCount,totalTime);
            LOG.infof("Thread [%s] processed '%d' messages in '%x' microseconds.",threadName,messageCount,totalTime);
            LOG.infof("Thread [%s] Average ration per messages/second is '%d/%d' microseconds.",threadName,messagesReceivedCnt,totalTime);
        }

        LOG.infof("[%s] %s",threadName,results.toString());

        LOG.infof("[%s]********************************",threadName);


    }*/

    @Override
    public void run() {

        try {

            processMessages();

            result.setTotalTime(totalTime);

            result.setMessagecount(messagesReceivedCnt);

            results.setResult(threadName, result);

        } catch (JMSClientException exitError) {

            LOG.error("ERROR", exitError);

            if (totalTime == 0) {
                totalTime = System.currentTimeMillis() - startTime;
            }
            //printResults(Thread.currentThread().getName(),totalTime,messageCount);

        }
    }

    public String sessionTypeToString(int type) {

        switch (type) {
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

    private boolean isDone(int msgCount) {

        if (expectedMessagesCount == msgCount) {

            return true;
        }

        return false;

    }

    private boolean isLargeMessage(Message message) {

        ActiveMQMessage msg = (ActiveMQMessage) message;

        ClientMessage clientMessage = msg.getCoreMessage();

        return clientMessage.isLargeMessage();

    }

    private int getBodySize(Message message) {

        ActiveMQMessage msg = (ActiveMQMessage) message;

        ClientMessage clientMessage = msg.getCoreMessage();

        return clientMessage.getBodySize();

    }

    private void delay(long delay) {

        try {

            Thread.sleep(delay);

        } catch (InterruptedException intEx) {

            LOG.warnf("[%s] No no no", threadName, intEx);

        }

    }

    private boolean createJMSObjects() {

        try {

            LOG.infof("[%s] Creating JMS resources", threadName);

            if (firsTime) {

                qcf = connectionManager.getConnection(Settings.getConnectionFactoryName());

                if (!Settings.getReInitiliseFactory()) {
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

            queueReceiver = queueSession.createReceiver(queue);

            LOG.infof("[%s] Queue sender for queue '%s' created.", threadName, queueReceiver.getQueue().getQueueName());

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
            if (queueReceiver != null) {

                queueReceiver.close();
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

            LOG.errorf(jmsException, "[%s] Got JMSException while disconnecting", threadName);


        } finally {
            //queueSender = null;
            //queueSession = null;
            //queueConnection = null;
        }
    }

    class ConnectionErrorHandle implements ExceptionListener {
        private Logger LOG = Logger.getLogger(ArtemisConsumerImpl.ConnectionErrorHandle.class);

        @Override
        public void onException(JMSException exception) {
            LOG.warnf(exception, "[%s] * * * * Exception handler called on connection * * * *", threadName);

            disconnect();

            createJMSObjects();

        }
    }
}
