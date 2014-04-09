package com.mediatek.snsone.interfaces;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Responsible for "Post" information and its operations, e.g. icon for post,
 * action for starting a SNS client to post when the icon is clicked.
 */
public interface IPostOperations {
    /**
     * Action for "Post".
     */
    static public final class Action {
        /**
         * Intent for "Post" icon clicked.
         */
        public Intent intent;

        /**
         * "Post" icon as a drawable.
         */
        public Drawable icon;

        /**
         * "Post" icon of bitmap format.
         */
        public Bitmap iconBitmap;
    }

    /**
     * Get a icon represents "Post" of a specific SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a drawable
     */
    public Drawable getPostIcon(String accountType);

    /**
     * Get a bitmap of the icon represents "Post" of a specific SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a bitmap
     */
    public Bitmap getPostIconBitmap(String accountType);

    /**
     * Get one post's action.You should check the returned action for success,
     * if it is null, then you had given it a invalid account type, if it is NOT
     * null, you should also check its icon member instead, if the icon is NOT
     * null, then succeed.
     * 
     * @param bundle
     *            information about one contact in SNS, include account type and
     *            user id
     * 
     * @return action
     */
    Action getPostAction(Bundle bundle);
}
