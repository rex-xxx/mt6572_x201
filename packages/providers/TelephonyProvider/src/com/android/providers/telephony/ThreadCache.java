/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.providers.telephony;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class ThreadCache {

    private static final String TAG = "ThreadCache";
    private static Context sContext;
    private static ThreadCache sInstance = null;
    private static Set<ThreadEntry> sThreadCache = null;

    private ThreadCache(Context context) {
        sContext = context;
        sThreadCache = new HashSet<ThreadEntry>();
    }

    public static synchronized void init(Context context) {
        logD("init");
        if (sInstance == null) {
            sContext = context;
            sInstance = new ThreadCache(context);
        }
    }

    public static ThreadCache getInstance() {
        return sInstance;
    }

    private static void logD(String string) {
        Log.d(TAG, string);
    }

    public void add(Cursor c, List<String> recipients) {
        if (sThreadCache == null) {
            return;
        }
        synchronized (sInstance) {

            if (c != null && c.moveToFirst() && recipients != null && recipients.size() > 0) {
                ThreadEntry entry = new ThreadEntry(c.getLong(0), recipients);
                sThreadCache.add(entry);
                logD("add item, threadId = " + c.getLong(0) + " ," +
                        " recipients count = " + recipients.size() + "," +
                        " cache size = " + sThreadCache.size());
            }
        }
    }

    public void remove(long threadId) {
        if (sThreadCache == null) {
            return;
        }
        logD("Remove item, threadId = " + threadId + "," +
                " before remove, cache size = " + sThreadCache.size());
        synchronized (sInstance) {

            HashSet<ThreadEntry> cacheTemp = new HashSet<ThreadEntry>(sThreadCache);
            for (ThreadEntry entry : cacheTemp) {
                if (entry.getThreadId() == threadId) {
                    sThreadCache.remove(entry);
                    break;
                }
            }
        }
        logD("Remove item, threadId = " + threadId + "," +
                " after remove, cache size = " + sThreadCache.size());
    }

    public void removeAll() {
        if (sThreadCache == null) {
            sInstance = null;
            return;
        }
        synchronized (sInstance) {

            logD("Remove all items");
            sThreadCache.clear();
            sThreadCache = null;
            sInstance = null;
        }
    }

    public long getThreadId(List<String> recipients, boolean useStrictPhoneNumberComparation) {
        if (sThreadCache == null) {
            return 0;
        }
        for(ThreadEntry threadEntry : sThreadCache) {
            if (isEquals(threadEntry.getAddresses(), recipients, useStrictPhoneNumberComparation)) {
                logD("Get related thread id = " + threadEntry.getThreadId());
                return threadEntry.getThreadId();
            }
        }
        logD("Can not get related thread id ");
        return 0;
    }

    private boolean isEquals(List<String> a, List<String> b, boolean useStrictPhoneNumberComparation) {
        if (a == null || b == null || a.size() != b.size()) {
            logD("isEquals, Different addr size");
            return false;
        }
        List<String> aTemp = toLowerCase(a);
        List<String> bTemp = toLowerCase(b);
        boolean result = false;
        while (aTemp.size() > 0) {
            result = false;
            for (int i = 0; i < bTemp.size(); i++) {
                if (aTemp.get(0).equals(bTemp.get(i)) || PhoneNumberUtils.compare(
                        aTemp.get(0), bTemp.get(i), useStrictPhoneNumberComparation)) {
                    bTemp.remove(i);
                    aTemp.remove(0);
                    result = true;
                    break;
                }
            }
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private List<String> toLowerCase(List<String> list) {
        List<String> temp = new ArrayList<String>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                String address = list.get(i);
                boolean isEmail = Mms.isEmailAddress(address);
                String refinedAddress = isEmail ? address.toLowerCase() : address;
                temp.add(refinedAddress);
            }
        }
        return temp;
    }

    public Cursor formCursor(long threadId) {
        logD("formCursor, threadId = " + threadId);
        if (threadId <= 0) {
            return null;
        }
        String[] projection = new String[] {"_id"};
        MatrixCursor cursor = new MatrixCursor(projection);
        cursor.addRow(new Object[] {threadId});
        return cursor;
    }

    class ThreadEntry {
        private long threadId = 0;
        private List<String> addresses = null;

        public ThreadEntry(long lthreadId, List<String> addrArray) {
            threadId = lthreadId;
            addresses = addrArray;
        }

        public long getThreadId() {
            return threadId;
        }

        public List<String> getAddresses() {
            return addresses;
        }
    }
}
