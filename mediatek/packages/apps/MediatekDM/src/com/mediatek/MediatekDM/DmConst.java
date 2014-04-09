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

package com.mediatek.MediatekDM;

public final class DmConst {
    public static final class TAG {
        public static final String Application = "DM/Application";
        public static final String Client = "DM/Client";
        public static final String Service = "DM/Service";
        public static final String Receiver = "DM/Receiver";
        public static final String Common = "DM/Common";
        public static final String Controller = "DM/Controller";
        public static final String Database = "DM/Database";
        public static final String Connection = "DM/Connection";
        public static final String XML = "DM/XML";
        public static final String Notification = "DM/Notification";
        public static final String NodeIOHandler = "DM/NodeIOHandler";

        public static final String Session = "DM/Session";
        public static final String PL = "DM/PL";
        public static final String Bootstrap = "DM/Bootstrap";
        public static final String CP = "DM/CP";
        public static final String Fumo = "DM/Fumo";
        public static final String Lawmo = "DM/Lawmo";
        public static final String MMI = "DM/MMI";
        public static final String Scomo = "DM/Scomo";
        public static final String SmsReg = "DM/SmsReg";

        public static final String Debug = "DM/Debug";
        public static final String Provider = "DM/ContentProvider";
    }

    public static final class TagName {
        public static final String Setting = "Setting";
        public static final String Name = "name";
        public static final String Node = "node";
        public static final String Text = "text";
        public static final String Timing = "timing";
    }

    public static final class Path {
        public static final String PathInSystem = "/system/etc/dm";
        public static final String PathInData = "/data/data/com.mediatek.MediatekDM/files";
        public static final String ReminderConfigFile = PathInSystem + "/reminder.xml";
        public static final String DmTreeFile = PathInData + "/tree.xml";
        public static final String PathInRom = "/system/etc/dm";
        public static final String DmTreeFileInRom = PathInRom + "/tree.xml";
        public static final String DmConfigFileInSystem = PathInSystem + "/config.xml";
        public static final String DmConfigFileIData = PathInData + "/config.xml";
        public static final String PathUpdateFile = "/data/data/com.mediatek.MediatekDM/files/updateResult";
        public static final String PathNia = "/data/data/com.mediatek.MediatekDM/files/NIA";
        public static final String PathWipe = "/data/data/com.mediatek.MediatekDM/wipe";
        public static final String pathSmsRegConfig = PathInSystem + "/smsSelfRegConfig.xml"; // modify:added
        public static final String pathDmValues = PathInData + "/dmvalues.xml";
        public static final String pathDelta = PathInData + "/delta.zip";
        public static final String pathResume = PathInData + "/dlresume.dat";

        public static final String FotaExecFlagFile = PathInData + "/fota_executing";
    }

    public static final class NodeUri {
        public static final String FumoRootUri = "./FwUpdate";
        public static final String LawmoRootUri = "./LAWMO";
        public static final String ScomoRootUri = "./SCOMO";
        public static final String DevDetailSwVUri = "./DevDetail/SwV";
        public static final String DevInfoDevId = "./DevInfo/DevId";
    }

    public static final class TimeValue {
        public static final int SECOND = 1;
        public static final int MINUTE = 60 * SECOND;
        public static final int TIMEOUT = 1 * MINUTE;
    }

    public static final class IntentAction {
        public static final String DM_WAP_PUSH = "android.provider.Telephony.WAP_PUSH_RECEIVED";
        public static final String DM_BOOT_COMPLETE = "android.intent.action.BOOT_COMPLETED";
        public static final String DM_REMINDER = "com.mediatek.MediatekDM.REMINDER";
        public static final String DM_DL_FOREGROUND = "com.mediatek.MediatekDM.DMDOWNLOADINGFOREGROUND";
        public static final String DM_SWUPDATE = "com.mediatek.DMSWUPDATE";
        public static final String PROXY_CHANGED = "android.intent.action.PROXY_CHANGE";
        public static final String NIA_RECEIVED = "com.mediatek.MediatekDM.NIA_RECEIVED";
        public static final String DM_START = "com.mediatek.MediatekDM.DM_STARTED";
        public static final String DM_NIA_START = "com.mediatek.MediatekDM.DM_NIA_START";
        public static final String NET_DETECT_TIMEOUT = "com.mediatek.MediatekDM.NETDETECTTIMEOUT";
        public static final String DM_FACTORYSET = "com.mediatek.MediatekDM.FACTORYSET";
        public static final String ACTION_DM_SERVE = "com.mediatek.MediatekDM.DMSERVE";
        public static final String DM_CLOSE_DIALOG = "com.mediatek.MediatekDM.CLOSE_DIALOG";

        public static final String ACTION_REBOOT_CHECK = "com.mediatek.MediatekDM.REBOOT_CHECK";
    }

    public static final class IntentType {
        public static final String DM_NIA = "application/vnd.syncml.notification";
        public static final String BOOTSTRAP_NIA = "application/vnd.syncml.dm.wbxml";
    }

    public static final class LawmoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class LawmoStatus {
        public static final int FULLY_LOCK = 10;
        public static final int PARTIALY_LOCK = 20;
    }

    public static final class FumoResult {
        public static final int OPERATION_SUCCESSSFUL = 200;
    }

    public static final class ServerMessage {
        public static final int TYPE_ALERT_1100 = 1;
        public static final int TYPE_ALERT_1101 = 2;
        public static final int TYPE_ALERT_1103_1104 = 3;
        public static final int TYPE_UIMODE_VISIBLE = 4;
        public static final int TYPE_UIMODE_INTERACT = 5;
    }
}
