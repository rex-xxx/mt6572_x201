package com.mediatek.encapsulation.com.mediatek.telephony;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import com.android.internal.telephony.IPhoneSubInfo;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyService;
import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedTelephonyProperties;
import com.mediatek.telephony.TelephonyManagerEx;

import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.util.Log;
import java.util.List;

public class EncapsulatedTelephonyManagerEx {
    private static final String TAG = "TelephonyManagerEx";

    private Context mContext = null;
    private ITelephonyRegistry mRegistry;


    /* Add for  Phone2 */
    private ITelephonyRegistry mRegistry2;
    private static int defaultSimId = EncapsulatedPhone.GEMINI_SIM_1;

    /** M: Add MTK TelephonyManagerEx */
    private TelephonyManagerEx mTelephonyManagerEx = TelephonyManagerEx.getDefault();

    /**
     * Construction function for TelephonyManager
     * @param context a context
     */
    public EncapsulatedTelephonyManagerEx(Context context) {
        Log.d( TAG,"getSubscriberInfo");
        mContext = context;
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry"));

        /* Add for Gemini Phone2 */
        mRegistry2 = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService(
                    "telephony.registry2"));
    }

    /*  Construction function for TelephonyManager */
    private EncapsulatedTelephonyManagerEx() {
    }

    private  static EncapsulatedTelephonyManagerEx sInstance = new EncapsulatedTelephonyManagerEx();

    /**
     *  Get static instance of TelephonyManager
     */
    public static EncapsulatedTelephonyManagerEx getDefault() {
        return sInstance;
    }

    /**
     * @param simId sim card id
     * @return Get IPhoneSubInfo service
     */
    public IPhoneSubInfo getSubscriberInfo(int simId) {
        Log.d( TAG,"getSubscriberInfo");
        // get it each time because that process crashes a lot
        if (EncapsulatedPhone.GEMINI_SIM_1 == simId) {
            return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
        } else {
            return IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo2"));
        }
    }

    //
    //
    // Device Info
    //
    //

    /**
     * Requires Permission:
     *  android.Manifest.permission READ_PHONE_STATE READ_PHONE_STATE
     *
     * @return  the unique device ID, for example, the IMEI for GSM and the MEID
     * for CDMA phones. Return null if device ID is not available.
     */
    public String getDeviceId(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getDeviceId(simId);
        } else {
            Log.d( TAG,"getDeviceId");
            try {
                return getSubscriberInfo(simId).getDeviceId();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    /**
     * Requires Permission:
     *  android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_COARSE_LOCATION or
     *  android.Manifest.permission#ACCESS_COARSE_LOCATION ACCESS_FINE_LOCATION.
     * @return the current location of the device.
     * Return null if current location is not available.
     *
     */
    public CellLocation getCellLocation(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getCellLocation(simId);
        } else {
            Log.d( TAG,"getCellLocation");
            try {
                Bundle bundle = getITelephony().getCellLocationGemini(simId);
                return CellLocation.newFromBundle(bundle);
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    /**
     * Requires Permission:
     *  android.Manifest.permission#ACCESS_COARSE_UPDATES
     *
     * @return List of NeighboringCellInfo or null if info unavailable.
     *
     */
    public List<NeighboringCellInfo> getNeighboringCellInfo(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getNeighboringCellInfo(simId);
        } else {
            Log.d( TAG,"getNeighboringCellInfo");
            try {
                return getITelephony().getNeighboringCellInfoGemini(simId);
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    /**
     *
     * @return a constant indicating the device phone type.
     *
     */
    public int getPhoneType(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getPhoneType(simId);
        } else {
            Log.d( TAG,"getPhoneType");
            try{
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getActivePhoneTypeGemini(simId);
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return getPhoneTypeFromProperty();
                }
            } catch (RemoteException ex) {
                // This shouldn't happen in the normal case, as a backup we
                // read from the system property.
                return getPhoneTypeFromProperty();
            } catch (NullPointerException ex) {
                // This shouldn't happen in the normal case, as a backup we
                // read from the system property.
                return getPhoneTypeFromProperty();
            }
        }
    }

    //
    // Current Network
    //

    /**
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use getPhoneType(int simId)) to determine if
     * on a CDMA network).
     *
     * @return the alphabetic name of current registered operator.
     */
    public String getNetworkOperatorName(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getNetworkOperatorName(simId);
        } else {
            Log.d( TAG,"getNetworkOperatorName");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ALPHA)
                : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ALPHA_2);
        }
    }

    /**
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use getPhoneType(int simId)) to determine if
     * on a CDMA network).
     * @return the numeric name (MCC+MNC) of current registered operator.
     */
    public String getNetworkOperator(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getNetworkOperator(simId);
        } else {
            Log.d( TAG,"getNetworkOperator");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_NUMERIC)
                : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_NUMERIC_2);
        }
    }

    /**
     *
     * @return true if the device is considered roaming on the current
     * network, for GSM purposes.
     * <p>
     * Availability: Only when user registered to a network.
     */
    public boolean isNetworkRoaming(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.isNetworkRoaming(simId);
        } else {
            Log.d( TAG,"isNetworkRoaming");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                ? "true".equals(SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ISROAMING))
                : "true".equals(SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ISROAMING_2));
        }
    }

    /**
     *
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use getPhoneType(int simId)) to determine if
     * on a CDMA network).
     * @return the ISO country code equivilent of the current registered
     * operator's MCC (Mobile Country Code).
     */
    public String getNetworkCountryIso(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getNetworkCountryIso(simId);
        } else {
            Log.d( TAG,"getNetworkCountryIso");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY)
                : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_2);
        }
    }

    /**
     *
     * @return a constant indicating the radio technology (network type)
     * currently in use on the device.
     *
     */
    public int getNetworkType(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getNetworkType(simId);
        } else {
            Log.d( TAG,"getNetworkType");
            try{
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getNetworkTypeGemini(simId);
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
                }
            } catch(RemoteException ex) {
                // This shouldn't happen in the normal case
                return android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
            }
        }
    }


    //
    //
    // SIM Card
    //
    //
    /**
     * @return true if a ICC card is present
     */
    public boolean hasIccCard(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.hasIccCard(simId);
        } else {
            Log.d( TAG,"hasIccCard");
            try {
                return getITelephony().hasIccCardGemini(simId);
            } catch (RemoteException ex) {
                // Assume no ICC card if remote exception which shouldn't happen
                return false;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return false;
            }
        }
    }

    /**
     *
     * @return a constant indicating the state of the
     * device SIM card.
     *
     */
    public int getSimState(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSimState(simId);
        } else {
            Log.d( TAG,"getSimState");
            String prop = (simId == EncapsulatedPhone.GEMINI_SIM_1)
                          ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_SIM_STATE)
                          : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_SIM_STATE_2);
            if ("ABSENT".equals(prop)) {
                return android.telephony.TelephonyManager.SIM_STATE_ABSENT;
            }
            else if ("PIN_REQUIRED".equals(prop)) {
                return android.telephony.TelephonyManager.SIM_STATE_PIN_REQUIRED;
            }
            else if ("PUK_REQUIRED".equals(prop)) {
                return android.telephony.TelephonyManager.SIM_STATE_PUK_REQUIRED;
            }
            else if ("NETWORK_LOCKED".equals(prop)) {
                return android.telephony.TelephonyManager.SIM_STATE_NETWORK_LOCKED;
            }
            else if ("READY".equals(prop)) {
                return android.telephony.TelephonyManager.SIM_STATE_READY;
            }
            else {
                return android.telephony.TelephonyManager.SIM_STATE_UNKNOWN;
            }
        }
    }

    /**
     * Availability: SIM state must be SIM_STATE_READY
     *
     * @return the MCC+MNC (mobile country code + mobile network code) of the
     * provider of the SIM. 5 or 6 decimal digits.
     */
    public String getSimOperator(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSimOperator(simId);
        } else {
            Log.d( TAG,"getSimOperator");
            if (EncapsulatedPhone.GEMINI_SIM_1 == simId) {
                return SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
            } else {
                return SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2);
            }
        }
    }

    /**
     *
     * Availability: SIM state must be SIM_STATE_READY.
     * @return the Service Provider Name (SPN).
     *
     */
    public String getSimOperatorName(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSimOperatorName(simId);
        } else {
            Log.d( TAG,"getSimOperatorName");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                   ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA)
                   : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA_2);
        }
    }

    /**
     *
     * @return the ISO country code equivalent for the SIM provider's country code.
     */
    public String getSimCountryIso(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSimCountryIso(simId);
        } else {
            Log.d( TAG,"getSimCountryIso");
            return (simId == EncapsulatedPhone.GEMINI_SIM_1)
                   ? SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY)
                   : SystemProperties.get(EncapsulatedTelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2);
        }
    }

    /**
     *
     * Requires Permission:
     *    android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE
     * @return the serial number of the SIM, if applicable. Return null if it is
     * unavailable.
     */
    public String getSimSerialNumber(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSimSerialNumber(simId);
        } else {
            Log.d( TAG,"getSimSerialNumber");
            try {
                return getSubscriberInfo(simId).getIccSerialNumber();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return null;
            }
        }
    }

    //
    //
    // Subscriber Info
    //
    //

    /**
     *
     * Requires Permission:
     *    android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE
     * @return the unique subscriber ID, for example, the IMSI for a GSM phone.
     * Return null if it is unavailable.
     */
    public String getSubscriberId(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSubscriberId(simId);
        } else {
            Log.d( TAG,"getSubscriberId");
            try {
                return getSubscriberInfo(simId).getSubscriberId();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return null;
            }
        }
    }

    /**
     * 
     * Requires Permission:
     *   android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE
     * @return the phone number string for line 1, for example, the MSISDN
     * for a GSM phone. Return null if it is unavailable.
     */
    public String getLine1Number(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getLine1Number(simId);
        } else {
            Log.d( TAG,"getLine1Number");
            try {
                return getSubscriberInfo(simId).getLine1Number();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return null;
            }
        }
    }

    /**
     *
     * Requires Permission:
     *   android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE
     * @return the voice mail number. Return null if it is unavailable.
     */
    public String getVoiceMailNumber(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getVoiceMailNumber(simId);
        } else {
            Log.d( TAG,"getVoiceMailNumber");
            try {
                return getSubscriberInfo(simId).getVoiceMailNumber();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return null;
            }
        }
    }

    /**
     *
     * Requires Permission:
     *   android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE
     * Retrieves the alphabetic identifier associated with the voice
     * mail number.
     */
    public String getVoiceMailAlphaTag(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getVoiceMailAlphaTag(simId);
        } else {
            Log.d( TAG,"getVoiceMailAlphaTag");
            try {
                return getSubscriberInfo(simId).getVoiceMailAlphaTag();
            } catch (RemoteException ex) {
                return null;
            } catch (NullPointerException ex) {
                // This could happen before phone restarts due to crashing
                return null;
            }
        }
    }

      /**
     *
     * @return a constant indicating the call state (cellular) on the device.
     */
    public int getCallState(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getCallState(simId);
        } else {
            Log.d( TAG,"getCallState");
            try {
                return getITelephony().getCallStateGemini(simId);
            } catch (RemoteException ex) {
                // the phone process is restarting.
                return android.telephony.TelephonyManager.CALL_STATE_IDLE;
            } catch (NullPointerException ex) {
              // the phone process is restarting.
              return android.telephony.TelephonyManager.CALL_STATE_IDLE;
            }
        }
    }

    /**
     * @return a constant indicating the type of activity on a data connection
     * (cellular).
     *
     */
    public int getDataActivity(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getDataActivity(simId);
        } else {
            Log.d( TAG,"getDataActivity");
            try {
                return getITelephony().getDataActivityGemini(simId);
            } catch (RemoteException ex) {
                // the phone process is restarting.
                return android.telephony.TelephonyManager.DATA_ACTIVITY_NONE;
            } catch (NullPointerException ex) {
              // the phone process is restarting.
              return android.telephony.TelephonyManager.DATA_ACTIVITY_NONE;
            }
        }
    }

    /**
     * @return a constant indicating the current data connection state
     * (cellular).
     *
     */
    public int getDataState(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getDataState(simId);
        } else {
            Log.d( TAG,"getDataState");
            try {
                return getITelephony().getDataStateGemini(simId);
            } catch (RemoteException ex) {
                // the phone process is restarting.
                return android.telephony.TelephonyManager.DATA_DISCONNECTED;
            } catch (NullPointerException ex) {
                return android.telephony.TelephonyManager.DATA_DISCONNECTED;
            }
        }
    }


    //
    //
    // PhoneStateListener
    //
    //


    /**
     *
     * Registers a listener object to receive notification of changes
     * in specified telephony states.
     *
     * To register a listener, pass a PhoneStateListener
     * and specify at least one telephony state of interest in
     * the events argument.
     *
     * At registration, and when a specified telephony state
     * changes, the telephony manager invokes the appropriate
     * callback method on the listener object and passes the
     * current (udpated) values.
     *
     * To unregister a listener, pass the listener object and set the
     * events argument to PhoneStateListener LISTEN_NONE LISTEN_NONE.
     *
     * @param listener The PhoneStateListener object to register
     *                 (or unregister)
     * @param events The telephony state(s) of interest to the listener,
     *               as a bitwise-OR combination of PhoneStateListener
     *               LISTEN_ flags.
     */
    public void listen(PhoneStateListener listener, int events, int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mTelephonyManagerEx.listen(listener, events, simId);
        } else {
            Log.d( TAG,"listen");
            String pkgForDebug = mContext != null ? mContext.getPackageName() : "<unknown>";
            try {
                Boolean notifyNow = (getITelephony() != null);
                if (EncapsulatedPhone.GEMINI_SIM_1 == simId) {
                    mRegistry.listen(pkgForDebug, listener.getCallback(), events, notifyNow);
                } else {
                    mRegistry2.listen(pkgForDebug, listener.getCallback(), events, notifyNow);
                }
            } catch (RemoteException ex) {
                // system process dead
            } catch (NullPointerException ex) {
                // system process dead
            }
        }
    }


    /**
     * @return the barcode number of the phone,
     *
     */
    /*Add by mtk80372 for Barcode number*/
    public String getSN() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManagerEx.getSN();
        } else {
            Log.d( TAG,"getSN");
            try {
                return getITelephony().getSN();
            } catch (RemoteException ex) {
                // the phone process is restarting.
                return null;
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }

    private EncapsulatedTelephonyService getITelephony() {
        return EncapsulatedTelephonyService.getInstance();
    }

    /* Get current active phone type by system property, GsmPhone or CdmaPhone */
    private int getPhoneTypeFromProperty() {
        int type =
            SystemProperties.getInt(EncapsulatedTelephonyProperties.CURRENT_ACTIVE_PHONE,
                        getPhoneTypeFromNetworkType());
        return type;
    }

    /* Get phone type by network type, GsmPhone or CdmaPhone */
    private int getPhoneTypeFromNetworkType() {
        // When the system property CURRENT_ACTIVE_PHONE, has not been set,
        // use the system property for default network type.
        // This is a fail safe, and can only happen at first boot.
        int mode = SystemProperties.getInt("ro.telephony.default_network", -1);
        if (mode == -1)
            return android.telephony.TelephonyManager.PHONE_TYPE_NONE;
        return PhoneFactory.getPhoneType(mode);
    }

   /**
       * Get service center address
       * @param simId SIM ID
    * @return Current service center address
    */
   public String getScAddress(int slotId) {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.getScAddress(slotId);
       } else {
           try {
               return getITelephony().getScAddressGemini(slotId);
           } catch(RemoteException e1) {
               return null;
           } catch(NullPointerException e2) {
               return null;
           }
       }
   }

   /**
       * Set service center address
       * @param address Address to be set
       * @param simId SIM ID
       * @return True for success, false for failure
    *
    */
   public boolean setScAddress(String address, int slotId) {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.setScAddress(address, slotId);
       } else {
           try {
               getITelephony().setScAddressGemini(address, slotId);
               return true;
           } catch(RemoteException e1) {
               return false;
           } catch(NullPointerException e2) {
               return false;
           }
       }
   }

   /**
     *get the services state for default SIM
     * @return sim indicator state.
     *
    */
   public int getSimIndicatorState() {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.getSimIndicatorState();
       } else {
           try {
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getSimIndicatorState();
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
                }
           } catch (RemoteException ex) {
               // the phone process is restarting.
               return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
           } catch (NullPointerException ex) {
               return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
           }
       }
   }

   /**
     *get the services state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return sim indicator state.
     *
    */
   public int getSimIndicatorStateGemini(int simId){
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.getSimIndicatorStateGemini(simId);
       } else {
           try {
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getSimIndicatorStateGemini(simId);
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
                }
           } catch (RemoteException ex) {
               // the phone process is restarting.
               return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
           } catch (NullPointerException ex) {
               return EncapsulatedPhone.SIM_INDICATOR_UNKNOWN;
           }
       }
   }

   /**
     *get the network service state for default SIM
     * @return service state.
     *
    */
   public Bundle getServiceState() {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.getServiceState();
       } else {
           try {
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getServiceState();
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return null;
                }
           } catch (RemoteException ex) {
               // the phone process is restarting.
               return null;
           } catch (NullPointerException ex) {
               return null;
           }
       }
   }

   /**
     * get the network service state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return service state.
     *
    */
   public Bundle getServiceState(int simId){
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return mTelephonyManagerEx.getServiceState(simId);
       } else {
           try {
                EncapsulatedTelephonyService telephony = getITelephony();
                if (telephony != null) {
                    return telephony.getServiceStateGemini(simId);
                } else {
                    // This can happen when the ITelephony interface is not up yet.
                    return null;
                }
           } catch (RemoteException ex) {
               // the phone process is restarting.
               return null;
           } catch (NullPointerException ex) {
               return null;
           }
       }
   }
}
