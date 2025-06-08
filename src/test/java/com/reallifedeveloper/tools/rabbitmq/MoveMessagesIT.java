package com.reallifedeveloper.tools.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

// @Disabled("Since this requires RabbitMQ to be instaled on localhost, with the guest user and virtual host '/'")
public class MoveMessagesIT {

    private static final String EXCHANGE = "foo.domain";
    private static final String EXCHANGE_DLX = "foo.domain.dlx";
    private static final String QUEUE1 = "foo.domain.queue1";
    private static final String QUEUE2 = "foo.domain.queue2";
    private static final String QUEUE_DLX = "foo.domain.dlq";
    private static final String[] TEST_MESSAGES = { "foo", "bar", "baz" };
    private static final String ROUTING_KEY1 = "rk1";
    private static final String ROUTING_KEY2 = "rk2";
    private static final String[] ROUTING_KEYS = { ROUTING_KEY1, ROUTING_KEY2, ROUTING_KEY1 };

    @Test
    public void moveMessages() throws Exception {
        MoveMessages moveMessages = MoveMessages.createInstance("localhost", "guest", "guest", "/");
        try (Connection connection = connectionFactory().newConnection()) {
            testMoveMessages(moveMessages, connection);
        }
    }

    /* package-private */ static void testMoveMessages(MoveMessages moveMessages, Connection connection)
            throws IOException, TimeoutException {
        // Given
        setUpExchangesAndQueues(connection);
        verifyMessagesBeforeMove(connection);

        // When
        moveMessages.moveAllMessagesToExchange(QUEUE_DLX, EXCHANGE);

        // Then
        verifyMessagesAfterMove(connection);
    }

    private static void setUpExchangesAndQueues(Connection connection) throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        // Cleanup from previous test
        channel.queueDelete(QUEUE_DLX);
        channel.queueDelete(QUEUE2);
        channel.queueDelete(QUEUE1);
        channel.exchangeDelete(EXCHANGE_DLX);
        channel.exchangeDelete(EXCHANGE);
        // EXCHANGE/QUEUEs
        channel.exchangeDeclare(EXCHANGE, "topic");
        Map<String, Object> queueArgs = new HashMap<>();
        queueArgs.put("x-message-ttl", 1000 * 1000);
        queueArgs.put("x-dead-letter-exchange", EXCHANGE_DLX);
        channel.queueDeclare(QUEUE1, true, false, false, queueArgs);
        channel.queueBind(QUEUE1, EXCHANGE, ROUTING_KEY1);
        channel.queueDeclare(QUEUE2, true, false, false, queueArgs);
        channel.queueBind(QUEUE2, EXCHANGE, ROUTING_KEY2);
        // DLX/DLQ
        channel.exchangeDeclare(EXCHANGE_DLX, "topic");
        channel.queueDeclare(QUEUE_DLX, true, false, false, null);
        channel.queueBind(QUEUE_DLX, EXCHANGE_DLX, "#");
        // Send test messages to DLQ
        for (int i = 0; i < TEST_MESSAGES.length; i++) {
            channel.basicPublish(EXCHANGE_DLX, ROUTING_KEYS[i], null, TEST_MESSAGES[i].getBytes());
        }
        channel.close();
    }

    private static void verifyMessagesBeforeMove(Connection connection) throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        // We do not use the verifyMessages method since that reads and removes all messages from the queues.
        // Instead, we just check that the number of messages in each queue is correct.
        assertEquals(0, channel.messageCount(QUEUE1));
        assertEquals(0, channel.messageCount(QUEUE2));
        assertEquals(3, channel.messageCount(QUEUE_DLX));
        channel.close();
    }

    private static void verifyMessagesAfterMove(Connection connection) throws IOException, TimeoutException {
        verifyMessages(connection, QUEUE1, ROUTING_KEY1, "foo", "baz");
        verifyMessages(connection, QUEUE2, ROUTING_KEY2, "bar");
        verifyMessages(connection, QUEUE_DLX, null);
    }

    // This method will remove all messages from the queue.
    private static void verifyMessages(Connection connection, String queue, String routingKey, String... messages)
            throws IOException, TimeoutException {
        Channel channel = connection.createChannel();
        List<String> messagesRead = new ArrayList<>();
        while (true) {
            GetResponse response = channel.basicGet(queue, true);
            if (response == null) {
                break;
            }
            Envelope envelope = response.getEnvelope();
            if (routingKey != null) {
                assertEquals(routingKey, envelope.getRoutingKey());
            }
            messagesRead.add(new String(response.getBody()));
        }
        assertArrayEquals(messages, messagesRead.toArray());
        channel.close();
    }

    private static ConnectionFactory connectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        return factory;
    }
}
