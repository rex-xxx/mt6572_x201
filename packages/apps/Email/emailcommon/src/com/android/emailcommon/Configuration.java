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

public class Configuration {
    // Bundle key for Exchange configuration (boolean value)
    public static final String EXCHANGE_CONFIGURATION_USE_ALTERNATE_STRINGS =
        "com.android.email.EXCHANGE_CONFIGURATION_USE_ALTERNATE_STRINGS";
    
    //The default port for pop3/imap/smtp/exchange
    public static final int IMAP_DEFAULT_PORT = 143;
    public static final int POP3_DEFAULT_PORT = 110;
    public static final int SMTP_DEFAULT_PORT = 25;
    public static final int EAS_DEFAULT_PORT  = 80;
    
    public static final int IMAP_DEFAULT_SSL_PORT = 993;
    public static final int POP3_DEFAULT_SSL_PORT = 995;
    public static final int SMTP_DEFAULT_SSL_PORT = 465;
    public static final int EAS_DEFAULT_SSL_PORT  = 443;

    ///M: The switch for test @{
    public static boolean mIsRunTestcase = false;
    public static boolean IS_TEST = false;

    public static void openTest() {
        mIsRunTestcase = true;
        IS_TEST = true;
    }

    public static void shutDownTest() {
        mIsRunTestcase = false;
        IS_TEST = false;
    }
    /// @}
}
