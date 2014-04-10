package com.mediatek.nfcdemo.nfc;

import android.util.Log;
import android.media.AudioManager;
import android.media.SoundPool;
import android.app.Application;
import android.content.Context;

import com.mediatek.nfcdemo.R;

public class NativeNfcManager {
    private static final String TAG = "NfcDemo";
    private static final boolean DBG = true;

    private byte [] mUid;
    private byte [] mResponse;
    private byte [] mHeader;
    private byte [] mDataPayload;    
    private byte [] mRawData;
    private boolean mReadUIDSuccess;
    private Callback mCallback;
    private int mErrorCode;
    private int mLength;
    
    static {
        try {
            System.loadLibrary("nfcdemo");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "NfcDemo library not found!");
        }
    }

    // for use with playSound()
    public static final int SOUND_START = 0;
    public static final int SOUND_END = 1;
    public static final int SOUND_ERROR = 2;

    int mStartSound;
    int mEndSound;
    int mErrorSound;
    SoundPool mSoundPool; // playback synchronized on this

    public NativeNfcManager(){
    }

    private native int doInitialize();
    private native int doDeinitialize();
    private native int doSinglePolling(int type);
    private native int doPollingLoop(int type);
    private native int doStopPollingLoop();
    private native int doPollingInfo();
    private native int doCardMode(int type);
    private native int doStopCardMode();
    private native int doWriteSwpReg();
    private native int doSingleDeviceDetect(int type);
    private native int doSetReceiveOnlyMode();
    private native int doDataExchange(int type, byte headerCounter, int dataLength, byte [] data);

    public int initialize() {
        if (DBG) Log.d(TAG, "initialize");  
        mUid = new byte[64];
        return doInitialize();
    }
    
    public int deinitialize() {
        if (DBG) Log.d(TAG, "deinitialize");
        mUid = null;
        mResponse = null;
        mHeader = null;
        mDataPayload = null;
        mRawData = null;
        return doDeinitialize();
    }

    //----- reader mode ------
    public int singlePolling(int type) {
        if (DBG) Log.d(TAG, "singlePolling, type: " + type);
        return doSinglePolling(type);
    }

    public int pollingLoop(int type, Callback callback) {
        if (DBG) Log.d(TAG, "pollingLoop, type: " + type);
        mCallback = callback;
        return doPollingLoop(type);
    }

    public int stopPollingLoop(){
        if (DBG) Log.d(TAG, "stop pollingLoop");
        return doStopPollingLoop();
    }

    public int getPollingInfo(){
        if (DBG) Log.d(TAG, "get PollingInfo");
        mResponse = new byte[50];
        return doPollingInfo();
    }

    //---- card mode -------
    public int cardMode(int type) {
        if (DBG) Log.d(TAG, "cardMode, type: " + type);
        return doCardMode(type);
    }

    public int stopCardMode() {
        if (DBG) Log.d(TAG, "Stop cardMode");
        return doStopCardMode();
    }

    //---- p2p mode -----
    public int singleDeviceDetect(int type) {
        if (DBG) Log.d(TAG, "SDD");
        return doSingleDeviceDetect(type);
    }

    public int setReceiveOnlyMode() {
        if (DBG) Log.d(TAG, "Read only mode");
        return doSetReceiveOnlyMode();
    }

    public int dataExchange(int type, byte headerCounter, int dataLength, byte [] data) {
        if (DBG) Log.d(TAG, "Data Exchange"); 
        return doDataExchange(type, headerCounter, dataLength, data);
    }
   
    //---- Write SWP Reg
    public int writeSwpReg() {
        if (DBG) Log.d(TAG, "writeSwpReg");
        return doWriteSwpReg();
    }
    
    public byte[] getUid() {
        return mUid;
    }

    public byte[] getResponse() {
        return mResponse;
    }

    public int getLength() {
        return mLength;
    }

    public byte[] getDepHeader() {
        return mHeader;
    }

    public byte[] getDepDataPayload() {
        return mDataPayload;
    }

    public byte[] getRawData() {
        return mRawData;
    }

    public int getErrorCode() {
        return mErrorCode;
    }
    
    public boolean readUidSuccess() {
        return mReadUIDSuccess;
    }

    public void detectCard(){
        mCallback.onDetectCard();
    }
    
    public interface Callback {
        public void onDetectCard();
    }

    public void initSoundPool(Context context) {
        synchronized(this) {
            if (mSoundPool == null) {
                mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
                mStartSound = mSoundPool.load(context, R.raw.start, 1);
                mEndSound = mSoundPool.load(context, R.raw.end, 1);
                mErrorSound = mSoundPool.load(context, R.raw.error, 1);
            }
        }
    }

    public void releaseSoundPool() {
        synchronized(this) {
            if (mSoundPool != null) {
                mSoundPool.release();
                mSoundPool = null;
            }
        }
    }

    public void playSound(int sound) {
        synchronized (this) {
            if (mSoundPool == null) {
                Log.w(TAG, "Not playing sound when NFC is disabled");
                return;
            }
            switch (sound) {
                case SOUND_START:
                    mSoundPool.play(mStartSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_END:
                    mSoundPool.play(mEndSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
                case SOUND_ERROR:
                    mSoundPool.play(mErrorSound, 1.0f, 1.0f, 0, 0, 1.0f);
                    break;
            }
        }
    }

}
