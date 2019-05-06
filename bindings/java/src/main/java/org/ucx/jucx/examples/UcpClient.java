/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx.examples;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.ucp.*;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class UcpClient implements Callable<Void> {
    @CommandLine.Option(names = {"-f", "--file"}, description = "File to transfer to receiver")
    String inputFile;

    @CommandLine.Option(names = {"-t", "--total-size"}, description = "Total size to transfer")
    int totalSize;

    @CommandLine.Option(names = {"-s", "--server-host"}, description = "Server host to connect",
        required = true)
    String serverAddress;

    @CommandLine.Option(names = {"-p", "--server-port"}, description = "Server port to connect",
        required = true)
    int serverPort;

    public static void main(String[] args) {
        CommandLine.call(new UcpClient(), args);
    }

    @Override
    public Void call() throws IOException {
        UcpParams params = new UcpParams().requestRmaFeature()
            .requestTagFeature().setEstimatedNumEps(1);
        UcpContext context = new UcpContext(params);

        ByteBuffer buf;
        if (inputFile != null) {
            RandomAccessFile file = new RandomAccessFile(inputFile, "rw");
            buf = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, totalSize);
        } else {
            buf = ByteBuffer.allocateDirect(totalSize);
            buf.asCharBuffer().put(new String(new char[totalSize]).replace("\0", "A"));
        }

        UcpMemory mem = context.registerMemory(buf);
        ByteBuffer rkeyBuf = mem.getRemoteKeyBuffer();

        UcpWorkerParams workerParams = new UcpWorkerParams();
        UcpWorker worker = new UcpWorker(context, workerParams);

        UcpEndpointParams endpointParams = new UcpEndpointParams()
            .setPeerErrorHadnlingMode().setSocketAddress(
                new InetSocketAddress(serverAddress, serverPort));
        UcpEndpoint endpoint = new UcpEndpoint(worker, endpointParams);

        endpoint.sendNonBlocking(worker.getAddress(), 0, null);

        // RkeyBuffer + address (8 bytes) + size (4 bytes)
        ByteBuffer sendBuffer = ByteBuffer.allocateDirect(rkeyBuf.capacity() + 8 + 4);
        sendBuffer.putLong(mem.getAddress());
        sendBuffer.putInt(totalSize);
        sendBuffer.put(rkeyBuf);
        endpoint.sendNonBlocking(sendBuffer, 1, null);

        ByteBuffer recvBuffer = ByteBuffer.allocateDirect(4096);
        AtomicBoolean success = new AtomicBoolean(false);
        worker.recvNonBlocking(recvBuffer, 3, new UcxCallback() {
            @Override
            public void onSuccess() {
                success.set(true);
                System.out.println("Received message:" +
                    recvBuffer.asCharBuffer().toString().trim());
            }
        });

        while (!success.get()) {
            worker.progress();
        }

        return null;
    }
}
