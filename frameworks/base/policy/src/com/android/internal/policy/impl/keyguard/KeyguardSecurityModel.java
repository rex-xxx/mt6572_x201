/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.internal.policy.impl.keyguard;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.widget.LockPatternUtils;

import com.mediatek.common.featureoption.FeatureOption;

public class KeyguardSecurityModel {
    /**
     * The different types of security available for {@link Mode#UnlockScreen}.
     * @see com.android.internal.policy.impl.LockPatternKeyguardView#getUnlockMode()
     */
    enum SecurityMode {
        Invalid, // NULL state
        None, // No security enabled
        Pattern, // Unlock by drawing a pattern.
        Password, // Unlock by entering an alphanumeric password
        PIN, // Strictly numeric password
        Biometric, // Unlock with a biometric key (e.g. finger print or face unlock)
        Account, // Unlock by entering an account's login and password.
        AlarmBoot, // add for power-off alarm.
        SimPinPukMe1, // Unlock by entering a sim pin/puk/me for sim or gemini sim1.
        SimPinPukMe2, // Unlock by entering a sim pin/puk/me for sim or gemini sim2.
        /// M: Support GeminiPlus
        SimPinPukMe3, // Unlock by entering a sim pin/puk/me for sim or gemini sim3.
        SimPinPukMe4, // Unlock by entering a sim pin/puk/me for sim or gemini sim4.
        Voice // Unlock with voice password
    }

    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    KeyguardSecurityModel(Context context) {
        mContext = context;
        mLockPatternUtils = new LockPatternUtils(context);
    }

    void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    /**
     * Returns true if biometric unlock is installed and selected.  If this returns false there is
     * no need to even construct the biometric unlock.
     */
    boolean isBiometricUnlockEnabled() {
        return mLockPatternUtils.usingBiometricWeak()
                && mLockPatternUtils.isBiometricWeakInstalled();
    }
    
    /**
     * M:
     * Returns true if voice unlock is support and selected.  If this returns false there is
     * no need to even construct the voice unlock.
     */
    boolean isVoiceUnlockEnabled() {
        return mLockPatternUtils.usingVoiceWeak()
                && FeatureOption.MTK_VOICE_UNLOCK_SUPPORT;
    }

    /**
     * Returns true if a condition is currently suppressing the biometric unlock.  If this returns
     * true there is no need to even construct the biometric unlock.
     */
    private boolean isBiometricUnlockSuppressed() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(mContext);
        final boolean backupIsTimedOut = monitor.getFailedUnlockAttempts() >=
                LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT;
        return monitor.getMaxBiometricUnlockAttemptsReached() || backupIsTimedOut
                || !monitor.isAlternateUnlockEnabled()
                || monitor.getPhoneState() != TelephonyManager.CALL_STATE_IDLE;
    }

    SecurityMode getSecurityMode() {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        /// M: If dm lock is on, we always show KeygaurdSelector view@{
        boolean dmLocked = updateMonitor.dmIsLocked();
        if (dmLocked) {
            return SecurityMode.None;
        }
        /// @}
        
        /// M: Support GeminiPlus
        SecurityMode mode = SecurityMode.None;
        if (isPinPukOrMeRequired(PhoneConstants.GEMINI_SIM_1)) {
            mode = SecurityMode.SimPinPukMe1;
        } else if (isPinPukOrMeRequired(PhoneConstants.GEMINI_SIM_2)) {
            mode = SecurityMode.SimPinPukMe2;
        } else if (isPinPukOrMeRequired(PhoneConstants.GEMINI_SIM_3)) {
            mode = SecurityMode.SimPinPukMe3;
        } else if (isPinPukOrMeRequired(PhoneConstants.GEMINI_SIM_4)) {
            mode = SecurityMode.SimPinPukMe4;
        } else if (KeyguardUpdateMonitor.isAlarmBoot()) {/// M: add for power-off alarm
            mode = SecurityMode.AlarmBoot;
        } else {
            final int security = mLockPatternUtils.getKeyguardStoredPasswordQuality();
            switch (security) {
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                    mode = mLockPatternUtils.isLockPasswordEnabled() ?
                            SecurityMode.PIN : SecurityMode.None;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    mode = mLockPatternUtils.isLockPasswordEnabled() ?
                            SecurityMode.Password : SecurityMode.None;
                    break;

                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    if (mLockPatternUtils.isLockPatternEnabled()) {
                        mode = mLockPatternUtils.isPermanentlyLocked() ?
                            SecurityMode.Account : SecurityMode.Pattern;
                    }
                    break;

                default:
                    throw new IllegalStateException("Unknown unlock mode:" + mode);
            }
        }
        return mode;
    }

    /**
     * Some unlock methods can have an alternate, such as biometric unlocks (e.g. face unlock).
     * This function decides if an alternate unlock is available and returns it. Otherwise,
     * returns @param mode.
     *
     * @param mode the mode we want the alternate for
     * @return alternate or the given mode
     */
    SecurityMode getAlternateFor(SecurityMode mode) {
        if (!isBiometricUnlockSuppressed() && (mode == SecurityMode.Password
                        || mode == SecurityMode.PIN
                        || mode == SecurityMode.Pattern)) {
            if (isBiometricUnlockEnabled()) {
                return SecurityMode.Biometric;
            } else if (isVoiceUnlockEnabled()) { ///M add for voice unlock
                return SecurityMode.Voice;
            }
        }
        return mode; // no alternate, return what was given
    }

    /**
     * Some unlock methods can have a backup which gives the user another way to get into
     * the device. This is currently only supported for Biometric and Pattern unlock.
     *
     * @return backup method or current security mode
     */
    SecurityMode getBackupSecurityMode(SecurityMode mode) {
        switch(mode) {
            case Biometric:
            case Voice: ///M: add for voice unlock
                return getSecurityMode();
            case Pattern:
                return SecurityMode.Account;
        }
        return mode; // no backup, return current security mode
    }

    /// M: Support GeminiPlus
    private boolean isPinPukOrMeRequired(int simId) {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        if(updateMonitor != null && KeyguardUtils.isValidSimId(simId)) {
            final IccCardConstants.State simState = updateMonitor.getSimState(simId);
                      // check PIN required
            return ( (simState == IccCardConstants.State.PIN_REQUIRED 
                && !updateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PIN))
                // check PUK required
                || (simState == IccCardConstants.State.PUK_REQUIRED 
                && !updateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PUK)
                && updateMonitor.getRetryPukCount(simId) != 0)
                // check ME required
                || (simState == IccCardConstants.State.NETWORK_LOCKED
                && !updateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_ME)
                && updateMonitor.getSimMeLeftRetryCount(simId) != 0)
                );
        } else {
            return false;
        }
    }
}
