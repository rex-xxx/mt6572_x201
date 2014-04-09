
package com.android.providers.downloads;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.NetworkInfo;


interface SystemFacade {
    /**
     * @see System#currentTimeMillis()
     */
    public long currentTimeMillis();

    /**
     * @return Currently active network, or null if there's no active
     *         connection.
     */
    public NetworkInfo getActiveNetworkInfo(int uid);

    public boolean isActiveNetworkMetered();

    /**
     * @see android.telephony.TelephonyManager#isNetworkRoaming
     */
    public boolean isNetworkRoaming();

    /**
     * @return maximum size, in bytes, of downloads that may go over a mobile connection; or null if
     * there's no limit
     */
    public Long getMaxBytesOverMobile();

    /**
     * @return recommended maximum size, in bytes, of downloads that may go over a mobile
     * connection; or null if there's no recommended limit.  The user will have the option to bypass
     * this limit.
     */
    public Long getRecommendedMaxBytesOverMobile();

    /**
     * Send a broadcast intent.
     */
    public void sendBroadcast(Intent intent);

    /**
     * Returns true if the specified UID owns the specified package name.
     */
    public boolean userOwnsPackage(int uid, String pckg) throws NameNotFoundException;

    /**
     * Start a thread.
     */
    public void startThread(Thread thread);
}
