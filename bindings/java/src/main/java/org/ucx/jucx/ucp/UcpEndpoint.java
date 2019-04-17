package org.ucx.jucx.ucp;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.UcxException;
import org.ucx.jucx.UcxNativeStruct;

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

    public void getNonBlocking(ByteBuffer data, UcpRemoteMemory remoteMemory,
                               UcxCallback callback) {
        if (!data.isDirect()) {
            throw new UcxException("Data buffer must be direct.");
        }
        if (callback == null) {
            callback = new UcxCallback();
        }
        getNonBlockingNative(getNativeId(), data, remoteMemory, callback);
    }

    private static native long createEndpointNative(UcpEndpointParams params, long workerId);

    private static native void destroyEndpointNative(long epId);

    private static native void getNonBlockingNative(long enpointId, ByteBuffer data,
                                                    UcpRemoteMemory remoteMemory,
                                                    UcxCallback callback);
}
