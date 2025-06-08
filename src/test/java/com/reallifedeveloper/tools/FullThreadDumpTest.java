package com.reallifedeveloper.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reallifedeveloper.tools.test.TestUtil;

public class FullThreadDumpTest {

    private JMXConnectorServer connector;

    @BeforeEach
    public void init() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        connector = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        connector.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        connector.stop();
    }

    @Test
    public void dump() throws Exception {
        FullThreadDump threadDump = new FullThreadDump(connector.getAddress());
        List<String> threadInfo = threadDump.dump();

        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        int numThreads = 0;
        for (String threadInfoLine : threadInfo) {
            if (threadInfoLine.startsWith("\"")) {
                // Threads start with thread name in double quotes.
                numThreads++;
            }
        }
        assertEquals(threads.size(), numThreads, "Wrong number of threads in dump: ");
    }

    @Test
    public void dumpWithDeadlock() throws Exception {
        Deadlock deadlock = new Deadlock();
        deadlock.t1.start();
        deadlock.t2.start();
        int i;
        do {
            i = deadlock.i;
            Thread.sleep(1);
        } while (i != deadlock.i);
        FullThreadDump threadDump = new FullThreadDump(connector.getAddress());
        List<String> threadInfos = threadDump.dump();
        boolean deadlockFound = false;
        boolean thread1Found = false;
        boolean thread2Found = false;
        for (String threadInfo : threadInfos) {
            if (threadInfo.startsWith("Deadlock found")) {
                deadlockFound = true;
            }
            if (deadlockFound && threadInfo.startsWith("\"Thread 1\"")) {
                thread1Found = true;
            }
            if (deadlockFound && threadInfo.startsWith("\"Thread 2\"")) {
                thread2Found = true;
            }
        }
        assertTrue(deadlockFound, "Deadlock not found");
        assertTrue(thread1Found, "Thread 1 not found among deadlocked threads");
        assertTrue(thread2Found, "Thread 2 not found among deadlocked threads");
    }

    @Test
    public void main() throws Exception {
        // Check that we don't get an exception.
        // Note: This requires the JVM to have been started with flags to enable remote JMX access (see pom.xml).
        // -Dcom.sun.management.jmxremote.port=4711
        FullThreadDump.main("localhost:4711");
    }

    @Test
    public void mainIncorrectPort() throws Exception {
        assertThrows(IOException.class, () -> FullThreadDump.main("localhost:" + TestUtil.findFreePort()));
    }

    @Test
    public void mainNotAHostnamePort() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FullThreadDump.main("foo"));
    }

    @Test
    public void mainNotAPortNumber() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FullThreadDump.main("foo:bar"));
    }

    @Test
    public void mainNegativePort() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FullThreadDump.main("foo:-4711"));
    }

    @Test
    public void mainNoArguments() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> FullThreadDump.main((String[]) null));
    }

    private static class Deadlock {
        private static final Logger LOG = LoggerFactory.getLogger(Deadlock.class);
        private String s1 = "foo";
        private String s2 = "bar";
        private int i;

        private Thread t1 = new Thread("Thread 1") {
            @Override
            public void run() {
                while (true) {
                    synchronized (s1) {
                        Deadlock.sleep(1);
                        synchronized (s2) {
                            LOG.trace("{}: {}", getName(), s1 + s2);
                            i++;
                        }
                    }
                }
            }
        };

        private Thread t2 = new Thread("Thread 2") {
            @Override
            public void run() {
                while (true) {
                    synchronized (s2) {
                        Deadlock.sleep(1);
                        synchronized (s1) {
                            LOG.trace("{}: {}", getName(), s1 + s2);
                            i++;
                        }
                    }
                }
            }
        };

        private static void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while sleeping", e);
            }
        }
    }
}
