/*
 * Copyright (C) Mellanox Technologies Ltd. 2001-2019.  ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx;

import org.junit.Test;
import org.ucx.jucx.ucp.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UcpEndpointTest {
    @Test
    public void testConnectToListenerByWorkerAddr() {
        UcpContext context = new UcpContext(new UcpParams().requestStreamFeature());
        UcpWorker worker = context.newWorker(new UcpWorkerParams());
        UcpEndpointParams epParams = new UcpEndpointParams().setUcpAddress(worker.getAddress())
            .setPeerErrorHadnlingMode().setNoLoopbackMode();
        UcpEndpoint endpoint = worker.newEndpoint(epParams);
        assertNotNull(endpoint.getNativeId());

        endpoint.close();
        worker.close();
        context.close();
    }

    @Test
    public void testConnectToListenerBySocketAddr() throws SocketException {
        UcpContext context = new UcpContext(new UcpParams().requestStreamFeature());
        UcpWorker worker = context.newWorker(new UcpWorkerParams());
        // Iterate over each network interface - got it's sockaddr - try to instantiate listener
        // And pass this sockaddr to endpoint.
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        boolean success = false;
        while (!success && interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress inetAddress = inetAddresses.nextElement();
                if (!inetAddress.isLoopbackAddress()) {
                    try {
                        InetSocketAddress addr = new InetSocketAddress(inetAddress,
                            UcpListenerTest.port);
                        UcpListener ucpListener = worker.newListener(
                            new UcpListenerParams().setSockAddr(addr));
                        UcpEndpointParams epParams =
                            new UcpEndpointParams().setSocketAddress(addr);
                        UcpEndpoint endpoint = worker.newEndpoint(epParams);
                        assertNotNull(endpoint.getNativeId());
                        success = true;
                        endpoint.close();
                        ucpListener.close();
                        break;
                    } catch (UcxException ex) {

                    }
                }
            }
        }
        assertTrue(success);

        worker.close();
        context.close();
    }

    @Test
    public void testGetNB() {
        // Crerate 2 contexts + 2 workers
        UcpParams params = new UcpParams().requestRmaFeature();
        UcpWorkerParams rdmaWorkerParams = new UcpWorkerParams().requestWakeupRMA();
        UcpContext context1 = new UcpContext(params);
        UcpContext context2 = new UcpContext(params);
        UcpWorker worker1 = context1.newWorker(rdmaWorkerParams);
        UcpWorker worker2 = context2.newWorker(rdmaWorkerParams);

        // Create endpoint worker1 -> worker2
        UcpEndpointParams epParams = new UcpEndpointParams().setPeerErrorHadnlingMode()
            .setUcpAddress(worker2.getAddress());
        UcpEndpoint endpoint = worker1.newEndpoint(epParams);

        // Allocate 2 source and 2 destination buffers, to perform 2 RDMA Read operations
        ByteBuffer src1 = ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE);
        ByteBuffer src2 = ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE);
        ByteBuffer dst1 = ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE);
        ByteBuffer dst2 = ByteBuffer.allocateDirect(UcpMemoryTest.MEM_SIZE);
        src1.asCharBuffer().put(UcpMemoryTest.RANDOM_TEXT);
        src2.asCharBuffer().put(UcpMemoryTest.RANDOM_TEXT + UcpMemoryTest.RANDOM_TEXT);

        // Register source buffers on context2
        UcpMemory memory1 = context2.registerMemory(src1);
        UcpMemory memory2 = context2.registerMemory(src2);

        UcpRemoteKey rkey1 = endpoint.unpackRemoteKey(memory1.getRemoteKeyBuffer());
        UcpRemoteKey rkey2 = endpoint.unpackRemoteKey(memory2.getRemoteKeyBuffer());

        AtomicInteger numCompletedRequests = new AtomicInteger(0);
        UcxCallback callback = new UcxCallback() {
            @Override
            public void onSuccess() {
                numCompletedRequests.incrementAndGet();
            }
        };

        // Submit 2 get requests
        UcxRequest request1 = endpoint.getNonBlocking(memory1.getAddress(), rkey1, dst1, callback);
        UcxRequest request2 = endpoint.getNonBlocking(memory2.getAddress(), rkey2, dst2, callback);

        // Wait for 2 get operations to complete
        while (numCompletedRequests.get() != 2) {
            worker1.progress();
        }

        assertTrue(request1.isCompleted() && request2.isCompleted());
        assertEquals(UcpMemoryTest.RANDOM_TEXT, dst1.asCharBuffer().toString().trim());
        assertEquals(UcpMemoryTest.RANDOM_TEXT + UcpMemoryTest.RANDOM_TEXT,
            dst2.asCharBuffer().toString().trim());


        memory1.deregister();
        memory2.deregister();
        rkey1.close();
        rkey2.close();
        endpoint.close();
        worker1.close();
        worker2.close();
        context1.close();
        context2.close();
    }
}
