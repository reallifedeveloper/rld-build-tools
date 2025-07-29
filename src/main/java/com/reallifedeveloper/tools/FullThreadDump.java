/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE
 * HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2 for more details (a copy is included in
 * the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.reallifedeveloper.tools;

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

import java.io.IOException;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This FullThreadDump class demonstrates the capability to get a full thread dump and also detect deadlock remotely.
 * <p>
 * Based on code by Sun Microsystems, Inc. and Oracle.
 *
 * @author Josh Bloch
 * @author Neal Gafter
 * @author RealLifeDeveloper
 */
@SuppressWarnings("PMD") // This builds on code from other sources
public final class FullThreadDump {

    private static final Logger LOG = LoggerFactory.getLogger(FullThreadDump.class);

    private MBeanServerConnection server;

    private JMXConnector jmxc;

    /**
     * Creates a new {@code FullThreadDump} object that is connected to a JMX server on the given host and port.
     *
     * @param hostname the name of the host running the JMX server
     * @param port     the port number the JMX server is listening on
     *
     * @throws IOException if connection to the JMX server fails
     */
    public FullThreadDump(String hostname, int port) throws IOException {
        LOG.info("Connecting to {}:{}", hostname.replaceAll("[\r\n]", ""), port);

        // Create an RMI connector client and connect it to
        // the RMI connector server
        String urlPath = "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi";
        try {
            JMXServiceURL url = new JMXServiceURL("rmi", "", 0, urlPath);
            connect(url);
        } catch (MalformedURLException cause) {
            // Should never happen
            InternalError ie = new InternalError(cause.getMessage());
            ie.initCause(cause);
            throw ie;
        }
    }

    /**
     * Creates a new {@code FullThreadDump} object that is connected to a JMX server at the given {@code JMXServiceURL}.
     *
     * @param url the {@code JMXServiceURL} of the JMX server
     *
     * @throws IOException if connection to the JMX server fails
     */
    public FullThreadDump(JMXServiceURL url) throws IOException {
        connect(url);
    }

    /**
     * Creates a thread dump with information about all the threads running in the Java process being monitored by the JMX server connected
     * to.
     * <p>
     * The information is meant to be read by humans and is not easily parsable.
     *
     * @return a list of strings with information about the threads, e.g., thread name, call stack an so on.
     *
     * @throws IOException if communication with the JMX server fails
     */
    public List<String> dump() throws IOException {
        ThreadMonitor monitor = new ThreadMonitor(server);
        List<String> threadInfo = monitor.threadDump();
        if (!monitor.findDeadlock()) {
            threadInfo.add("No deadlock found.");
        }
        return threadInfo;
    }

    /**
     * Connect to a JMX agent of a given URL.
     *
     * @throws IOException
     */
    @SuppressWarnings("BanJNDI")
    private void connect(JMXServiceURL url) throws IOException {
        Map<String, Object> env = new HashMap<>();
        // String[] credentials = { "controlRole", "control" };
        // env.put(JMXConnector.CREDENTIALS, credentials);
        // env.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
        this.jmxc = JMXConnectorFactory.connect(url, env);
        // this.jmxc = JMXConnectorFactory.connect(url);
        this.server = jmxc.getMBeanServerConnection();
    }

    /**
     * Connects to a JMX server at the given host and port number and creates a thread dump that is logged on INFO level.
     *
     * @param args should be one string on the form "hostname:port", e.g., "localhost:4711"
     *
     * @throws IOException if communication with the JMX server fails
     */
    public static void main(String... args) throws IOException {
        if (args == null || args.length != 1) {
            throw new IllegalArgumentException(usage());
        }

        String[] arg2 = args[0].split(":");
        if (arg2.length != 2) {
            throw new IllegalArgumentException(usage());
        }
        String hostname = arg2[0];
        int port = -1;
        try {
            port = Integer.parseInt(arg2[1]);
        } catch (NumberFormatException x) {
            throw new IllegalArgumentException(usage());
        }
        if (port < 0) {
            throw new IllegalArgumentException(usage());
        }

        // get full thread dump and perform deadlock detection
        FullThreadDump ftd = new FullThreadDump(hostname, port);
        List<String> threadInfo = ftd.dump();
        for (String threadInfoLine : threadInfo) {
            LOG.info(threadInfoLine.replaceAll("[\r\n]", ""));
        }
    }

    private static String usage() {
        return "Usage: java " + FullThreadDump.class.getName() + " <hostname>:<port>";
    }

    /**
     * Example of using the java.lang.management API to dump stack trace and to perform deadlock detection.
     */
    private static final class ThreadMonitor {

        private static final List<String> THREAD_INFO = new ArrayList<>();

        private static final String INDENT = "    ";

        private ThreadMXBean tmbean;

        /**
         * Constructs a ThreadMonitor object to get thread information in a remote JVM.
         */
        ThreadMonitor(MBeanServerConnection server) throws IOException {
            this.tmbean = newPlatformMXBeanProxy(server, THREAD_MXBEAN_NAME, ThreadMXBean.class);
        }

        /**
         * Gives the thread dump information as a list of strings, one line per string.
         */
        public List<String> threadDump() {
            if (tmbean.isObjectMonitorUsageSupported() && tmbean.isSynchronizerUsageSupported()) {
                // Print lock info if both object monitor usage
                // and synchronizer usage are supported.
                // This sample code can be modified to handle if
                // either monitor usage or synchronizer usage is supported.
                dumpThreadInfoWithLocks();
            }
            return THREAD_INFO;
        }

        /**
         * Saves the thread dump information with locks info in THREAD_INFO.
         */
        private void dumpThreadInfoWithLocks() {
            THREAD_INFO.add("Full Java thread dump with locks info");

            ThreadInfo[] tinfos = tmbean.dumpAllThreads(true, true);
            for (ThreadInfo ti : tinfos) {
                printThreadInfo(ti);
                LockInfo[] syncs = ti.getLockedSynchronizers();
                addLockInfo(syncs);
            }
        }

        private void printThreadInfo(ThreadInfo ti) {
            addThreadInfo(ti);

            StackTraceElement[] stacktrace = ti.getStackTrace();
            MonitorInfo[] monitors = ti.getLockedMonitors();
            for (int i = 0; i < stacktrace.length; i++) {
                StackTraceElement ste = stacktrace[i];
                THREAD_INFO.add(INDENT + "at " + ste.toString());
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == i) {
                        THREAD_INFO.add(INDENT + "  - locked " + mi);
                    }
                }
            }
        }

        private void addThreadInfo(ThreadInfo ti) {
            StringBuilder sb = new StringBuilder(
                    "\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState());
            if (ti.getLockName() != null) {
                sb.append(" on lock=" + ti.getLockName());
            }
            if (ti.isSuspended()) {
                sb.append(" (suspended)");
            }
            if (ti.isInNative()) {
                sb.append(" (running in native)");
            }
            THREAD_INFO.add(sb.toString());
            if (ti.getLockOwnerName() != null) {
                THREAD_INFO.add(INDENT + " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());
            }
        }

        private void addLockInfo(LockInfo[] locks) {
            THREAD_INFO.add(INDENT + "Locked synchronizers: count = " + locks.length);
            for (LockInfo li : locks) {
                THREAD_INFO.add(INDENT + "  - " + li);
            }
        }

        /**
         * Checks if any threads are deadlocked. If any, save the thread dump information.
         */
        public boolean findDeadlock() {
            long[] tids = tmbean.findDeadlockedThreads();
            if (tids == null) {
                return false;
            }

            THREAD_INFO.add("Deadlock found :-");
            ThreadInfo[] infos = tmbean.getThreadInfo(tids, true, true);
            for (ThreadInfo ti : infos) {
                printThreadInfo(ti);
                addLockInfo(ti.getLockedSynchronizers());
            }

            return true;
        }
    }
}
