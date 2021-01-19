package org.acme.activemq.jms.client.utils;

import org.jboss.logging.Logger;

import javax.jms.Session;

public class Helper {
    private static final Logger LOG = Logger.getLogger(Helper.class);

    public Helper(){

    }

    public static String getQueueName(String name){

        String[] buffer = name.split("/");

        if (LOG.isDebugEnabled()){
            LOG.debugf("Found queue name = %s",buffer[buffer.length - 1]);
        }

        return buffer[buffer.length-1];
    }

    public static void doDelay(long delay){

        try {

            Thread.sleep(delay);

        } catch (InterruptedException interruptedException) {

            LOG.warnf("This shouldn't happen.");

        }
    }

    public static String sessionTypeToString(int type) {

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
}
