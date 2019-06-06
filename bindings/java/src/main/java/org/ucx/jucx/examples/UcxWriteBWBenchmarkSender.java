/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx.examples;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.UcxRequest;
import org.ucx.jucx.ucp.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class UcxWriteBWBenchmarkSender extends UcxBenchmark {

    public static void main(String[] args) throws IOException {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        InetSocketAddress sockaddr = new InetSocketAddress(serverHost, serverPort);
        UcpListener listener = worker.newListener(
            new UcpListenerParams().setSockAddr(sockaddr));
        resources.push(listener);

        // In java ByteBuffer can be allocated up to 2GB (int max size).
        if (totalSize >= Integer.MAX_VALUE) {
            throw new IOException("Max size must be no greater then " + Integer.MAX_VALUE);
        }
        ByteBuffer data = ByteBuffer.allocateDirect(totalSize);
        data.asCharBuffer().put("Test 1234567");
        data.clear();

        ByteBuffer recvBuffer = ByteBuffer.allocateDirect(7096);
        UcxRequest recvRequest = worker.recvTaggedNonBlocking(recvBuffer,
            new UcxCallback() {
                @Override
                public void onSuccess(UcxRequest request) {
                    System.out.println("Received a message:");
                }
            });

        while (!recvRequest.isCompleted()) {
            worker.progress();
        }

        int workerAddressSize = recvBuffer.getInt();
        ByteBuffer workerAddress = ByteBuffer.allocateDirect(workerAddressSize);

        for (int i = 0; i < workerAddressSize; i++) {
            workerAddress.put(recvBuffer.get());
        }

        long remoteAddress = recvBuffer.getLong();
        int rkeySize = recvBuffer.getInt();
        ByteBuffer rkeyBuffer = ByteBuffer.allocateDirect(rkeySize);

        for (int i = 0; i < rkeySize; i++) {
            rkeyBuffer.put(recvBuffer.get());
        }

        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setUcpAddress(workerAddress));

        UcpRemoteKey rkey = endpoint.unpackRemoteKey(rkeyBuffer);

        System.out.println("Will do RDMA write to " + remoteAddress);
        UcxRequest req = endpoint.putNonBlocking(data, remoteAddress, rkey, new UcxCallback() {
            @Override
            public void onSuccess(UcxRequest request) {
                System.out.println("Done RDMA writing");
            }
        });

        while (!req.isCompleted()) {
            worker.progress();
        }

        ByteBuffer doneMsg = ByteBuffer.allocateDirect(4096);
        doneMsg.asCharBuffer().put("DONE");
        req = endpoint.sendTaggedNonBlocking(doneMsg, null);

        while (!req.isCompleted()) {
            worker.progress();
        }

        closeResources();
    }
}
