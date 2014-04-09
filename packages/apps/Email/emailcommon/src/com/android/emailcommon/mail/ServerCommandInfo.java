package com.android.emailcommon.mail;

public class ServerCommandInfo {

    /**
     * Exchange command "Oof" information
     * @author MTK
     *
     */
    public class OofInfo {
        /**
         * OofStatus:
         * use 0 point to server not support Oof, MTK define;
         * use 1 point to set or get Oof successful, protocol define;
         * use 2 point to set or get Oof fail, protocol define;
         * use 3 point to server Access denied or TCP down, MTK define.
         * use 4 point to the account may be not initialized for sync oof
         * */
        public static final int SERVER_NOT_SUPPORT_OOF = 0;
        public static final int SET_OR_SAVE_SUCCESS = 1;
        public static final int SET_OR_SAVE_FAIL = 2;
        public static final int NETWORK_SHUT_DOWN = 3;
        public static final int SYNC_OOF_UNINITIALIZED = 4;
        /**
         * OofState:
         * use 0 point to Oof is disabled of this account, protocol define;
         * use 1 point to Oof is global of this account, protocol define;
         * use 2 point to Oof is time based of this account, protocol define;
         */
        public static final int OOF_IS_DISABLED = 0;
        public static final int OOF_IS_GLOBAL = 1;
        public static final int OOF_IS_TIME_BASED = 2;
    }

}
