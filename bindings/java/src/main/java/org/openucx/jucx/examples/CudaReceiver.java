/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.UcxUtils;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class CudaReceiver extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        InetSocketAddress sockaddr = new InetSocketAddress(serverHost, serverPort);
        AtomicLong connRequest = new AtomicLong(0);
        UcpListener listener = worker.newListener(
            new UcpListenerParams()
                .setConnectionHandler(ucpConnectionRequest -> connRequest.set(ucpConnectionRequest))
                .setSockAddr(sockaddr));
        resources.push(listener);
        System.out.println("Waiting for connections on " + sockaddr + " ...");

        while (connRequest.get() == 0) {
            worker.progress();
        }

        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setConnectionRequest(connRequest.get())
            .setPeerErrorHadnlingMode());

        // Temporary workaround until new connection establishment protocol in UCX.
        for (int i = 0; i < 10; i++) {
            worker.progress();
            try {
                Thread.sleep(10);
            } catch (Exception e) {
            }
        }

        long cudaBuffer1 = context.cudaMalloc(totalSize);
        UcpMemMapParams mmapParams = new UcpMemMapParams().setAddress(cudaBuffer1)
            .setLength(totalSize);
        UcpMemory memory = context.memoryMap(mmapParams);
        resources.add(memory);
        UcpRequest recvCudaBuffer = worker.recvTaggedNonBlocking(cudaBuffer1,
            totalSize, 0, 0, new UcxCallback() {
                @Override
                public void onSuccess(UcpRequest request) {
                    System.out.println("Recv cuda buffer of size " + totalSize);
                }
            });
        worker.progressRequest(recvCudaBuffer);

        UcpRequest closeRequest = endpoint.closeNonBlockingFlush();
        worker.progressRequest(closeRequest);
        // Close request won't be return to pull automatically, since there's no callback.
        resources.push(closeRequest);

        closeResources();
    }
}
