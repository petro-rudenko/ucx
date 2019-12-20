package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class UcxSender extends UcxBenchmark {
    public static void main(String[] args) {
        if (!initializeArguments(args)) {
            return;
        }

        UcpContext context = new UcpContext(new UcpParams().requestTagFeature()
            .requestWakeupFeature());
        UcpWorker ucpWorker = context.newWorker(new UcpWorkerParams()
            .requestThreadSafety().requestWakeupRX());


        UcpEndpoint localhost = ucpWorker.newEndpoint(new UcpEndpointParams().setSocketAddress(
            new InetSocketAddress(argsMap.get("s"), 5001)
        ));

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ucpWorker.progress();
                }
            }
        });
        t.setDaemon(true);
        t.start();


        Queue<UcpRequest> requests = new LinkedList<>();

        BlockingQueue<ByteBuffer> buff = new ArrayBlockingQueue<>(10);

        for (int i = 0; i < 2; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            buff.add(buffer);
        }

        for (int i = 0; i < 100; i++) {
            ByteBuffer buffer = buff.poll();
            if (buffer == null) {
                i--;
                continue;
            }
            buffer.putInt(Integer.BYTES * 2);
            buffer.putInt(i);
            System.out.println("Posting : " + buffer);
            buffer.limit(buffer.position());
            buffer.position(0);

            System.out.println("Posting After : " + buffer);
            UcpRequest sendMessage = localhost.sendTaggedNonBlocking(buffer, 0,
                new UcxCallback() {
                    @Override
                    public void onSuccess(UcpRequest request) {
                        System.out.println("Success ");

                        buff.add(buffer);
                    }

                    @Override
                    public void onError(int ucsStatus, String errorMsg) {
                        System.out.println(errorMsg);
                    }
                });
            requests.add(sendMessage);
        }

        while (!requests.isEmpty()) {
            if (requests.peek().isCompleted()) {
                requests.poll();
            }
            ucpWorker.progress();
            localhost.flushNonBlocking(null);
        }


    }
}
