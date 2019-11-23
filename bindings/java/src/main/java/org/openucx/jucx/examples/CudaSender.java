package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.UcxRequest;
import org.openucx.jucx.UcxUtils;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class CudaSender  extends UcxBenchmark {

    public static void main(String[] args) throws Exception {
        if (!initializeArguments(args)) {
            return;
        }

        createContextAndWorker();

        String serverHost = argsMap.get("s");
        UcpEndpoint endpoint = worker.newEndpoint(new UcpEndpointParams()
            .setSocketAddress(new InetSocketAddress(serverHost, serverPort)));

        UcxRequest flushed = endpoint.flushNonBlocking(null);

        while (!flushed.isCompleted()) {
            worker.progress();
        }

        long cudaAddress = UcpContext.cudaMalloc(totalSize);

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
        Thread.sleep(1000);
        endpoint.close();

        Thread.sleep(100);
        worker.close();
        context.close();

    }
}
