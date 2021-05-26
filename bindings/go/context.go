/*
 * Copyright (C) Mellanox Technologies Ltd. 2021. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package ucp

// #cgo pkg-config: ucp
// #include <ucp/api/ucp.h>

import "C"

type UcpContext struct {
	context C.ucp_context_h
}

func NewUcpContext(params *UcpParams) *UcpContext {

}

func (p *UcpContext) Close() error {
	C.ucp_cleanup(p.context)
	return nil
}
