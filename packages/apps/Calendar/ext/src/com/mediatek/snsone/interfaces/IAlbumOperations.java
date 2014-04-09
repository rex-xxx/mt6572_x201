package com.mediatek.snsone.interfaces;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Responsible for "Album" information and related operations, e.g. a album
 * icon, and a intent used to open one specific contact's album when the album's
 * icon is clicked.
 */
public interface IAlbumOperations {
    /**
     * Action for "Album".
     */
    static public final class Action {
        /**
         * Intent for "Album" icon clicked.
         */
        public Intent intent;

        /**
         * "Album" icon as a drawable.
         */
        public Drawable icon;

        /**
         * "Album" icon of bitmap format.
         */
        public Bitmap iconBitmap;
    }

    /**
     * Get a icon represents "Album" of a specific SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a icon
     */
    public Drawable getAlbumIcon(String accountType);

    /**
     * Get a bitmap of the icon represents "Album" of a specific SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a bitmap
     */
    public Bitmap getAlbumIconBitmap(String accountType);

    /**
     * Get a album's action. You should check the returned action for success,
     * if it is null, then you had given it a invalid account type, if it is NOT
     * null, you should also check its icon member instead, if the icon is NOT
     * null, then succeed.
     * 
     * @param bundle
     *            information about one contact in SNS, it should include the
     *            SNS account type, user id
     * 
     * @return a action
     */
    Action getAlbumAction(Bundle bundle);
}
