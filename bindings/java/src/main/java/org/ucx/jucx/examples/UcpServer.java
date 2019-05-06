/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx.examples;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.ucp.*;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


@CommandLine.Command(name = "UcpServer", mixinStandardHelpOptions = true)
public class UcpServer implements Runnable {
    @Option(names = {"-h", "--host"}, description = "IP address on which start server",
        required = true)
    String ipAddress;

    @Option(names = {"-p", "--port"}, description = "Port on which start server", required = true)
    int port;


    public static void main(String[] args) {
        CommandLine.run(new UcpServer(), args);
    }

    @Override
    public void run() {
        UcpParams params = new UcpParams().requestRmaFeature().requestTagFeature()
            .setEstimatedNumEps(1);
        UcpContext context = new UcpContext(params);

        UcpWorkerParams workerParams = new UcpWorkerParams().requestWakeupRMA();
        UcpWorker worker = new UcpWorker(context, workerParams);

        InetSocketAddress serverAddress = new InetSocketAddress(ipAddress, port);
        UcpListenerParams listenerParams = new UcpListenerParams().setSockAddr(serverAddress);
        UcpListener listener = new UcpListener(worker, listenerParams);

        System.out.println("Starting UcpListener on address: " + serverAddress);

        ByteBuffer clientWorkerAddress = ByteBuffer.allocateDirect(4096);
        ByteBuffer clientMemory = ByteBuffer.allocateDirect(4096);
        ByteBuffer success = ByteBuffer.allocateDirect(4096);

        AtomicReference<UcpEndpoint> endpointAtomicReference = new AtomicReference<>(null);

        worker.recvNonBlocking(clientWorkerAddress, 0, new UcxCallback() {
            @Override
            public void onSuccess() {
                UcpEndpointParams epParams = new UcpEndpointParams().setPeerErrorHadnlingMode()
                    .setUcpAddress(clientWorkerAddress);
                System.out.println("Received client worker address. Making connection to it.");
                endpointAtomicReference.set(new UcpEndpoint(worker, epParams));
            }
        });

        // Make progress on client while we don't get client worker address.
        while (endpointAtomicReference.get() == null) {
            worker.progress();
        }

        AtomicReference<UcpRemoteKey> rkey = new AtomicReference<>(null);

        worker.recvNonBlocking(clientMemory, 1, new UcxCallback() {
            @Override
            public void onSuccess() {
                long address = clientMemory.getLong();
                int size = clientMemory.getInt();
                ByteBuffer clientRkey = clientMemory.slice();
                System.out.printf("Received client memory. Will read %d bytes from address %d \n",
                    size, address);
                rkey.set(endpointAtomicReference.get().unpackRemoteKey(clientRkey));
            }
        });

        while (rkey.get() == null) {
            worker.progress();
        }

        clientMemory.clear();
        long address = clientMemory.getLong();
        int size = clientMemory.getInt();

        ByteBuffer result = ByteBuffer.allocateDirect(size);
        endpointAtomicReference.get()
            .getNonBlocking(address, rkey.get(), result, new UcxCallback() {
            @Override
            public void onSuccess() {
                System.out.println("Read remote content: \n" + result.asCharBuffer().toString());
                rkey.get().close();
            }
        });


        AtomicBoolean sent = new AtomicBoolean(false);
        success.asCharBuffer().put("Succesfully get " + size + "bytes");
        endpointAtomicReference.get().sendNonBlocking(success, 3, new UcxCallback() {
            @Override
            public void onSuccess() {
                sent.set(true);
            }
        });

        while (!sent.get()) {
            worker.progress();
        }

        endpointAtomicReference.get().close();
        listener.close();
        worker.close();
        context.close();
    }
}
