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


package android.filterfw.core;

import android.filterfw.core.FrameFormat;
import android.filterfw.core.FrameManager;
import android.graphics.Bitmap;
import android.util.Log;

import java.nio.ByteBuffer;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Closeable;
import java.io.File;

import android.os.SystemClock;

import android.opengl.GLES20;
import java.nio.ByteBuffer;

/**
 * @hide
 */
public abstract class Frame {

    public final static int NO_BINDING = 0;

    public final static long TIMESTAMP_NOT_SET = -2;
    public final static long TIMESTAMP_UNKNOWN = -1;

    private FrameFormat mFormat;
    private FrameManager mFrameManager;
    private boolean mReadOnly = false;
    private boolean mReusable = false;
    private int mRefCount = 1;
    private int mBindingType = NO_BINDING;
    private long mBindingId = 0;
    private long mTimestamp = TIMESTAMP_NOT_SET;

    Frame(FrameFormat format, FrameManager frameManager) {
        mFormat = format.mutableCopy();
        mFrameManager = frameManager;
    }

    Frame(FrameFormat format, FrameManager frameManager, int bindingType, long bindingId) {
        mFormat = format.mutableCopy();
        mFrameManager = frameManager;
        mBindingType = bindingType;
        mBindingId = bindingId;
    }

    public FrameFormat getFormat() {
        return mFormat;
    }

    public int getCapacity() {
        return getFormat().getSize();
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public int getBindingType() {
        return mBindingType;
    }

    public long getBindingId() {
        return mBindingId;
    }

    public void setObjectValue(Object object) {
        assertFrameMutable();

        // Attempt to set the value using a specific setter (which may be more optimized), and
        // fall back to the setGenericObjectValue(...) in case of no match.
        if (object instanceof int[]) {
            setInts((int[])object);
        } else if (object instanceof float[]) {
            setFloats((float[])object);
        } else if (object instanceof ByteBuffer) {
            setData((ByteBuffer)object);
        } else if (object instanceof Bitmap) {
            setBitmap((Bitmap)object);
        } else {
            setGenericObjectValue(object);
        }
    }

    public abstract Object getObjectValue();

    public abstract void setInts(int[] ints);

    public abstract int[] getInts();

    public abstract void setFloats(float[] floats);

    public abstract float[] getFloats();

    public abstract void setData(ByteBuffer buffer, int offset, int length);

    public void setData(ByteBuffer buffer) {
        setData(buffer, 0, buffer.limit());
    }

    public void setData(byte[] bytes, int offset, int length) {
        setData(ByteBuffer.wrap(bytes, offset, length));
    }

    public abstract ByteBuffer getData();

    public abstract void setBitmap(Bitmap bitmap);

    public abstract Bitmap getBitmap();

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setDataFromFrame(Frame frame) {
        setData(frame.getData());
    }

    protected boolean requestResize(int[] newDimensions) {
        return false;
    }

    public int getRefCount() {
        return mRefCount;
    }

    public Frame release() {
        if (mFrameManager != null) {
            return mFrameManager.releaseFrame(this);
        } else {
            return this;
        }
    }

    public Frame retain() {
        if (mFrameManager != null) {
            return mFrameManager.retainFrame(this);
        } else {
            return this;
        }
    }

    public FrameManager getFrameManager() {
        return mFrameManager;
    }

    protected void assertFrameMutable() {
        if (isReadOnly()) {
            throw new RuntimeException("Attempting to modify read-only frame!");
        }
    }

    protected void setReusable(boolean reusable) {
        mReusable = reusable;
    }

    protected void setFormat(FrameFormat format) {
        mFormat = format.mutableCopy();
    }

    protected void setGenericObjectValue(Object value) {
        throw new RuntimeException(
            "Cannot set object value of unsupported type: " + value.getClass());
    }

    protected static Bitmap convertBitmapToRGBA(Bitmap bitmap) {
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            return bitmap;
        } else {
            Bitmap result = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            if (result == null) {
                throw new RuntimeException("Error converting bitmap to RGBA!");
            } else if (result.getRowBytes() != result.getWidth() * 4) {
                throw new RuntimeException("Unsupported row byte count in bitmap!");
            }
            return result;
        }
    }

	public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }
	
	private static final int BUFSIZE = 4096;
	public void saveFrame(String name)
	{
		File file = new File("/sdcard/output/"+SystemClock.uptimeMillis()+"_"+name+".png");
		Uri uri = Uri.fromFile(file);

		Log.i("Frame", "save frame "+name+" path=" + file.getPath());
		
		Bitmap bitmap = getBitmap();

		FileOutputStream f = null;
        BufferedOutputStream b = null;
        DataOutputStream d = null;
        try {
            f = new FileOutputStream(file);
            b = new BufferedOutputStream(f, BUFSIZE);
            d = new DataOutputStream(b);
            d.writeUTF(uri.toString());
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, d);
            d.close();
        } catch (IOException e) {
            Log.e("Frame", "Fail to store bitmap. path=" + file.getPath(), e);
        } finally {
            closeSilently(f);
            closeSilently(b);
            closeSilently(d);
        }
	}

	public void wait3DReady()
	{
	    int w = 1;
		int h = 1;
		ByteBuffer buffer = ByteBuffer.allocate(w * h * 4);
		GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
	}
	
    protected void reset(FrameFormat newFormat) {
        mFormat = newFormat.mutableCopy();
        mReadOnly = false;
        mRefCount = 1;
    }

    /**
     * Called just before a frame is stored, such as when storing to a cache or context.
     */
    protected void onFrameStore() {
    }

    /**
     * Called when a frame is fetched from an internal store such as a cache.
     */
    protected void onFrameFetch() {
    }

    // Core internal methods ///////////////////////////////////////////////////////////////////////
    protected abstract boolean hasNativeAllocation();

    protected abstract void releaseNativeAllocation();

    final int incRefCount() {
        ++mRefCount;
        return mRefCount;
    }

    final int decRefCount() {
        --mRefCount;
        return mRefCount;
    }

    final boolean isReusable() {
        return mReusable;
    }

    final void markReadOnly() {
        mReadOnly = true;
    }

}
