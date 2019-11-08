/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */
package org.openucx.jucx.ucp;

public class UcpRegisterMemoryParams {
    private long flags;
    private long address;
    private long length;

    public UcpRegisterMemoryParams setAddress(long address) {
        this.address = address;
        return this;
    }

    public long getAddress() {
        return address;
    }

    public UcpRegisterMemoryParams setLength(long length) {
        this.length = length;
        return this;
    }

    public long getLength() {
        return length;
    }

    /**
     * Identify requirement for allocation, if passed address is not a null-pointer
     * then it will be used as a hint or direct address for allocation.
     */
    public UcpRegisterMemoryParams allocate() {
        flags |= UcpConstants.UCP_MEM_MAP_ALLOCATE;
        return this;
    }

    /**
     * Complete the registration faster, possibly by not populating the pages up-front,
     * and mapping them later when they are accessed by communication routines.
     */
    public UcpRegisterMemoryParams nonBlocking() {
        flags |= UcpConstants.UCP_MEM_MAP_NONBLOCK;
        return this;
    }

    /**
     * Don't interpret address as a hint: place the mapping at exactly that
     * address. The address must be a multiple of the page size.
     */
    public UcpRegisterMemoryParams fixed() {
        flags |= UcpConstants.UCP_MEM_MAP_FIXED;
        return this;
    }

    long getFlags() {
        return flags;
    }
}
