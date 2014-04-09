/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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
package com.android.mms.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.text.TextUtils;

// add for IP message
import com.android.mms.R;
import com.mediatek.mms.ipmessage.IpMessageConsts;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser2 {
    // Singleton stuff
    private static SmileyParser2 sInstance;

    public static SmileyParser2 getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        sInstance = new SmileyParser2(context);
    }

    private final Context mContext;
    private final String[] mSmileyTexts;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;
    private final HashMap<String, Integer> mLargeCnRes;
    private final HashMap<String, Integer> mLargeEnRes;
    private final HashMap<String, Integer> mDynamicEnRes;
    private final HashMap<String, Integer> mDynamicCnRes;
    private final HashMap<String, Integer> mAdEnRes;
    private final HashMap<String, Integer> mAdCnRes;
    private final HashMap<String, Integer> mXmEnRes;
    private final HashMap<String, Integer> mXmCnRes;
    private final String[] mLargeCnTexts;
    private final String[] mLargeEnTexts;
    private final String[] mDynamicEnTexts;
    private final String[] mDynamicCnTexts;
    private final String[] mAdCnTexts;
    private final String[] mAdEnTexts;
    private final String[] mXmEnTexts;
    private final String[] mXmCnTexts;

    private SmileyParser2(Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        mLargeCnTexts = mContext.getResources().getStringArray(LARGE_SMILEY_CN);
        mLargeEnTexts = mContext.getResources().getStringArray(LARGE_SMILEY_EN);
        mDynamicEnTexts = mContext.getResources().getStringArray(DYNAMIC_SMIPEY_EN);
        mDynamicCnTexts = mContext.getResources().getStringArray(DYNAMIC_SMIPEY_CN);
        mAdCnTexts = mContext.getResources().getStringArray(AD_SMIPEY_CN);
        mAdEnTexts = mContext.getResources().getStringArray(AD_SMIPEY_EN);
        mXmCnTexts = mContext.getResources().getStringArray(XM_SMIPEY_CN);
        mXmEnTexts = mContext.getResources().getStringArray(XM_SMIPEY_EN);
        mSmileyToRes = buildSmileyToRes();
        mLargeCnRes = buildLargeCnRes();
        mLargeEnRes = buildLargeEnRes();
        mDynamicEnRes = buildDynamicEnRes();
        mDynamicCnRes = buildDynamicCnRes();
        mAdCnRes = buildAdCnRes();
        mAdEnRes = buildAdEnRes();
        mXmEnRes = buildXmEnRes();
        mXmCnRes = buildXmCnRes();
        mPattern = buildPattern();
    }

    static class Smileys {
        private static int[] sIconIds = MessageConsts.emoticonIdList;

        public static final int HAPPY = 0;
        public static final int WINKING = 1;
        public static final int SAD = 2;
        public static final int CRYING = 3;
        public static final int COOL = 4;
        public static final int KISSING = 5;
        public static final int TONGUE = 6;
        public static final int YRLLING = 7;
        public static final int ANGEL = 8;
        public static final int SURPRISED = 9;
        public static final int EMBARRASSED = 10;
        public static final int MONRY = 11;
        public static final int WRONG = 12;
        public static final int UNDECIDED = 13;
        public static final int LAUGHING = 14;
        public static final int CONFUSED = 15;
        public static final int LIPSARESEALED = 16;
        public static final int HEART = 17;
        public static final int MAD = 18;
        public static final int SMIRK = 19;
        public static final int POKER = 20;
        public static final int SLEEP = 21;
        public static final int VOMIT = 22;
        public static final int CHARMING = 23;
        public static final int SPEECHLESS = 24;
        public static final int DEMONS = 25;
        public static final int GRIEVANCE = 26;
        public static final int ABSORBED = 27;
        public static final int CUTE = 28;
        public static final int SLEEPY = 29;
        public static final int STRUGGLE = 30;
        public static final int ANGRY = 31;
        public static final int HORROR = 32;
        public static final int FAINT = 33;
        public static final int SLOBBER = 34;
        public static final int BADLOL = 35;
        public static final int GOBALLISTIC = 36;
        public static final int BOMB = 37;
        public static final int DOMINEERING = 38;
        public static final int DEPRESSED = 39;
        public static final int GOOD = 40;
        public static final int NO = 41;
        public static final int OK = 42;
        public static final int VICROTY = 43;
        public static final int SEDUCE = 44;
        public static final int DOWN = 45;
        public static final int RAIN = 46;
        public static final int LIGHTNING = 47;
        public static final int SUN = 48;
        public static final int MICROPHONE = 49;
        public static final int CLOCK = 50;
        public static final int EMAIL = 51;
        public static final int CANDLE = 52;
        public static final int BIRTHDAYCAKE = 53;
        public static final int GIFT = 54;
        public static final int STAR = 55;
        public static final int REDHEART = 56;
        public static final int BROKENHEART = 57;
        public static final int BULB = 58;
        public static final int MUSIC = 59;
        public static final int SHENMA = 60;
        public static final int FUYUN = 61;
        public static final int RICE = 62;
        public static final int ROSES = 63;
        public static final int FILM = 64;
        public static final int AEROPLANE = 65;
        public static final int UMBRELLA = 66;
        public static final int CAONIMA = 67;
        public static final int PENGUIN = 68;
        public static final int PIG = 69;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    static class LargeSmileys {
        private static int[] sIconIds = IpMessageConsts.LARGE_ICON_ARR;

        public static final int PRAISE = 0;
        public static final int GIFT = 1;
        public static final int KONGFU = 2;
        public static final int SHOWER = 3;
        public static final int SCARE = 4;
        public static final int ILL = 5;
        public static final int RICH = 6;
        public static final int FLY = 7;
        public static final int ANGRY = 8;
        public static final int APPROVE = 9;
        public static final int BORING = 10;
        public static final int CRY = 11;
        public static final int DRIVING = 12;
        public static final int EATING = 13;
        public static final int HAPPY = 14;
        public static final int HOLD = 15;
        public static final int HOLIDAY = 16;
        public static final int LOVE = 17;
        public static final int PRAY = 18;
        public static final int PRESSURE = 19;
        public static final int SING = 20;
        public static final int SLEEP = 21;
        public static final int SPORTS = 22;
        public static final int SWIMMING = 23;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    static class DynamicSmileys {
        private static int[] sIconIds = IpMessageConsts.DYNAMIC_ICON_ARR;

        public static final int HAPPY = 0;
        public static final int CLAPPING = 1;
        public static final int LOVE = 2;
        public static final int PROUDLY = 3;
        public static final int DISDAIN = 4;
        public static final int IMPATIENT = 5;
        public static final int SLAP = 6;
        public static final int ANGRY = 7;
        public static final int CURSE = 8;
        public static final int HOWEVER = 9;
        public static final int TANGLE = 10;
        public static final int CRY = 11;
        public static final int DISCOURAGING = 12;
        public static final int ANGRY_TOO = 13;
        public static final int AROUND = 14;
        public static final int PASSING = 15;
        public static final int WORSHIP = 16;
        public static final int PURE = 17;
        public static final int BYE = 18;
        public static final int INNOCENT = 19;
        public static final int AMAZING = 20;
        public static final int HUNGRY = 21;
        public static final int SLEEP = 22;
        public static final int WISHES = 23;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    static class AdSmileys {
        private static int[] sIconIds = IpMessageConsts.AD_ICON_ARR;

        public static final int BRETING = 0;
        public static final int SIDESHOW = 1;
        public static final int CRY = 2;
        public static final int HALO = 3;
        public static final int HEARTBEAT = 4;
        public static final int DOZING = 5;
        public static final int LAUGH = 6;
        public static final int IMPATIENCE = 7;
        public static final int RUNNING = 8;
        public static final int CURSE = 9;
        public static final int SWEAT = 10;
        public static final int COMPLACENT = 11;
        public static final int SNEEZE = 12;
        public static final int DANCING = 13;
        public static final int ANGRY = 14;
        public static final int JUMPING = 15;
        public static final int PEEP = 16;
        public static final int PRATFALL = 17;
        public static final int EATING = 18;
        public static final int HOOP = 19;
        public static final int PASSING = 20;
        public static final int SHAKE = 21;
        public static final int BYE = 22;
        public static final int ASTRICTION = 23;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    static class XmSmileys {
        private static int[] sIconIds = IpMessageConsts.XM_ICON_ARR;

        public static final int WINK = 0;
        public static final int QUESTION = 1;
        public static final int VOMIT = 2;
        public static final int BADLUCK = 3;
        public static final int LIKE = 4;
        public static final int CUTE = 5;
        public static final int DIZZY = 6;
        public static final int TIRED = 7;
        public static final int CONFUSED = 8;
        public static final int KISS = 9;
        public static final int LOVE = 10;
        public static final int CURSE = 11;
        public static final int MONEY = 12;
        public static final int SMILE = 13;
        public static final int CRY = 14;
        public static final int ANGRY = 15;
        public static final int PLEASED = 16;
        public static final int NAUGHTY = 17;
        public static final int THRILLER = 18;
        public static final int DAZE = 19;
        public static final int EMBARRASSED = 20;
        public static final int SLEEP = 21;
        public static final int DISAPPEAR = 22;
        public static final int GRIEVANCE = 23;

        public static int getSmileyResource(int which) {
            return sIconIds[which];
        }
    }

    // NOTE: if you change anything about this array, you must make the
    // corresponding change
    // to the string arrays: default_smiley_texts and default_smiley_names in
    // res/values/arrays.xml
    public static final int[] DEFAULT_SMILEY_RES_IDS = { Smileys.getSmileyResource(Smileys.HAPPY), // 0
            Smileys.getSmileyResource(Smileys.WINKING), // 1
            Smileys.getSmileyResource(Smileys.SAD), // 2
            Smileys.getSmileyResource(Smileys.CRYING), // 3
            Smileys.getSmileyResource(Smileys.COOL), // 4
            Smileys.getSmileyResource(Smileys.KISSING), // 5
            Smileys.getSmileyResource(Smileys.TONGUE), // 6
            Smileys.getSmileyResource(Smileys.YRLLING), // 7
            Smileys.getSmileyResource(Smileys.ANGEL), // 8
            Smileys.getSmileyResource(Smileys.SURPRISED), // 9
            Smileys.getSmileyResource(Smileys.EMBARRASSED), // 10
            Smileys.getSmileyResource(Smileys.MONRY), // 11
            Smileys.getSmileyResource(Smileys.WRONG), // 12
            Smileys.getSmileyResource(Smileys.UNDECIDED), // 13
            Smileys.getSmileyResource(Smileys.LAUGHING), // 14
            Smileys.getSmileyResource(Smileys.CONFUSED), // 15
            Smileys.getSmileyResource(Smileys.LIPSARESEALED), // 16
            Smileys.getSmileyResource(Smileys.HEART), // 17
            Smileys.getSmileyResource(Smileys.MAD), // 18
            Smileys.getSmileyResource(Smileys.SMIRK), // 19
            Smileys.getSmileyResource(Smileys.POKER), // 20
            Smileys.getSmileyResource(Smileys.SLEEP), // 21
            Smileys.getSmileyResource(Smileys.VOMIT), // 22
            Smileys.getSmileyResource(Smileys.CHARMING), // 23
            Smileys.getSmileyResource(Smileys.SPEECHLESS), // 24
            Smileys.getSmileyResource(Smileys.DEMONS), // 25
            Smileys.getSmileyResource(Smileys.GRIEVANCE), // 26
            Smileys.getSmileyResource(Smileys.ABSORBED), // 27
            Smileys.getSmileyResource(Smileys.CUTE), // 28
            Smileys.getSmileyResource(Smileys.SLEEPY), // 29
            Smileys.getSmileyResource(Smileys.STRUGGLE), // 30
            Smileys.getSmileyResource(Smileys.ANGRY), // 31
            Smileys.getSmileyResource(Smileys.HORROR), // 32
            Smileys.getSmileyResource(Smileys.FAINT), // 33
            Smileys.getSmileyResource(Smileys.SLOBBER), // 34
            Smileys.getSmileyResource(Smileys.BADLOL), // 35
            Smileys.getSmileyResource(Smileys.GOBALLISTIC), // 36
            Smileys.getSmileyResource(Smileys.BOMB), // 37
            Smileys.getSmileyResource(Smileys.DOMINEERING), // 38
            Smileys.getSmileyResource(Smileys.DEPRESSED), // 39
            Smileys.getSmileyResource(Smileys.GOOD), // 40
            Smileys.getSmileyResource(Smileys.NO), // 41
            Smileys.getSmileyResource(Smileys.OK), // 42
            Smileys.getSmileyResource(Smileys.VICROTY), // 43
            Smileys.getSmileyResource(Smileys.SEDUCE), // 44
            Smileys.getSmileyResource(Smileys.DOWN), // 45
            Smileys.getSmileyResource(Smileys.RAIN), // 46
            Smileys.getSmileyResource(Smileys.LIGHTNING), // 47
            Smileys.getSmileyResource(Smileys.SUN), // 48
            Smileys.getSmileyResource(Smileys.MICROPHONE), // 49
            Smileys.getSmileyResource(Smileys.CLOCK), // 50
            Smileys.getSmileyResource(Smileys.EMAIL), // 51
            Smileys.getSmileyResource(Smileys.CANDLE), // 52
            Smileys.getSmileyResource(Smileys.BIRTHDAYCAKE), // 53
            Smileys.getSmileyResource(Smileys.GIFT), // 54
            Smileys.getSmileyResource(Smileys.STAR), // 55
            Smileys.getSmileyResource(Smileys.REDHEART), // 56
            Smileys.getSmileyResource(Smileys.BROKENHEART), // 57
            Smileys.getSmileyResource(Smileys.BULB), // 58
            Smileys.getSmileyResource(Smileys.MUSIC), // 59
            Smileys.getSmileyResource(Smileys.SHENMA), // 60
            Smileys.getSmileyResource(Smileys.FUYUN), // 61
            Smileys.getSmileyResource(Smileys.RICE), // 62
            Smileys.getSmileyResource(Smileys.ROSES), // 63
            Smileys.getSmileyResource(Smileys.FILM), // 64
            Smileys.getSmileyResource(Smileys.AEROPLANE), // 65
            Smileys.getSmileyResource(Smileys.UMBRELLA), // 66
            Smileys.getSmileyResource(Smileys.CAONIMA), // 67
            Smileys.getSmileyResource(Smileys.PENGUIN), // 68
            Smileys.getSmileyResource(Smileys.PIG), // 69

    };

    public static final int[] LARGE_SMILEY_RES_IDS = {
            LargeSmileys.getSmileyResource(LargeSmileys.PRAISE), // 0
            LargeSmileys.getSmileyResource(LargeSmileys.GIFT), // 1
            LargeSmileys.getSmileyResource(LargeSmileys.KONGFU), // 2
            LargeSmileys.getSmileyResource(LargeSmileys.SHOWER), // 3
            LargeSmileys.getSmileyResource(LargeSmileys.SCARE), // 4
            LargeSmileys.getSmileyResource(LargeSmileys.ILL), // 5
            LargeSmileys.getSmileyResource(LargeSmileys.RICH), // 6
            LargeSmileys.getSmileyResource(LargeSmileys.FLY), // 7
            LargeSmileys.getSmileyResource(LargeSmileys.ANGRY), // 8
            LargeSmileys.getSmileyResource(LargeSmileys.APPROVE), // 9
            LargeSmileys.getSmileyResource(LargeSmileys.BORING), // 10
            LargeSmileys.getSmileyResource(LargeSmileys.CRY), // 11
            LargeSmileys.getSmileyResource(LargeSmileys.DRIVING), // 12
            LargeSmileys.getSmileyResource(LargeSmileys.EATING), // 13
            LargeSmileys.getSmileyResource(LargeSmileys.HAPPY), // 14
            LargeSmileys.getSmileyResource(LargeSmileys.HOLD), // 15
            LargeSmileys.getSmileyResource(LargeSmileys.HOLIDAY), // 16
            LargeSmileys.getSmileyResource(LargeSmileys.LOVE), // 17
            LargeSmileys.getSmileyResource(LargeSmileys.PRAY), // 18
            LargeSmileys.getSmileyResource(LargeSmileys.PRESSURE), // 19
            LargeSmileys.getSmileyResource(LargeSmileys.SING), // 20
            LargeSmileys.getSmileyResource(LargeSmileys.SLEEP), // 21
            LargeSmileys.getSmileyResource(LargeSmileys.SPORTS), // 22
            LargeSmileys.getSmileyResource(LargeSmileys.SWIMMING), // 23
    };

    public static final int[] DYNAMIC_SMILEY_RES_IDS = {
            DynamicSmileys.getSmileyResource(DynamicSmileys.HAPPY), // 0
            DynamicSmileys.getSmileyResource(DynamicSmileys.CLAPPING), // 1
            DynamicSmileys.getSmileyResource(DynamicSmileys.LOVE), // 2
            DynamicSmileys.getSmileyResource(DynamicSmileys.PROUDLY), // 3
            DynamicSmileys.getSmileyResource(DynamicSmileys.DISDAIN), // 4
            DynamicSmileys.getSmileyResource(DynamicSmileys.IMPATIENT), // 5
            DynamicSmileys.getSmileyResource(DynamicSmileys.SLAP), // 6
            DynamicSmileys.getSmileyResource(DynamicSmileys.ANGRY), // 7
            DynamicSmileys.getSmileyResource(DynamicSmileys.CURSE), // 8
            DynamicSmileys.getSmileyResource(DynamicSmileys.HOWEVER), // 9
            DynamicSmileys.getSmileyResource(DynamicSmileys.TANGLE), // 10
            DynamicSmileys.getSmileyResource(DynamicSmileys.CRY), // 11
            DynamicSmileys.getSmileyResource(DynamicSmileys.DISCOURAGING), // 12
            DynamicSmileys.getSmileyResource(DynamicSmileys.ANGRY_TOO), // 13
            DynamicSmileys.getSmileyResource(DynamicSmileys.AROUND), // 14
            DynamicSmileys.getSmileyResource(DynamicSmileys.PASSING), // 15
            DynamicSmileys.getSmileyResource(DynamicSmileys.WORSHIP), // 16
            DynamicSmileys.getSmileyResource(DynamicSmileys.PURE), // 17
            DynamicSmileys.getSmileyResource(DynamicSmileys.BYE), // 18
            DynamicSmileys.getSmileyResource(DynamicSmileys.INNOCENT), // 19
            DynamicSmileys.getSmileyResource(DynamicSmileys.AMAZING), // 20
            DynamicSmileys.getSmileyResource(DynamicSmileys.HUNGRY), // 21
            DynamicSmileys.getSmileyResource(DynamicSmileys.SLEEP), // 22
            DynamicSmileys.getSmileyResource(DynamicSmileys.WISHES), // 23
    };

    public static final int[] AD_SMILEY_RES_IDS = { AdSmileys.getSmileyResource(AdSmileys.BRETING), // 0
            AdSmileys.getSmileyResource(AdSmileys.SIDESHOW), // 1
            AdSmileys.getSmileyResource(AdSmileys.CRY), // 2
            AdSmileys.getSmileyResource(AdSmileys.HALO), // 3
            AdSmileys.getSmileyResource(AdSmileys.HEARTBEAT), // 4
            AdSmileys.getSmileyResource(AdSmileys.DOZING), // 5
            AdSmileys.getSmileyResource(AdSmileys.LAUGH), // 6
            AdSmileys.getSmileyResource(AdSmileys.IMPATIENCE), // 7
            AdSmileys.getSmileyResource(AdSmileys.RUNNING), // 8
            AdSmileys.getSmileyResource(AdSmileys.CURSE), // 9
            AdSmileys.getSmileyResource(AdSmileys.SWEAT), // 10
            AdSmileys.getSmileyResource(AdSmileys.COMPLACENT), // 11
            AdSmileys.getSmileyResource(AdSmileys.SNEEZE), // 12
            AdSmileys.getSmileyResource(AdSmileys.DANCING), // 13
            AdSmileys.getSmileyResource(AdSmileys.ANGRY), // 14
            AdSmileys.getSmileyResource(AdSmileys.JUMPING), // 15
            AdSmileys.getSmileyResource(AdSmileys.PEEP), // 16
            AdSmileys.getSmileyResource(AdSmileys.PRATFALL), // 17
            AdSmileys.getSmileyResource(AdSmileys.EATING), // 18
            AdSmileys.getSmileyResource(AdSmileys.HOOP), // 19
            AdSmileys.getSmileyResource(AdSmileys.PASSING), // 20
            AdSmileys.getSmileyResource(AdSmileys.SHAKE), // 21
            AdSmileys.getSmileyResource(AdSmileys.BYE), // 22
            AdSmileys.getSmileyResource(AdSmileys.ASTRICTION), // 23
    };

    public static final int[] XM_SMILEY_RES_IDS = { XmSmileys.getSmileyResource(XmSmileys.WINK), // 0
            XmSmileys.getSmileyResource(XmSmileys.QUESTION), // 1
            XmSmileys.getSmileyResource(XmSmileys.VOMIT), // 2
            XmSmileys.getSmileyResource(XmSmileys.BADLUCK), // 3
            XmSmileys.getSmileyResource(XmSmileys.LIKE), // 4
            XmSmileys.getSmileyResource(XmSmileys.CUTE), // 5
            XmSmileys.getSmileyResource(XmSmileys.DIZZY), // 6
            XmSmileys.getSmileyResource(XmSmileys.TIRED), // 7
            XmSmileys.getSmileyResource(XmSmileys.CONFUSED), // 8
            XmSmileys.getSmileyResource(XmSmileys.KISS), // 9
            XmSmileys.getSmileyResource(XmSmileys.LOVE), // 10
            XmSmileys.getSmileyResource(XmSmileys.CURSE), // 11
            XmSmileys.getSmileyResource(XmSmileys.MONEY), // 12
            XmSmileys.getSmileyResource(XmSmileys.SMILE), // 13
            XmSmileys.getSmileyResource(XmSmileys.CRY), // 14
            XmSmileys.getSmileyResource(XmSmileys.ANGRY), // 15
            XmSmileys.getSmileyResource(XmSmileys.PLEASED), // 16
            XmSmileys.getSmileyResource(XmSmileys.NAUGHTY), // 17
            XmSmileys.getSmileyResource(XmSmileys.THRILLER), // 18
            XmSmileys.getSmileyResource(XmSmileys.DAZE), // 19
            XmSmileys.getSmileyResource(XmSmileys.EMBARRASSED), // 20
            XmSmileys.getSmileyResource(XmSmileys.SLEEP), // 21
            XmSmileys.getSmileyResource(XmSmileys.DISAPPEAR), // 22
            XmSmileys.getSmileyResource(XmSmileys.GRIEVANCE), // 23
    };

    public static final int DEFAULT_SMILEY_TEXTS = R.array.emoticon_name;
    public static final int LARGE_SMILEY_CN = R.array.large_emoticon_name_ch;
    public static final int LARGE_SMILEY_EN = R.array.large_emoticon_name_en;
    public static final int DYNAMIC_SMIPEY_EN = R.array.dynamic_emoticon_name_en;
    public static final int DYNAMIC_SMIPEY_CN = R.array.dynamic_emoticon_name_ch;
    public static final int AD_SMIPEY_EN = R.array.ad_emoticon_name_en;
    public static final int AD_SMIPEY_CN = R.array.ad_emoticon_name_ch;
    public static final int XM_SMIPEY_EN = R.array.xm_emoticon_name_en;
    public static final int XM_SMIPEY_CN = R.array.xm_emoticon_name_ch;

    /**
     * Builds the hashtable we use for mapping the string version of a smiley
     * (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        if (DEFAULT_SMILEY_RES_IDS.length != mSmileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], DEFAULT_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildLargeCnRes() {
        if (LARGE_SMILEY_RES_IDS.length != mLargeCnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mLargeCnTexts.length);
        for (int i = 0; i < mLargeCnTexts.length; i++) {
            smileyToRes.put(mLargeCnTexts[i], LARGE_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildLargeEnRes() {
        if (LARGE_SMILEY_RES_IDS.length != mLargeEnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mLargeEnTexts.length);
        for (int i = 0; i < mLargeEnTexts.length; i++) {
            smileyToRes.put(mLargeEnTexts[i], LARGE_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildDynamicEnRes() {
        if (DYNAMIC_SMILEY_RES_IDS.length != mDynamicEnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mDynamicEnTexts.length);
        for (int i = 0; i < mDynamicEnTexts.length; i++) {
            smileyToRes.put(mDynamicEnTexts[i], DYNAMIC_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildDynamicCnRes() {
        if (DYNAMIC_SMILEY_RES_IDS.length != mDynamicCnTexts.length) {
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mDynamicCnTexts.length);
        for (int i = 0; i < mDynamicCnTexts.length; i++) {
            smileyToRes.put(mDynamicCnTexts[i], DYNAMIC_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildAdCnRes() {
        if (AD_SMILEY_RES_IDS.length != mAdCnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mAdCnTexts.length);
        for (int i = 0; i < mAdCnTexts.length; i++) {
            smileyToRes.put(mAdCnTexts[i], AD_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildAdEnRes() {
        if (AD_SMILEY_RES_IDS.length != mAdEnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mAdEnTexts.length);
        for (int i = 0; i < mAdEnTexts.length; i++) {
            smileyToRes.put(mAdEnTexts[i], AD_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildXmCnRes() {
        if (XM_SMILEY_RES_IDS.length != mXmCnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mXmCnTexts.length);
        for (int i = 0; i < mXmCnTexts.length; i++) {
            smileyToRes.put(mXmCnTexts[i], XM_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    private HashMap<String, Integer> buildXmEnRes() {
        if (XM_SMILEY_RES_IDS.length != mXmEnTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mXmEnTexts.length);
        for (int i = 0; i < mXmEnTexts.length; i++) {
            smileyToRes.put(mXmEnTexts[i], XM_SMILEY_RES_IDS[i]);
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find smileys in
     * {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smilies
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }

    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such as
     * :-) with a graphical version.
     *
     * @param text
     *            A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any recognized
     *         emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            Drawable drawable = mContext.getResources().getDrawable(resId);
            int bound = mContext.getResources()
                    .getDimensionPixelOffset(R.dimen.emoticon_bound_size);
            drawable.setBounds(0, 0, bound, bound);
            builder.setSpan(new ImageSpan(drawable), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }

    /** M:
     *  this method can parse maxNumber spans.
     */
    public int addSmileySpans(Editable text, int maxNumber) {
        ImageSpan[] emoticonList = text.getSpans(0, text.length(), ImageSpan.class);
        if (emoticonList.length != 0) {
            for (int i = 0; i < emoticonList.length; i++) {
                text.removeSpan(emoticonList[i]);
            }
        }
        Matcher matcher = mPattern.matcher(text);
        int counter = 0;
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            Drawable drawable = mContext.getResources().getDrawable(resId);
            int bound = mContext.getResources()
                    .getDimensionPixelOffset(R.dimen.emoticon_bound_size);
            drawable.setBounds(0, 0, bound, bound);
            text.setSpan(new ImageSpan(drawable), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            counter++;
            if (counter == maxNumber) {
                return counter;
            }
        }
        return counter;
    }

    public int getLargeRes(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        Integer resId = mLargeCnRes.get(text);
        if (null == resId) {
            resId = mLargeEnRes.get(text);
            if (null == resId) {
                return 0;
            }
        }
        return resId;
    }

    public int getDynamicRes(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        Integer resId = mDynamicCnRes.get(text);
        if (null == resId) {
            resId = mDynamicEnRes.get(text);
            if (null == resId) {
                return 0;
            }
        }
        return resId;
    }

    public int getAdRes(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        Integer resId = mAdCnRes.get(text);
        if (null == resId) {
            resId = mAdEnRes.get(text);
            if (null == resId) {
                return 0;
            }
        }
        return resId;
    }

    public int getXmRes(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0;
        }
        Integer resId = mXmCnRes.get(text);
        if (null == resId) {
            resId = mXmEnRes.get(text);
            if (null == resId) {
                return 0;
            }
        }
        return resId;
    }
}
