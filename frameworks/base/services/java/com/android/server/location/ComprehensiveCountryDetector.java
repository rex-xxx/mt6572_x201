/*
 * Copyright (C) 2010 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.location;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.Geocoder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.Phone;
import com.mediatek.common.featureoption.FeatureOption;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is used to detect the country where the user is. The sources of
 * country are queried in order of reliability, like
 * <ul>
 * <li>Mobile network</li>
 * <li>Location</li>
 * <li>SIM's country</li>
 * <li>Phone's locale</li>
 * </ul>
 * <p>
 * Call the {@link #detectCountry()} to get the available country immediately.
 * <p>
 * To be notified of the future country change, using the
 * {@link #setCountryListener(CountryListener)}
 * <p>
 * Using the {@link #stop()} to stop listening to the country change.
 * <p>
 * The country information will be refreshed every
 * {@link #LOCATION_REFRESH_INTERVAL} once the location based country is used.
 *
 * @hide
 */
public class ComprehensiveCountryDetector extends CountryDetectorBase {

    private final static String TAG = "CountryDetector";
    /* package */ static final boolean DEBUG = true;

    /**
     * Max length of logs to maintain for debugging.
     */
    private static final int MAX_LENGTH_DEBUG_LOGS = 20;

    /**
     * The refresh interval when the location based country was used
     */
    private final static long LOCATION_REFRESH_INTERVAL = 1000 * 60 * 60 * 24; // 1 day

    protected CountryDetectorBase mLocationBasedCountryDetector;
    protected Timer mLocationRefreshTimer;

    private Country mCountry;
    private final TelephonyManager mTelephonyManager;
    private ITelephony mITelephony;
    private ITelephonyRegistry mRegistry;
    private ITelephonyRegistry mRegistry2;
    private Country mCountryFromLocation;
    private boolean mStopped = false;

    private PhoneStateListener mPhoneStateListener;

    /**
     * List of the most recent country state changes for debugging. This should have
     * a max length of MAX_LENGTH_LOGS.
     */
    private final ConcurrentLinkedQueue<Country> mDebugLogs = new ConcurrentLinkedQueue<Country>();

    /**
     * Most recent {@link Country} result that was added to the debug logs {@link #mDebugLogs}.
     * We keep track of this value to help prevent adding many of the same {@link Country} objects
     * to the logs.
     */
    private Country mLastCountryAddedToLogs;

    /**
     * Object used to synchronize access to {@link #mLastCountryAddedToLogs}. Be careful if
     * using it to synchronize anything else in this file.
     */
    private final Object mObject = new Object();

    /**
     * Start time of the current session for which the detector has been active.
     */
    private long mStartTime;

    /**
     * Stop time of the most recent session for which the detector was active.
     */
    private long mStopTime;

    /**
     * The sum of all the time intervals in which the detector was active.
     */
    private long mTotalTime;

    /**
     * Number of {@link PhoneStateListener#onServiceStateChanged(ServiceState state)} events that
     * have occurred for the current session for which the detector has been active.
     */
    private int mCountServiceStateChanges;

    /**
     * Total number of {@link PhoneStateListener#onServiceStateChanged(ServiceState state)} events
     * that have occurred for all time intervals in which the detector has been active.
     */
    private int mTotalCountServiceStateChanges;

    /**
     * The listener for receiving the notification from LocationBasedCountryDetector.
     */
    private CountryListener mLocationBasedCountryDetectionListener = new CountryListener() {
        @Override
        public void onCountryDetected(Country country) {
            if (DEBUG) Slog.d(TAG, "Country detected via LocationBasedCountryDetector");
            Log.d(TAG, "LBS CountryDetector CountryDetected From country: "
                            + (mCountryFromLocation != null ? mCountryFromLocation.getCountryIso()
                                    : "null")
                            + " To country: "
                            + (country != null ? country.getCountryIso() : "null"));
            mCountryFromLocation = country;
            // Don't start the LocationBasedCountryDetector.
            detectCountry(true, false);
            stopLocationBasedDetector();
        }
    };
    
    /*
     * MTK add start attribute MTK81084 chen.wang[ALPS00244092/ALPS00244650]
     * ALPS00244092 [Need Patch] [Volunteer Patch] Country Detect Service can
     * not updates its result after contury changes ALPS00244650 [Need Patch]
     * [Volunteer Patch] add Gemini Support for Country Detect Service
     */
    private Country mNotifyCountry;
    private ServiceState mLastState;
    private ServiceState mLastState2;
    private PhoneStateListener mPhoneStateListener2;
    private Context mContext = null;

    // MTK add end

    public ComprehensiveCountryDetector(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(mTelephonyManager == null)
              Log.d("country","mTelephonyManager is null");   
        else
              Log.d("country","mTelephonyManager is not null");  
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        if(mITelephony == null)
              Log.d("country","mITelephony is null");   
        else
              Log.d("country","mITelephony is not null");   
        mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
        if(mRegistry == null)
              Log.d("country","mRegistry is null");   
        else
              Log.d("country","mRegistry is not null");   
        mRegistry2 = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry2"));
        if(mRegistry2 == null)
              Log.d("country","mRegistry2 is null");   
        else
              Log.d("country","mRegistry2 is not null");   
    }

    @Override
    public Country detectCountry() {
        // Don't start the LocationBasedCountryDetector if we have been stopped.
        return detectCountry(false, !mStopped);
    }

    @Override
    public void stop() {
        // Note: this method in this subclass called only by tests.
        Slog.i(TAG, "Stop the detector.");
        cancelLocationRefresh();
        removePhoneStateListener();
        stopLocationBasedDetector();
        mListener = null;
        mStopped = true;
    }

    /**
     * Get the country from different sources in order of the reliability.
     */
    private Country getCountry() {
        Country result = null;
        if (isAirplaneModeOff()) {
            result = getNetworkBasedCountry();
            Log.d(TAG, "getNetworkBasedCountry: "
                    + (result != null ? result.getCountryIso() : "null"));
            if (result == null) {
                result = getLastKnownLocationBasedCountry();
                Log.d(TAG, "getLastKnownLocationBasedCountry: "
                        + (result != null ? result.getCountryIso() : "null"));
            }
            if (result == null) {
                result = getSimBasedCountry();
                Log.d(TAG, "getSimBasedCountry: "
                        + (result != null ? result.getCountryIso() : "null"));
            }
        }
        if (result == null) {
            result = getLocaleCountry();
            Log.d(TAG, "getLocaleCountry: " + (result != null ? result.getCountryIso() : "null"));
        }
        addToLogs(result);
        return result;
    }

    /**
     * Attempt to add this {@link Country} to the debug logs.
     */
    private void addToLogs(Country country) {
        if (country == null) {
            return;
        }
        // If the country (ISO and source) are the same as before, then there is no
        // need to add this country as another entry in the logs. Synchronize access to this
        // variable since multiple threads could be calling this method.
        synchronized (mObject) {
            if (mLastCountryAddedToLogs != null && mLastCountryAddedToLogs.equals(country)) {
                return;
            }
            mLastCountryAddedToLogs = country;
        }
        // Manually maintain a max limit for the list of logs
        if (mDebugLogs.size() >= MAX_LENGTH_DEBUG_LOGS) {
            mDebugLogs.poll();
        }
        if (DEBUG) {
            Slog.d(TAG, country.toString());
        }
        mDebugLogs.add(country);
    }

    private boolean isNetworkCountryCodeAvailable() {
        // On CDMA TelephonyManager.getNetworkCountryIso() just returns SIM country.  We don't want
        // to prioritize it over location based country, so ignore it.
        
        //final int phoneType = mTelephonyManager.getPhoneType();
        /*
         * MTK code change: This can not be final as this function maight be
         * called very early in the phone booting process. which caused the
         * getPhoneType() function returns invalid values and as the final
         * variable can only be initialized once. MTK81084 chen.wang
         */
        int phoneType = mTelephonyManager.getPhoneType();
        if (DEBUG) Slog.v(TAG, "phonetype=" + phoneType);
        return phoneType == TelephonyManager.PHONE_TYPE_GSM;
    }

    /**
     * @return the country from the mobile network.
     */
    /*MTK code change: For GEMINI support MTK81084 chen.wang
    */
    protected Country getNetworkBasedCountry() {
        String countryIso = null;
        if (isNetworkCountryCodeAvailable()) {
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                countryIso = mTelephonyManager.getNetworkCountryIso();
                if (!TextUtils.isEmpty(countryIso)) {
                    return new Country(countryIso, Country.COUNTRY_SOURCE_NETWORK);
                }
            } else {
                try {
                     mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                     if(mITelephony != null)
                          countryIso = mITelephony.getNetworkCountryIsoGemini(0);
                     else
                          Log.d("country","mITelephony is null");   
                } catch (RemoteException e) {
                      
                }
                if (!TextUtils.isEmpty(countryIso)) {
                    return new Country(countryIso, Country.COUNTRY_SOURCE_NETWORK);
                } else {
	            try {
                        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                        if(mITelephony != null)
                    	         countryIso = mITelephony.getNetworkCountryIsoGemini(1);
                        else
                                    Log.d("country","mITelephony is null");   
                    } catch (RemoteException e) {
                      
                     }
                    if (!TextUtils.isEmpty(countryIso)) {
                        return new Country(countryIso, Country.COUNTRY_SOURCE_NETWORK);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return the cached location based country.
     */
    protected Country getLastKnownLocationBasedCountry() {
        return mCountryFromLocation;
    }

    /**
     * @return the country from SIM card
     */
    /*MTK code change: For GEMINI support MTK81084 chen.wang
    */
    protected Country getSimBasedCountry() {
        String countryIso = null;
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            try {
                 mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                 if(mITelephony != null)
                      countryIso = mITelephony.getSimCountryIso(0);
                else
                     Log.d("country","mITelephony is null");   
            } catch (RemoteException e) {
                      
            }
            if (!TextUtils.isEmpty(countryIso)) {
                return new Country(countryIso, Country.COUNTRY_SOURCE_SIM);
            }
        } else {
            try {
                 mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                 if(mITelephony != null)
                          countryIso = mITelephony.getSimCountryIso(0);
                 else
                          Log.d("country","mITelephony is null");   
            } catch (RemoteException e) {
                      
            }
            if (!TextUtils.isEmpty(countryIso)) {
                return new Country(countryIso, Country.COUNTRY_SOURCE_SIM);
            } else {
            try {
                 mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                    if(mITelephony != null)
                          countryIso = mITelephony.getSimCountryIso(1);
                    else
                          Log.d("country","mITelephony is null");   
            	} catch (RemoteException e) {
                      
            	}
                if (!TextUtils.isEmpty(countryIso)) {
                    return new Country(countryIso, Country.COUNTRY_SOURCE_SIM);
                }
            }
        }
        return null;
    }

    /**
     * @return the country from the system's locale.
     */
    protected Country getLocaleCountry() {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null) {
            return new Country(defaultLocale.getCountry(), Country.COUNTRY_SOURCE_LOCALE);
        } else {
            return null;
        }
    }

    /**
     * @param notifyChange indicates whether the listener should be notified the change of the
     * country
     * @param startLocationBasedDetection indicates whether the LocationBasedCountryDetector could
     * be started if the current country source is less reliable than the location.
     * @return the current available UserCountry
     */
    private Country detectCountry(boolean notifyChange, boolean startLocationBasedDetection) {
        Country country = getCountry();
        runAfterDetectionAsync(mCountry != null ? new Country(mCountry) : mCountry, country,
                notifyChange, startLocationBasedDetection);
        mCountry = country;
        return mCountry;
    }

    /**
     * Run the tasks in the service's thread.
     */
    protected void runAfterDetectionAsync(final Country country, final Country detectedCountry,
            final boolean notifyChange, final boolean startLocationBasedDetection) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                runAfterDetection(
                        country, detectedCountry, notifyChange, startLocationBasedDetection);
            }
        });
    }

    @Override
    public void setCountryListener(CountryListener listener) {
        CountryListener prevListener = mListener;
        mListener = listener;
        if (mListener == null) {
            // Stop listening all services
            removePhoneStateListener();
            stopLocationBasedDetector();
            cancelLocationRefresh();
            mStopTime = SystemClock.elapsedRealtime();
            mTotalTime += mStopTime;
        } else if (prevListener == null) {
            addPhoneStateListener();
            detectCountry(false, true);
            mStartTime = SystemClock.elapsedRealtime();
            mStopTime = 0;
            mCountServiceStateChanges = 0;
        }
    }

    void runAfterDetection(final Country country, final Country detectedCountry,
            final boolean notifyChange, final boolean startLocationBasedDetection) {
        if (notifyChange) {
            //notifyIfCountryChanged(country, detectedCountry);
            notifyIfCountryChanged(mNotifyCountry, detectedCountry);
        }
        if (DEBUG) {
            Slog.d(TAG, "startLocationBasedDetection=" + startLocationBasedDetection
                    + " detectCountry=" + (detectedCountry == null ? null :
                        "(source: " + detectedCountry.getSource()
                        + ", countryISO: " + detectedCountry.getCountryIso() + ")")
                    + " isAirplaneModeOff()=" + isAirplaneModeOff()
                    + " mListener=" + mListener
                    + " isGeoCoderImplemnted()=" + isGeoCoderImplemented());
        }

        if (startLocationBasedDetection && (detectedCountry == null
                || detectedCountry.getSource() > Country.COUNTRY_SOURCE_LOCATION)
                && isAirplaneModeOff() && mListener != null && isGeoCoderImplemented()) {
            if (DEBUG) Slog.d(TAG, "run startLocationBasedDetector()");
            // Start finding location when the source is less reliable than the
            // location and the airplane mode is off (as geocoder will not
            // work).
            // TODO : Shall we give up starting the detector within a
            // period of time?
            startLocationBasedDetector(mLocationBasedCountryDetectionListener);
        }
        if (detectedCountry == null
                || detectedCountry.getSource() >= Country.COUNTRY_SOURCE_LOCATION) {
            // Schedule the location refresh if the country source is
            // not more reliable than the location or no country is
            // found.
            // TODO: Listen to the preference change of GPS, Wifi etc,
            // and start detecting the country.
            scheduleLocationRefresh();
        } else {
            // Cancel the location refresh once the current source is
            // more reliable than the location.
            cancelLocationRefresh();
            stopLocationBasedDetector();
        }
    }

    /**
     * Find the country from LocationProvider.
     */
    private synchronized void startLocationBasedDetector(CountryListener listener) {
        if (mLocationBasedCountryDetector != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "starts LocationBasedDetector to detect Country code via Location info "
                    + "(e.g. GPS)");
        }
        mLocationBasedCountryDetector = createLocationBasedCountryDetector();
        mLocationBasedCountryDetector.setCountryListener(listener);
        mLocationBasedCountryDetector.detectCountry();
    }

    private synchronized void stopLocationBasedDetector() {
        if (DEBUG) {
            Slog.d(TAG, "tries to stop LocationBasedDetector "
                    + "(current detector: " + mLocationBasedCountryDetector + ")");
        }
        if (mLocationBasedCountryDetector != null) {
            mLocationBasedCountryDetector.stop();
            mLocationBasedCountryDetector = null;
        }
    }

    protected CountryDetectorBase createLocationBasedCountryDetector() {
        return new LocationBasedCountryDetector(mContext);
    }

    protected boolean isAirplaneModeOff() {
        return Settings.System.getInt(
                mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 0;
    }

    /**
     * Notify the country change.
     */
    private void notifyIfCountryChanged(final Country country, final Country detectedCountry) {
        if (detectedCountry != null && mListener != null
                && (country == null || !country.equals(detectedCountry))) {
            if (DEBUG) {
                Slog.d(TAG, "" + country + " --> " + detectedCountry);
            }
            //MTK add
            mNotifyCountry = detectedCountry;
            //MTK add
            notifyListener(detectedCountry);
        }
    }

    /**
     * Schedule the next location refresh. We will do nothing if the scheduled task exists.
     */
    private synchronized void scheduleLocationRefresh() {
        if (mLocationRefreshTimer != null) return;
        if (DEBUG) {
            Slog.d(TAG, "start periodic location refresh timer. Interval: "
                    + LOCATION_REFRESH_INTERVAL);
        }
        mLocationRefreshTimer = new Timer();
        mLocationRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (DEBUG) {
                    Slog.d(TAG, "periodic location refresh event. Starts detecting Country code");
                }
                mLocationRefreshTimer = null;
                detectCountry(false, true);
            }
        }, LOCATION_REFRESH_INTERVAL);
    }

    /**
     * Cancel the scheduled refresh task if it exists
     */
    private synchronized void cancelLocationRefresh() {
        if (mLocationRefreshTimer != null) {
            mLocationRefreshTimer.cancel();
            mLocationRefreshTimer = null;
        }
    }

    protected synchronized void addPhoneStateListener() {
        if (mPhoneStateListener == null) {
            // mtk add start
            mLastState = null;
            // mtk add end
            mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    mCountServiceStateChanges++;
                    mTotalCountServiceStateChanges++;
                    /*
                     * Google default if (!isNetworkCountryCodeAvailable()) {
                     * return; } if (DEBUG) Slog.d(TAG,
                     * "onServiceStateChanged: " + serviceState.getState());
                     * detectCountry(true, true);
                     */
                    // mtk add
                    Slog.d(TAG, "onServiceStateChanged serviceState:" + serviceState);
                    // We only care the state change
                    if (mLastState == null || mLastState.getState() != serviceState.getState()) {
                        mCountServiceStateChanges++;
                        mTotalCountServiceStateChanges++;
                        Log.d(TAG, "new detect country is called in onServiceStateChanged");
                        detectCountry(true, true);
                        mLastState = new ServiceState(serviceState);
                    }
                    // mtk add
                }
            };
            // mTelephonyManager.listen(mPhoneStateListener,
            // PhoneStateListener.LISTEN_SERVICE_STATE);
            // mtk add
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mLastState2 = null;
                mPhoneStateListener2 = new PhoneStateListener() {
                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        // TODO: Find out how often we will be notified, if this
                        // method is called too
                        // many times, let's consider querying the network.
                        Slog.d(TAG, "onServiceStateChanged serviceState:" + serviceState);
                        // We only care the state change
                        if (mLastState2 == null
                                || mLastState2.getState() != serviceState.getState()) {
                            mCountServiceStateChanges++;
                            mTotalCountServiceStateChanges++;
                            Log.d(TAG, "new detect country is called in onServiceStateChanged");
                            detectCountry(true, true);
                            mLastState2 = new ServiceState(serviceState);
                        }
                    }
                };
                listen(mPhoneStateListener,PhoneStateListener.LISTEN_SERVICE_STATE, 0);
                listen(mPhoneStateListener2,PhoneStateListener.LISTEN_SERVICE_STATE, 1);
            } else {
                mTelephonyManager.listen(mPhoneStateListener,
                        PhoneStateListener.LISTEN_SERVICE_STATE);
            }
            // mtk add
        }
      }
    
	public void listen(PhoneStateListener listener, int events, int simId) {
		String pkgForDebug = mContext != null ? mContext.getPackageName()
				: "<unknown>";
		try {
			Boolean notifyNow = true;
			if (0 == simId) {
                                    if(mRegistry != null)
				        mRegistry.listen(pkgForDebug, listener.getCallback(), events,
						notifyNow);
			} else {
                                   if(mRegistry2 != null)
				         mRegistry2.listen(pkgForDebug, listener.getCallback(), events,
						notifyNow);
			}
		} catch (RemoteException ex) {
			// system process dead
		} catch (NullPointerException ex) {
			// system process dead
		}
	}

    protected synchronized void removePhoneStateListener() {
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            if (mPhoneStateListener != null) {
                mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener = null;
            }
        } else {
            if (mPhoneStateListener != null) {
                listen(mPhoneStateListener,PhoneStateListener.LISTEN_NONE, 0);
                mPhoneStateListener = null;
            }
            if (mPhoneStateListener2 != null) {
                listen(mPhoneStateListener2,PhoneStateListener.LISTEN_NONE, 1);
                mPhoneStateListener2 = null;
            }
        }
    }

    protected boolean isGeoCoderImplemented() {
        return Geocoder.isPresent();
    }

    @Override
    public String toString() {
        long currentTime = SystemClock.elapsedRealtime();
        long currentSessionLength = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("ComprehensiveCountryDetector{");
        // The detector hasn't stopped yet --> still running
        if (mStopTime == 0) {
            currentSessionLength = currentTime - mStartTime;
            sb.append("timeRunning=" + currentSessionLength + ", ");
        } else {
            // Otherwise, it has already stopped, so take the last session
            sb.append("lastRunTimeLength=" + (mStopTime - mStartTime) + ", ");
        }
        sb.append("totalCountServiceStateChanges=" + mTotalCountServiceStateChanges + ", ");
        sb.append("currentCountServiceStateChanges=" + mCountServiceStateChanges + ", ");
        sb.append("totalTime=" + (mTotalTime + currentSessionLength) + ", ");
        sb.append("currentTime=" + currentTime + ", ");
        sb.append("countries=");
        for (Country country : mDebugLogs) {
            sb.append("\n   " + country.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
