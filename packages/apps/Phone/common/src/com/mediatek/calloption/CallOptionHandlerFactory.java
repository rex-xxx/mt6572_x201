package com.mediatek.calloption;

public abstract class CallOptionHandlerFactory {

    public FirstCallOptionHandler getFirstCallOptionHandler() {
        return new FirstCallOptionHandler();
    }

    public abstract InternetCallOptionHandler getInternetCallOptionHandler();

    public abstract VideoCallOptionHandler getVideoCallOptionHandler();

    public abstract InternationalCallOptionHandler getInternationalCallOptionHandler();

    public abstract SimSelectionCallOptionHandler getSimSelectionCallOptionHandler();

    public abstract SimStatusCallOptionHandler getSimStatusCallOptionHandler();

    public abstract IpCallOptionHandler getIpCallOptionHandler();

    public abstract VoiceMailCallOptionHandler getVoiceMailCallOptionHandler();

    public FinalCallOptionHandler getFinalCallOptionHandler() {
        return new FinalCallOptionHandler();
    }
}
