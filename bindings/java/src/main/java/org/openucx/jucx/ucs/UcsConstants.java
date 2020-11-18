/*
 * Copyright (C) Mellanox Technologies Ltd. 2019. ALL RIGHTS RESERVED.
 * See file LICENSE for terms.
 */

package org.openucx.jucx.ucs;

import org.openucx.jucx.NativeLibs;

public class UcsConstants {
    static {
        load();
    }

    public static class ThreadMode {
        static {
            load();
        }
        /**
         * Multiple threads can access concurrently
         */
        public static int UCS_THREAD_MODE_MULTI;
    }

    /**
     * Status codes
     */
    public enum STATUS {
        /* Operation completed successfully */
        UCS_OK                        (0),

        /* Operation is queued and still in progress */
        UCS_INPROGRESS                (1),

        /* Failure codes */
        UCS_ERR_NO_MESSAGE            (-1),
        UCS_ERR_NO_RESOURCE           (-2),
        UCS_ERR_IO_ERROR              (-3),
        UCS_ERR_NO_MEMORY             (-4),
        UCS_ERR_INVALID_PARAM         (-5),
        UCS_ERR_UNREACHABLE           (-6),
        UCS_ERR_INVALID_ADDR          (-7),
        UCS_ERR_NOT_IMPLEMENTED       (-8),
        UCS_ERR_MESSAGE_TRUNCATED     (-9),
        UCS_ERR_NO_PROGRESS           (-10),
        UCS_ERR_BUFFER_TOO_SMALL      (-11),
        UCS_ERR_NO_ELEM               (-12),
        UCS_ERR_SOME_CONNECTS_FAILED  (-13),
        UCS_ERR_NO_DEVICE             (-14),
        UCS_ERR_BUSY                  (-15),
        UCS_ERR_CANCELED              (-16),
        UCS_ERR_SHMEM_SEGMENT         (-17),
        UCS_ERR_ALREADY_EXISTS        (-18),
        UCS_ERR_OUT_OF_RANGE          (-19),
        UCS_ERR_TIMED_OUT             (-20),
        UCS_ERR_EXCEEDS_LIMIT         (-21),
        UCS_ERR_UNSUPPORTED           (-22),
        UCS_ERR_REJECTED              (-23),
        UCS_ERR_NOT_CONNECTED         (-24),
        UCS_ERR_CONNECTION_RESET      (-25),

        UCS_ERR_FIRST_LINK_FAILURE    (-40),
        UCS_ERR_LAST_LINK_FAILURE     (-59),
        UCS_ERR_FIRST_ENDPOINT_FAILURE(-60),
        UCS_ERR_ENDPOINT_TIMEOUT      (-80),
        UCS_ERR_LAST_ENDPOINT_FAILURE (-89),

        UCS_ERR_LAST                  (-100);

        public final int value;

        STATUS(int value) {
            this.value = value;
        }
    }

    private static void load() {
        NativeLibs.load();
        loadConstants();
    }

    private static native void loadConstants();
}
