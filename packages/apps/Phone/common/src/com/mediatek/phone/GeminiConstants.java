package com.mediatek.phone;

import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

/**
 * GEMINI Constant Values
 */
public class GeminiConstants {
    /** ------ SLOT ------ */
    public static final String SLOT_ID_KEY = PhoneConstants.GEMINI_SIM_ID_KEY;

    public static final int SLOT_ID_1 = PhoneConstants.GEMINI_SIM_1;
    public static final int SLOT_ID_2 = PhoneConstants.GEMINI_SIM_2;
    public static final int SLOT_ID_3 = PhoneConstants.GEMINI_SIM_3;
    public static final int SLOT_ID_4 = PhoneConstants.GEMINI_SIM_4;
    public static final int SOLT_NUM = PhoneConstants.GEMINI_SIM_NUM;
    public static final int[] SLOTS;

    /** ------ NETWORK SELECTION ------ */
    public static final String NETWORK_SELECTION_KEY = PhoneBase.NETWORK_SELECTION_KEY;
    public static final String NETWORK_SELECTION_KEY_2 = PhoneBase.NETWORK_SELECTION_KEY_2;
    public static final String NETWORK_SELECTION_KEY_3 = PhoneBase.NETWORK_SELECTION_KEY_3;
    public static final String NETWORK_SELECTION_KEY_4 = PhoneBase.NETWORK_SELECTION_KEY_4;
    public static final String[] NETWORK_SELECTION_KEYS;

    public static final String NETWORK_SELECTION_NAME_KEY = PhoneBase.NETWORK_SELECTION_NAME_KEY;
    public static final String NETWORK_SELECTION_NAME_KEY_2 = PhoneBase.NETWORK_SELECTION_NAME_KEY_2;
    public static final String NETWORK_SELECTION_NAME_KEY_3 = PhoneBase.NETWORK_SELECTION_NAME_KEY_3;
    public static final String NETWORK_SELECTION_NAME_KEY_4 = PhoneBase.NETWORK_SELECTION_NAME_KEY_4;
    public static final String[] NETWORK_SELECTION_NAME_KEYS;

    /** ------ Telephony Properties ------ */
    public static final String PROPERTY_OPERATOR_ALPHA = TelephonyProperties.PROPERTY_OPERATOR_ALPHA;
    public static final String PROPERTY_OPERATOR_ALPHA_2 = TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2;
    public static final String PROPERTY_OPERATOR_ALPHA_3 = TelephonyProperties.PROPERTY_OPERATOR_ALPHA_3;
    public static final String PROPERTY_OPERATOR_ALPHA_4 = TelephonyProperties.PROPERTY_OPERATOR_ALPHA_4;
    public static final String[] PROPERTY_OPERATOR_ALPHAS;

    public static final String PROPERTY_SIM_STATE = TelephonyProperties.PROPERTY_SIM_STATE;
    public static final String PROPERTY_SIM_STATE_2 = TelephonyProperties.PROPERTY_SIM_STATE_2;
    public static final String PROPERTY_SIM_STATE_3 = TelephonyProperties.PROPERTY_SIM_STATE_3;
    public static final String PROPERTY_SIM_STATE_4 = TelephonyProperties.PROPERTY_SIM_STATE_4;
    public static final String[] PROPERTY_SIM_STATES;

    public static final String PROPERTY_CS_NETWORK_TYPE = TelephonyProperties.PROPERTY_CS_NETWORK_TYPE;
    public static final String PROPERTY_CS_NETWORK_TYPE_2 = TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2;
    public static final String PROPERTY_CS_NETWORK_TYPE_3 = TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_3;
    public static final String PROPERTY_CS_NETWORK_TYPE_4 = TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_4;
    public static final String[] PROPERTY_CS_NETWORK_TYPES;

    public static final String FDN_CONTENT = "content://icc/fdn";
    public static final String FDN_CONTENT_SIM1 = "content://icc/fdn1";
    public static final String FDN_CONTENT_SIM2 = "content://icc/fdn2";
    public static final String FDN_CONTENT_SIM3 = "content://icc/fdn3";
    public static final String FDN_CONTENT_SIM4 = "content://icc/fdn4";
    public static final String[] FDN_CONTENT_GEMINI;

    public static final String GSM_SIM_RETRY_PIN = "gsm.sim.retry.pin";
    public static final String GSM_SIM_RETRY_PIN_2 = "gsm.sim.retry.pin1.2";
    public static final String GSM_SIM_RETRY_PIN_3 = "gsm.sim.retry.pin1.3";
    public static final String GSM_SIM_RETRY_PIN_4 = "gsm.sim.retry.pin1.4";
    public static final String[] GSM_SIM_RETRY_PIN_GEMINI;

    public static final String GSM_SIM_RETRY_PIN2 = "gsm.sim.retry.pin2";
    public static final String GSM_SIM_RETRY_PIN2_2 = "gsm.sim.retry.pin2.2";
    public static final String GSM_SIM_RETRY_PIN2_3 = "gsm.sim.retry.pin2.3";
    public static final String GSM_SIM_RETRY_PIN2_4 = "gsm.sim.retry.pin2.4";
    public static final String[] GSM_SIM_RETRY_PIN2_GEMINI;

    public static final String GSM_SIM_RETRY_PUK2 = "gsm.sim.retry.puk2";
    public static final String GSM_SIM_RETRY_PUK2_2 = "gsm.sim.retry.puk2.2";
    public static final String GSM_SIM_RETRY_PUK2_3 = "gsm.sim.retry.puk2.3";
    public static final String GSM_SIM_RETRY_PUK2_4 = "gsm.sim.retry.puk2.4";
    public static final String[] GSM_SIM_RETRY_PUK2_GEMINI;

    public static final String GSM_ROAMING_INDICATOR_NEEDED = "gsm.roaming.indicator.needed";
    public static final String GSM_ROAMING_INDICATOR_NEEDED_2 = "gsm.roaming.indicator.needed.2";
    public static final String GSM_ROAMING_INDICATOR_NEEDED_3 = "gsm.roaming.indicator.needed.3";
    public static final String GSM_ROAMING_INDICATOR_NEEDED_4 = "gsm.roaming.indicator.needed.4";
    public static final String[] GSM_ROAMING_INDICATOR_NEEDED_GEMINI;

    static {
        switch (SOLT_NUM) {
        case 2:
            // slots
            SLOTS = new int[] { SLOT_ID_1, SLOT_ID_2 };
            // network selection
            NETWORK_SELECTION_KEYS = new String[] { NETWORK_SELECTION_KEY, NETWORK_SELECTION_KEY_2 };
            NETWORK_SELECTION_NAME_KEYS = new String[] { NETWORK_SELECTION_NAME_KEY,
                    NETWORK_SELECTION_NAME_KEY_2 };
            // telephony properties
            PROPERTY_OPERATOR_ALPHAS = new String[] { PROPERTY_OPERATOR_ALPHA,
                    PROPERTY_OPERATOR_ALPHA_2 };
            PROPERTY_SIM_STATES = new String[] { PROPERTY_SIM_STATE, PROPERTY_SIM_STATE_2 };
            PROPERTY_CS_NETWORK_TYPES = new String[] { PROPERTY_CS_NETWORK_TYPE,
                    PROPERTY_CS_NETWORK_TYPE_2 };
            FDN_CONTENT_GEMINI = new String[] { FDN_CONTENT_SIM1, FDN_CONTENT_SIM2 };
            GSM_SIM_RETRY_PIN_GEMINI = new String[] { GSM_SIM_RETRY_PIN, GSM_SIM_RETRY_PIN_2 };
            GSM_SIM_RETRY_PIN2_GEMINI = new String[] { GSM_SIM_RETRY_PIN2, GSM_SIM_RETRY_PIN2_2 };
            GSM_SIM_RETRY_PUK2_GEMINI = new String[] { GSM_SIM_RETRY_PUK2, GSM_SIM_RETRY_PUK2_2 };
            GSM_ROAMING_INDICATOR_NEEDED_GEMINI = new String[] { GSM_ROAMING_INDICATOR_NEEDED,
                    GSM_ROAMING_INDICATOR_NEEDED_2 };
            break;
        case 3:
            // slots
            SLOTS = new int[] { SLOT_ID_1, SLOT_ID_2, SLOT_ID_3 };
            // network selection
            NETWORK_SELECTION_KEYS = new String[] { NETWORK_SELECTION_KEY, NETWORK_SELECTION_KEY_2,
                    NETWORK_SELECTION_KEY_3 };
            NETWORK_SELECTION_NAME_KEYS = new String[] { NETWORK_SELECTION_NAME_KEY,
                    NETWORK_SELECTION_NAME_KEY_2, NETWORK_SELECTION_NAME_KEY_3 };
            // telephony properties
            PROPERTY_OPERATOR_ALPHAS = new String[] { PROPERTY_OPERATOR_ALPHA,
                    PROPERTY_OPERATOR_ALPHA_2, PROPERTY_OPERATOR_ALPHA_3 };
            PROPERTY_SIM_STATES = new String[] { PROPERTY_SIM_STATE, PROPERTY_SIM_STATE_2,
                    PROPERTY_SIM_STATE_3 };
            PROPERTY_CS_NETWORK_TYPES = new String[] { PROPERTY_CS_NETWORK_TYPE,
                    PROPERTY_CS_NETWORK_TYPE_2, PROPERTY_CS_NETWORK_TYPE_3 };
            FDN_CONTENT_GEMINI = new String[] { FDN_CONTENT_SIM1, FDN_CONTENT_SIM2,
                    FDN_CONTENT_SIM3 };
            GSM_SIM_RETRY_PIN_GEMINI = new String[] { GSM_SIM_RETRY_PIN, GSM_SIM_RETRY_PIN_2,
                    GSM_SIM_RETRY_PIN_3 };
            GSM_SIM_RETRY_PIN2_GEMINI = new String[] { GSM_SIM_RETRY_PIN2, GSM_SIM_RETRY_PIN2_2,
                    GSM_SIM_RETRY_PIN2_3 };
            GSM_SIM_RETRY_PUK2_GEMINI = new String[] { GSM_SIM_RETRY_PUK2, GSM_SIM_RETRY_PUK2_2,
                    GSM_SIM_RETRY_PUK2_3 };
            GSM_ROAMING_INDICATOR_NEEDED_GEMINI = new String[] { GSM_ROAMING_INDICATOR_NEEDED,
                    GSM_ROAMING_INDICATOR_NEEDED_2, GSM_ROAMING_INDICATOR_NEEDED_3 };
            break;

        case 4:
            // slots
            SLOTS = new int[] { SLOT_ID_1, SLOT_ID_2, SLOT_ID_3, SLOT_ID_4 };
            // network selection
            NETWORK_SELECTION_KEYS = new String[] { NETWORK_SELECTION_KEY, NETWORK_SELECTION_KEY_2,
                    NETWORK_SELECTION_KEY_3, NETWORK_SELECTION_KEY_4 };
            NETWORK_SELECTION_NAME_KEYS = new String[] { NETWORK_SELECTION_NAME_KEY,
                    NETWORK_SELECTION_NAME_KEY_2, NETWORK_SELECTION_NAME_KEY_3,
                    NETWORK_SELECTION_NAME_KEY_4 };
            PROPERTY_OPERATOR_ALPHAS = new String[] { PROPERTY_OPERATOR_ALPHA,
                    PROPERTY_OPERATOR_ALPHA_2, PROPERTY_OPERATOR_ALPHA_3, PROPERTY_OPERATOR_ALPHA_4 };
            PROPERTY_SIM_STATES = new String[] { PROPERTY_SIM_STATE, PROPERTY_SIM_STATE_2,
                    PROPERTY_SIM_STATE_3, PROPERTY_SIM_STATE_4 };
            PROPERTY_CS_NETWORK_TYPES = new String[] { PROPERTY_CS_NETWORK_TYPE,
                    PROPERTY_CS_NETWORK_TYPE_2, PROPERTY_CS_NETWORK_TYPE_3,
                    PROPERTY_CS_NETWORK_TYPE_4 };
            FDN_CONTENT_GEMINI = new String[] { FDN_CONTENT_SIM1, FDN_CONTENT_SIM2,
                    FDN_CONTENT_SIM3, FDN_CONTENT_SIM4 };
            GSM_SIM_RETRY_PIN_GEMINI = new String[] { GSM_SIM_RETRY_PIN, GSM_SIM_RETRY_PIN_2,
                    GSM_SIM_RETRY_PIN_3, GSM_SIM_RETRY_PIN_4 };
            GSM_SIM_RETRY_PIN2_GEMINI = new String[] { GSM_SIM_RETRY_PIN2, GSM_SIM_RETRY_PIN2_2,
                    GSM_SIM_RETRY_PIN2_3, GSM_SIM_RETRY_PIN2_4 };
            GSM_SIM_RETRY_PUK2_GEMINI = new String[] { GSM_SIM_RETRY_PUK2, GSM_SIM_RETRY_PUK2_2,
                    GSM_SIM_RETRY_PUK2_3, GSM_SIM_RETRY_PUK2_4 };
            GSM_ROAMING_INDICATOR_NEEDED_GEMINI = new String[] { GSM_ROAMING_INDICATOR_NEEDED,
                    GSM_ROAMING_INDICATOR_NEEDED_2, GSM_ROAMING_INDICATOR_NEEDED_3,
                    GSM_ROAMING_INDICATOR_NEEDED_4 };
            break;
        default:
            // slots
            SLOTS = new int[] { SLOT_ID_1 };
            // network selection
            NETWORK_SELECTION_KEYS = new String[] { NETWORK_SELECTION_KEY };
            NETWORK_SELECTION_NAME_KEYS = new String[] { NETWORK_SELECTION_NAME_KEY };
            // telephony properties
            PROPERTY_OPERATOR_ALPHAS = new String[] { PROPERTY_OPERATOR_ALPHA };
            PROPERTY_SIM_STATES = new String[] { PROPERTY_SIM_STATE };
            PROPERTY_CS_NETWORK_TYPES = new String[] { PROPERTY_CS_NETWORK_TYPE };
            FDN_CONTENT_GEMINI = new String[] { FDN_CONTENT };
            GSM_SIM_RETRY_PIN_GEMINI = new String[] { GSM_SIM_RETRY_PIN };
            GSM_SIM_RETRY_PIN2_GEMINI = new String[] { GSM_SIM_RETRY_PIN2 };
            GSM_SIM_RETRY_PUK2_GEMINI = new String[] { GSM_SIM_RETRY_PUK2 };
            GSM_ROAMING_INDICATOR_NEEDED_GEMINI = new String[] { GSM_ROAMING_INDICATOR_NEEDED };
            break;
        }
    }

}