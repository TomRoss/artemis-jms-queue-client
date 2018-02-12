package org.activemq.jms.client.utils;

import javax.jms.JMSException;
import javax.naming.NamingException;

public interface ConnectionManager {

   public <T> T createConnection() throws JMSException,NamingException;

   public <T> T createDestination(String destinationName) throws JMSException,NamingException;

}
