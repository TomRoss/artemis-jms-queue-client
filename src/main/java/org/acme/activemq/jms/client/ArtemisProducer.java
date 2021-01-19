package org.acme.activemq.jms.client;

public interface ArtemisProducer extends ArtemisClient {
    public String getMessagePayLoad(int size);
}
