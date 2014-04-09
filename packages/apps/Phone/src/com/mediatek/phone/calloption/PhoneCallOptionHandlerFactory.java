package com.mediatek.phone.calloption;

import com.mediatek.calloption.CallOptionHandlerFactory;
import com.mediatek.calloption.InternationalCallOptionHandler;
import com.mediatek.calloption.InternetCallOptionHandler;
import com.mediatek.calloption.IpCallOptionHandler;
import com.mediatek.calloption.SimSelectionCallOptionHandler;
import com.mediatek.calloption.SimStatusCallOptionHandler;
import com.mediatek.calloption.VideoCallOptionHandler;
import com.mediatek.calloption.VoiceMailCallOptionHandler;


public class PhoneCallOptionHandlerFactory extends CallOptionHandlerFactory {

    private InternetCallOptionHandler mInternetCallOptionHandler;
    private VideoCallOptionHandler mVideoCallOptionHandler;
    private InternationalCallOptionHandler mInternationalCallOptionHandler;
    private SimSelectionCallOptionHandler mSimSelectionCallOptionHandler;
    private SimStatusCallOptionHandler mSimStatusCallOptionHandler;
    private IpCallOptionHandler mIpCallOptionHandler;
    private VoiceMailCallOptionHandler mVoiceMailCallOptionHandler;

    public PhoneCallOptionHandlerFactory() {
        mInternetCallOptionHandler = new PhoneInternetCallOptionHandler();
        mVideoCallOptionHandler = new PhoneVideoCallOptionHandler();
        mInternationalCallOptionHandler = new PhoneInternationalCallOptionHandler();
        mSimSelectionCallOptionHandler = new PhoneSimSelectionCallOptionHandler();
        mSimStatusCallOptionHandler = new PhoneSimStatusCallOptionHandler();
        mIpCallOptionHandler = new PhoneIpCallOptionHandler();
        mVoiceMailCallOptionHandler = new PhoneVoiceMailCallOptionHandler();
    }
    
    public InternetCallOptionHandler getInternetCallOptionHandler() {
        return mInternetCallOptionHandler;
    }

    public VideoCallOptionHandler getVideoCallOptionHandler() {
        return mVideoCallOptionHandler;
    }

    public InternationalCallOptionHandler getInternationalCallOptionHandler() {
        return mInternationalCallOptionHandler;
    }

    public SimSelectionCallOptionHandler getSimSelectionCallOptionHandler() {
        return mSimSelectionCallOptionHandler;
    }

    public SimStatusCallOptionHandler getSimStatusCallOptionHandler() {
        return mSimStatusCallOptionHandler;
    }

    public IpCallOptionHandler getIpCallOptionHandler() {
        return mIpCallOptionHandler;
    }

    public VoiceMailCallOptionHandler getVoiceMailCallOptionHandler() {
        return mVoiceMailCallOptionHandler;
    }
}
