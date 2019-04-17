/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.ucx.jucx.ucp;

import java.nio.ByteBuffer;

public class UcpRemoteMemory {
    private ByteBuffer remoteKey;

    private long address;

    public UcpRemoteMemory(ByteBuffer remoteKey, long address) {
        this.address = address;
        this.remoteKey = remoteKey;
    }

    public UcpRemoteMemory(UcpMemory memory) {
        this.address = memory.getAddress();
        this.remoteKey = memory.getRemoteKeyBuffer();
    }
}
