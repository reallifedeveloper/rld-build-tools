package com.reallifedeveloper.tools.rabbitmq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

/**
 * Moves messages from a queue, e.g., a DLQ, to an exchange. This can be useful for redelivering
 * messages that have been dead-lettered.
 *
 * @author RealLifeDeveloper
 */
public class MoveMessages {

    private final String host;
    private final String username;
    private final String password;
    private final String virtualHost;

    /**
     * Creates a new <code>MoveMessages</code> object that connects to RabbitMQ on the given host and vhost,
     * using the given username and password.
     *
     * @param host the name of the host on which RabbitMQ is running
     * @param username the username to use for authentication
     * @param password the password to use for authentication
     * @param virtualHost the vhost to connect to
     */
    public MoveMessages(String host, String username, String password, String virtualHost) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    /**
     * Moves all messages from a queue to an exchange.
     *
     * @param fromQueue the queue from which to read messages
     * @param toExchange the exchange to which to send messages
     *
     * @throws IOException if moving the messages failed because of an I/O problem
     * @throws TimeoutException if connecting to the broker timed out
     */
    public void moveAllMessagesToExchange(String fromQueue, String toExchange) throws IOException, TimeoutException {
        Connection connection = connectionFactory().newConnection();
        Channel channel = connection.createChannel();
        while (true) {
            GetResponse response = channel.basicGet(fromQueue, false);
            if (response == null) {
                return;
            }
            Envelope envelope = response.getEnvelope();
            String routingKey = envelope.getRoutingKey();
            channel.basicPublish(toExchange, routingKey, response.getProps(), response.getBody());
            channel.basicAck(envelope.getDeliveryTag(), false);
        }
    }

    private ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }
}
