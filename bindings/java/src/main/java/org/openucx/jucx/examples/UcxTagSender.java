/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class UcxTagSender extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        context = new UcpContext(new UcpParams().requestTagFeature());
        worker = context.newWorker(new UcpWorkerParams());

        String serverHost = argsMap.get("s");
        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setSocketAddress(new InetSocketAddress(serverHost, serverPort)));

        for (int i = 0; i < numIterations; i++) {
            final int j = i;
            worker.progressRequest(
                endpoint.sendTaggedNonBlocking(ByteBuffer.allocateDirect(4096),
                    new UcxCallback() {
                        @Override
                        public void onSuccess(UcpRequest request) {
                            System.out.println("Message " + j + " sent");
                        }
                    }));
        }

        UcpRequest closeRequest = endpoint.closeNonBlockingFlush();
          worker.progressRequest(closeRequest);
        resources.push(closeRequest);

        closeResources();
    }
}
