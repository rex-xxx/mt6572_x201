package com.mediatek.contacts.calloption;

import com.mediatek.calloption.CallOptionHandlerFactory;
import com.mediatek.calloption.FirstCallOptionHandler;
import com.mediatek.calloption.InternationalCallOptionHandler;
import com.mediatek.calloption.InternetCallOptionHandler;
import com.mediatek.calloption.IpCallOptionHandler;
import com.mediatek.calloption.SimSelectionCallOptionHandler;
import com.mediatek.calloption.SimStatusCallOptionHandler;
import com.mediatek.calloption.VideoCallOptionHandler;
import com.mediatek.calloption.VoiceMailCallOptionHandler;

public class ContactsCallOptionHandlerFactory extends CallOptionHandlerFactory {

    private FirstCallOptionHandler mFirstCallOptionHandler;
    private InternetCallOptionHandler mInternetCallOptionHandler;
    private VideoCallOptionHandler mVideoCallOptionHandler;
    private InternationalCallOptionHandler mInternationalCallOptionHandler;
    private SimSelectionCallOptionHandler mSimSelectionCallOptionHandler;
    private SimStatusCallOptionHandler mSimStatusCallOptionHandler;
    private IpCallOptionHandler mIpCallOptionHandler;
    private VoiceMailCallOptionHandler mVoiceMailCallOptionHandler;

    public ContactsCallOptionHandlerFactory() {
        mFirstCallOptionHandler = new ContactsFirstCallOptionHandler();
        mInternetCallOptionHandler = new ContactsInternetCallOptionHandler();
        mVideoCallOptionHandler = new ContactsVideoCallOptionHandler();
        mInternationalCallOptionHandler = new ContactsInternationalCallOptionHandler();
        mSimSelectionCallOptionHandler = new ContactsSimSelectionCallOptionHandler();
        mSimStatusCallOptionHandler = new ContactsSimStatusCallOptionHandler();
        mIpCallOptionHandler = new ContactsIpCallOptionHandler();
        mVoiceMailCallOptionHandler = new ContactsVoiceMailCallOptionHandler();
    }

    public FirstCallOptionHandler getFirstCallOptionHandler() {
        return mFirstCallOptionHandler;
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
