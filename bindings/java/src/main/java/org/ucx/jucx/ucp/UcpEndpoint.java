/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */
package org.ucx.jucx.ucp;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.UcxException;
import org.ucx.jucx.UcxNativeStruct;
import org.ucx.jucx.UcxRequest;

import java.io.Closeable;
import java.nio.ByteBuffer;

public class UcpEndpoint extends UcxNativeStruct implements Closeable {

    public UcpEndpoint(UcpWorker worker, UcpEndpointParams params) {
        setNativeId(createEndpointNative(params, worker.getNativeId()));
    }

    @Override
    public void close() {
        destroyEndpointNative(getNativeId());
        setNativeId(null);
    }

    /**
     * This routine unpacks the remote key (RKEY) object into the local memory
     * such that it can be accessed and used by UCP routines.
     * @param rkeyBuffer - Packed remote key buffer
     *                     (see {@link UcpMemory#getRemoteKeyBuffer()}).
     */
    public UcpRemoteKey unpackRemoteKey(ByteBuffer rkeyBuffer) {
        return unpackRemoteKey(getNativeId(), rkeyBuffer);
    }

    /**
     * Non-blocking remote memory get operation.
     * This routine initiates a load of a contiguous block of data that is
     * described by the remote memory address {@param remoteAddress} and the
     * {@param remoteKey} "memory handle". The routine returns immediately and <b>does</b> not
     * guarantee that remote data is loaded and stored under the local {@param dst} buffer.
     * {@param callback} is invoked on completion of this operation.
     * @return {@link UcxRequest} object that can be monitored for completion.
     */
    public UcxRequest getNonBlocking(long remoteAddress, UcpRemoteKey remoteKey,
                                     ByteBuffer dst, UcxCallback callback) {
        if (!dst.isDirect()) {
            throw new UcxException("Data buffer must be direct.");
        }
        if (remoteKey.getNativeId() == null) {
            throw new UcxException("Remote key is null.");
        }
        if (callback == null) {
            callback = new UcxCallback();
        }
        return getNonBlockingNative(getNativeId(), remoteAddress, remoteKey.getNativeId(),
            dst, callback);
    }

    public UcxRequest sendNonBlocking(ByteBuffer data, long tag, UcxCallback callback) {
        if (callback == null) {
            callback = new UcxCallback();
        }
        return sendNonBlockingNative(getNativeId(), data, tag, callback);
    }

    public UcxRequest sendNonBlocking(ByteBuffer data, UcxCallback callback) {
        return sendNonBlocking(data, 0, callback);
    }

    public UcxRequest putNonBlocking(ByteBuffer data, long remoteAddress, UcpRemoteKey remoteKey,
                                     UcxCallback callback) {
        if (!data.isDirect()) {
            throw new UcxException("Data buffer must be direct.");
        }
        if (remoteKey.getNativeId() == null) {
            throw new UcxException("Remote key is null.");
        }
        if (callback == null) {
            callback = new UcxCallback();
        }
        return putNonBlockingNative(getNativeId(), data, remoteAddress,
            remoteKey.getNativeId(), callback);
    }

    private static native long createEndpointNative(UcpEndpointParams params, long workerId);

    private static native void destroyEndpointNative(long epId);

    private static native UcpRemoteKey unpackRemoteKey(long epId, ByteBuffer rkeyBuffer);

    private static native UcxRequest getNonBlockingNative(long enpointId,
                                                          long remoteAddress,
                                                          long ucpRkeyId,
                                                          ByteBuffer localData,
                                                          UcxCallback callback);

    private static native UcxRequest sendNonBlockingNative(long enpointId, ByteBuffer data,
                                                           long tag,
                                                           UcxCallback callback);

    private static native UcxRequest putNonBlockingNative(long enpointId, ByteBuffer data,
                                                          long remoteAddr, long ucpRkeyId,
                                                          UcxCallback callback);
}
