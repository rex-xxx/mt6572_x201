package com.mediatek.snsone.interfaces;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * Responsible for information of SNS accounts, e.g. if a account is supported,
 * get icon of a specific SNS account, and some account related
 * resources.Current only supports FaceBook.
 */
public interface IAccountInfo {
    /**
     * Check if the given account type is supported.
     * 
     * @param accountType
     *            Identifier of a SNS account type
     * 
     * @return true
     */
    public boolean isAccountSupported(String accountType);

    /**
     * Get a icon represents a SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a drawable icon
     */
    public Drawable getAccountIcon(String accountType);

    /**
     * Get a bitmap format of the icon represents a SNS account.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a bitmap
     */
    public Bitmap getAccountIconBitmap(String accountType);

    /**
     * Get a birthday string of a specific account type.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return a birthday string
     */
    public String getAccountBirthdayString(String accountType);
}
