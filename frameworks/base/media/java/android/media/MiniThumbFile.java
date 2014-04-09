/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.media;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.zip.Adler32;

import com.mediatek.xlog.Xlog;

/**
 * This class handles the mini-thumb file. A mini-thumb file consists
 * of blocks, indexed by id. Each block has BYTES_PER_MINTHUMB bytes in the
 * following format:
 *
 * 1 byte status (0 = empty, 1 = mini-thumb available)
 * 8 bytes magic (a magic number to match what's in the database)
 * 4 bytes data length (LEN)
 * LEN bytes jpeg data
 * (the remaining bytes are unused)
 *
 * @hide This file is shared between MediaStore and MediaProvider and should remained internal use
 *       only.
 */
public class MiniThumbFile {
    private static final String TAG = "MiniThumbFile";
    private static final boolean LOG = true;
    
    // M: add 1 on google's version.
    // this version add check code for thumbdata.
    // 2: for cache mechanism enhancement
    private static final int MINI_THUMB_DATA_FILE_VERSION = 3 + 2;
    public static final int BYTES_PER_MINTHUMB = 17000;//10000;
    private static final int HEADER_SIZE = 1 + 8 + 4 + 8;
    private Uri mUri;
    private final HashMap<Long, RandomAccessFile> mMiniThumbFiles = new HashMap<Long, RandomAccessFile>();
    private final HashMap<Long, FileChannel> mChannels = new HashMap<Long, FileChannel>();
    private ByteBuffer mBuffer;
    private static final Hashtable<String, MiniThumbFile> sThumbFiles =
        new Hashtable<String, MiniThumbFile>();
    private Adler32 mChecker = new Adler32();
    private static final long UNKNOWN_CHECK_CODE = -1;
    /**
     * We store different types of thumbnails in different files. To remain backward compatibility,
     * we should hashcode of content://media/external/images/media remains the same.
     */
    public static synchronized void reset() {
        for (MiniThumbFile file : sThumbFiles.values()) {
            file.deactivate();
        }
        sThumbFiles.clear();
    }

    public static synchronized MiniThumbFile instance(Uri uri) {
        String type = uri.getPathSegments().get(1);
        MiniThumbFile file = sThumbFiles.get(type);
        // Log.v(TAG, "get minithumbfile for type: "+type);
        if (file == null) {
            file = new MiniThumbFile(
                    Uri.parse("content://media/external/" + type + "/media"));
            sThumbFiles.put(type, file);
        }

        return file;
    }

    private String randomAccessFilePath(int version) {
        String directoryName =
                Environment.getExternalStorageDirectory().toString()
                + "/DCIM/.thumbnails";
        return directoryName + "/.thumbdata" + version + "-" + mUri.hashCode();
    }

    private void removeOldFile() {
        String oldPath = randomAccessFilePath(MINI_THUMB_DATA_FILE_VERSION - 1);
        File oldFile = new File(oldPath);
        if (oldFile.exists()) {
            try {
                oldFile.delete();
            } catch (SecurityException ex) {
                Xlog.e(TAG, "removeOldFile: SecurityException! path=" + oldPath, ex);
            }
        }
    }

    private RandomAccessFile miniThumbDataFile(long id) {
        long fileindex = getFileIndex(id);
        RandomAccessFile miniThumbFile = mMiniThumbFiles.get(fileindex);
        if (miniThumbFile == null) {
            removeOldFile(id);
            String path = randomAccessFilePath(MINI_THUMB_DATA_FILE_VERSION, id);
            File directory = new File(path).getParentFile();
            if (!directory.isDirectory()) {
                if (!directory.mkdirs()) {
                    Log.e(TAG, "Unable to create .thumbnails directory "
                            + directory.toString());
                }
            }
            File f = new File(path);
            try {
                miniThumbFile = new RandomAccessFile(f, "rw");
            } catch (IOException ex) {
                // Open as read-only so we can at least read the existing
                // thumbnails.
                Xlog.e(TAG, "miniThumbDataFile: IOException! path=" + path, ex);
                try {
                    miniThumbFile = new RandomAccessFile(f, "r");
                } catch (IOException ex2) {
                    Xlog.e(TAG, "miniThumbDataFile: IOException(r)! path=" + path, ex2);
                }
            }
            if (miniThumbFile != null) {
                FileChannel channel = miniThumbFile.getChannel();
                mMiniThumbFiles.put(fileindex, miniThumbFile);
                mChannels.put(fileindex, channel);
            }
        }
        return mMiniThumbFiles.get(fileindex);
    }

    public MiniThumbFile(Uri uri) {
        mUri = uri;
        mBuffer = ByteBuffer.allocateDirect(BYTES_PER_MINTHUMB);
    }

    public synchronized void deactivate() {
        if (LOG) Xlog.v(TAG, "deactivate() begin");
        for(Long key : mMiniThumbFiles.keySet()) {
            RandomAccessFile miniThumbFile = mMiniThumbFiles.get(key);
            if (miniThumbFile != null) {
                try {
                    miniThumbFile.close();
                    miniThumbFile = null;
                } catch (IOException ex) {
                    Xlog.e(TAG, "deactivate: IOException!", ex);
                }
            }
        }
        mMiniThumbFiles.clear();
        mChannels.clear();
        if (LOG) Xlog.v(TAG, "deactivate() end");
    }

    // Get the magic number for the specified id in the mini-thumb file.
    // Returns 0 if the magic is not available.
    public synchronized long getMagic(long id) {
        // check the mini thumb file for the right data.  Right is
        // defined as having the right magic number at the offset
        // reserved for this "id".
        RandomAccessFile r = miniThumbDataFile(id);
        if (r != null) {
            long pos = getFilePos(id);
            FileLock lock = null;
            try {
                mBuffer.clear();
                mBuffer.limit(1 + 8);
                
                FileChannel channel = getFileChannel(id);
                lock = channel.lock(pos, 1 + 8, true);
                // check that we can read the following 9 bytes
                // (1 for the "status" and 8 for the long)
                if (channel.read(mBuffer, pos) == 9) {
                    mBuffer.position(0);
                    if (mBuffer.get() == 1) {
                        return mBuffer.getLong();
                    }
                }
            } catch (IOException ex) {
                Log.v(TAG, "Got exception checking file magic: ", ex);
            } catch (RuntimeException ex) {
                // Other NIO related exception like disk full, read only channel..etc
                Log.e(TAG, "Got exception when reading magic, id = " + id +
                        ", disk full or mount read-only? " + ex.getClass());
            } finally {
                try {
                    if (lock != null) lock.release();
                }
                catch (IOException ex) {
                    Xlog.e(TAG, "getMagic: can not release lock!", ex);
                }
            }
        }
        return 0;
    }

    public synchronized void saveMiniThumbToFile(byte[] data, long id, long magic)
            throws IOException {
        RandomAccessFile r = miniThumbDataFile(id);
        if (r == null) return;

        long pos = getFilePos(id);
        FileLock lock = null;
        try {
            if (data != null) {
                if (data.length > BYTES_PER_MINTHUMB - HEADER_SIZE) {
                    // not enough space to store it.
                    return;
                }
                mBuffer.clear();
                mBuffer.put((byte) 1);
                mBuffer.putLong(magic);
                mBuffer.putInt(data.length);
                
                /// M: add check code for data
                long check = UNKNOWN_CHECK_CODE;
                synchronized(mChecker) {
                    mChecker.reset();
                    mChecker.update(data);
                    check = mChecker.getValue();
                }
                mBuffer.putLong(check);
                if (LOG) Xlog.v(TAG, "saveMiniThumbToFile(" + id + ") flag=1, magic="
                        + magic + ", length=" + data.length + ", check=" + check);
                
                mBuffer.put(data);
                mBuffer.flip();
                FileChannel channel = getFileChannel(id);
                lock = channel.lock(pos, BYTES_PER_MINTHUMB, false);
                channel.write(mBuffer, pos);
            }
        } catch (IOException ex) {
            Log.e(TAG, "couldn't save mini thumbnail data for "
                    + id + "; ", ex);
            throw ex;
        } catch (RuntimeException ex) {
            // Other NIO related exception like disk full, read only channel..etc
            Log.e(TAG, "couldn't save mini thumbnail data for "
                    + id + "; disk full or mount read-only? " + ex.getClass());
        } finally {
            try {
                if (lock != null) lock.release();
            }
            catch (IOException ex) {
                Xlog.e(TAG, "saveMiniThumbToFile: can not release lock!", ex);
            }
        }
    }
    /**
     * Gallery app can use this method to retrieve mini-thumbnail. Full size
     * images share the same IDs with their corresponding thumbnails.
     *
     * <br/>Now, it calls getMiniThumbFromFile(id, data, null) to get mini thumbnail. 
     * @param id the ID of the image (same of full size image).
     * @param data the buffer to store mini-thumbnail.
     * @see MiniThumbFile#getMiniThumbFromFile(long, byte[], ThumbResult)
     * @deprecated for no check code result to be returned
     */
    public synchronized byte[] getMiniThumbFromFile(long id, byte [] data) {
        return getMiniThumbFromFile(id, data, null);
    }

    /**
     * Gallery app can use this method to retrieve mini-thumbnail. Full size
     * images share the same IDs with their corresponding thumbnails.
     * 
     * If check code of read data is different from written,
     * null will be return instead of returning wrong data.
     * If inputed result is not null, 
     * result.getDetail() will return right detail)
     * @param id
     * @param data
     * @param result output the detail info for get mini thumb from file.
     * @return
     */
    public synchronized byte[] getMiniThumbFromFile(long id, byte [] data, ThumbResult result) {
        RandomAccessFile r = miniThumbDataFile(id);
        if (r == null) return null;

        long pos = getFilePos(id);
        FileLock lock = null;
        try {
            mBuffer.clear();
            FileChannel channel = getFileChannel(id);
            lock = channel.lock(pos, BYTES_PER_MINTHUMB, true);
            int size = channel.read(mBuffer, pos);
            if (size > 1 + 8 + 4 + 8) { // flag, magic, length, check code
                mBuffer.position(0);
                byte flag = mBuffer.get();
                long magic = mBuffer.getLong();
                int length = mBuffer.getInt();
                long check = mBuffer.getLong();
                if (LOG) Xlog.v(TAG, "getMiniThumbFromFile(" + id + ") flag=" + flag
                        + ", magic=" + magic + ", length=" + length + ", check=" + check);
                
                long newCheck = UNKNOWN_CHECK_CODE;
                if (size >= 1 + 8 + 4 + 8 + length && data.length >= length) {
                    mBuffer.get(data, 0, length);
                    synchronized (mChecker) {
                        mChecker.reset();
                        mChecker.update(data, 0, length);
                        newCheck = mChecker.getValue();
                    }
                    if (newCheck != UNKNOWN_CHECK_CODE && newCheck == check) {
                        if (result != null) {
                            result.setDetail(ThumbResult.SUCCESS);
                        }
                        return data;
                    } else {
                        if (result != null) {
                            result.setDetail(ThumbResult.WRONG_CHECK_CODE);
                        }
                        Xlog.w(TAG, "getMiniThumbFromFile(" + id + ") wrong check code! newCheck="
                                + newCheck + ", oldCheck=" + check);
                    }
                }
            }
        } catch (IOException ex) {
            Log.w(TAG, "got exception when reading thumbnail id=" + id + ", exception: " + ex);
        } catch (RuntimeException ex) {
            // Other NIO related exception like disk full, read only channel..etc
            Log.e(TAG, "Got exception when reading thumbnail, id = " + id +
                    ", disk full or mount read-only? " + ex.getClass());
        } finally {
            try {
                if (lock != null) lock.release();
            }
            catch (IOException ex) {
                Xlog.e(TAG, "getMiniThumbFromFile: can not release lock!", ex);
            }
        }
        if (LOG) Xlog.v(TAG, "getMiniThumbFromFile(" + id + ") return null.");
        return null;
    }

    /**
     * M: Get the real path for thumbdata.
     * @param uri the Uri same as instance(Uri uri).
     * @return
     */
    public static String getThumbdataPath(Uri uri) {
        String type = uri.getPathSegments().get(1);
        Uri thumbFileUri = Uri.parse("content://media/external/" + type + "/media");
        String directoryName = Environment.getExternalStorageDirectory().toString()
            + "/DCIM/.thumbnails";
        String path = directoryName + "/.thumbdata" + MINI_THUMB_DATA_FILE_VERSION + "-" + thumbFileUri.hashCode();
        if (LOG) Xlog.v(TAG, "getThumbdataPath(" + uri + ") return " + path);
        return path;
    }

    /**
     * M: create or get thumb result for more detail.
     */
    public static class ThumbResult {
        /**
         * unspecified
         */
        public static final int UNSPECIFIED = 0;
        /**
         * check code is not right.
         */
        public static final int WRONG_CHECK_CODE = 1;
        /**
         * success.
         */
        public static final int SUCCESS = 2;
        
        private int mDetail = UNSPECIFIED;
        
        /*package*/ void setDetail(int detail) {
            mDetail = detail;
        }
        
        /**
         * @return return the detail for process the thumbnail.
         */
        public int getDetail() {
            return mDetail;
        }
    }

    /// M: for new cache mechanism enhancement
    //Note: we should use new remove mechanism after 5
    private long MAX_THUMB_COUNT = 5000;
    private long getFileIndex(long id) {
        long fileindex = id / MAX_THUMB_COUNT;
        return fileindex;
    }

    private FileChannel getFileChannel(long id) {
        return mChannels.get(getFileIndex(id));
    }

    private long getFilePos(long id) {
        long pos = (id % MAX_THUMB_COUNT) * BYTES_PER_MINTHUMB;
        return pos;
    }

    private String randomAccessFilePath(int version, long id) {
        long fileindex = getFileIndex(id);
        String directoryName =
                Environment.getExternalStorageDirectory().toString()
                + "/DCIM/.thumbnails";
        return directoryName + "/.thumbdata" + version + "-" + mUri.hashCode() + "_" + fileindex;
    }

    private void removeOldFile(long id) {
        if (LOG) Xlog.v(TAG, "removeOldFile(" + id + ")");
        removeThumbdataFile(MINI_THUMB_DATA_FILE_VERSION - 1, id);
    }

    private void removeThumbdataFile(int version, long id) {
        String oldPath = randomAccessFilePath(version, id);
        File oldFile = new File(oldPath);
        if (oldFile.exists()) {
            try {
                oldFile.delete();
            } catch (SecurityException ex) {
                Xlog.e(TAG, "removeThumbdataFile: SecurityException! path=" + oldPath, ex);
            }
        }
        if (LOG) Xlog.v(TAG, "removeThumbdataFile(" + version + ", " + id + ")");
    }
}
