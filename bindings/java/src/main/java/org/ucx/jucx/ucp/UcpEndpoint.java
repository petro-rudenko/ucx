package org.ucx.jucx.ucp;

import org.ucx.jucx.UcxCallback;
import org.ucx.jucx.UcxException;
import org.ucx.jucx.UcxNativeStruct;

import java.io.Closeable;

public class UcpEndpoint extends UcxNativeStruct implements Closeable {

    public UcpEndpoint(UcpWorker worker, UcpEndpointParams params) {
        UcxCallback errorHandler = params.getErrorHandler();
        if (errorHandler != null) {
            params.setErrorHandler(new UcxCallback(){
                @Override
                public void onError(int ucsStatus, String errorMsg) {
                    close();
                    errorHandler.onError(ucsStatus, errorMsg);
                }
            });
        } else {
            params.setErrorHandler(new UcxCallback(){
                @Override
                public void onError(int ucsStatus, String errorMsg) {
                    close();
                    throw new UcxException("UcpEndpoint transport failure: " + errorMsg);
                }
            });
        }
        setNativeId(createEndpointNative(params, worker.getNativeId()));
    }

    @Override
    public void close() {
        destroyEndpointNative(getNativeId());
        setNativeId(null);
    }

    private static native long createEndpointNative(UcpEndpointParams params, long workerId);

    private static native void destroyEndpointNative(long epId);
}
