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

package com.mediatek.common;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.Uri;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.mediatek.common.geocoding.IGeoCodingQuery;
import com.mediatek.common.gifdecoder.IGifDecoder;
import com.mediatek.common.mpodecoder.IMpoDecoder;
import com.mediatek.common.IFwkExt;
import com.mediatek.common.stereo3d.IJpsParser;
import com.mediatek.common.stereo3d.IStereo3DConversion;

import com.mediatek.common.agps.MtkAgpsManager;
import com.mediatek.common.agps.IMtkAgpsManager;
import com.mediatek.common.dcfdecoder.IDcfDecoder;
import com.mediatek.common.epo.MtkEpoClientManager;
import com.mediatek.common.epo.IMtkEpoClientManager;
import com.mediatek.common.policy.IKeyguardUtilExt;
import com.mediatek.common.wifi.IWifiFwkExt;
import com.mediatek.common.net.IConnectivityServiceExt;

import com.mediatek.common.audioprofile.IAudioProfileExtension;
import com.mediatek.common.audioprofile.IAudioProfileService;
import com.mediatek.common.audioprofile.IAudioProfileManager;
import com.mediatek.common.tvout.ITVOUTNative;
import com.mediatek.common.hdmi.IHDMINative;
import com.mediatek.common.hdmi.IHDMIObserver;

import com.mediatek.common.lowstorage.*;
import com.mediatek.common.pluginmanager.IPluginManager;
import com.mediatek.common.storage.IStorageManagerEx;
import com.mediatek.common.telephony.ICallerInfoExt;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.common.telephony.ITetheringExt;
import com.mediatek.common.telephony.IPhoneNumberExt;
import com.mediatek.common.telephony.ITelephonyExt;
import com.mediatek.common.telephony.ITelephonyProviderExt;
import com.mediatek.common.telephony.IGsmDCTExt;
import com.mediatek.common.telephony.IGsmConnectionExt;
import com.mediatek.common.telephony.IBipManagerExt;
import com.mediatek.common.telephony.ISimInfoUpdate;

import com.mediatek.common.aee.IExceptionLog;
import com.mediatek.common.util.IPatterns;
import com.mediatek.common.bootanim.IBootAnimExt;
import com.mediatek.common.media.IOmaSettingHelper;
import com.mediatek.common.media.IAudioServiceExt;

import com.mediatek.common.sms.IWapPushFwkExt;
import com.mediatek.common.sms.IDupSmsFilterExt;
import com.mediatek.common.amsplus.IAmsPlus;

import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.IVoicePhoneDetection;
import com.mediatek.common.webkit.IPicker;
/// M: MSG Logger Manager @{
import com.mediatek.common.msgmonitorservice.IMessageLogger;
import com.mediatek.common.msgmonitorservice.IMessageLoggerWrapper;
/// MSG Logger Manager @}

import com.mediatek.common.search.ISearchEngineManager;
import com.mediatek.common.search.ISearchEngineManagerService;

public final class MediatekClassFactory {

    private static final boolean DEBUG_PERFORMANCE = true;
    private static final boolean DEBUG_GETINSTANCE = true;
    private static final String TAG = "MediatekClassFactory";
    private static final String mOPFactoryName = "com.mediatek.op.MediatekOPClassFactory";
    private static Method mOpGetIfClassName = null;
    private static boolean mOpChecked = false;

    // mediatek-common.jar public interface map used for interface class
    // matching.
    private static Map<Class, String> commonInterfaceMap = new HashMap<Class, String>();
    static {
        commonInterfaceMap.put(ISimInfoUpdate.class,
                "com.mediatek.telephony.SimInfoUpdateAdp");
        commonInterfaceMap.put(IExceptionLog.class,
                "com.mediatek.exceptionlog.ExceptionLog");
        commonInterfaceMap.put(ILowStorageHandle.class,
                "com.mediatek.lowstorage.LowStorageHandle");
        commonInterfaceMap.put(MtkAgpsManager.class,
                "com.mediatek.agps.MtkAgpsManagerImpl");
        commonInterfaceMap.put(IMtkAgpsManager.class,
                "com.mediatek.agps.MtkAgpsManagerService");
        commonInterfaceMap.put(IDcfDecoder.class,
                "com.mediatek.dcfdecoder.DcfDecoder");
        commonInterfaceMap.put(MtkEpoClientManager.class,
                "com.mediatek.epo.MtkEpoClientManagerImpl");
        commonInterfaceMap.put(IMtkEpoClientManager.class,
                "com.mediatek.epo.MtkEpoClientManagerService");
        commonInterfaceMap.put(IGifDecoder.class,
                "com.mediatek.gifdecoder.GifDecoder");
        commonInterfaceMap.put(IAudioProfileService.class,
                "com.mediatek.audioprofile.AudioProfileService");
        commonInterfaceMap.put(IAudioProfileManager.class,
                "com.mediatek.audioprofile.AudioProfileManager");
        commonInterfaceMap.put(ITVOUTNative.class,
                "com.mediatek.tvout.TVOUTNative");
        commonInterfaceMap.put(IHDMIObserver.class,
                "com.mediatek.hdmi.HDMIObserver");
        commonInterfaceMap.put(IHDMINative.class,
                "com.mediatek.hdmi.HDMINative");
        commonInterfaceMap.put(IVoiceCommandManager.class,
                "com.mediatek.voicecommand.app.VoiceCommandManager");
        commonInterfaceMap.put(IVoicePhoneDetection.class,
                "com.mediatek.voicecommand.app.VoicePhoneDetection");
        commonInterfaceMap.put(IAmsPlus.class,
            	"com.mediatek.amsplus.ActivityStackPlus");
        commonInterfaceMap.put(IMessageLogger.class,
                "com.mediatek.msglogger.MessageMonitorService");
        commonInterfaceMap.put(IMessageLoggerWrapper.class,
                "com.mediatek.msglogger.MessageLoggerWrapper");
        commonInterfaceMap.put(IBipManagerExt.class,
                "com.mediatek.internal.telephony.cat.BipManager");
        commonInterfaceMap.put(ISearchEngineManager.class,
                "com.mediatek.search.SearchEngineManager");
        commonInterfaceMap.put(ISearchEngineManagerService.class,
                "com.mediatek.search.SearchEngineManagerService");
    }

    // mediatek-op.jar public interface map used for interface class matching.
    private static Map<Class, String> opInterfaceMap = new HashMap<Class, String>();
    static {
        opInterfaceMap.put(IFwkExt.class, "com.mediatek.op.FwkExt");
        opInterfaceMap.put(IWifiFwkExt.class,
                "com.mediatek.op.wifi.DefaultWifiFwkExt");
        opInterfaceMap.put(IConnectivityServiceExt.class,
                "com.mediatek.op.net.DefaultConnectivityServiceExt");
        opInterfaceMap.put(IPatterns.class,
                "com.mediatek.op.util.DefaultPatterns");
        opInterfaceMap.put(IBootAnimExt.class,
                "com.mediatek.op.bootanim.DefaultBootAnimExt");
        opInterfaceMap.put(IKeyguardUtilExt.class,
                "com.mediatek.op.policy.DefaultKeyguardUtilExt");
        opInterfaceMap.put(IServiceStateExt.class,
                "com.mediatek.op.telephony.ServiceStateExt");
        opInterfaceMap.put(ITetheringExt.class,
                "com.mediatek.op.telephony.TetheringExt");
        opInterfaceMap.put(IPhoneNumberExt.class,
                "com.mediatek.op.telephony.PhoneNumberExt");
        opInterfaceMap.put(IGsmConnectionExt.class,
                "com.mediatek.op.telephony.GsmConnectionExt");
        opInterfaceMap.put(ITelephonyExt.class,
                "com.mediatek.op.telephony.TelephonyExt");
        opInterfaceMap.put(ICallerInfoExt.class,
                "com.mediatek.op.telephony.CallerInfoExt");
        opInterfaceMap.put(IOmaSettingHelper.class,
                "com.mediatek.op.media.DefaultOmaSettingHelper");
        opInterfaceMap.put(IAudioServiceExt.class,
                "com.mediatek.common.media.IAudioServiceExt");
        opInterfaceMap.put(IAudioProfileExtension.class,
                "com.mediatek.op.audioprofile.DefaultAudioProfileExtension");
        opInterfaceMap.put(
                IAudioProfileExtension.IDefaultProfileStatesGetter.class,
                "com.mediatek.op.audioprofile.DefaultProfileStatesGetter");
        opInterfaceMap.put(IWapPushFwkExt.class,
                "com.mediatek.op.sms.WapPushFwkExt");
        opInterfaceMap.put(IDupSmsFilterExt.class, 
                "com.mediatek.op.sms.DupSmsFilterExt");
        opInterfaceMap.put(ITelephonyProviderExt.class,
                "com.mediatek.op.telephony.TelephonyProviderExt");
        opInterfaceMap.put(IGsmDCTExt.class,
                "com.mediatek.op.telephony.GsmDCTExt");
    }

    /**
     * Use factory method design pattern.
     * 
     * @throws Exception
     */
    public static <T> T createInstance(Class<?> clazz, Object... args) {

        if (DEBUG_PERFORMANCE) {
            Log.d(TAG, "createInstance(): Begin = "
                    + SystemClock.uptimeMillis());
        }

        String ifClassName = null;
        Object obj = null;

        if (DEBUG_GETINSTANCE) {
            Log.d(TAG, "create Instance with :  " + clazz);
        }

        if (commonInterfaceMap.containsKey(clazz)) {

            ifClassName = commonInterfaceMap.get(clazz);

            if (DEBUG_GETINSTANCE) {
                Log.d(TAG,
                        "create Instance from mediatek-framework library :  "
                                + ifClassName);
            }

            obj = getInstanceHelper(ifClassName, args);
        } else if (opInterfaceMap.containsKey(clazz)) {

            ifClassName = getOpIfClassName(clazz);

            if (DEBUG_GETINSTANCE) {
                Log.d(TAG, "create Instance from operator library :  "
                        + ifClassName);
            }

            if (clazz == IPatterns.class) {
                Object patternsObj = null;
                try {
                    Class<?> clz = Class.forName(ifClassName);
                    if (clz != null && args.length > 0) {
                        if (args[0].equals(IPatterns.GET_WEBURL)) {
                            Method method = clz.getMethod(IPatterns.GET_WEBURL,
                                    String.class, String.class, String.class);
                            if (method != null) {
                                patternsObj = method.invoke(null, args[1],
                                        args[2], args[3]);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "createInstance:got exception for "
                            + ifClassName);
                    e.printStackTrace();
                }
                obj = patternsObj;
            } else {
                obj = getInstanceHelper(ifClassName, args);
            }
        } else if (clazz == IMpoDecoder.class) {
            Object mpoDecoderObj = null;
            try {
                Class<?> clz = Class.forName("com.mediatek.mpo.MpoDecoder");
                if (clz != null && args.length > 0) {
                    if (args[0].equals(IMpoDecoder.DECODE_FILE)) {
                        Method method = clz.getMethod(IMpoDecoder.DECODE_FILE,
                                String.class);
                        if (method != null) {
                            mpoDecoderObj = method.invoke(null, args[1]);
                        }
                    } else if (args[0].equals(IMpoDecoder.DECODE_URI)) {
                        Method method = clz.getMethod(IMpoDecoder.DECODE_URI,
                                ContentResolver.class, Uri.class);
                        if (method != null) {
                            mpoDecoderObj = method.invoke(null, args[1],
                                    args[2]);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "createInstance:got exception for MpoDecoder");
                e.printStackTrace();
            }
            obj = mpoDecoderObj;
        } else if (clazz == IJpsParser.class) {
            Object jpsParserObj = null;
            try {
                Class<?> clz = Class.forName("com.mediatek.stereo3d.JpsParser");
                if (clz != null && args.length > 0) {

                    if (args[0].equals(IJpsParser.PARSE)) {
                        Method method = clz.getMethod(IJpsParser.PARSE,
                                File.class);

                        if (method != null) {
                            jpsParserObj = method.invoke(null, args[1]);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "createInstance:got exception for JpsParser");
                e.printStackTrace();
            }
            obj = jpsParserObj;
        } else if (clazz == IStereo3DConversion.class) {
            Object bitmapObj = null;
            try {
                Class<?> clz = Class
                        .forName("com.mediatek.stereo3d.Stereo3DConversion");
                if (clz != null && args.length > 0) {
                    Log
                            .d(TAG, "Stereo3DConversion args legnth: "
                                    + args.length);

                    if (args[0].equals(IStereo3DConversion.EXECUTE)) {
                        if (args.length == 2) {
                            Method method = clz.getMethod(
                                    IStereo3DConversion.EXECUTE, Bitmap.class);

                            if (method != null) {
                                bitmapObj = method.invoke(null, args[1]);
                            }
                        } else if (args.length == 3) {
                            Method method = clz.getMethod(
                                    IStereo3DConversion.EXECUTE, Bitmap.class,
                                    boolean.class);

                            if (method != null) {
                                bitmapObj = method.invoke(null, args[1],
                                        args[2]);
                            }
                        } else if (args.length == 4) {
                            Method method = clz.getMethod(
                                    IStereo3DConversion.EXECUTE, Bitmap.class,
                                    int.class, int.class);

                            if (method != null) {
                                bitmapObj = method.invoke(null, args[1],
                                        args[2], args[3]);
                            }
                        } else if (args.length == 5) {
                            Method method = clz.getMethod(
                                    IStereo3DConversion.EXECUTE, Bitmap.class,
                                    int.class, int.class, boolean.class);

                            if (method != null) {
                                bitmapObj = method.invoke(null, args[1],
                                        args[2], args[3], args[4]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,
                        "createInstance:got exception for Stereo3DConversion");
                e.printStackTrace();
            }
            obj = bitmapObj;
        } else if (clazz == IGeoCodingQuery.class) {
            Object geoCodingQueryObj = null;
            try {
                Class<?> clz = Class.forName("com.mediatek.geocoding.GeoCodingQuery");
                if (clz != null && args.length > 0) {
                    if (args[0].equals(IGeoCodingQuery.GET_INSTANCE)) {
                        Method method = clz.getMethod(IGeoCodingQuery.GET_INSTANCE, Context.class);
                        if (method != null) {
                            geoCodingQueryObj = method.invoke(null, args[1]);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "createInstance:got exception for GeoCodingQuery");
                e.printStackTrace();
            }
            obj = geoCodingQueryObj;
        } else if (clazz == IPluginManager.class) {
            Object pluginMgr = null;
            try {
                Class<?> clz = Class
                        .forName("com.mediatek.pluginmanager.PluginManager");
                if (clz != null && args.length > 0) {
                    Log.d(TAG, "PluginManager args length: " + args.length);
                    if (args[0].equals(IPluginManager.CREATE)) {
                        Log.d(TAG, "IPluginManager.CREATE");
                        /*
                         * we only allow partial API 1. create(Context context,
                         * String pluginIntent, String version) 2.
                         * create(Context context, String pluginIntent, String
                         * version, Signature signature)
                         */
                        if (args.length == 4) {
                            Method method = clz.getMethod(
                                    IPluginManager.CREATE, Context.class,
                                    String.class, String.class,
                                    Signature[].class);
                            if (method != null) {
                                pluginMgr = method.invoke(null, args[1],
                                        args[2], args[3], null);
                            }
                        } else if (args.length == 5) {
                            Method method = clz.getMethod(
                                    IPluginManager.CREATE, Context.class,
                                    String.class, String.class,
                                    Signature[].class);
                            if (method != null) {
                                pluginMgr = method.invoke(null, args[1],
                                        args[2], args[3], args[4]);
                            }
                        }
                    } else if (args[0]
                            .equals(IPluginManager.CREATE_PLUGIN_OBJECT)) {
                        Log.d(TAG, "IPluginManager.CREATE_PLUGIN_OBJECT");
                        /*
                         * we only allow partial API 1.
                         * createPluginObject(Context context, String
                         * pluginIntent, String version, String metaName) 2.
                         * createPluginObject(Context context, String
                         * pluginIntent, String version, String metaName,
                         * Signature signatures)
                         */
                        if (args.length == 5) {
                            Method method = clz.getMethod(
                                    IPluginManager.CREATE_PLUGIN_OBJECT,
                                    Context.class, String.class, String.class,
                                    String.class, Signature[].class);
                            if (method != null) {
                                pluginMgr = method.invoke(null, args[1],
                                        args[2], args[3], args[4], null);
                            }
                        } else if (args.length == 6) {
                            Method method = clz.getMethod(
                                    IPluginManager.CREATE_PLUGIN_OBJECT,
                                    Context.class, String.class, String.class,
                                    String.class, Signature[].class);
                            if (method != null) {
                                pluginMgr = method.invoke(null, args[1],
                                        args[2], args[3], args[4], args[5]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "createInstance:got exception for PluginManager");
                e.printStackTrace();
            }
            obj = pluginMgr;
        } else if (clazz == IPicker.class) {
            Object pickerObj = null;
            try {
                Class<?> clz = Class.forName("com.mediatek.webkit.PickerManager");
                Log.i(TAG, "IPicker init args length : " + args.length +
                    ", args[0] : " + args[0] + ", args[1] : " + args[1]);
                if (clz != null && args.length > 0) {
                    if (args[0].equals(IPicker.GET_INSTANCE)) {
                        Method method = clz.getMethod(IPicker.GET_INSTANCE, String.class);
                        if (method != null) {
                            pickerObj = method.invoke(null, args[1]);
                            Log.i(TAG, "IPicker init invoke : " + pickerObj);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "createInstance:got exception for Webkit Picker");
                e.printStackTrace();
            }
            obj = pickerObj;
        }else if (clazz == IStorageManagerEx.class) {
            Object storageManagerExObj = null;
            try {
                Class<?> clz = Class.forName("com.mediatek.storage.StorageManagerEx");
                if (clz != null && args.length > 0) {
                    if (args[0].equals(IStorageManagerEx.GET_DEFAULT)) {
                        Method method = clz.getMethod(IStorageManagerEx.GET_DEFAULT);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null);
                        }
                    } else if (args[0].equals(IStorageManagerEx.SET_DEFAULT)) {
                        Method method = clz.getMethod(IStorageManagerEx.SET_DEFAULT, String.class);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null, args[1]);
                        }
                       /**
                                          * Because this function return null, this will cause throw RunTimeException
                                          * to avoid this, we return directly.
                                          */
                        return null;
                    } else if (args[0].equals(IStorageManagerEx.GET_EXTERNAL_CACHE)) {
                        Method method = clz.getMethod(IStorageManagerEx.GET_EXTERNAL_CACHE, String.class);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null, args[1]);
                        }
                    } else if (args[0].equals(IStorageManagerEx.GET_EXTERNAL_STORAGE)) {
                        Method method = clz.getMethod(IStorageManagerEx.GET_EXTERNAL_STORAGE);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null);
                        }
                    } else if (args[0].equals(IStorageManagerEx.GET_INTERNAL_STORAGE)) {
                        Method method = clz.getMethod(IStorageManagerEx.GET_INTERNAL_STORAGE);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null);
                        }
                    } else if (args[0].equals(IStorageManagerEx.GET_SWAP_STATE)) {
                        Method method = clz.getMethod(IStorageManagerEx.GET_SWAP_STATE);
                        if (method != null) {
                            storageManagerExObj = method.invoke(null);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "createInstance:got exception for StorageManagerEx");
                e.printStackTrace();
            }
            obj = storageManagerExObj;
        } else {
            Log.e(TAG, "Unsupported class: " + clazz);
            if (DEBUG_PERFORMANCE) {
                Log.d(TAG, "createInstance(): End = "
                        + SystemClock.uptimeMillis());
            }
        }

        if (DEBUG_PERFORMANCE) {
            Log.d(TAG, "createInstance(): End = " + SystemClock.uptimeMillis());
        }

        // Cannot return null object.
        if (obj == null) {
            Log.d(TAG, "null object during finding :  " + clazz);
            throw new RuntimeException();
        }
        return (T) obj;

    }

    public static Object getInstanceHelper(String className, Object[] args) {

        if (className == null) {
            Log.e(TAG, "Interface full class name is null");
            return null;
        }

        try {
            Class<?> clz = Class.forName(className);

            if (args.length == 0) {
                // Default constructor.
                return clz.getConstructor().newInstance();
            }

            // More than one parameters. Look for compatible constructor to the
            // input arguments.
            Constructor<?> ctorList[] = clz.getConstructors();
            for (int i = 0; i < ctorList.length; i++) {
                boolean matched = true;
                Constructor<?> ct = ctorList[i];
                Class<?> paramTypes[] = ct.getParameterTypes();
                if (paramTypes.length != args.length) {
                    continue;
                }

                for (int j = 0; j < paramTypes.length; j++) {
                    Class paramType = paramTypes[j];
                    Class actualType = args[j].getClass();

                    Log.d(TAG, "getInstanceHelper: paramType=" + paramType
                            + ", actualType=" + actualType);

                    if (!paramType.isAssignableFrom(actualType)
                            && !(paramType.isPrimitive() && primitiveMap.get(
                                    paramType).equals(actualType))) {
                        Log.d(TAG, "Parameter not matched, skip");
                        matched = false;
                        break;
                    }

                    Log.d(TAG, "Parameter matched");
                }

                // All parameter matched. Create the instance from the
                // constructor.
                if (matched) {
                    Log.d(TAG, "Constructor matched");
                    return ct.newInstance(args);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e.getClass().getName());
        }

        return null;
    }

    private static String getOpIfClassName(Class<?> clazz) {
        String ifClassName = null;

        // the mOpGetIfClassName will be cached
        if (mOpGetIfClassName == null) {
            // try to find the OpCreateInst and cache it
            // we only need the getInstance method, don't need to check the
            // number of argument
            try {
                Class<?> clz = Class.forName(mOPFactoryName);
                mOpGetIfClassName = clz.getMethod("getOpIfClassName",
                        Class.class);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "OP not exist!, Get obj from default class");
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "Not Such Method Exception: "
                        + e.getClass().getName());
            }
        }

        // get the class name from operator's class factory
        if (mOpGetIfClassName != null) {
            try {
                ifClassName = (String) mOpGetIfClassName.invoke(null, clazz);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "IllegalAccessException Exception: "
                        + e.getClass().getName());
            } catch (InvocationTargetException e) {
                Log.w(TAG, "InvocationTargetException Exception: "
                        + e.getClass().getName());
            }
        }

        if (ifClassName == null) {
            // give the default class name
            ifClassName = opInterfaceMap.get(clazz);
        }

        return ifClassName;
    }

    // Primitive type map used for parameter type matching.
    private static Map<Class, Class> primitiveMap = new HashMap<Class, Class>();
    static {
        primitiveMap.put(boolean.class, Boolean.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(short.class, Short.class);
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(double.class, Double.class);
    }
}
