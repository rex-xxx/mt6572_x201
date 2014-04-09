
package com.mediatek.contacts.util;

import android.content.Intent;

/**
 * ContactsIntent is a helper class to manage all of Intent not defined in
 * Android.
 */

public final class ContactsIntent {

    /**
     * Check whether intent is defined in ContactsIntent or not
     * 
     * @param intent
     * @return true: intent is defined in ContactsIntent; otherwise return
     *         false.
     */
    public static boolean contain(Intent intent) {

        if (null == intent) {
            return false;
        }

        String action = intent.getAction();
        if (LIST.ACTION_PICK_MULTICONTACTS.equals(action)
                || LIST.ACTION_PICK_MULTIEMAILS.equals(action)
                || LIST.ACTION_PICK_MULTIPHONES.equals(action)
                || LIST.ACTION_DELETE_MULTICONTACTS.equals(action)
                || LIST.ACTION_GROUP_MOVE_MULTICONTACTS.equals(action)
                || LIST.ACTION_PICK_MULTIPHONEANDEMAILS.equals(action)
                || LIST.ACTION_SHARE_MULTICONTACTS.equals(action)
                || LIST.ACTION_GROUP_ADD_MULTICONTACTS.equals(action)
                || LIST.ACTION_PICK_MULTIDATAS.equals(action)) {
            return true;
        }
        return false;
    }

    /**
     * The action for com.mediatek.contacts.list.
     */
    public static final class LIST {
        public static final String ACTION_PICK_MULTICONTACTS = "android.intent.action.contacts.list.PICKMULTICONTACTS";
        public static final String ACTION_SHARE_MULTICONTACTS = "android.intent.action.contacts.list.SHAREMULTICONTACTS";
        public static final String ACTION_DELETE_MULTICONTACTS = "android.intent.action.contacts.list.DELETEMULTICONTACTS";
        public static final String ACTION_PICK_MULTIEMAILS = "android.intent.action.contacts.list.PICKMULTIEMAILS";
        public static final String ACTION_PICK_MULTIPHONES = "android.intent.action.contacts.list.PICKMULTIPHONES";
        public static final String ACTION_PICK_MULTIDATAS = "android.intent.action.contacts.list.PICKMULTIDATAS";
        public static final String ACTION_PICK_MULTIPHONEANDEMAILS = 
                "android.intent.action.contacts.list.PICKMULTIPHONEANDEMAILS";
        public static final String ACTION_GROUP_MOVE_MULTICONTACTS = 
                "android.intent.action.contacts.list.group.MOVEMULTICONTACTS";
        public static final String ACTION_GROUP_ADD_MULTICONTACTS = 
                "android.intent.action.contacts.list.group.ADDMULTICONTACTS";
    }

    /**
     * The action for multiple choice.
     */
    public static final class MULTICHOICE {
        public static final String ACTION_MULTICHOICE_PROCESS_FINISH = 
            "com.mediatek.intent.action.contacts.multichoice.process.finish";
    }
}
