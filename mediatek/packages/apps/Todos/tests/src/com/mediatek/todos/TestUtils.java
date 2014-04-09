/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.todos;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;


import com.jayway.android.robotium.solo.Solo;

import com.mediatek.todos.TodoInfo;
import com.mediatek.todos.tests.TodosInfoTest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestUtils {
    private static final String TAG  = "TestUtils";

    public TestUtils() {
    }

    // try to define all functions as static
    public String getAttribute(TodoInfo info, int type) {
        String string = null;
        switch (type) {
        case TodosInfoTest.ID:
            string = info.getId();
            break;
        case TodosInfoTest.TITLE:
            string = info.getTitle();
            break;
        case TodosInfoTest.CREATE_TIME:
            string = info.getCreateTime();
            break;
        case TodosInfoTest.DESCRIPTION:
            string = info.getDescription();
            break;
        case TodosInfoTest.DTEND:
            string = Long.toString(info.getDueDate());
            break;
        case TodosInfoTest.COMPLETE_TIME:
            string = Long.toString(info.getCompleteTime());
            break;
        case TodosInfoTest.STATUS:
            string = info.getStatus();
            break;
        default:
            break;
        }
        return string;
    }

    public void setStatus(TodoInfo info, String status) {
        info.setStatus(status);
    }

    public void setTitle(TodoInfo info, String title) {
        info.setTitle(title);
    }

    public void setDescription(TodoInfo info, String description) {
        info.setDescription(description);
    }

    public void setDueDay(TodoInfo info, long dueDayMillis) {
        info.setDueDay(dueDayMillis);
    }

    public void setCreateTime(TodoInfo info, String createTime) {
        info.setCreateTime(createTime);
    }

    public void setCompleteDay(TodoInfo info, long completeDayMillis) {
        info.setCompleteDay(completeDayMillis);
    }

    public void updateStatus(TodoInfo info, String status) {
        info.updateStatus(status);
    }

    public static class ListenerForReceiver implements TimeChangeReceiver.TimeChangeListener {
        public static final int DATE_CHANGE = 0;
        public static final int TIME_CHANGE = 1;
        private int mReceiveData = -1;

        public void onDateChange() {
            mReceiveData = DATE_CHANGE;
        }

        public void onTimePick() {
            mReceiveData = TIME_CHANGE;
        }

        public int getReceiverData() {
            return mReceiveData;
        }
    }
    
    public static Boolean isMenuItemEbable(String name, Activity activity) {
        try {
            Field filed = activity.getClass().getDeclaredField(name);
            filed.setAccessible(true);
            Method method = filed.getClass().getMethod("isEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(filed);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "error:"+ e);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "error:"+ e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "error:"+ e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "error:"+ e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "error:"+ e);
        }
        return false;
    }
    
    public static void clickMenuItem(int itemStringId, Instrumentation instrumentation) {
        Solo solo = new Solo(instrumentation);
        solo.clickOnMenuItem(instrumentation.getContext().getResources().getString(itemStringId));
    }
    
    public static void clickButton(int buttonStringId, Instrumentation instrumentation){
        Solo solo = new Solo(instrumentation);
        solo.clickOnButton(instrumentation.getContext().getResources().getString(buttonStringId));
    }
}
