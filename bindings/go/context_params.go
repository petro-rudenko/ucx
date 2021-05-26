/*
 * Copyright (C) Mellanox Technologies Ltd. 2021. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package ucp

// #cgo pkg-config: ucp
// #include <ucp/api/ucp.h>

import "C"

type UcpParams struct {
	params C.ucp_params_t
}

func (p *UcpParams) SetTagSenderMask(tagSenderMask uint64) {
	p.params.tag_sender_mask = C.ulong(tagSenderMask)
	p.params.field_mask |= C.UCP_PARAM_FIELD_TAG_SENDER_MASK
}

func (p *UcpParams) SetEstimatedNumEPS(estimatedNumEPS uint64) {
	p.params.estimated_num_eps = C.ulong(estimatedNumEPS)
	p.params.field_mask |= C.UCP_PARAM_FIELD_ESTIMATED_NUM_EPS
}

func (p *UcpParams) EnableSharedWorkers() {
	p.params.mt_workers_shared = 1
	p.params.field_mask |= C.UCP_PARAM_FIELD_MT_WORKERS_SHARED
}

func (p *UcpParams) EnableTag() {
	p.params.features |= C.UCP_FEATURE_TAG
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableRMA() {
	p.params.features |= C.UCP_FEATURE_RMA
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableAtomic32Bit() {
	p.params.features |= C.UCP_FEATURE_AMO32
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableAtomic64Bit() {
	p.params.features |= C.UCP_FEATURE_AMO64
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableWakeup() {
	p.params.features |= C.UCP_FEATURE_WAKEUP
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableStream() {
	p.params.features |= C.UCP_FEATURE_STREAM
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}

func (p *UcpParams) EnableAM() {
	p.params.features |= C.UCP_FEATURE_AM
	p.params.field_mask |= C.UCP_PARAM_FIELD_FEATURES
}
