package com.reallifedeveloper.tools.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import com.reallifedeveloper.tools.test.TestUtil;

public class MoveMessagesTest {

    @Test
    public void moveMessages() throws Exception {
        ConnectionFactory connectionFactory = new MockConnectionFactory();
        MoveMessages moveMessages = new MoveMessages(connectionFactory);
        try (Connection connection = connectionFactory.newConnection()) {
            MoveMessagesIT.testMoveMessages(moveMessages, connection);
        }
    }

    @Test
    public void createInstanceSetsConnectionFactory() {
        String host = "myHost";
        String username = "myUsername";
        String password = "myPassword";
        String vhost = "myVhost";
        MoveMessages moveMessages = MoveMessages.createInstance(host, username, password, vhost);
        Object connectionFactoryObj = TestUtil.getFieldValue(moveMessages, "connectionFactory");
        assertNotNull(connectionFactoryObj);
        assertEquals(ConnectionFactory.class, connectionFactoryObj.getClass());
        ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryObj;
        assertEquals(host, connectionFactory.getHost());
        assertEquals(username, connectionFactory.getUsername());
        assertEquals(password, connectionFactory.getPassword());
        assertEquals(vhost, connectionFactory.getVirtualHost());
    }

    @Test
    @SuppressWarnings("NullAway")
    public void constructorWithNullConnectionFacctoryThrowsException() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new MoveMessages(null));
        assertEquals("connectionFactory must not be null", e.getMessage());
    }
}
