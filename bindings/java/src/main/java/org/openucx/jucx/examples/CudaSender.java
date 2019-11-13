package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.UcxRequest;
import org.openucx.jucx.ucp.UcpContext;
import org.openucx.jucx.ucp.UcpEndpoint;
import org.openucx.jucx.ucp.UcpEndpointParams;
import org.openucx.jucx.ucp.UcpRegisterMemoryParams;

import java.net.InetSocketAddress;

public class CudaSender extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setSocketAddress(new InetSocketAddress(serverHost, serverPort)));

        long cudaAddress = UcpContext.cudaMalloc(totalSize);
        //context.registerMemory(new UcpRegisterMemoryParams().setAddress(cudaAddress)
        //    .setLength(totalSize));
        UcxRequest sent = endpoint.sendTaggedNonBlocking(cudaAddress, totalSize, 0,
            new UcxCallback() {
                @Override
                public void onSuccess(UcxRequest request) {
                    System.out.println("Cuda buffer sent");
                }
            });

        while (!sent.isCompleted()) {
            worker.progress();
        }

        System.out.println("DONE");
        Thread.sleep(1000000);
        endpoint.close();

        Thread.sleep(100);
        worker.close();
        context.close();
    }
}
