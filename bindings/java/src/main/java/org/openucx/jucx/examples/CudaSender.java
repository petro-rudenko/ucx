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


public class CudaSender extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setPeerErrorHadnlingMode()
            .setSocketAddress(new InetSocketAddress(serverHost, serverPort)));

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
        UcpRequest sendCudaBuffer = endpoint.sendTaggedNonBlocking(cudaBuffer1, totalSize, 0,
            new UcxCallback() {
                @Override
                public void onSuccess(UcpRequest request) {
                    System.out.println("Sent cuda buffer of size " + totalSize);
                }
            }
        );
        worker.progressRequest(sendCudaBuffer);

        UcpRequest closeRequest = endpoint.closeNonBlockingFlush();
        worker.progressRequest(closeRequest);
        resources.push(closeRequest);

        closeResources();
    }
}
