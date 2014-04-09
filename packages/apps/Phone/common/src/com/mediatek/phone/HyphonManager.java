package com.mediatek.phone;

import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

public final class HyphonManager implements CountryListener {

    private static final String TAG = "HyphonManager";
    private static final boolean DBG = true;

    private static HyphonManager sMe;

    private HashMap<String, String> mHyphonMaps;
    private Context mContext;
    private String mCurrentCountryIso;

    private HyphonManager() {
        if (DBG) {
            log("HyphonManager()");
        }
        mHyphonMaps = new HashMap<String, String>();
    }

    public static HyphonManager getInstance() {
        if (sMe == null) {
            sMe = new HyphonManager();
        }
        return sMe;
    }

    public void init(Context context) {
        mContext = context;
        mCurrentCountryIso = detectCountry(true);
    }

    public String formatNumber(String number) {
        if (null == number) {
            return null;
        }

        if (null == mContext) {
            if (DBG) {
                log("formatNumber(), mContext is null, just return null");
            }
            return null;
        }

        if (mCurrentCountryIso == null) {
            // try to detect country if it's null
            mCurrentCountryIso = detectCountry(false);
        }

        String match = mHyphonMaps.get(number);

        if (match != null) {
            return match;
        }
        match = PhoneNumberUtils.formatNumber(number, mCurrentCountryIso);

        // invalid number...
        if (match != null) {
            mHyphonMaps.put(number, match);
        } else {
            match = number;
        }

        return match;
    }

    public void onDestroy() {
        if (null != mContext) {
            CountryDetector detector =
                (CountryDetector) mContext.getSystemService(Context.COUNTRY_DETECTOR);
            detector.removeCountryListener(this);
        }
    }

    private String detectCountry(final boolean isNeedToAddListener) {
        CountryDetector detector =
            (CountryDetector) mContext.getSystemService(Context.COUNTRY_DETECTOR);
        if (isNeedToAddListener) {
            detector.addCountryListener(this, null);
        }
        final Country country = detector.detectCountry();
        if (country != null) {
            if (DBG) {
                log("detect country, iso = " + country.getCountryIso() + " source = " + country.getSource());
            }
            return country.getCountryIso();
        }
        return null;
    }

    public void onCountryDetected(Country country) {
        if (DBG) {
            log("onCountryDetected, country = " + country);
            log("mCurrentCountryIso = " + mCurrentCountryIso + ", countryIso = " + country.getCountryIso());
        }
        if (mCurrentCountryIso == null || !mCurrentCountryIso.equals(country.getCountryIso())) {
            mCurrentCountryIso = country.getCountryIso();
            mHyphonMaps.clear();
        }
    }

    private static void log(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
