/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx;

import java.nio.ByteBuffer;

/**
 * Request object, that returns by ucp operations (GET, PUT, SEND, etc.).
 * Call {@link UcxRequest#isCompleted()} to monitor completion of request.
 */
public class UcxRequest {

    private ByteBuffer data;

    private boolean completed;

    /**
     * @return whether this request is completed.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * @return destination data buffer associated with this request.
     */
    public ByteBuffer getData() {
        return data;
    }
}
