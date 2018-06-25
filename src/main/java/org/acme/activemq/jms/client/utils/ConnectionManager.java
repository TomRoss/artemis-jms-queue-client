package org.acme.activemq.jms.client.utils;

import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.naming.NamingException;

public interface ConnectionManager {

   public <T> T createConnection() throws Exception;

   public <T> T createDestination(String destinationName) throws Exception;

}
