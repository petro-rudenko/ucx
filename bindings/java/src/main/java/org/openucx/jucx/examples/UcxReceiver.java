package org.openucx.jucx.examples;

import org.openucx.jucx.UcxCallback;
import org.openucx.jucx.ucp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UcxReceiver extends UcxBenchmark {
    public static void main(String[] args) {
        if (!initializeArguments(args)) {
            return;
        }

        UcpContext context = new UcpContext(new UcpParams().requestTagFeature()
            .requestWakeupFeature().requestStreamFeature().setMtWorkersShared(true));
        UcpWorker ucpWorker = context.newWorker(new UcpWorkerParams().requestThreadSafety());

        UcpListener localhost = ucpWorker.newListener(new UcpListenerParams().setSockAddr(
            new InetSocketAddress(argsMap.get("s"), 5001)));

        final AtomicBoolean doProgress = new AtomicBoolean(true);

        Thread t = new Thread(() -> {
            while (doProgress.get()) {
                ucpWorker.progress();
            }
        });
        t.setName("progress thread");
        t.start();


        Queue<ByteBuffer> buff = new LinkedBlockingQueue<>(2);

        for (int i = 0; i < 2; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
            buff.add(buffer);
        }


        Queue<UcpRequest> requests = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            ByteBuffer recvBuffer = buff.peek();

            if (recvBuffer == null) {
                ucpWorker.progress();
                i--;
                continue;
            }

            buff.poll();

            UcpRequest ucxRequest = ucpWorker.recvTaggedNonBlocking(recvBuffer, 0,
                0xffff, new UcxCallback() {

                    @Override
                    public void onSuccess(UcpRequest request) {
                        System.out.println(recvBuffer.asIntBuffer().get(1));
                        if (request.getRecvSize() != recvBuffer.asIntBuffer().get(0)) {
                            System.out.println("Invalid data size : Expected : " +
                                recvBuffer.asIntBuffer().get(0)
                                + ", Found : " + request.getRecvSize());
                        }
                        buff.add(recvBuffer);
                    }

                    @Override
                    public void onError(int ucsStatus, String errorMsg) {
                        System.out.println("Failed to recv : " + ucsStatus);
                        System.out.println(errorMsg);
                    }
                });

            requests.add(ucxRequest);
        }

        doProgress.set(false);

        while (!requests.isEmpty()) {
            UcpRequest peek = requests.peek();
            if (peek.isCompleted()) {
                requests.poll();
            }
            ucpWorker.progress();
        }


        localhost.close();

    }
}
