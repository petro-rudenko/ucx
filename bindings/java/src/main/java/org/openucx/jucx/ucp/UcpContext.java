/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.openucx.jucx.ucp;

import java.io.Closeable;
import java.nio.ByteBuffer;

import org.openucx.jucx.NativeLibs;
import org.openucx.jucx.UcxException;
import org.openucx.jucx.UcxNativeStruct;
import org.openucx.jucx.UcxUtils;

/**
 * UCP application context (or just a context) is an opaque handle that holds a
 * UCP communication instance's global information. It represents a single UCP
 * communication instance. The communication instance could be an OS process
 * (an application) that uses UCP library.  This global information includes
 * communication resources, endpoints, memory, temporary file storage, and
 * other communication information directly associated with a specific UCP
 * instance. The context also acts as an isolation mechanism, allowing
 * resources associated with the context to manage multiple concurrent
 * communication instances. For example, users can isolate their communication
 * by allocating and using separate contexts. Alternatively, users can share the
 * communication resources (memory, network resource context, etc.) between
 * them by using the same application context. A message sent or a RMA
 * operation performed in one application context cannot be received in any
 * other application context.
 */
public class UcpContext extends UcxNativeStruct implements Closeable {
    static {
        NativeLibs.load();
    }

    public UcpContext(UcpParams params) {
        setNativeId(createContextNative(params));
    }

    @Override
    public void close() {
        cleanupContextNative(getNativeId());
        this.setNativeId(null);
    }

    /**
     * Creates new UcpWorker on current context.
     */
    public UcpWorker newWorker(UcpWorkerParams params) {
        return new UcpWorker(this, params);
    }

    /**
     * Associates memory allocated/mapped region with communication operations.
     * The network stack associated with an application context
     * can typically send and receive data from the mapped memory without
     * CPU intervention; some devices and associated network stacks
     * require the memory to be registered to send and receive data.
     */
    public UcpMemory registerMemory(ByteBuffer buf) {
        if (!buf.isDirect()) {
            throw new UcxException("Registered buffer must be direct");
        }
        UcpMemory result = registerMemoryNative(getNativeId(), UcxUtils.getAddress(buf),
            buf.remaining(), 0L);

        result.setByteBufferReference(buf);
        return result;
    }

    public UcpMemory registerMemory(UcpRegisterMemoryParams params) {
        return registerMemoryNative(getNativeId(), params.getAddress(),
            params.getLength(), params.getFlags());
    }

    private static native long createContextNative(UcpParams params);

    private static native void cleanupContextNative(long contextId);

    private native UcpMemory registerMemoryNative(long conetxtId, long address, long length,
                                                  long flags);

    public static native long cudaMalloc(long size);
}
