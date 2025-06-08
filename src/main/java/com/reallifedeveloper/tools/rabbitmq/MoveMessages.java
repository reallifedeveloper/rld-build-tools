package com.reallifedeveloper.tools.rabbitmq;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

/**
 * Moves messages from a queue, e.g., a DLQ, to an exchange. This can be useful for redelivering messages that have been dead-lettered.
 *
 * @author RealLifeDeveloper
 */
public final class MoveMessages {

    private final ConnectionFactory connectionFactory;

    /**
     * Creates a new {@code MoveMessages} instance that uses the given {@code ConnectionFactory} to connect to RabbitMQ.
     *
     * @param connectionFactory the {@code ConnectionFactory} to use to connect to Rabb8itMQ
     */
    public MoveMessages(ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("connectionFactory must not be null");
        }
        this.connectionFactory = connectionFactory.clone();
    }

    /**
     * Factory method that creates a new {@code MoveMessages} object that connects to RabbitMQ on the given host and vhost, using the given
     * username and password.
     *
     * @param host        the name of the host on which RabbitMQ is running
     * @param username    the username to use for authentication
     * @param password    the password to use for authentication
     * @param virtualHost the vhost to connect to
     *
     * @return the new {@code MoveMessages} instance
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static MoveMessages createInstance(String host, String username, String password, String virtualHost) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return new MoveMessages(factory);
    }

    /**
     * Moves all messages from a queue to an exchange.
     *
     * @param fromQueue  the queue from which to read messages
     * @param toExchange the exchange to which to send messages
     *
     * @throws IOException      if moving the messages failed because of an I/O problem
     * @throws TimeoutException if connecting to the broker timed out
     */
    public void moveAllMessagesToExchange(String fromQueue, String toExchange) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection(); Channel channel = connection.createChannel()) {
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
    }

}
