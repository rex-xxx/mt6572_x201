package com.mediatek.phone.gemini;

import android.os.Handler;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gemini.MTKCallManager;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;

import junit.framework.Assert;

public class GeminiRegister {
    private static final String TAG = "GeminiRegister";
    private static final boolean DEBUG = true;

    /**
     * Notifies when a new ringing or waiting connection has appeared.
     *
     * Messages received from this: Message.obj will be an AsyncResult
     * AsyncResult.userObj = obj AsyncResult.result = a Connection.
     *
     * Please check Connection.isRinging() to make sure the Connection has not
     * dropped since this message was posted. If Connection.isRinging() is true,
     * then Connection.getCall() == Phone.getRingingCall()
     *
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForNewRingingConnection(Object callManager, Handler handler,
            int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.registerForNewRingingConnectionGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForNewRingingConnection(handler, what, null);
        }
    }

    /**
     * Unregisters for new ringing connection notification. Extraneous calls are
     * tolerated silently
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForNewRingingConnection(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForNewRingingConnectionGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForNewRingingConnection(handler);
        }
    }

    /**
     * @see registerAllForDisconnect(Object , Handler , int[] )
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForDisconnect(Object callManager, Handler handler, int what) {
        final int[] geminiSlots = GeminiUtils.getSlots();
        final int count = geminiSlots.length;
        int[] whats = new int[count];
        for (int i = 0; i < whats.length; i++) {
            whats[i] = what;
        }
        registerForDisconnect(callManager, handler, whats);
    }

    /**
     * Notifies when a voice connection has disconnected, either due to local or
     * remote hangup or error. Messages received from this will have the
     * following members:
     * 
     * Message.obj will be an AsyncResult AsyncResult.userObj = obj
     * AsyncResult.result = a Connection object that is no longer connected.
     * 
     * @param callManager
     * @param handler
     * @param whats
     */
    public static void registerForDisconnect(Object callManager, Handler handler, int[] whats) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();

            Assert.assertTrue(whats.length >= geminiSlots.length);

            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.registerForDisconnectGemini(handler, whats[i], null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForDisconnect(handler, whats[0], null);
        }
    }

    /**
     * Unregisters for voice disconnection notification. Extraneous calls are
     * tolerated silently
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForDisconnect(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForDisconnectGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForDisconnect(handler);
        }
    }

    /**
     * @see registerAllForPreciseCallStateChanged(Object , Handler , int ,
     *      Object )
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForPreciseCallStateChanged(Object callManager, Handler handler,
            int what) {
        registerForPreciseCallStateChanged(callManager, handler, what, null);
    }

    /**
     * Register for getting notifications for change in the Call State
     * Call.State This is called PreciseCallState because the call state is more
     * precise than the Phone.State which can be obtained using the
     * PhoneStateListener Resulting events will have an AsyncResult in
     * Message.obj. AsyncResult.userData will be set to the obj argument here.
     * The h parameter is held only by a weak reference.
     * 
     * @param callManager
     * @param handler
     * @param what
     * @param obj
     */
    public static void registerForPreciseCallStateChanged(Object callManager, Handler handler,
            int what, Object obj) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.registerForPreciseCallStateChangedGemini(handler, what, obj, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForPreciseCallStateChanged(handler, what, obj);
        }
    }

    /**
     * Unregisters for voice call state change notifications. Extraneous calls
     * are tolerated silently.
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForPreciseCallStateChanged(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForPreciseCallStateChangedGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForPreciseCallStateChanged(handler);
        }
    }

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has
     * appeared. This is likely due to some other entity (eg, SIM card
     * application) initiating a call.
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForUnknownConnection(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.registerForUnknownConnectionGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForUnknownConnection(handler, what, null);
        }
    }

    /**
     * Unregisters for unknown connection notifications.
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForUnknownConnection(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForUnknownConnectionGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForUnknownConnection(handler);
        }
    }

    /**
     * Notifies when an incoming call rings.
     * 
     * Messages received from this: Message.obj will be an AsyncResult
     * AsyncResult.userObj = obj AsyncResult.result = a Connection.
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForIncomingRing(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.registerForIncomingRingGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForIncomingRing(handler, what, null);
        }
    }

    /**
     * Unregisters for ring notification. Extraneous calls are tolerated
     * silently
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForIncomingRing(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForIncomingRingGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForIncomingRing(handler);
        }
    }

    /**
     * Notifies when out-band ringback tone is needed.
     * 
     * Messages received from this: Message.obj will be an AsyncResult
     * AsyncResult.userObj = obj AsyncResult.result = boolean, true to start
     * play ringback tone and false to stop.
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForRingbackTone(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                mtkCm.registerForRingbackToneGemini(handler, what, null, gs);
            }
        } else {
            ((CallManager) callManager).registerForRingbackTone(handler, what, null);
        }
    }

    /**
     * Unregisters for ringback tone notification.
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForRingbackTone(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            MTKCallManager mtkCm = (MTKCallManager) callManager;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCm.unregisterForDisconnectGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForRingbackTone(handler);
        }
    }

    /**
     * find the 3G slot and register for VT ringInfo
     * 
     * @param callManager
     * @param handler
     * @param ringInfo
     */
    public static void registerForVtRingInfo(Object callManager, Handler handler, int ringInfo) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                // as when register is too early, framework init may not completed,
                // the get3GCapabilitySIM() will return wrong value, so register for every slot
                /*
                final int slotId = GeminiUtils.get3GCapabilitySIM();
                ((MTKCallManager) callManager).registerForVtRingInfoGemini(handler, ringInfo, null,
                        slotId);
                */
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.registerForVtRingInfoGemini(handler, ringInfo, null, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).registerForVtRingInfo(handler, ringInfo, null);
            }
        }
    }

    /**
     * find the 3G slot and unregister VT ringInfo
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForVtRingInfo(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.unregisterForVtRingInfoGemini(handler, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).unregisterForVtRingInfo(handler);
            }
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param statusInfo
     */
    public static void registerForVtStatusInfo(Object callManager, Handler handler, int statusInfo) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                // as when register is too early, framework init may not completed,
                // the get3GCapabilitySIM() will return wrong value, so register for every slot
                /*
                final int slotId = GeminiUtils.get3GCapabilitySIM();
                ((MTKCallManager) callManager).registerForVtStatusInfoGemini(handler, statusInfo,
                        null, slotId);
                */
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.registerForVtStatusInfoGemini(handler, statusInfo, null, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).registerForVtStatusInfo(handler, statusInfo, null);
            }
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForVtStatusInfo(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.unregisterForVtStatusInfoGemini(handler, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).unregisterForVtStatusInfo(handler);
            }
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForVtReplaceDisconnect(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                // as when register is too early, framework init may not completed,
                // the get3GCapabilitySIM() will return wrong value, so register for every slot
                /*
                final int slotId = GeminiUtils.get3GCapabilitySIM();
                ((MTKCallManager) callManager).registerForVtReplaceDisconnectGemini(handler, what,
                        null, slotId);
                */
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.registerForVtReplaceDisconnectGemini(handler, what, null, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).registerForVtReplaceDisconnect(handler, what, null);
            }
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForVtReplaceDisconnect(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (callManager instanceof MTKCallManager) {
                MTKCallManager mtkCm = (MTKCallManager) callManager;
                final int[] geminiSlots = GeminiUtils.getSlots();
                for (int i = 0; i < geminiSlots.length; i++) {
                    mtkCm.unregisterForVtReplaceDisconnectGemini(handler, geminiSlots[i]);
                }
            } else {
                ((CallManager) callManager).unregisterForVtReplaceDisconnect(handler);
            }
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForInCallVoicePrivacyOn(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                ((MTKCallManager) callManager).unregisterForInCallVoicePrivacyOnGemini(handler,
                        geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForInCallVoicePrivacyOn(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForInCallVoicePrivacyOff(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                ((MTKCallManager) callManager).unregisterForInCallVoicePrivacyOffGemini(handler,
                        geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForInCallVoicePrivacyOff(handler);
        }
    }

    /**
     * 
     * @param phone
     * @param handler
     * @param whats
     */
    public static void registerForServiceStateChanged(Phone phone, Handler handler, int[] whats) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone gPhone = (GeminiPhone) phone;
            final int[] geminiSlots = GeminiUtils.getSlots();

            Assert.assertTrue(whats.length >= geminiSlots.length);

            for (int i = 0; i < geminiSlots.length; i++) {
                gPhone.unregisterForServiceStateChangedGemini(handler, geminiSlots[i]);
                gPhone
                        .registerForServiceStateChangedGemini(handler, whats[i], null,
                                geminiSlots[i]);
            }
        } else {
            // Safe even if not currently registered
            phone.unregisterForServiceStateChanged(handler);
            phone.registerForServiceStateChanged(handler, whats[0], null);
        }
    }

    /**
     * 
     * @param phone
     * @param handler
     * @param whats
     */
    public static void unregisterForServiceStateChanged(Phone phone, Handler handler, int[] whats) {
        // the method is safe to call even if we haven't set phone yet.
        if (phone != null) {
            if (GeminiUtils.isGeminiSupport()) {
                final int[] geminiSlots = GeminiUtils.getSlots();

                Assert.assertTrue(whats.length >= geminiSlots.length);

                for (int i = 0; i < geminiSlots.length; i++) {
                    handler.removeMessages(whats[i]);
                    // Safe even if not currently registered
                    ((GeminiPhone) phone).unregisterForServiceStateChangedGemini(handler,
                            geminiSlots[i]);
                }
            } else {
                // Safe even if not currently registered
                phone.unregisterForServiceStateChanged(handler);
            }
        }

        // Clean up any pending message too
        handler.removeMessages(whats[0]);
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param whats
     */
    public static void registerForMmiComplete(Object callManager, Handler handler, int[] whats) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;

            Assert.assertTrue(whats.length >= geminiSlots.length);

            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForMmiCompleteGemini(handler, whats[i], null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForMmiComplete(handler, whats[0], null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForMmiComplete(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForMmiCompleteGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForMmiComplete(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param whats
     */
    public static void registerForMmiInitiate(Object callManager, Handler handler, int[] whats) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;

            Assert.assertTrue(whats.length >= geminiSlots.length);

            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForMmiInitiateGemini(handler, whats[i], null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForMmiInitiate(handler, whats[0], null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForMmiInitiate(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForMmiInitiateGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForMmiInitiate(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForCrssSuppServiceNotification(Object callManager,
            Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForCrssSuppServiceNotificationGemini(handler, what, null,
                        geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForCrssSuppServiceNotification(handler, what, null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForCrssSuppServiceNotification(Object callManager,
            Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForCrssSuppServiceNotificationGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForCrssSuppServiceNotification(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForPostDialCharacter(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForPostDialCharacterGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForPostDialCharacter(handler, what, null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForPostDialCharacter(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForPostDialCharacterGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForPostDialCharacter(handler);
        }
    }

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForSuppServiceFailed(Object callManager, Handler handler, int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForSuppServiceFailedGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForSuppServiceFailed(handler, what, null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForSuppServiceFailed(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForSuppServiceFailedGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForSuppServiceFailed(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForSuppServiceNotification(Object callManager, Handler handler,
            int what) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForSuppServiceNotificationGemini(handler, what, null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForSuppServiceNotification(handler, what, null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForSuppServiceNotification(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForSuppServiceNotificationGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForSuppServiceNotification(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param what
     */
    public static void registerForSpeechInfo(Object callManager, Handler handler, int what) {
        final int[] geminiSlots = GeminiUtils.getSlots();
        final int count = GeminiUtils.getSlotCount();
        int[] whats = new int[count];
        for (int i = 0; i < count; i++) {
            whats[i] = what;
        }
        registerForSpeechInfo(callManager, handler, whats);
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param whats
     */
    public static void registerForSpeechInfo(Object callManager, Handler handler, int[] whats) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;

            Assert.assertTrue(whats.length >= geminiSlots.length);

            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.registerForSpeechInfoGemini(handler, whats[i], null, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).registerForSpeechInfo(handler, whats[0], null);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     * @param slotId
     */
    public static void unregisterForSpeechInfo(Object callManager, Handler handler, int slotId) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            ((MTKCallManager) callManager).unregisterForSpeechInfoGemini(handler, slotId);
        } else {
            ((CallManager) callManager).unregisterForSpeechInfo(handler);
        }
    }

    /**
     * 
     * @param callManager
     * @param handler
     */
    public static void unregisterForSpeechInfo(Object callManager, Handler handler) {
        Assert.assertNotNull(callManager);
        if (callManager instanceof MTKCallManager) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            MTKCallManager mtkCM = (MTKCallManager) callManager;
            for (int i = 0; i < geminiSlots.length; i++) {
                mtkCM.unregisterForSpeechInfoGemini(handler, geminiSlots[i]);
            }
        } else {
            ((CallManager) callManager).unregisterForSpeechInfo(handler);
        }
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     * 
     * @param phone
     * @param handler
     * @param whats
     */
    public static void registerForNetworkLocked(Phone phone, Handler handler, int[] whats) {
        Assert.assertNotNull(phone);
        final int[] geminiSlots = GeminiUtils.getSlots();

        Assert.assertTrue(whats.length >= geminiSlots.length);

        for (int i = 0; i < geminiSlots.length; i++) {
            IccCard iccCard = GeminiUtils.getIccCard(phone, geminiSlots[i]);
            if (iccCard != null) {
                iccCard.registerForNetworkLocked(handler, whats[i], null);
            }
        }
    }

    /**
     * Find the slot id for the value. ("value" is a member in "array").
     * 
     * @param value
     * @param array
     * @return
     */
    public static int getSlotIdByRegisterEvent(int value, int[] array) {
        Assert.assertNotNull(array);
        final int index = GeminiUtils.getIndexInArray(value, array);
        if (index != -1) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            return geminiSlots[index];
        }
        return -1;
    }

    /**
     * Pick the best slot for ECC. The best slot should be radio on and in
     * service, if not, it should be on radio, else GEMINI_SIM_1.
     * 
     * @param phone
     * @param number
     * @return
     */
    public static int pickBestSlotForEmergencyCall(Phone phone, String number) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone gPhone = (GeminiPhone) phone;
            final int[] geminiSlots = GeminiUtils.getSlots();
            final int count = geminiSlots.length;
            boolean[] isRadioOn = new boolean[count];
            for (int i = 0; i < count; i++) {
                isRadioOn[i] = gPhone.isRadioOnGemini(geminiSlots[i]);
                int state = gPhone.getServiceStateGemini(geminiSlots[i]).getState();
                if (isRadioOn[i] && state == ServiceState.STATE_IN_SERVICE) {
                    // the slot is radio on & state is in service
                    log("pickBestSlotForEmergencyCallm, radio on & in service, slot:"
                            + geminiSlots[i]);
                    return geminiSlots[i];
                }
            }
            for (int i = 0; i < count; i++) {
                if (isRadioOn[i]) {
                    // the slot is radio on
                    log("pickBestSlotForEmergencyCallm, radio on, slot:" + geminiSlots[i]);
                    return geminiSlots[i];
                }
            }
        }
        log("pickBestSlotForEmergencyCallm, no gemini");
        return GeminiUtils.getDefaultSlot();
    }

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     * 
     * @param callManager
     * @param numberToDial
     * @param slotId
     * @param isSipPhone
     * @return
     */
    public static Connection dial(Object callManager, Phone phone, String numberToDial, int slotId)
            throws CallStateException {
        Assert.assertNotNull(callManager);
        Assert.assertNotNull(phone);
        boolean isSipPhone = phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP;
        Connection connection = null;
        try {
            if (GeminiUtils.isGeminiSupport() && !isSipPhone) {
                int dialSlot = slotId;
                if (!GeminiUtils.isValidSlot(slotId)) {
                    dialSlot = SystemProperties.getInt(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, -1);
                }
                connection = ((MTKCallManager) callManager).dialGemini(phone, numberToDial,
                        dialSlot);
            } else {
                connection = ((CallManager) callManager).dial(phone, numberToDial);
            }
        } catch (CallStateException ex) {
            throw new CallStateException("cannot dial, numberToDial:" + numberToDial + ", slotId:"
                    + slotId);
        }
        return connection;
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
