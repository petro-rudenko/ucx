/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */
#include "jucx_common_def.h"
#include "org_openucx_jucx_ucp_UcpRequest.h"


JNIEXPORT void JNICALL
Java_org_openucx_jucx_ucp_UcpRequest_updateRequestStatus(JNIEnv *env,
                                                         jobject jucx_request, jlong ucp_req_ptr)
{
    update_jucx_request_status(env, jucx_request,
                               ucp_request_check_status((void *)ucp_req_ptr));
}

JNIEXPORT void JNICALL
Java_org_openucx_jucx_ucp_UcpRequest_closeRequestNative(JNIEnv *env, jclass cls,
                                                        jlong ucp_req_ptr)
{
    ucp_request_free((void *)ucp_req_ptr);
}
