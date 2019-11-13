package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.UcxRequest;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;

public class CudaReceiver extends UcxBenchmark {
    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        InetSocketAddress sockaddr = new InetSocketAddress(serverHost, serverPort);
        UcpListener listener = worker.newListener(
            new UcpListenerParams().setSockAddr(sockaddr));
        resources.push(listener);

        long cudaAddress = UcpContext.cudaMalloc(totalSize);
        /*
        UcpMemory memory = context.registerMemory(new UcpRegisterMemoryParams()
            .setAddress(cudaAddress)
            .setLength(totalSize));
        resources.push(memory);
         */
        UcxRequest recv = worker.recvTaggedNonBlocking(cudaAddress, totalSize, 0, 0,
            new UcxCallback() {
                @Override
                public void onSuccess(UcxRequest request) {
                    System.out.println("Received cuda message");
                }
            });

        while (!recv.isCompleted()) {
            worker.progress();
        }

        System.out.println("DONE");
        Thread.sleep(1000000);
        closeResources();
    }
}
