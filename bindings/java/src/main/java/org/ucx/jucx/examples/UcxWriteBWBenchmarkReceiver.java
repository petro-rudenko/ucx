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

public class UcxWriteBWBenchmarkReceiver extends UcxBenchmark {

    public static void main(String[] args) throws IOException {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");

        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setSocketAddress(new InetSocketAddress(serverHost, serverPort)));
        resources.push(endpoint);

        ByteBuffer msg = ByteBuffer.allocateDirect(4096);
        ByteBuffer data = ByteBuffer.allocateDirect(totalSize);
        UcpMemory mem = context.registerMemory(data);
        ByteBuffer rkey = mem.getRemoteKeyBuffer();

        ByteBuffer workerAddress = worker.getAddress();
        msg.putInt(workerAddress.capacity());
        msg.put(workerAddress);
        msg.putLong(mem.getAddress());
        msg.putInt(rkey.capacity());
        msg.put(rkey);

        UcxRequest sent = endpoint.sendTaggedNonBlocking(msg, null);

        while (!sent.isCompleted()) {
            worker.progress();
        }

        ByteBuffer recvBuf = ByteBuffer.allocateDirect(4096);
        UcxRequest recv = worker.recvTaggedNonBlocking(recvBuf, new UcxCallback(){
            @Override
            public void onSuccess(UcxRequest request) {
                System.out.println("Received a message" + recvBuf.asCharBuffer().toString().trim());
                System.out.println("Memory content:" + data.asCharBuffer().toString().trim());
                System.out.println("Memory content2:" + mem.getData()
                    .asCharBuffer().toString().trim());
            }
        });

        while (!recv.isCompleted()) {
            worker.progress();
        }

        mem.deregister();
        closeResources();
    }
}
