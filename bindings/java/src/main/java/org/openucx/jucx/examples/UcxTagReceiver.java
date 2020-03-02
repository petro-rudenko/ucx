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
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class UcxTagReceiver extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        context = new UcpContext(new UcpParams().requestTagFeature());
        worker = context.newWorker(new UcpWorkerParams());

        String serverHost = argsMap.get("s");
        InetSocketAddress sockaddr = new InetSocketAddress(serverHost, serverPort);
        UcpListener listener = worker.newListener(new UcpListenerParams().setSockAddr(sockaddr));
        resources.push(listener);
        System.out.println("Waiting for connections on " + sockaddr + " ...");

        for (int i = 0; i < numIterations; i++) {
            final int j = i;
            ByteBuffer recvBuffer = ByteBuffer.allocateDirect(4096);
            worker.progressRequest(worker.recvTaggedNonBlocking(recvBuffer,
                new UcxCallback() {
                    @Override
                    public void onSuccess(UcpRequest request) {
                        System.out.println("Message " + j + " received");
                    }
                }));
        }

        // Close request won't be return to pull automatically, since there's no callback.
        Collections.addAll(resources, listener, worker, listener);

        closeResources();
    }
}
