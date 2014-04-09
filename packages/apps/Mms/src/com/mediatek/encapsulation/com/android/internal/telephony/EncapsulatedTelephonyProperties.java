package com.mediatek.encapsulation.com.android.internal.telephony;

import com.android.internal.telephony.TelephonyProperties;
import com.mediatek.encapsulation.EncapsulationConstant;

public interface EncapsulatedTelephonyProperties extends TelephonyProperties {

    //MTK-START [mtk04070][111118][ALPS00093395]MTK added
    static final String PROPERTY_OPERATOR_ALPHA_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2 : "gsm.operator.alpha.2";
    static final String PROPERTY_OPERATOR_NUMERIC_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_2 : "gsm.operator.numeric.2";
    static final String PROPERTY_OPERATOR_ISMANUAL_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_OPERATOR_ISMANUAL_2 : "operator.ismanual.2";
    static final String PROPERTY_OPERATOR_ISROAMING_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_OPERATOR_ISROAMING_2 : "gsm.operator.isroaming.2";
    static final String PROPERTY_OPERATOR_ISO_COUNTRY_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_2 : "gsm.operator.iso-country.2";
    static String PROPERTY_SIM_STATE_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_SIM_STATE_2 : "gsm.sim.state.2";
    static String PROPERTY_ICC_OPERATOR_NUMERIC_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2 : "gsm.sim.operator.numeric.2";
    static String PROPERTY_ICC_OPERATOR_ALPHA_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA_2 : "gsm.sim.operator.alpha.2";
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_ICC_OPERATOR_ISO_COUNTRY_2 : "gsm.sim.operator.iso-country.2";

    /** PROPERTY_ICC_OPERATOR_DEFAULT_NAME is the operator name for plmn which origins the SIM.
     *  Availablity: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME = EncapsulationConstant.USE_MTK_PLATFORM ?
                  TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME : "gsm.sim.operator.default-name";
    static String PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                  TelephonyProperties.PROPERTY_ICC_OPERATOR_DEFAULT_NAME_2 : "gsm.sim.operator.default-name.2";
    static String PROPERTY_DATA_NETWORK_TYPE_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                  TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_2 : "gsm.network.type.2";

    /**
     * Indicate how many SIM cards are inserted
     */
    static final String PROPERTY_GSM_SIM_INSERTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_GSM_SIM_INSERTED : "gsm.sim.inserted";

    /**
    * Indicate CS network type
    */
    static final String PROPERTY_CS_NETWORK_TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_CS_NETWORK_TYPE : "gsm.cs.network.type";

    /**
    * Indicate CS network type
    */
    static final String PROPERTY_CS_NETWORK_TYPE_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2 : "gsm.cs.network.type.2";

    /**
    * Indicate whether the SIM info has been updated
    */
    static final String PROPERTY_SIM_INFO_READY = EncapsulationConstant.USE_MTK_PLATFORM ?
                        TelephonyProperties.PROPERTY_SIM_INFO_READY : "gsm.siminfo.ready";

    /**
    * Indicate if Roaming Indicator needed for SIM/USIM in slot1
    */
    static final String PROPERTY_ROAMING_INDICATOR_NEEDED = EncapsulationConstant.USE_MTK_PLATFORM ?
                   TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED : "gsm.roaming.indicator.needed";

    /**
    * Indicate if Roaming Indicator needed for SIM/USIM in slot2
    */
    static final String PROPERTY_ROAMING_INDICATOR_NEEDED_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                   TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_2 : "gsm.roaming.indicator.needed.2";
    //MTK-END [mtk04070][111118][ALPS00093395]MTK added

    /**
    * Indicate Modem version for slot2
    */
    static final String PROPERTY_BASEBAND_VERSION_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                   TelephonyProperties.PROPERTY_BASEBAND_VERSION_2 : "gsm.version.baseband.2";

    /**
    * Indicate if chaneing to SIM locale is processing
    */
    static final String PROPERTY_SIM_LOCALE_SETTINGS = EncapsulationConstant.USE_MTK_PLATFORM ?
                   TelephonyProperties.PROPERTY_SIM_LOCALE_SETTINGS : "gsm.sim.locale.waiting";
}
