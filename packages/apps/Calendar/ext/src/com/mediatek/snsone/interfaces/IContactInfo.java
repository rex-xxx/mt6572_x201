package com.mediatek.snsone.interfaces;

import android.os.Bundle;

/**
 * Responsible for information of one contact, include status update, MIME
 * information.
 */
public interface IContactInfo {
    /**
     * Get the status update of one contact.
     * 
     * @param bundle
     *            Data including account type and contact user id
     * 
     * @return the update
     */
    public String getUpdate(Bundle bundle);

    /**
     * Get the mime type of one SNS's contacts.
     * 
     * @param accountType
     *            Identifier of a SNS account
     * 
     * @return mimeType a string
     */
    public String getMimeType(String accountType);

    /**
     * Check if the MimeType of one SNS's contacts is available. A SNS account
     * may contain many MimeTypes.
     * 
     * @param mimeType
     *            such as :vnd.android.cursor.item/vnd.facebook.profile
     * 
     * @return true if Support this kinds of mimetype otherwise false
     */
    public boolean isMimeTypeSupported(String mimeType);
}
