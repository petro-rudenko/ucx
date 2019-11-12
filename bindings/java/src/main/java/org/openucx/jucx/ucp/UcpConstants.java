/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.openucx.jucx.ucp;

import org.openucx.jucx.NativeLibs;

public class UcpConstants {
    static {
        NativeLibs.load();
        loadConstants();
    }

    /**
     * UCP context parameters field mask.
     *
     * <p>The enumeration allows specifying which fields in {@link UcpParams} are
     * present. It is used for the enablement of backward compatibility support.
     */
    public static long UCP_PARAM_FIELD_FEATURES;
    public static long UCP_PARAM_FIELD_TAG_SENDER_MASK;
    public static long UCP_PARAM_FIELD_MT_WORKERS_SHARED;
    public static long UCP_PARAM_FIELD_ESTIMATED_NUM_EPS;

    /**
     * UCP configuration features
     *
     * <p>The enumeration list describes the features supported by UCP.
     * An application can request the features using "UCP parameters"
     * during "UCP initialization" process.
     */
    public static long UCP_FEATURE_TAG;
    public static long UCP_FEATURE_RMA;
    public static long UCP_FEATURE_AMO32;
    public static long UCP_FEATURE_AMO64;
    public static long UCP_FEATURE_WAKEUP;
    public static long UCP_FEATURE_STREAM;

    /**
     * UCP worker parameters field mask.
     *
     * <p>The enumeration allows specifying which fields in {@link UcpWorker} are
     * present. It is used for the enablement of backward compatibility support.
     */
    public static long UCP_WORKER_PARAM_FIELD_THREAD_MODE;
    public static long UCP_WORKER_PARAM_FIELD_CPU_MASK;
    public static long UCP_WORKER_PARAM_FIELD_EVENTS;
    public static long UCP_WORKER_PARAM_FIELD_USER_DATA;
    public static long UCP_WORKER_PARAM_FIELD_EVENT_FD;

    /**
     * Mask of events which are expected on wakeup.
     * If it's not set all types of events will trigger on
     * wakeup.
     */
    public static long UCP_WAKEUP_RMA;
    public static long UCP_WAKEUP_AMO;
    public static long UCP_WAKEUP_TAG_SEND;
    public static long UCP_WAKEUP_TAG_RECV;
    public static long UCP_WAKEUP_TX;
    public static long UCP_WAKEUP_RX;
    public static long UCP_WAKEUP_EDGE;

    /**
     * UCP listener parameters field mask.
     */
    public static long UCP_LISTENER_PARAM_FIELD_SOCK_ADDR;
    public static long UCP_LISTENER_PARAM_FIELD_ACCEPT_HANDLER;
    public static long UCP_LISTENER_PARAM_FIELD_CONN_HANDLER;

    /**
     * UCP endpoint parameters field mask.
     */
    public static long UCP_EP_PARAM_FIELD_REMOTE_ADDRESS;
    public static long UCP_EP_PARAM_FIELD_ERR_HANDLING_MODE;
    public static long UCP_EP_PARAM_FIELD_ERR_HANDLER;
    public static long UCP_EP_PARAM_FIELD_USER_DATA;
    public static long UCP_EP_PARAM_FIELD_SOCK_ADDR;
    public static long UCP_EP_PARAM_FIELD_FLAGS;
    public static long UCP_EP_PARAM_FIELD_CONN_REQUEST;

    /**
     * UCP error handling mode.
     */
    public static int UCP_ERR_HANDLING_MODE_PEER;

    /**
     * The enumeration list describes the endpoint's parameters flags.
     */
    public static long UCP_EP_PARAMS_FLAGS_CLIENT_SERVER;
    public static long UCP_EP_PARAMS_FLAGS_NO_LOOPBACK;

    /**
     *  The enumeration list describes the memory mapping flags.
     */
    public static long UCP_MEM_MAP_NONBLOCK;
    public static long UCP_MEM_MAP_ALLOCATE;
    public static long UCP_MEM_MAP_FIXED;

    private static native void loadConstants();
}
