/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.android.emailcommon.service;

oneway interface EmailExternalCalls {
    /**
     * Callback for send an email.
     * 
     * @param ex If null, the operation completed without error
     * @param accountId The account being operated on
     * @param resultType {@link EmailExternalConstants#TYPE_SEND} or
     *            {@link EmailExternalConstants#TYPE_DELIVER}
     */
    void sendCallback(int result, long accountId, int resultType);

    /**
     * Callback for update inbox.
     * 
     * @param ex If null, the operation completed without error
     * @param accountId The account being operated on
     * @param mailboxId The maibox being operated on
     */
    void updateCallback(int result, long accountId, long mailboxId);
    
}
