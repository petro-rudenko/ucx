/*
 * Copyright (C) Mellanox Technologies Ltd. 2001-2019.  ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */
package org.openucx.jucx;

import org.junit.Test;
import org.openucx.jucx.ucp.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class UcpListenerTest {
    static final int port = Integer.parseInt(
        System.getenv().getOrDefault("JUCX_TEST_PORT", "55321"));

    @Test
    public void testCreateUcpListener() {
        UcpContext context = new UcpContext(new UcpParams().requestStreamFeature());
        UcpWorker worker = context.newWorker(new UcpWorkerParams());
        InetSocketAddress ipv4 = new InetSocketAddress("0.0.0.0", port);
        try {
            UcpListener ipv4Listener = worker.newListener(
                new UcpListenerParams().setSockAddr(ipv4));

            assertNotNull(ipv4Listener);
            ipv4Listener.close();
        } catch (UcxException ex) {

        }

        try {
            InetSocketAddress ipv6 = new InetSocketAddress("::", port);
            UcpListener ipv6Listener = worker.newListener(
                new UcpListenerParams().setSockAddr(ipv6));

            assertNotNull(ipv6Listener);
            ipv6Listener.close();
        } catch (UcxException ex) {

        }

        worker.close();
        context.close();
    }

    static Stream<NetworkInterface> getInterfaces() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .filter(iface -> {
                    try {
                        return iface.isUp() && !iface.isLoopback();
                    } catch (SocketException e) {
                        return false;
                    }
                });
        } catch (SocketException e) {
            return Stream.empty();
        }
    }

    /**
     * Iterates over network interfaces and tries to bind and create listener
     * on a specific socket address.
     */
    static UcpListener tryBindListener(UcpWorker worker, UcpListenerParams params) {
        UcpListener result = null;
        List<InetAddress> addresses = getInterfaces().flatMap(iface ->
            Collections.list(iface.getInetAddresses()).stream())
            .collect(Collectors.toList());
        for (InetAddress address : addresses) {
            try {
                result = worker.newListener(
                    params.setSockAddr(new InetSocketAddress(address, port)));
                break;
            } catch (UcxException ex) {

            }
        }
        assertNotNull("Could not find socket address to start UcpListener", result);
        return result;
    }

    @Test
    public void testConnectionHandler() {
        UcpContext context1 = new UcpContext(new UcpParams().requestRmaFeature()
            .requestTagFeature());
        UcpContext context2 = new UcpContext(new UcpParams().requestRmaFeature()
            .requestTagFeature());
        UcpWorker worker1 = context1.newWorker(new UcpWorkerParams());
        UcpWorker worker2 = context2.newWorker(new UcpWorkerParams());

        AtomicLong connRequest = new AtomicLong(0);

        // Create listener and set connection handler
        UcpListenerParams listenerParams = new UcpListenerParams()
            .setConnectionHandler(connectionRequest -> connRequest.set(connectionRequest));
        UcpListener listener = tryBindListener(worker1, listenerParams);

        UcpEndpoint clientToServer = worker2.newEndpoint(new UcpEndpointParams()
            .setSocketAddress(listener.getAddress()));

        while (connRequest.get() == 0) {
            worker1.progress();
            worker2.progress();
        }

        UcpEndpoint serverToClient = worker1.newEndpoint(
            new UcpEndpointParams().setConnectionRequest(connRequest.get()));
        
        // Temporary workaround until new connection establishment protocol in UCX.
        for (int i = 0; i < 10; i++) {
            worker1.progress();
            worker2.progress();
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }

        UcpRequest sent = serverToClient.sendTaggedNonBlocking(
            ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE), null);
        UcpRequest recv = worker2.recvTaggedNonBlocking(
            ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE), null);

        while (!sent.isCompleted() || !recv.isCompleted()) {
            worker1.progress();
            worker2.progress();
        }

        clientToServer.close();
        serverToClient.close();
        listener.close();
        worker1.close();
        worker2.close();
        context1.close();
        context2.close();
    }
}
