/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.emailcommon;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.xlog.Xlog;

public class Logging {
    private static final boolean XLOG_ENABLED;
    public static final String LOG_TAG = "Email";
    public static final String EmailSend_TAG = "MSG_Send";
    public static final String EmailReceive_TAG = "MSG_Receive";
    /**
     * Set this to 'true' to enable as much Email logging as possible.
     */
    public static final boolean LOGD;

    /**
     * If this is enabled then logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static final boolean DEBUG_SENSITIVE;

    /**
     * If true, logging regarding UI (such as activity/fragment lifecycle) will be enabled.
     * 
     * TODO rename it to DEBUG_UI.
     */
    public static final boolean DEBUG_LIFECYCLE;

    /**
     * Set this to 'true' to enable Email Performance logging.
     */
    public static final boolean LOG_PERFORMANCE;

    static {
        // Declare values here to avoid dead code warnings; it means we have some extra
        // "if" statements in the byte code that always evaluate to "if (false)"
        LOGD = Build.TYPE.equals("eng") ? true : false; // DO NOT CHECK IN WITH TRUE
        LOG_PERFORMANCE = false; // DO NOT CHECK IN WITH TRUE
        DEBUG_SENSITIVE = false; // DO NOT CHECK IN WITH TRUE
        DEBUG_LIFECYCLE = false; // DO NOT CHECK IN WITH TRUE
        /** M: MTK Dependence */
        XLOG_ENABLED = true;
    }

    public static boolean DEBUG = Build.TYPE.equals("eng") ? true : false;

    public static void v(String tag, String msg) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.v(tag, msg);
        } else {
            if (DEBUG) {
                Log.v(tag, msg);
            }
        }
    }

    public static void v(String msg) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.v(LOG_TAG, msg);
        } else {
            if (DEBUG) {
                Log.v(LOG_TAG, msg);
            }
        }
    }

    public static void v(String tag, String msg, Throwable t) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.v(tag, msg, t);
        } else {
            if (DEBUG) {
                Log.v(tag, msg, t);
            }
        }
    }

    public static void v(String msg, Throwable t) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.v(LOG_TAG, msg, t);
        } else {
            if (DEBUG) {
                Log.v(LOG_TAG, msg, t);
            }
        }
    }

    public static void d(String tag, String msg) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.d(tag, msg);
        } else {
            if (DEBUG) {
                Log.d(tag, msg);
            }
        }
    }

    public static void d(String msg) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.d(LOG_TAG, msg);
        } else {
            if (DEBUG) {
                Log.d(LOG_TAG, msg);
            }
        }
    }

    public static void d(String tag, String msg, Throwable t) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.d(tag, msg, t);
        } else {
            if (DEBUG) {
                Log.d(tag, msg, t);
            }
        }
    }

    public static void d(String msg, Throwable t) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.d(LOG_TAG, msg, t);
        } else {
            if (DEBUG) {
                Log.d(LOG_TAG, msg, t);
            }
        }
    }

    public static void i(String tag, String msg) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.i(tag, msg);
        } else {
            if (DEBUG) {
                Log.i(tag, msg);
            }
        }
    }

    public static void i(String msg) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.i(LOG_TAG, msg);
        } else {
            if (DEBUG) {
                Log.i(LOG_TAG, msg);
            }
        }
    }

    public static void i(String tag, String msg, Throwable t) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.i(tag, msg, t);
        } else {
            if (DEBUG) {
                Log.i(tag, msg, t);
            }
        }
    }

    public static void i(String msg, Throwable t) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.i(LOG_TAG, msg, t);
        } else {
            if (DEBUG) {
                Log.i(LOG_TAG, msg, t);
            }
        }
    }

    public static void w(String tag, String msg) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.w(tag, msg);
        } else {
            if (DEBUG) {
                Log.w(tag, msg);
            }
        }
    }

    public static void w(String msg) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.w(LOG_TAG, msg);
        } else {
            if (DEBUG) {
                Log.w(LOG_TAG, msg);
            }
        }
    }

    public static void w(String tag, String msg, Throwable t) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.w(tag, msg, t);
        } else {
            if (DEBUG) {
                Log.w(tag, msg, t);
            }
        }
    }

    public static void w(String msg, Throwable t) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.w(LOG_TAG, msg, t);
        } else {
            if (DEBUG) {
                Log.w(LOG_TAG, msg, t);
            }
        }
    }

    public static void e(String tag, String msg) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.e(tag, msg);
        } else {
            if (DEBUG) {
                Log.e(tag, msg);
            }
        }
    }

    public static void e(String msg) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.e(LOG_TAG, msg);
        } else {
            if (DEBUG) {
                Log.e(LOG_TAG, msg);
            }
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        tag = TextUtils.isEmpty(tag) ? LOG_TAG : LOG_TAG + "/" + tag;
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.e(tag, msg, t);
        } else {
            if (DEBUG) {
                Log.e(tag, msg, t);
            }
        }
    }

    public static void e(String msg, Throwable t) {
        if (XLOG_ENABLED) {
            /** M: MTK Dependence */
            Xlog.e(LOG_TAG, msg, t);
        } else {
            if (DEBUG) {
                Log.e(LOG_TAG, msg, t);
            }
        }
    }
}
