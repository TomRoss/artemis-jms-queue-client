package org.acme.activemq.jms.client.utils;

import org.jboss.logging.Logger;

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
}
