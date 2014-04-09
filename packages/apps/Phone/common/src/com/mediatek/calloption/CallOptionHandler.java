package com.mediatek.calloption;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.mediatek.CellConnService.CellConnMgr;

import java.util.LinkedList;
import java.util.ListIterator;

public class CallOptionHandler {

    private static final String TAG = "CallOptionHandler";

    protected LinkedList<CallOptionBaseHandler> mCallOptionHandlerList;
    protected CallOptionHandlerFactory mCallOptionHandlerFactory;

    public CallOptionHandler(CallOptionHandlerFactory callOptionHandlerFactory) {
        mCallOptionHandlerFactory = callOptionHandlerFactory;
        mCallOptionHandlerList = new LinkedList<CallOptionBaseHandler>();

        mCallOptionHandlerList.add(callOptionHandlerFactory.getFirstCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getInternetCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getVideoCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getSimSelectionCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getSimStatusCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getVoiceMailCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getInternationalCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getIpCallOptionHandler());
        mCallOptionHandlerList.add(callOptionHandlerFactory.getFinalCallOptionHandler());
    }

    /**
     * The entry for making an call
     * @param intent the call intent
     */
    public void doCallOptionHandle(Context activityContext, Context applicationContext, Intent intent,
                                   CallOptionBaseHandler.ICallOptionResultHandle resultHandler,
                                   CellConnMgr cellConnMgr, ITelephony telephonyInterface,
                                   boolean isMultipleSim, boolean is3GSwitchSupport) {
        ListIterator<CallOptionBaseHandler> iterator = mCallOptionHandlerList.listIterator();
        CallOptionBaseHandler previousHandler = iterator.next();
        while (iterator.hasNext()) {
            CallOptionBaseHandler currentHandler = (CallOptionBaseHandler)iterator.next();
            previousHandler.setSuccessor(currentHandler);
            previousHandler = currentHandler;
        }

        Request request = new Request(activityContext, applicationContext, intent, resultHandler,
                                      cellConnMgr, telephonyInterface, isMultipleSim, is3GSwitchSupport,
                                      mCallOptionHandlerFactory);
        mCallOptionHandlerList.getFirst().handleRequest(request);
    }

    public void dismissDialogs() {
        ListIterator<CallOptionBaseHandler> iterator = mCallOptionHandlerList.listIterator();
        while (iterator.hasNext()) {
            ((CallOptionBaseHandler)iterator.next()).dismissDialog();
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
