/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

#include "jucx_common_def.h"
#include "org_ucx_jucx_ucp_UcpEndpoint.h"

#include <string.h>    /* memset */


JNIEXPORT jlong JNICALL
Java_org_ucx_jucx_ucp_UcpEndpoint_createEndpointNative(JNIEnv *env, jclass cls,
                                                       jobject ucp_ep_params,
                                                       jlong worker_ptr)
{
    ucp_ep_params_t ep_params;
    jfieldID field;
    ucp_worker_h ucp_worker = (ucp_worker_h)worker_ptr;
    ucp_ep_h endpoint;

    // Get field mask
    jclass ucp_ep_params_class = env->GetObjectClass(ucp_ep_params);
    field = env->GetFieldID(ucp_ep_params_class, "fieldMask", "J");
    ep_params.field_mask = env->GetLongField(ucp_ep_params, field);

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_REMOTE_ADDRESS) {
        field = env->GetFieldID(ucp_ep_params_class, "ucpAddress", "Ljava/nio/ByteBuffer;");
        jobject buf = env->GetObjectField(ucp_ep_params, field);
        ep_params.address = static_cast<const ucp_address_t *>(env->GetDirectBufferAddress(buf));
    }

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_ERR_HANDLING_MODE) {
        field = env->GetFieldID(ucp_ep_params_class, "errorHandlingMode", "I");
        ep_params.err_mode =  static_cast<ucp_err_handling_mode_t>(env->GetIntField(ucp_ep_params, field));
    }

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_USER_DATA) {
        field = env->GetFieldID(ucp_ep_params_class, "userData", "Ljava/nio/ByteBuffer;");
        jobject user_data = env->GetObjectField(ucp_ep_params, field);
        ep_params.user_data = env->GetDirectBufferAddress(user_data);
    }

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_FLAGS) {
        field = env->GetFieldID(ucp_ep_params_class, "flags", "J");
        ep_params.flags = env->GetLongField(ucp_ep_params, field);
    }

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_SOCK_ADDR) {
        struct sockaddr_storage worker_addr;
        socklen_t addrlen;
        memset(&worker_addr, 0, sizeof(struct sockaddr_storage));

        field = env->GetFieldID(ucp_ep_params_class,
                                "socketAddress", "Ljava/net/InetSocketAddress;");
        jobject sock_addr = env->GetObjectField(ucp_ep_params, field);

        if (j2cInetSockAddr(env, sock_addr, worker_addr, addrlen)) {
            ep_params.sockaddr.addr = (const struct sockaddr*)&worker_addr;
            ep_params.sockaddr.addrlen = addrlen;
        }
    }

    ucs_status_t status = ucp_ep_create(ucp_worker, &ep_params, &endpoint);
    if (status != UCS_OK) {
        JNU_ThrowExceptionByStatus(env, status);
    }

    return (native_ptr)endpoint;
}

JNIEXPORT void JNICALL
Java_org_ucx_jucx_ucp_UcpEndpoint_destroyEndpointNative(JNIEnv *env, jclass cls,
                                                        jlong ep_ptr)
{
    ucp_ep_destroy((ucp_ep_h) ep_ptr);
}

JNIEXPORT void JNICALL
Java_org_ucx_jucx_ucp_UcpEndpoint_getNonBlockingNative(JNIEnv *env, jclass cls,
                                                       jlong ep_ptr, jobject data,
                                                       jobject rmem, jobject clbk)
{
    jfieldID field;
    ucp_rkey_h rkey_p;
    ucs_status_t status;

    jclass ucp_rmem_cls = env->GetObjectClass(rmem);
    // 1. Get remote memory address from UcpMemory object.
    field = env->GetFieldID(ucp_rmem_cls, "address", "J");
    uint64_t remote_address = env->GetLongField(rmem, field);

    // 2. Get remoteKey from UcpMemory object.
    field = env->GetFieldID(ucp_rmem_cls, "remoteKey", "Ljava/nio/ByteBuffer;");
    jobject rkey = env->GetObjectField(rmem, field);
    void *rkeyBuffer = env->GetDirectBufferAddress(rkey);
    status = ucp_ep_rkey_unpack((ucp_ep_h)ep_ptr, rkeyBuffer, &rkey_p);
    if (status != UCS_OK) {
        JNU_ThrowExceptionByStatus(env, status);
    }

    // 3. Get result buffer
    void *result_address = env->GetDirectBufferAddress(data);
    size_t result_size = env->GetDirectBufferCapacity(data);

    ucs_status_ptr_t request = ucp_get_nb((ucp_ep_h)ep_ptr, result_address, result_size,
                                          remote_address, rkey_p, send_callback);
    if (UCS_PTR_IS_PTR(request)) {
        ((struct jucx_context *)request)->callback = env->NewGlobalRef(clbk);
        ((struct jucx_context *)request)->rkey_p = rkey_p;
    } else {
        struct jucx_context ctx;
        ctx.callback = env->NewGlobalRef(clbk);
        ctx.rkey_p = rkey_p;
        if (UCS_PTR_IS_ERR(request)) {
            JNU_ThrowExceptionByStatus(env, UCS_PTR_STATUS(request));
            send_callback(&ctx, UCS_PTR_STATUS(request));
        } else if(UCS_PTR_STATUS(request) == UCS_OK) {
            send_callback(&ctx, UCS_OK);
        }
    }
}
