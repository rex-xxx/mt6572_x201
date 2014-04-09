/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "BootAnimation"

#include <stdint.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>

#include <cutils/properties.h>

#include <androidfw/AssetManager.h> //Jelly Bean added
#include <binder/IPCThreadState.h>
#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/threads.h>

#include <ui/PixelFormat.h>
#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>
#include <ui/FramebufferNativeWindow.h>

#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>
#include <private/gui/LayerState.h>

#include <core/SkBitmap.h>
#include <core/SkStream.h>
#include <images/SkImageDecoder.h>

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>

#include <media/mediaplayer.h>
#include <media/MediaPlayerInterface.h>
#include <system/audio.h>

#include "BootAnimation.h"

#define USER_BOOTANIMATION_FILE "/data/local/bootanimation.zip"
#define SYSTEM_BOOTANIMATION_FILE "/system/media/bootanimation.zip"
#define SYSTEM_ENCRYPTED_BOOTANIMATION_FILE "/system/media/bootanimation-encrypted.zip"
#define EXIT_PROP_NAME "service.bootanim.exit"

extern "C" int clock_nanosleep(clockid_t clock_id, int flags,
                           const struct timespec *request,
                           struct timespec *remain);

namespace android {

// ---------------------------------------------------------------------------

BootAnimation::BootAnimation() : Thread(false)
{
	XLOGD("[BootAnimation %s %s %d]",__FILE__,__FUNCTION__,__LINE__);

    mSession = new SurfaceComposerClient();
}

BootAnimation::BootAnimation(bool bSetBootOrShutDown, bool bSetPlayMP3,bool bSetRotated) : Thread(false)
{
	XLOGD("[BootAnimation %s %s %d]",__FILE__,__FUNCTION__,__LINE__);

	mSession = new SurfaceComposerClient();
	//force portrait for both boot up and shut down  ALPS00120158
//	mSession->setOrientation(0, ISurfaceComposer::eOrientationDefault,0);

	bBootOrShutDown = bSetBootOrShutDown;
	bShutRotate = bSetRotated;
	bPlayMP3 = bSetPlayMP3;
	mBackgroundLength = 0;
	XLOGD("[BootAnimation %s %d]bBootOrShutDown=%d,bPlayMP3=%d,bShutRotate=%d",__FUNCTION__,__LINE__,bBootOrShutDown,bPlayMP3,bShutRotate);
}

BootAnimation::~BootAnimation() {
	XLOGD("[BootAnimation %s %d]",__FUNCTION__,__LINE__);	
        if (mBackgroundLength) {
            XLOGD("mBackgroundLength: %d", mBackgroundLength);
            delete[] glTextureBackground;
        }
}

void BootAnimation::onFirstRef() {
	XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);

    status_t err = mSession->linkToComposerDeath(this);
    if(err != 0){
    ALOGE_IF(err, "linkToComposerDeath failed (%s) ", strerror(-err)); //Jelly Bean changed
    }
    if (err == NO_ERROR) {
        run("BootAnimation", PRIORITY_DISPLAY);
    }
	XLOGD("[BootAnimation %s %d]end",__FUNCTION__,__LINE__);
}

sp<SurfaceComposerClient> BootAnimation::session() const {
    return mSession;
}


void BootAnimation::binderDied(const wp<IBinder>& who)
{
	XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);
    // woah, surfaceflinger died!
    XLOGD("SurfaceFlinger died, exiting...");

    // calling requestExit() is not enough here because the Surface code
    // might be blocked on a condition variable that will never be updated.
    kill( getpid(), SIGKILL );
    requestExit();
	XLOGD("[BootAnimation %s %d]end",__FUNCTION__,__LINE__);
}

status_t BootAnimation::initTexture(Texture* texture, AssetManager& assets,
        const char* name) {
    Asset* asset = assets.open(name, Asset::ACCESS_BUFFER);
    if (!asset)
        return NO_INIT;
    SkBitmap bitmap;
    SkImageDecoder::DecodeMemory(asset->getBuffer(false), asset->getLength(),
            &bitmap, SkBitmap::kNo_Config, SkImageDecoder::kDecodePixels_Mode);
    asset->close();
    delete asset;

    // ensure we can call getPixels(). No need to call unlock, since the
    // bitmap will go out of scope when we return from this method.
    bitmap.lockPixels();

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    texture->w = w;
    texture->h = h;

    int tw = 1 << (31 - __builtin_clz(w));
    int th = 1 << (31 - __builtin_clz(h));
    if (tw < w) tw <<= 1;
    if (th < h) th <<= 1;
    XLOGD("[BootAnimation %s %d]w=%d,h=%d,tw=%d,th=%d",__FUNCTION__,__LINE__,w,h,tw,th);


    glGenTextures(1, &texture->name);
    glBindTexture(GL_TEXTURE_2D, texture->name);

    switch (bitmap.getConfig()) {
        case SkBitmap::kA8_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case SkBitmap::kARGB_4444_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_SHORT_4_4_4_4, p);
            break;
        case SkBitmap::kARGB_8888_Config:
#ifdef USES_ARGB_ORDER
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, tw, th, 0, GL_BGRA,
                        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_BGRA, GL_UNSIGNED_BYTE, p);
            } else {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, w, h, 0, GL_BGRA,
                    GL_UNSIGNED_BYTE, p);
            }
#else
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, p);
            } else {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, p);
            }
#endif
            break;
        case SkBitmap::kRGB_565_Config:
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, p);
            } else {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, p);
            }
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    return NO_ERROR;
}

status_t BootAnimation::initTexture(void* buffer, size_t len)
{
    //StopWatch watch("blah");
    XLOGD("[BootAnimation %s %d]start init and decode logo",__FUNCTION__,__LINE__);
    SkBitmap bitmap;
    SkMemoryStream  stream(buffer, len);
    SkImageDecoder* codec = SkImageDecoder::Factory(&stream);
    codec->setDitherImage(false);
    if (codec) {
        codec->decode(&stream, &bitmap,
            SkBitmap::kARGB_8888_Config,  // SkBitmap::kRGB_565_Config, // Jelly Bean Changed
            SkImageDecoder::kDecodePixels_Mode);
        delete codec;
    }
    XLOGD("[BootAnimation %s %d]decode finish",__FUNCTION__,__LINE__);

    // ensure we can call getPixels(). No need to call unlock, since the
    // bitmap will go out of scope when we return from this method.
    bitmap.lockPixels();

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    int tw = 1 << (31 - __builtin_clz(w));
    int th = 1 << (31 - __builtin_clz(h));
    if (tw < w) tw <<= 1;
    if (th < h) th <<= 1;
    XLOGD("[BootAnimation %s %d]w=%d,h=%d,tw=%d,th=%d",__FUNCTION__,__LINE__,w,h,tw,th);
/*
    //add to print pixels
    int * pixel= (int *)p;
    //pixel = p;

    XLOGD("[BootAnimation %s %d]print bitmap pixel",__FUNCTION__,__LINE__);
    int ii=0;
    for(;ii<10000;ii=ii+100)
    {
    XLOGD("i= %d, %d",ii,pixel[ii]);
    }
    //add print log
*/	
    // @add default blackground to fix green line issue
    const size_t kLength = tw * th * 4;
    if (kLength > mBackgroundLength) {
        if (mBackgroundLength) {
            delete[] glTextureBackground;
        }
        glTextureBackground = new unsigned char[kLength];
        memset(glTextureBackground, 0, kLength);
    }
    switch (bitmap.getConfig()) {
        case SkBitmap::kARGB_8888_Config:
            if (kLength > mBackgroundLength) {
                for (size_t i = 3; i <= kLength; i = i + 4) {
                    *(glTextureBackground + i) = 0xff;
                }
                mBackgroundLength = kLength;
            }
#ifdef USES_ARGB_ORDER
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, tw, th, 0, GL_BGRA,
                        GL_UNSIGNED_BYTE, (void *)glTextureBackground);
                //glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, tw, th, 0, GL_BGRA,
                //        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_BGRA, GL_UNSIGNED_BYTE, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, tw, th, 0, GL_BGRA,
                        GL_UNSIGNED_BYTE, p);
            }
#else
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, (void *)glTextureBackground);
                //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                //        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, p);
            }
#endif
            break;

        case SkBitmap::kRGB_565_Config:
            if(kLength > mBackgroundLength) {
                mBackgroundLength = kLength;
            }
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, (void *)glTextureBackground);
                //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                //        GL_UNSIGNED_SHORT_5_6_5, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, p);
            }
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    return NO_ERROR;
}

status_t BootAnimation::readyToRun() {
    XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);
    mAssets.addDefaultAssets();

    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));  //MR1 ADDED
    DisplayInfo dinfo;
    XLOGD("readyToRun getDisplayInfo()...");
    status_t status = SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    if (status)
        return -1;

 
    XLOGD("[BootAnimation %s %d]set default orientation",__FUNCTION__,__LINE__);

    SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientationDefault, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));          
       // mSession->setOrientation(0, ISurfaceComposer::eOrientationDefault,0);


    //for case with hwrotation
    char property[PROPERTY_VALUE_MAX];
    if (property_get("ro.sf.hwrotation", property, NULL) > 0) {

        XLOGD("[BootAnimation %s %d]ro.sf.hwrotation= %s",__FUNCTION__,__LINE__,property);

        int width = dinfo.w;
        int height = dinfo.h;
        switch (atoi(property)) {
        case 90:
            dinfo.w = height;
            dinfo.h = width;
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation90, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));
            XLOGD("***** bootanimation with hw rotation 90");
            break;
        case 180:
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation180, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));
            XLOGD("***** bootanimation with hw rotation 180");
            break;
        case 270:
            dinfo.w = height;
            dinfo.h = width;
            SurfaceComposerClient::setDisplayProjection(dtoken, DisplayState::eOrientation270, Rect(dinfo.w, dinfo.h), Rect(dinfo.w, dinfo.h));          
            XLOGD("***** bootanimation with hw rotation 270");
            break;
        default:
            break;
        }
    }

    // create the native surface
    XLOGD("***** bootanimation createSurface(dinfo.w %d dinfo.h %d) *****",dinfo.w,dinfo.h);
    sp<SurfaceControl> control = session()->createSurface(String8("BootAnimation"),
            dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);

    SurfaceComposerClient::openGlobalTransaction();
   
    status = control->setLayer(2000000);
    if (status) {
        XLOGE("control->setLayer(2000000) return status %d",status);
    }
    SurfaceComposerClient::closeGlobalTransaction();

    sp<Surface> s = control->getSurface();

    // initialize opengl and egl
    XLOGD("control->getSurface()");
    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_NONE
    };
    EGLint w, h, dummy;
    EGLint numConfigs;
    EGLConfig config;
    EGLSurface surface;
    EGLContext context;

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    XLOGD("initialize opengl and egl");
    EGLBoolean eglret = eglInitialize(display, 0, 0);
    if (eglret == EGL_FALSE) {
        XLOGE("eglInitialize(display, 0, 0) return EGL_FALSE");
    }
    eglChooseConfig(display, attribs, &config, 1, &numConfigs);
    surface = eglCreateWindowSurface(display, config, s.get(), NULL);
    context = eglCreateContext(display, config, NULL, NULL);
    eglret = eglQuerySurface(display, surface, EGL_WIDTH, &w);
    if (eglret == EGL_FALSE) {
        XLOGE("eglQuerySurface(display, surface, EGL_WIDTH, &w) return EGL_FALSE");
    }
    eglret = eglQuerySurface(display, surface, EGL_HEIGHT, &h);
    if (eglret == EGL_FALSE) {
        XLOGE("eglQuerySurface(display, surface, EGL_HEIGHT, &h) return EGL_FALSE");
    }

    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE) {
        XLOGE("eglMakeCurrent(display, surface, surface, context) return EGL_FALSE");
        return NO_INIT;
    }
    mDisplay = display;
    mContext = context;
    mSurface = surface;
    mWidth = w;
    mHeight = h;
    mFlingerSurfaceControl = control;
    mFlingerSurface = s;
    XLOGV("open bootanimation.zip");
	XLOGD("[BootAnimation %s %d]open bootanimation.zip, ms=%lld",__FUNCTION__,__LINE__,ns2ms(systemTime()));    
    mAndroidAnimation = false;
    if (bBootOrShutDown) {
        status_t err = mZip.open("/data/local/bootanimation.zip");
		XLOGD("[BootAnimation %s %d]err=%d,%s,/data/local/bootanimation.zip",__FUNCTION__,__LINE__,err,strerror(-err));
        if (err != NO_ERROR) {
           err = mZip.open("/system/media/bootanimation.zip");
		   XLOGD("[BootAnimation %s %d]err=%d,%s,/system/media/bootanimation.zip",__FUNCTION__,__LINE__,err,strerror(-err));
           if (err != NO_ERROR) {
               mAndroidAnimation = true;
           }
        }
    } else {
	if (!bShutRotate) {
	    status_t err = mZip.open("/data/local/shutanimation.zip");
		XLOGD("[BootAnimation %s %d]err=%d,%s,/data/local/shutanimation.zip",__FUNCTION__,__LINE__,err,strerror(-err));
     	    if (err != NO_ERROR) {
        	err = mZip.open("/system/media/shutanimation.zip");
			XLOGD("[BootAnimation %s %d]err=%d,%s,/system/media/shutanimation.zip",__FUNCTION__,__LINE__,err,strerror(-err));
        	if (err != NO_ERROR) {
            	    mAndroidAnimation = true;
        	}
            }
	} else {
	    status_t err = mZip.open("/data/local/shutrotate.zip");
		XLOGD("[BootAnimation %s %d]err=%d,%s,/data/local/shutrotate.zip",__FUNCTION__,__LINE__,err,strerror(-err));
     	    if (err != NO_ERROR) {
                err = mZip.open("/system/media/shutrotate.zip");
				XLOGD("[BootAnimation %s %d]err=%d,%s,/system/media/shutrotate.zip",__FUNCTION__,__LINE__,err,strerror(-err));
        	if (err != NO_ERROR) {
            	    mAndroidAnimation = true;
        	}
    	    }
	}
    }
    XLOGV("after check bootanimation.zip");
	XLOGD("[BootAnimation %s %d]after check bootanimation.zip,mAndroidAnimation=%d, ms=%lld",__FUNCTION__,__LINE__,mAndroidAnimation,ns2ms(systemTime()));
	
    /*
    mAndroidAnimation = true;

    // If the device has encryption turned on or is in process 
    // of being encrypted we show the encrypted boot animation.
    char decrypt[PROPERTY_VALUE_MAX];
    property_get("vold.decrypt", decrypt, "");

    bool encryptedAnimation = atoi(decrypt) != 0 || !strcmp("trigger_restart_min_framework", decrypt);

    if ((encryptedAnimation &&
            (access(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE, R_OK) == 0) &&
            (mZip.open(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE) == NO_ERROR)) ||

            ((access(USER_BOOTANIMATION_FILE, R_OK) == 0) &&
            (mZip.open(USER_BOOTANIMATION_FILE) == NO_ERROR)) ||

            ((access(SYSTEM_BOOTANIMATION_FILE, R_OK) == 0) &&
            (mZip.open(SYSTEM_BOOTANIMATION_FILE) == NO_ERROR))) {
        mAndroidAnimation = false;
    } */

	XLOGD("[BootAnimation %s %d]end",__FUNCTION__,__LINE__);
    return NO_ERROR;
}

bool BootAnimation::threadLoop()
{
	XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);
    bool r;
    XLOGD("enter threadLoop()");
    const char *pSoundFileName;
    const char *pBackupSoundFileName;



/* MR1 ADDED
    if (mAndroidAnimation) {
        r = android();
    } else {
        r = movie();
    }
*/

    if (!bBootOrShutDown) {
        pSoundFileName= "/data/local/shutaudio.mp3";
	    pBackupSoundFileName="/system/media/shutaudio.mp3";
    } else {
        pSoundFileName= "/data/local/bootaudio.mp3";
	    pBackupSoundFileName="/system/media/bootaudio.mp3";
    }
    bool bexist = false;
    int nMp3Exist = -1;

    bexist = (access(pSoundFileName, F_OK) == 0);
    if (!bexist) {
	    bexist = (access(pBackupSoundFileName, F_OK) == 0);
	    if(bexist) {
	        nMp3Exist = 1;
	    }
    } 
	XLOGD("[BootAnimation %s %d]bexist=%d,nMp3Exist=%d,pSoundFileName=%s,pBackupSoundFileName=%s, ms=%lld",__FUNCTION__,__LINE__,bexist,nMp3Exist,pSoundFileName,pBackupSoundFileName,ns2ms(systemTime()));

	if (bexist && bPlayMP3) {
		sp<MediaPlayer> mediaplayer=new MediaPlayer();
		status_t mediastatus;
		if (nMp3Exist < 0) {
			mediastatus = mediaplayer->setDataSource(pSoundFileName, NULL);
		} else {
			mediastatus = mediaplayer->setDataSource(pBackupSoundFileName, NULL);
		}
		if (mediastatus == NO_ERROR) {
		 	mediaplayer->setAudioStreamType(AUDIO_STREAM_BOOT);
			mediastatus = mediaplayer->prepare();
		}

		if (mediastatus!=NO_ERROR && nMp3Exist < 0) {
			mediastatus = mediaplayer->setDataSource(pBackupSoundFileName,NULL);
			if (mediastatus == NO_ERROR) {
			 	mediaplayer->setAudioStreamType(AUDIO_STREAM_BOOT);
				mediastatus = mediaplayer->prepare();
			}
		}

		if (mediastatus == NO_ERROR) {		    
			mediastatus = mediaplayer->start();
		}

		XLOGD("[BootAnimation %s %d]mAndroidAnimation=%d, ms=%lld",__FUNCTION__,__LINE__,mAndroidAnimation,ns2ms(systemTime()));

		if (mAndroidAnimation) {
			r = android();
		} else {
			XLOGD("threadLoop() movie()");
			r = movie();
		}

		if (mediastatus == NO_ERROR) {		    
			mediaplayer->stop();
			mediaplayer->disconnect();       
		}

	} else {
		XLOGD("[BootAnimation %s %d]mAndroidAnimation=%d",__FUNCTION__,__LINE__,mAndroidAnimation);
		if (mAndroidAnimation) {
			r = android();
		} else {
			XLOGD("threadLoop() movie()");
			r = movie();
		}    
	}

        // No need to force exit anymore
        property_set(EXIT_PROP_NAME, "0"); //Jelly Bean added

    XLOGD("[BootAnimation %s %d]threadLoop() exit movie()",__FUNCTION__,__LINE__);
    eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(mDisplay, mContext);
    eglDestroySurface(mDisplay, mSurface);
    mFlingerSurface.clear();
    mFlingerSurfaceControl.clear();
    eglTerminate(mDisplay);
    IPCThreadState::self()->stopProcess();

	XLOGD("threadLoop() exit");
	XLOGD("[BootAnimation %s %d]end & exit r=%d",__FUNCTION__,__LINE__,r);
    return r;
}

bool BootAnimation::android()
{
	XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);

    initTexture(&mAndroid[0], mAssets, "images/android-logo-mask.png");
    initTexture(&mAndroid[1], mAssets, "images/android-logo-shine.png");

    // clear screen
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glClearColor(0,0,0,1);
    glClear(GL_COLOR_BUFFER_BIT);
    eglSwapBuffers(mDisplay, mSurface);

    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    XLOGD("***** bootanimation init(%d %d %d %d) *****", mWidth, mHeight, mAndroid[0].w, mAndroid[0].h);
    GLint xc = (mWidth  - mAndroid[0].w) / 2;    
    GLint yc = (mHeight - mAndroid[0].h) / 2;
// const Rect updateRect(xc, yc, xc + mAndroid[0].w, yc + mAndroid[0].h);  //MR1 ADDED

    int x = xc, y = yc;
    int w = mAndroid[0].w, h = mAndroid[0].h;
    if (x < 0) {
        w += x;
        x  = 0;
    }
    if (y < 0) {
        h += y;
        y  = 0;
    }
    if (w > mWidth) {
        w = mWidth;
    }
    if (h > mHeight) {
        h = mHeight;
    }
	XLOGD("[BootAnimation %s %d]x=%d,y=%d,w=%d,h=%d",__FUNCTION__,__LINE__,x,y,w,h);

    const Rect updateRect(x, y, x+w, y+h);

    glScissor(updateRect.left, mHeight - updateRect.bottom, updateRect.width(),
            updateRect.height());

    // Blend state
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const nsecs_t startTime = systemTime();
    do {
        nsecs_t now = systemTime();
        double time = now - startTime;
		XLOGD("[BootAnimation %s %d]time=%f",__FUNCTION__,__LINE__,time);
		
        float t = 4.0f * float(time / us2ns(16667)) / mAndroid[1].w;
        GLint offset = (1 - (t - floorf(t))) * mAndroid[1].w;
        GLint x = xc - offset;

        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[1].name);
        glDrawTexiOES(x,                 yc, 0, mAndroid[1].w, mAndroid[1].h);
        glDrawTexiOES(x + mAndroid[1].w, yc, 0, mAndroid[1].w, mAndroid[1].h);

        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[0].name);
        glDrawTexiOES(xc, yc, 0, mAndroid[0].w, mAndroid[0].h);

        EGLBoolean res = eglSwapBuffers(mDisplay, mSurface);
        if (res == EGL_FALSE)
            break;

        // 12fps: don't animate too fast to preserve CPU
        const nsecs_t sleepTime = 83333 - ns2us(systemTime() - now);
        if (sleepTime > 0)
            usleep(sleepTime);

        checkExit(); //Jelly Bean added
    } while (!exitPending());

    glDeleteTextures(1, &mAndroid[0].name);
    glDeleteTextures(1, &mAndroid[1].name);
	XLOGD("[BootAnimation %s %d]end",__FUNCTION__,__LINE__);
    return false;
}


void BootAnimation::checkExit() { //Jelly Bean added
    // Allow surface flinger to gracefully request shutdown
    char value[PROPERTY_VALUE_MAX];
    property_get(EXIT_PROP_NAME, value, "0");
    int exitnow = atoi(value);
	XLOGD("[BootAnimation %s %d]exitnow=%d",__FUNCTION__,__LINE__,exitnow);
    if (exitnow) {
		XLOGD("[BootAnimation %s %d]exitnow==%d",__FUNCTION__,__LINE__,exitnow);
        requestExit();
    }
}

bool BootAnimation::movie()
{
	XLOGD("[BootAnimation %s %d]start",__FUNCTION__,__LINE__);

    ZipFileRO& zip(mZip);

    size_t numEntries = zip.getNumEntries();
    ZipEntryRO desc = zip.findEntryByName("desc.txt");
    FileMap* descMap = zip.createEntryFileMap(desc);
    ALOGE_IF(!descMap, "descMap is null");
    if (!descMap) {
		XLOGD("[BootAnimation %s %d]error",__FUNCTION__,__LINE__);
        return false;
    }

    String8 desString((char const*)descMap->getDataPtr(),
            descMap->getDataLength());
    char const* s = desString.string();

    Animation animation;

    // Parse the description file
    for (;;) {
        const char* endl = strstr(s, "\n");
        if (!endl) break;
        String8 line(s, endl - s);
        const char* l = line.string();
        int fps, width, height, count, pause;
        char path[256];
        char pathType;
		XLOGD("[BootAnimation %s %d]l=%s",__FUNCTION__,__LINE__,l);
		
        if (sscanf(l, "%d %d %d", &width, &height, &fps) == 3) {
            XLOGD("> w=%d, h=%d, fps=%d", width, height, fps); // add log
            animation.width = width;
            animation.height = height;
            animation.fps = fps;
        }
        else if (sscanf(l, " %c %d %d %s", &pathType, &count, &pause, path) == 4) {
            XLOGD("> type=%c, count=%d, pause=%d, path=%s", pathType, count, pause, path); // add log
            Animation::Part part;
            part.playUntilComplete = pathType == 'c';
            part.count = count;
            part.pause = pause;
            part.path = path;
            animation.parts.add(part);
        }

        s = ++endl;
    }

    // read all the data structures
    const size_t pcount = animation.parts.size();
	XLOGD("[BootAnimation %s %d]numEntries=%d,pcount=%d",__FUNCTION__,__LINE__,numEntries,pcount);
    for (size_t i=0 ; i<numEntries ; i++) {
        char name[256];
        ZipEntryRO entry = zip.findEntryByIndex(i);
        if (zip.getEntryFileName(entry, name, 256) == 0) {
            const String8 entryName(name);
            const String8 path(entryName.getPathDir());
            const String8 leaf(entryName.getPathLeaf());
			XLOGD("[BootAnimation %s %d]path=%s,leaf=%s,size=%d,i=%d",__FUNCTION__,__LINE__,path.string(),leaf.string(),leaf.size(),i);
			
            if (leaf.size() > 0) {
                for (int j=0 ; j<pcount ; j++) {
					XLOGD("[BootAnimation %s %d]path=%s,%s,j=%d",__FUNCTION__,__LINE__,path.string(),animation.parts[j].path.string(),j);
					
                    if (path == animation.parts[j].path) {
                        int method;
                        // supports only stored png files
                        if (zip.getEntryInfo(entry, &method, 0, 0, 0, 0, 0)) {
                            if (method == ZipFileRO::kCompressStored) {
                                FileMap* map = zip.createEntryFileMap(entry);
                                if (map) {
									int add_tmp;
                                    Animation::Frame frame;
                                    frame.name = leaf;
                                    frame.map = map;
                                    Animation::Part& part(animation.parts.editItemAt(j));
                                    add_tmp = part.frames.add(frame);
									XLOGD("[BootAnimation %s %d]path=%s,%s,j=%d,add=%d,leaf=%s",__FUNCTION__,__LINE__,path.string(),animation.parts[j].path.string(),j,add_tmp,leaf.string());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // init setup
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_BLEND);
    glClearColor(0,0,0,1);

    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    const int xc = (mWidth - animation.width) / 2;
    const int yc = ((mHeight - animation.height) / 2);
    nsecs_t lastFrame = systemTime();
    nsecs_t frameDuration = s2ns(1) / animation.fps;
	XLOGD("[BootAnimation %s %d]xc=%d,yc=%d,lastFrame=%lld,frameDuration=%lld,ms=%lld",__FUNCTION__,__LINE__,xc,yc,lastFrame,frameDuration,ns2ms(systemTime()));

    Region clearReg(Rect(mWidth, mHeight));
    clearReg.subtractSelf(Rect(xc, yc, xc+animation.width, yc+animation.height));

    for (int i=0 ; i<pcount ; i++) {
        const Animation::Part& part(animation.parts[i]);
        const size_t fcount = part.frames.size();
		XLOGD("[BootAnimation %s %d]i=%d,i<pcount=%d,r<part.count=%d,j<fcount=%d",__FUNCTION__,__LINE__,i,pcount,part.count,fcount);
        glBindTexture(GL_TEXTURE_2D, 0);

        for (int r=0 ; !part.count || r<part.count ; r++) {
            // Exit any non playuntil complete parts immediately
            if(exitPending() && !part.playUntilComplete){
				XLOGD("[BootAnimation %s %d]break,i=%d,pcount=%d,fcount=%d,part.count=%d,exitPending()=%d,part.playUntilComplete=%d",__FUNCTION__,__LINE__,i,pcount,fcount,part.count,exitPending(),part.playUntilComplete);
				break;
            	}

            for (int j=0 ; j<fcount && (!exitPending() || part.playUntilComplete) ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                nsecs_t lastFrame = systemTime();
				XLOGD("[BootAnimation %s %d]i=%d,r=%d,j=%d,lastFrame=%lld(%lld ms),file=%s",__FUNCTION__,__LINE__,i,r,j,lastFrame,ns2ms(lastFrame),frame.name.string());
								
                if (r > 0) {
					XLOGD("[BootAnimation %s %d]glBindTexture,i=%d,r=%d,j=%d,frame.tid=%d,name=%s",__FUNCTION__,__LINE__,i,r,j,frame.tid,frame.name.string());
                    glBindTexture(GL_TEXTURE_2D, frame.tid);
                } else {
                    if (part.count != 1) {
                        glGenTextures(1, &frame.tid);
                        glBindTexture(GL_TEXTURE_2D, frame.tid);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
						XLOGD("[BootAnimation %s %d]glGenTextures,i=%d,r=%d,j=%d,frame.tid=%d,name=%s",__FUNCTION__,__LINE__,i,r,j,frame.tid,frame.name.string());
                    }
                    initTexture(
                            frame.map->getDataPtr(),
                            frame.map->getDataLength());
					XLOGD("[BootAnimation %s %d]initTexture,i=%d,r=%d,j=%d,frame.tid=%d,name=%s",__FUNCTION__,__LINE__,i,r,j,frame.tid,frame.name.string());
                }

                if (!clearReg.isEmpty()) {
					XLOGD("[BootAnimation %s %d]clearReg.isEmpty,i=%d,r=%d,j=%d,frame.tid=%d",__FUNCTION__,__LINE__,i,r,j,frame.tid);
                    Region::const_iterator head(clearReg.begin());
                    Region::const_iterator tail(clearReg.end());
                    glEnable(GL_SCISSOR_TEST);
                    while (head != tail) {
                        const Rect& r(*head++);
                        glScissor(r.left, mHeight - r.bottom,
                                r.width(), r.height());
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                    glDisable(GL_SCISSOR_TEST);
                }
                glDrawTexiOES(xc, yc, 0, animation.width, animation.height);
                eglSwapBuffers(mDisplay, mSurface);

                nsecs_t now = systemTime();
                nsecs_t delay = frameDuration - (now - lastFrame);
               
				XLOGD("[BootAnimation %s %d]%lld,delay=%lld",__FUNCTION__,__LINE__,ns2ms(now - lastFrame), ns2ms(delay));
                //ALOGD("%lld, %lld", ns2ms(now - lastFrame), ns2ms(delay));
//                lastFrame = now; // Android default

                if (delay > 0) {
                    struct timespec spec;
                    spec.tv_sec  = (now + delay) / 1000000000;
                    spec.tv_nsec = (now + delay) % 1000000000;
                    int err;
                    do {
                        err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, NULL);
                    } while (err<0 && errno == EINTR);
                }
                lastFrame = systemTime();//mtk54232

                checkExit();
            }

            usleep(part.pause * ns2us(frameDuration));

            // For infinite parts, we've now played them at least once, so perhaps exit
            if(exitPending() && !part.count){
				XLOGD("[BootAnimation %s %d]break,exitPending()=%d,part.count=%d",__FUNCTION__,__LINE__,exitPending(),part.count);

                break;
            	}
        }

        // free the textures for this part
        if (part.count != 1) {
            for (int j=0 ; j<fcount ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                glDeleteTextures(1, &frame.tid);
				XLOGD("[BootAnimation %s %d]del,part.count=%d,j=%d,fcount=%d",__FUNCTION__,__LINE__,part.count,j,fcount);
            }
        }
    }
	XLOGD("[BootAnimation %s %d]end",__FUNCTION__,__LINE__);
    return false;
}

// ---------------------------------------------------------------------------

}
; // namespace android
