/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx.ucp;

import java.io.Closeable;
import java.nio.ByteBuffer;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.UcxException;
import org.ucx.jucx.UcxNativeStruct;
import org.ucx.jucx.UcxRequest;

/**
 * UCP worker is an opaque object representing the communication context.  The
 * worker represents an instance of a local communication resource and progress
 * engine associated with it. Progress engine is a construct that is
 * responsible for asynchronous and independent progress of communication
 * directives. The progress engine could be implement in hardware or software.
 * The worker object abstract an instance of network resources such as a host
 * channel adapter port, network interface, or multiple resources such as
 * multiple network interfaces or communication ports. It could also represent
 * virtual communication resources that are defined across multiple devices.
 * Although the worker can represent multiple network resources, it is
 * associated with a single {@link UcpContext} "UCX application context".
 * All communication functions require a context to perform the operation on
 * the dedicated hardware resource(s) and an "endpoint" to address the
 * destination.
 *
 * <p>Worker are parallel "threading points" that an upper layer may use to
 * optimize concurrent communications.
 */
public class UcpWorker extends UcxNativeStruct implements Closeable {

    public UcpWorker(UcpContext context, UcpWorkerParams params) {
        setNativeId(createWorkerNative(params, context.getNativeId()));
    }

    /**
     * Creates new UcpEndpoint on current worker.
     */
    public UcpEndpoint newEndpoint(UcpEndpointParams params) {
        return new UcpEndpoint(this, params);
    }

    /**
     * Creates new UcpListener on current worker.
     */
    public UcpListener newListener(UcpListenerParams params) {
        return new UcpListener(this, params);
    }

    @Override
    public void close() {
        releaseWorkerNative(getNativeId());
        setNativeId(null);
    }

    /**
     * This routine explicitly progresses all communication operations on a worker.
     * @return Non-zero if any communication was progressed, zero otherwise.
     */
    public int progress() {
        return progressWorkerNative(getNativeId());
    }

    /**
     * This routine returns the address of the worker object. This address can be
     * passed to remote instances of the UCP library in order to connect to this
     * worker. Ucp worker address - is an opaque object that is used as an
     * identifier for a {@link UcpWorker} instance.
     */
    public ByteBuffer getAddress() {
        ByteBuffer nativeUcpAddress = workerGetAddressNative(getNativeId());
        // 1. Allocating java native ByteBuffer (managed by java's reference count cleaner).
        ByteBuffer result = ByteBuffer.allocateDirect(nativeUcpAddress.capacity());
        // 2. Copy content of native ucp address to java's buffer.
        result.put(nativeUcpAddress);
        result.clear();
        // 3. Release an address of the worker object. Memory allocated in JNI must be freed by JNI.
        releaseAddressNative(getNativeId(), nativeUcpAddress);
        return result;
    }

    public UcxRequest recvNonBlocking(ByteBuffer recvBuffer, long tag,
                                UcxCallback callback) {
        if (!recvBuffer.isDirect()) {
            throw new UcxException("Recv buffer must be direct.");
        }
        if (callback == null) {
            callback = new UcxCallback();
        }
        return recvNonBlockingNative(getNativeId(), recvBuffer, tag, callback);
    }

    public UcxRequest recvNonBlocking(ByteBuffer recvBuffer, UcxCallback callback) {
        return recvNonBlocking(recvBuffer, 0, callback);
    }

    private static native long createWorkerNative(UcpWorkerParams params, long ucpContextId);

    private static native void releaseWorkerNative(long workerId);

    private static native ByteBuffer workerGetAddressNative(long workerId);

    private static native void releaseAddressNative(long workerId, ByteBuffer addressId);

    private static native int progressWorkerNative(long workerId);

    private static native UcxRequest recvNonBlockingNative(long workerId, ByteBuffer recvBuffer,
                                                           long tag, UcxCallback callback);
}
