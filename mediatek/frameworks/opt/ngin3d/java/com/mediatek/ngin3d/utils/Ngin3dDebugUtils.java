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

package com.mediatek.ngin3d.utils;

import android.util.Log;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Color;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.animation.MasterClock;
import com.mediatek.ngin3d.debugtools.android.serveragent.IDebugee;
import com.mediatek.ngin3d.debugtools.android.serveragent.ParamObject;
import com.mediatek.ngin3d.presentation.PresentationEngine;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Ngin3dDebugUtils implements IDebugee {

    public static final boolean DEBUG = true;
    private static final String TAG = "Ngin3dDebugUtils";

    /**
     * The method to print log
     *
     * @param tag the tag of the class
     * @param msg the log message to print
     */
    private static void log(String tag, String msg) {
        if (Ngin3dDebugUtils.DEBUG) {
            Log.d(tag, msg);
        }
    }

    private final Stage mStage;
    private final PresentationEngine mPresentationEngine;
    private long mStartPauseTime;

    public Ngin3dDebugUtils(PresentationEngine pe, Stage stage) {
        log(TAG, "new Ngin3dDebugUtils");
        mStage = stage;
        mPresentationEngine = pe;
    }

    /**
     * get a Actor dump information (JSON format) by tag
     *
     * @param tag , the Actor's tag
     * @return String, dump information. otherwise return null.
     */
    public String dumpActorByTag(String tag) {
        log(TAG, "dumpActorByTag , tag : " + tag);
        Actor actor = mStage.findChildByTag(Integer.parseInt(tag));
        if (actor == null) {
            return null;
        } else {
            return actor.dump();
        }
    }

    /**
     * get a Actor dump information (JSON format) by id
     *
     * @param id , the Actor's ID
     * @return String, dump information. otherwise return null.
     */
    public String dumpActorByID(int id) {
        log(TAG, "dumpActorByID , id : " + id);
        Actor actor = mStage.findChildById(id);
        if (actor == null) {
            return null;
        } else {
            log(TAG, "dumpAnimation for test  : " + actor.dumpAnimation());
            return actor.dump();
        }
    }

    /**
     * get a Stage dump information (JSON format)
     *
     * @return String, dump information. otherwise return null.
     */
    public String dumpStage() {
        log(TAG, "dumpStage: ");
        return mStage.dump();
    }

    /**
     * update a property of Actor by tag
     *
     * @param tag , the Actor's tag
     * @param paramObj , a class that contain parameters.
     * @return return true if update succeed, otherwise return false.
     */
    public boolean setParameterByTag(String tag, ParamObject paramObj) {
        log(TAG, "setParameterByTag , tag : " + tag + "paramObj " + paramObj);
        if (tag == null || paramObj == null) {
            return false;
        }

        Actor actor = mStage.findChildByTag(Integer.parseInt(tag));
        if (actor == null) {
            return false;
        }
        return setParameter(actor, paramObj);
    }

    /**
     * update a property of Actor by ID
     *
     * @param id the Actor's ID
     * @param paramObj a class that contain parameters.
     * @return return true if update succeed, otherwise return false.
     */
    public boolean setParameterByID(int id, ParamObject paramObj) {
        log(TAG, "setParameterByID , id : " + id + "paramObj " + paramObj);
        if (paramObj == null) {
            return false;
        }

        Actor actor = mStage.findChildById(id);
        if (actor == null) {
            return false;
        }
        return setParameter(actor, paramObj);
    }

    /**
     * set a property of Actor
     *
     * @param actor the Actor
     * @param paramObj , a class that contain parameters.
     * @return return true if update succeed, otherwise return false.
     */
    private boolean setParameter(Actor actor, ParamObject paramObj) {
        log(TAG, "setParameter");
        if (actor == null || paramObj == null) {
            return false;
        }

        switch (paramObj.mParameterType) {
        case ParamObject.PARAMETER_TYPE_NAME:
            if (paramObj.mNameR == null) {
                return false;
            }
            actor.setName(paramObj.mNameR);
            break;
        case ParamObject.PARAMETER_TYPE_COLOR:
            if (paramObj.mColorR < 0 || paramObj.mColorR > 255 || paramObj.mColorG < 0 || paramObj.mColorG > 255
                || paramObj.mColorB < 0 || paramObj.mColorB > 255) {
                return false;
            }
            Color color = new Color(paramObj.mColorR, paramObj.mColorG, paramObj.mColorB, paramObj.mColorH);
            actor.setColor(color);
            break;
        case ParamObject.PARAMETER_TYPE_ROTATION:
            Rotation rotation = new Rotation(paramObj.mRotationX, paramObj.mRotationY, paramObj.mRotationZ);
            actor.setRotation(rotation);
            break;
        case ParamObject.PARAMETER_TYPE_SCALE:
            Scale scale = new Scale(paramObj.mScaleX, paramObj.mScaleY, paramObj.mScaleZ);
            actor.setScale(scale);
            break;
        case ParamObject.PARAMETER_TYPE_ANCHOR:
            if (paramObj.mAnchorX > 1 || paramObj.mAnchorX < 0 || paramObj.mAnchorY > 1 || paramObj.mAnchorY < 0
                || paramObj.mAnchorZ > 1 || paramObj.mAnchorZ < 0) {
                return false;
            }
            Point anchorPoint = new Point(paramObj.mAnchorX, paramObj.mAnchorY, paramObj.mAnchorZ);
            actor.setAnchorPoint(anchorPoint);
            break;
        case ParamObject.PARAMETER_TYPE_POSITION:
            Point positionPoint = new Point(paramObj.mPositionX, paramObj.mPositionY, paramObj.mPositionZ);
            actor.setPosition(positionPoint);
            break;
        case ParamObject.PARAMETER_TYPE_VISIBLE:
            actor.setVisible(paramObj.mIsVisible);
            break;
        case ParamObject.PARAMETER_TYPE_FLAG:
        case ParamObject.PARAMETER_TYPE_ZORDER_ON_TOP:
        default:
            return false;
        }
        return true;
    }

    /**
     * pause the ngin3d render
     *
     */
    public void pauseRender() {
        log(TAG, "pauseRender");

        /* set masterClock state to frozen */
        mPresentationEngine.pauseRendering();

        /* get start pause time */
        mStartPauseTime = MasterClock.getTime();
        log(TAG, "mStartPauseTime : " + mStartPauseTime);
    }

    /**
     * resume the ngin3d render
     *
     */
    public void resumeRender() {
        log(TAG, "resumeRender");

        /* set masterClock state to started */
        mPresentationEngine.resumeRendering();

        /* reset start pause time */
        mStartPauseTime = 0;
    }

    /**
     * tick the ngin3d render by specific time
     *
     * @param time the specific time which wants to tick
     */
    public void tickRender(int time) {
        log(TAG, "tickRender , mStartPauseTime enter : " + mStartPauseTime + "time: " + time);

        if (time < 1000000) {
            MasterClock.getDefault().tick(mStartPauseTime + time);
            mPresentationEngine.render();
            mStartPauseTime +=  time;
        }
    }

    /**
     * get the frame interval
     *
     * @return return int, frame interval, otherwise return -1
     */
    public double getFPS() {
        log(TAG, "getFPS");
        log(TAG, "getFPS, fps : " + mPresentationEngine.getFPS());
        return mPresentationEngine.getFPS();
    }

    /**
     * get the device memory info including uss, pss, rss
     *
     * @return return String, the format is "uss pss rss", otherwise return null
     */
    public String getMemoryInfo() {
        log(TAG, "getMemoryInfo");

        StringBuilder memStringBuilder = new StringBuilder();

        memStringBuilder.append("CImage:" + mPresentationEngine.getTotalCImageBytes()
                                                + " " + "Texture:" + mPresentationEngine.getTotalTextureBytes());

        log(TAG, memStringBuilder.toString());
        return memStringBuilder.toString();

    }

    /**
     * get the device total cpu info
     *
     * @return return int, cpu usage, otherwise return -1
     */
    public int getCpuUsage() {
        log(TAG, "getCpuUsage");

        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            String[] toks = load.split(" ");
            long idle1 = Long.parseLong(toks[5]);
            long beginCPU = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                        + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                        + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reader.seek(0);
            load = reader.readLine();
            reader.close();
            toks = load.split(" ");
            long idle2 = Long.parseLong(toks[5]);
            long endCPU = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                        + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                        + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            return (int) (100 * (endCPU - beginCPU) / ((endCPU + idle2) - (beginCPU + idle1)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    /**
     * remove actor by id
     */
    public void removeActorByID(int id) {
        mStage.remove(mStage.findChildById(id));
    }

    /**
     * dump animation by actor id
     *
     * @return string in json format
     */
    public String animationDumpToJSONByID(int id) {
        Actor actor = mStage.findChildById(id);
        if (actor == null) {
            return null;
        } else {
            return actor.dumpAnimation();
        }
    }

    /**
     * get frame interval
     *
     * @return frame interval
     */
    public int getFrameInterval() {
        return mStage.getFrameInterval();
    }

}
