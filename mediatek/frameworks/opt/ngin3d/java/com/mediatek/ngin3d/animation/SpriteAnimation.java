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

package com.mediatek.ngin3d.animation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.mediatek.ngin3d.Actor;
import com.mediatek.ngin3d.Box;
import com.mediatek.ngin3d.Dimension;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Utils;
import com.mediatek.ngin3d.utils.Ngin3dException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @deprecated Sprite animation will be removed in a future release.
 */
@Deprecated
public class SpriteAnimation extends BasicAnimation {
    private static final String FRAMES = "frames";
    private static final String FRAME = "frame";
    private static final String SOURCE_SIZE = "sourceSize";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String WIDTH = "w";
    private static final String HEIGHT = "h";

    private Image mTarget;
    private SpriteSource mSpriteSource;
    private SpriteSheet mSheet;

    public SpriteAnimation(Image target, int duration) {
        init(new SpriteImages(), target, duration);
    }

    public SpriteAnimation(int duration) {
        init(new SpriteImages(), null, duration);
    }

    public SpriteAnimation(Image target, SpriteSheet sheet, int duration) {
        ExplicitSheet explicitSheet = new ExplicitSheet(sheet);
        explicitSheet.loadAllSprites();
        init(explicitSheet, target, duration);
    }

    public SpriteAnimation(SpriteSheet sheet, int duration) {
        this(null, sheet, duration);
    }

    public SpriteAnimation(Resources res, Image target, int imageId, int duration, int width, int height) {
        ImplicitSheet sheet = new ImplicitSheet(res, imageId);
        sheet.loadAllSprites(res, width, height);
        init(sheet, target, duration);
    }

    public SpriteAnimation(Resources res, int imageId, int duration, int width, int height) {
        this(res, null, imageId, duration, width, height);
    }

    private void init(SpriteSource source, Image target, int duration) {
        mSpriteSource = source;
        if (target != null) {
            setTarget(target);
        }
        setDuration(duration);

        mTimeline.addListener(new Timeline.Listener() {
            public void onStarted(Timeline timeline) {
                mSpriteSource.onAnimate(0, mTarget);
            }

            public void onNewFrame(Timeline timeline, int elapsedMsecs) {
                mSpriteSource.onAnimate(elapsedMsecs, mTarget);
            }

            public void onMarkerReached(Timeline timeline, int elapsedMsecs, String marker, int direction) {
                // onMarkerReached callback function
            }

            public void onPaused(Timeline timeline) {
                // do nothing now
            }

            public void onCompleted(Timeline timeline) {
                // do nothing now
            }

            public void onLooped(Timeline timeline) {
                // do nothing now
            }
        });
    }

    /**
     * Represents a single sprite image in the resource.
     */
    private static class SpriteImage {
        // this inner class is for ArrayList to keep SpriteImage.
        private final Resources mResource;
        private final int mResId;

        protected SpriteImage(Resources resource, int resId) {
            mResource = resource;
            mResId = resId;
        }

        Resources getRes() {
            return mResource;
        }

        int getResId() {
            return mResId;
        }
    }

    /**
     * Represent a frame in a sprite sheet.
     */
    public static class SpriteFrame {
        private final String mName;
        private final Box mRect = new Box();
        private final Dimension mSize = new Dimension();

        public SpriteFrame(String name) {
            mName = name;
        }

        void setBox(int x, int y, int w, int h) {
            mRect.set(x, y, w + x, h + y);
        }

        void setDim(int w, int h) {
            mSize.width = w;
            mSize.height = h;
        }

        Box getBox() {
            return mRect;
        }

        Dimension getDim() {
            return mSize;
        }
    }

    /**
     * Represents a sprite sheet which contain multiple sprite frames.
     */
    public static class SpriteSheet {
        private final Resources mRes;
        private final int mImageId;
        private final int mScriptId;

        private JSONObject mFrames;

        public SpriteSheet(Resources res, int imageId, int scriptId) {
            mRes = res;
            mImageId = imageId;
            mScriptId = scriptId;
        }

        public JSONObject getFrames() {
            if (mFrames != null) {
                return mFrames;
            }

            InputStream is = mRes.openRawResource(mScriptId);
            try {
                int length = is.available();
                byte[] b = new byte[length];
                if (is.read(b, 0, length) == length) {
                    final String s = new String(b);
                    JSONObject atlas = new JSONObject(s);
                    mFrames = atlas.optJSONObject(FRAMES);
                    return mFrames;
                } else {
                    throw new Ngin3dException("JSON of Packer List doesn't read completely");
                }
            } catch (IOException e) {
                throw new Ngin3dException(e);
            } catch (JSONException e) {
                throw new Ngin3dException(e);
            } finally {
                Utils.closeQuietly(is);
            }
        }

        private SpriteFrame getSpriteFrameByName(String name) {
            // Give a file name in json file. This method will grab the necessary info.
            SpriteFrame spriteFrame = new SpriteFrame(name);

            JSONObject resObject = mFrames.optJSONObject(name);
            JSONObject frame = resObject.optJSONObject(FRAME);
            spriteFrame.setBox(frame.optInt(X), frame.optInt(Y), frame.optInt(WIDTH), frame.optInt(HEIGHT));

            JSONObject sourceSize = resObject.optJSONObject(SOURCE_SIZE);
            spriteFrame.setDim(sourceSize.optInt(WIDTH), sourceSize.optInt(HEIGHT));

            return spriteFrame;
        }
    }

    /**
     * Represents the source of sprite frames
     */
    private interface SpriteSource {
        void addSprite(Resources res, int resId);

        void addSprite(String name);

        void setRange(int start, int end);

        void onAnimate(int timeMs, Image target);

        int getSpriteFrameCount();

        void clear();
    }

    /**
     * For sprite animation consists of multiple independent sprite images.
     */
    private class SpriteImages implements SpriteSource {

        private final List<SpriteImage> mSpriteImages = new ArrayList<SpriteImage>();

        public void addSprite(Resources res, int resId) {
            SpriteImage spriteImage = new SpriteImage(res, resId);
            mSpriteImages.add(spriteImage);
        }

        public void addSprite(String name) {
            throw new Ngin3dException("Use addSprite(Resources res, int resId), not addSprite(String name).");
        }

        public void setRange(int start, int end) {
            throw new Ngin3dException("Use addSprite(Resources res, int resId), not setRange(int start, int end).");
        }

        public void onAnimate(int timeMs, Image target) {
            if (target == null) {
                return;
            }
            if (mSpriteImages.isEmpty()) {
                throw new Ngin3dException("Unspecified SpriteImages. Use addSprite(Resources res, int resId) to specify sprites");
            }
            int index = mSpriteImages.size() * timeMs / getDuration();
            if (index >= mSpriteImages.size()) {
                index = mSpriteImages.size() - 1;
            }
            if (index < 0) {
                index = 0;
            }
            target.setImageFromResource(mSpriteImages.get(index).getRes(), mSpriteImages.get(index).getResId());
        }

        public int getSpriteFrameCount() {
            return mSpriteImages.size();
        }

        public void clear() {
            mSpriteImages.clear();
        }
    }

    /**
     * Base class for sprite sheet implementation.
     */
    private abstract class SpriteSheetDisplay implements SpriteSource {

        protected List<SpriteFrame> mFrames = new ArrayList<SpriteFrame>();

        public void onAnimate(int timeMs, Image target) {
            if (target == null || mFrames.isEmpty()) {
                return;
            }

            int index = mFrames.size() * timeMs / getDuration();
            if (index >= mFrames.size()) {
                index = mFrames.size() - 1;
            }
            if (index < 0) {
                index = 0;
            }

            target.setImageFromResource(mSheet.mRes, mSheet.mImageId);
            target.setSourceRect(mFrames.get(index).getBox());
            target.setSize(mFrames.get(index).getDim());
        }

        public int getSpriteFrameCount() {
            return mFrames.size();
        }
    }

    /**
     * For sprite animation using a sprite sheet image and its corresponding info file.
     */
    private class ExplicitSheet extends SpriteSheetDisplay {

        protected List<String> mFrameNames;
        protected List<SpriteFrame> mAllFrames = new ArrayList<SpriteFrame>();

        public ExplicitSheet(SpriteSheet sheet) {
            mSheet = sheet;
        }

        public void loadAllSprites() {
            JSONObject frames = mSheet.getFrames();

            // load all sprite in the Sheet.
            mFrameNames = sortByName(frames);
            for (int i = 0; i < mFrameNames.size(); i++) {
                mAllFrames.add(mSheet.getSpriteFrameByName(mFrameNames.get(i)));
            }
        }

        private ArrayList<String> sortByName(JSONObject frames) {
            ArrayList<String> sortedNames = new ArrayList<String>();

            // sort the file names in json file.
            Iterator it = frames.keys();
            while (it.hasNext()) {
                String s = (String) it.next();
                sortedNames.add(s);
            }

            Collections.sort(sortedNames);
            return sortedNames;
        }

        public void addSprite(Resources res, int resId) {
            throw new Ngin3dException("Use addSprite(String name) to specify sprite in sheet or do nothing to run all sprites.");
        }

        public void addSprite(String name) {
            int index = mFrameNames.indexOf(name);
            mFrames.add(mAllFrames.get(index));
        }

        public void setRange(int start, int end) {
            if (end > mAllFrames.size()) {
                throw new Ngin3dException("Out of range. The range is between 0 and " + mAllFrames.size());
            }
            for (int i = start; i <= end; i++) {
                mFrames.add(mAllFrames.get(i));
            }
        }

        public void clear() {
            mFrames.clear();
        }
    }

    /**
     * For sprite animation using a sprite sheet image without any info file.
     */
    private class ImplicitSheet extends SpriteSheetDisplay {

        protected List<SpriteFrame> mAllFrames = new ArrayList<SpriteFrame>();

        public ImplicitSheet(Resources res, int imageId) {
            mSheet = new SpriteSheet(res, imageId, 0);
        }

        private void loadAllSprites(Resources res, int width, int height) {
            BitmapFactory.Options defaultOptions = new BitmapFactory.Options();
            defaultOptions.inJustDecodeBounds = true;
            defaultOptions.inSampleSize = 1;
            defaultOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            BitmapFactory.decodeResource(res, mSheet.mImageId, defaultOptions);
            int srcWidth = defaultOptions.outWidth - width;
            int srcHeight = defaultOptions.outHeight - height;

            for (int i = 0; i <= srcHeight; i += height) {
                for (int j = 0; j <= srcWidth; j += width) {
                    SpriteFrame spriteFrame = new SpriteFrame(mName);
                    spriteFrame.setBox(j, i, width, height);
                    spriteFrame.setDim(width, height);
                    mAllFrames.add(spriteFrame);
                }
            }

            mFrames.addAll(mAllFrames);
        }

        public void addSprite(Resources res, int resId) {
            throw new Ngin3dException("Specify range or do nothing to run all sprites.");
        }

        public void addSprite(String name) {
            throw new Ngin3dException("Specify range or do nothing to run all sprites.");
        }

        public void setRange(int start, int end) {
            mFrames.clear();
            for (int i = start; i <= end; i++) {
                mFrames.add(mAllFrames.get(i));
            }
        }

        public void clear() {
            mFrames.clear();
        }

        public int getFrameCount() {
            return mFrames.size();
        }
    }

    public void addSprite(Resources res, int resId) {
        mSpriteSource.addSprite(res, resId);
    }

    public void addSprite(String name) {
        mSpriteSource.addSprite(name);
    }

    public void setRange(int start, int end) {
        mSpriteSource.setRange(start, end);
    }

    public int getSpriteFrameCount() {
        return mSpriteSource.getSpriteFrameCount();
    }

    public void clear() {
        mSpriteSource.clear();
    }

    @Override
    @SuppressWarnings("BC_UNCONFIRMED_CAST")
    public final Animation setTarget(Actor target) {
        if (target instanceof Image) {
            mTarget = (Image) target;
            return this;
        }

        throw new Ngin3dException("Target must be of Image type.");
    }

    @Override
    public Actor getTarget() {
        return mTarget;
    }

    @Override
    public Animation start() {
        if (mTarget == null) {
            throw new Ngin3dException("Must specify target before starting animation.");
        }
        super.start();
        return this;
    }
}
