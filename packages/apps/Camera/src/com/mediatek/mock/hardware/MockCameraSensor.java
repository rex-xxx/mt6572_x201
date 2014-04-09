package com.mediatek.mock.hardware;

import android.hardware.Camera.CameraInfo;

import com.android.gallery3d.app.Log;

public class MockCameraSensor {

    private static final String TAG = "MockCameraSensor";
    private static final String BACKPARA = "ae-mode=1;ae-mode-values=1,2,3,4,5,6,7,8,9,10,11,13,14,15,16,17,18,19,20,21," +
            "22;af-draw=0;af-x=0;af-y=0;afeng-mode=0;afeng-pos=0;antibanding=auto;antibanding-values=60hz,50hz,auto,off;" +
            "auto-exposure-lock-supported=true;auto-whitebalance-lock-supported=true;awb-2pass=on;awb-2pass-values=off,on;" +
            "brightness=middle;brightness-values=low,middle,high;burst-num=1;mtk-cam-mode=0;cap-mode=normal;cap-mode-values=" +
            "normal,continuousshot,smileshot,bestshot,evbracketshot,mav,hdr,autorama,asd,pano_3d,single_3d,face_beauty;" +
            "capfname=/sdcard/DCIM/cap00;contrast=middle;contrast-values=low,middle,high;edge=middle;edge-values=low," +
            "middle,high;effect=none;effect-values=none,mono,sepia,negative,aqua,blackboard,whiteboard;exposure=0;" +
            "exposure-compensation=0;exposure-compensation-step=1.0;exposure-meter=center;exposure-meter-values=center," +
            "spot,average;exposure-values=0,1,2,3,-1,-2,-3;fast-continuous-shot=off;fast-continuous-shot-values=off,on;" +
            "focal-length=3.5;focus-distances=0.95,1.9,Infinity;focus-meter=spot;focus-meter-values=spot,multi;" +
            "focus-mode=auto;focus-mode-values=auto,continuous-picture,continuous-video,macro,infinity,manual,fullscan;" +
            "horizontal-view-angle=360;hue=middle;hue-values=low,middle,high;iso-speed=auto;iso-speed-eng=0;" +
            "iso-speed-values=auto,100,200,400,800,1600;isp-mode=0;jpeg-quality=85;jpeg-thumbnail-height=120;" +
            "jpeg-thumbnail-quality=85;jpeg-thumbnail-size-values=0x0,160x120,320x240;jpeg-thumbnail-width=160;" +
            "max-exposure-compensation=3;max-num-detected-faces-hw=15;max-num-detected-faces-sw=0;max-num-focus-areas=1;" +
            "max-num-metering-areas=1;max-zoom=10;min-exposure-compensation=-3;pano-dir=right;pano-idx=0;" +
            "picture-format=jpeg;picture-format-values=jpeg;picture-size=2560x1920;picture-size-values=640x480,1280x768," +
            "1280x960,1600x1200,2048x1536,2560x1712,2560x1920;preview-format=yuv420sp;preview-format-values=yuv420sp,yuv420p," +
            "yuv420i-yyuvyy-3plane;preview-fps-range=5000,30000;preview-fps-range-values=(5000,30000);" +
            "preview-frame-rate=30;preview-frame-rate-values=15,24,30;preview-size=640x480;preview-size-values=176x144," +
            "320x240,352x288,432x320,480x320,480x368,640x480,720x480,728x480,782x480,800x480,854x480,800x600,864x480,888x540,960x540,1280x720,1920x1088;" +
            "prv-int-fmt=yuv420i-yyuvyy-3plane;rawfname=/sdcard/DCIM/raw00.raw;rawsave-mode=0;rotation=0;" +
            "saturation=middle;saturation-values=low,middle,high;scene-mode=auto;scene-mode-values=auto,portrait," +
            "landscape,night,night-portrait,theatre,beach,snow,sunset,steadyphoto,fireworks,sports,party,candlelight;" +
            "smooth-zoom-supported=false;stereo3d-image-format=jps;stereo3d-image-format-values=jps,mpo;stereo3d-mode=off;" +
            "stereo3d-mode-values=off,on;stereo3d-picture-size=2560x720;stereo3d-picture-size-values=2560x720;" +
            "stereo3d-preview-size=640x360;stereo3d-preview-size-values=640x360,854x480,960x540,1280x720;" +
            "stereo3d-type=off;stereo3d-type-values=off;tv-delay=240;vertical-view-angle=360;video-stabilization=false;" +
            "video-stabilization-supported=true;video-stabilization-values=false,true;whitebalance=auto;" +
            "whitebalance-values=auto,daylight,cloudy-daylight,shade,twilight,fluorescent,warm-fluorescent,incandescent;" +
            "zoom=0;zoom-ratios=100,114,132,151,174,200,229,263,303,348,400;zoom-supported=true;zsd-mode=on;" +
            "zsd-mode-values=off,on;zsd-supported=true";
    private static final String FRONTPARA = "ae-mode=1;ae-mode-values=1;af-draw=0;af-x=0;af-y=0;afeng-mode=0;afeng-pos=0;" +
            "aflamp-mode=off;aflamp-mode-values=off;antibanding=50hz;antibanding-values=60hz,50hz;auto-exposure-lock=" +
            "false;auto-exposure-lock-supported=true;auto-whitebalance-lock=false;auto-whitebalance-lock-supported=true;" +
            "awb-2pass=on;awb-2pass-values=off,on;burst-num=1;mtk-cam-mode=0;cap-mode=normal;cap-mode-values=normal;capfname=" +
            "/sdcard/DCIM/cap00;effect=none;effect-values=none,mono,sepia,negative,sepiagreen,sepiablue;exposure=0;" +
            "exposure-compensation=0;exposure-compensation-step=1.0;exposure-values=0,0.3,0.7,1,1.3,-0.3,-0.7,-1,-1.3;" +
            "fast-continuous-shot=off;fast-continuous-shot-values=off,on;focal-length=3.5;focus-distances=0.95,1.9," +
            "Infinity;focus-mode=infinity;focus-mode-values=infinity;horizontal-view-angle=360;iso-speed=auto;" +
            "iso-speed-eng=0;iso-speed-values=auto;isp-mode=0;jpeg-quality=85;jpeg-thumbnail-height=120;" +
            "jpeg-thumbnail-quality=85;jpeg-thumbnail-size-values=0x0,160x120,320x240;jpeg-thumbnail-width=160;" +
            "max-exposure-compensation=1;max-num-detected-faces-hw=15;max-num-detected-faces-sw=0;max-num-focus-areas=0;" +
            "max-num-metering-areas=0;max-zoom=10;min-exposure-compensation=-1;pano-dir=right;pano-idx=0;" +
            "picture-format=jpeg;picture-format-values=jpeg;picture-size=640x480;picture-size-values=320x240,640x480,640x384,1440x960;" +
            "preview-format=yuv420sp;preview-format-values=yuv420sp,yuv420p,yuv420i-yyuvyy-3plane;preview-fps-range=" +
            "5000,30000;preview-fps-range-values=(5000,30000);preview-frame-rate=20;preview-frame-rate-values=10,20;" +
            "preview-size=640x480;preview-size-values=176x144,320x240,352x288,432x320,480x320,640x480,640x384,728x480,782x480,888x540;prv-int-fmt=" +
            "yuv420i-yyuvyy-3plane;rawfname=/sdcard/DCIM/raw00.raw;rawsave-mode=0;recording-hint=false;rotation=0;" +
            "scene-mode=auto;scene-mode-values=auto,night;smooth-zoom-supported=false;stereo3d-type=off;" +
            "stereo3d-type-values=off;tv-delay=240;vertical-view-angle=360;video-stabilization=false;" +
            "video-stabilization-supported=false;video-stabilization-values=false;whitebalance=auto;" +
            "whitebalance-values=auto,daylight,cloudy-daylight,fluorescent,incandescent,tungsten;zoom=0;" +
            "zoom-ratios=100,114,132,151,174,200,229,263,303,348,400;zoom-supported=true;zsd-mode=off;zsd-mode-values=" +
            "off,off;zsd-supported=false";

    private static final CameraInfo[] CAMERA_INFO = new CameraInfo[2];
    static {
        CAMERA_INFO[0] = new CameraInfo();
        CAMERA_INFO[0].facing = CameraInfo.CAMERA_FACING_BACK;
        CAMERA_INFO[0].orientation = 90;
        CAMERA_INFO[1] = new CameraInfo();
        CAMERA_INFO[1].facing = CameraInfo.CAMERA_FACING_FRONT;
        CAMERA_INFO[1].orientation = 90;
    }

    private final String mSensorType;
    private int mId;
    private byte[] mBuff;

    public MockCameraSensor(int id) {
        Log.i(TAG, "Constructor");
        mId = id;
        if (mId != CameraInfo.CAMERA_FACING_FRONT
                && mId != CameraInfo.CAMERA_FACING_BACK) {
            mSensorType = "UnKnown";
            throw new RuntimeException();
        } else if (mId == CameraInfo.CAMERA_FACING_BACK) {
            mSensorType = "OVxxxx";
        } else {
            mSensorType = "...xxx";
        }
        init();
    }
    
    private void init() {
        mBuff = new byte[100];
    }

    public byte[] requestBuff() {
        return mBuff;
    }

    public int getId() {
        return mId;
    }

    public String getSensorType() {
        return mSensorType;
    }

    public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        cameraInfo.facing =  CAMERA_INFO[cameraId].facing;
        cameraInfo.orientation = CAMERA_INFO[cameraId].orientation;
    }

    public String defaultParameters() {
        Log.i(TAG, "get default Parameters, mId = " + mId);
        if (mId == CameraInfo.CAMERA_FACING_BACK) {
            return BACKPARA;
        } else {
            return FRONTPARA;
        }
    }

    public void open() {
        
    }

    public void close() {
        
    }
}
