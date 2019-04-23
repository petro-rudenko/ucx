/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

#include "jucx_common_def.h"
#include "org_ucx_jucx_ucp_UcpEndpoint.h"

#include <string.h>    /* memset */


static void error_handler(void *arg, ucp_ep_h ep, ucs_status_t status)
{
    jobject *callback = (jobject *)arg;
    JNIEnv *env = get_jni_env();
    jclass callback_cls = env->GetObjectClass(*callback);
    jmethodID on_error = env->GetMethodID(callback_cls, "onError", "(I,Ljava/lang/String;)V");
    env->CallVoidMethod(callback, on_error, status, ucs_status_string(status));
    env->DeleteGlobalRef(*callback);
}

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

    if (ep_params.field_mask & UCP_EP_PARAM_FIELD_ERR_HANDLER) {
        field = env->GetFieldID(ucp_ep_params_class, "errorHandler", "Lorg/ucx/jucx/UcxCallback;");
        jobject callback = env->GetObjectField(ucp_ep_params, field);
        ep_params.err_handler.cb = error_handler;
        ep_params.err_handler.arg = env->NewGlobalRef(callback);
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
    env->DeleteGlobalRef(*(jobject *)ucp_ep_ext_gen((ucp_ep_h) ep_ptr)->err_handler.arg);
    ucp_ep_destroy((ucp_ep_h) ep_ptr);
}
