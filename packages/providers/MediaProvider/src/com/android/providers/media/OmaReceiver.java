package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;

import java.util.ArrayList;

public class OmaReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaProvider/OmacpReceiver";

    // intent actions
    private static final String ACTION_SETTING = "com.mediatek.omacp.settings";
    private static final String ACTION_SETTING_RESULT = "com.mediatek.omacp.settings.result";
    private static final String ACTION_CAPABILITY = "com.mediatek.omacp.capability";
    private static final String ACTION_CAPABILITY_RESULT = "com.mediatek.omacp.capability.result";

    // capablility key
    private static final String KEY_CAPABILITY_RTSP = "rtsp";
    private static final String KEY_CAPABILITY_PROVIDER_ID = "rtsp_provider_id";
    private static final String KEY_CAPABILITY_NAME = "rtsp_name";
    private static final String KEY_CAPABILITY_TO_PROXY = "rtsp_to_proxy";
    private static final String KEY_CAPABILITY_TO_NAPID = "rtsp_to_napid";
    private static final String KEY_CAPABILITY_MAX_BANDWIDTH = "rtsp_max_bandwidth";
    private static final String KEY_CAPABILITY_NET_INFO = "rtsp_net_info";
    private static final String KEY_CAPABILITY_MIN_UDP_PORT = "rtsp_min_udp_port";
    private static final String KEY_CAPABILITY_MAX_UDP_PORT = "rtsp_max_udp_port";

    // setting key
    private static final String KEY_APPID = "APPID";
    private static final String KEY_RESULT = "result";
    private static final String KEY_RESULT_APPID = "appId";

    private static final String KEY_NAME = "NAME";
    private static final String KEY_PROVIDER_ID = "PROVIDER-ID";
    private static final String KEY_TO_PROXY = "TO-PROXY";
    private static final String KEY_TO_NAPID = "TO-NAPID";
    private static final String KEY_MAX_BANDWIDTH = "MAX-BANDWIDTH";
    private static final String KEY_NETINFO = "NETINFO";
    private static final String KEY_MIN_UDP_PORT = "MIN-UDP-PORT";
    private static final String KEY_MAX_UDP_PORT = "MAX-UDP-PORT";
    private static final String KEY_SIM_ID = "simId";

    // app info
    private static final String MY_APP_ID = "554";// defined in oma cp protocol
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        MtkLog.v(TAG, "onReceive(" + intent.getAction() + ")");
        mContext = context;
        if (ACTION_SETTING.equalsIgnoreCase(intent.getAction())) {
            saveRtspSetting(intent.getExtras());
        } else if (ACTION_CAPABILITY.equalsIgnoreCase(intent.getAction())) {
            sendRtspCapability();
        }
    }

    private void sendRtspCapability() {
        Intent intent = new Intent(ACTION_CAPABILITY_RESULT);
        intent.putExtra(KEY_RESULT_APPID, MY_APP_ID);
        intent.putExtra(KEY_CAPABILITY_RTSP, true);
        intent.putExtra(KEY_CAPABILITY_TO_PROXY, false);
        intent.putExtra(KEY_CAPABILITY_TO_NAPID, false);
        intent.putExtra(KEY_CAPABILITY_NET_INFO, false);
        intent.putExtra(KEY_CAPABILITY_MIN_UDP_PORT, true);
        intent.putExtra(KEY_CAPABILITY_MAX_UDP_PORT, true);
        intent.putExtra(KEY_CAPABILITY_NAME, false);
        intent.putExtra(KEY_CAPABILITY_PROVIDER_ID, false);
        intent.putExtra(KEY_CAPABILITY_MAX_BANDWIDTH, false);
        mContext.sendBroadcast(intent);
        MtkLog.v(TAG, "sendRtspCapability()...");
    }

    private void saveRtspSetting(Bundle extras) {
        MtkLog.v(TAG, "saveRtspSetting(" + extras + ")");
        if (extras != null) {
            String appid = extras.getString(KEY_APPID);
            if (MY_APP_ID.equalsIgnoreCase(appid)) {
                Intent intent = new Intent(ACTION_SETTING_RESULT);
                intent.putExtra(KEY_RESULT_APPID, MY_APP_ID);
                intent.putExtra(KEY_RESULT, writeSetting(extras));
                mContext.sendBroadcast(intent);
            } else {
                MtkLog.v(TAG, "not rtsp app id. appid=" + appid);
            }
        } else {
            MtkLog.w(TAG, "extras is null. cannot set rtsp configuration!");
        }

    }

    private boolean writeSetting(Bundle extras) {
        MtkLog.v(TAG, "writeSetting(" + extras + ")");
        int simid = extras.getInt(KEY_SIM_ID);
        String proxy = "";
        String port = "-1";
        boolean enableProxy = false;// validate(proxy, port);
        String[] key = new String[] {
                MediaStore.Streaming.Setting.NAME,
                MediaStore.Streaming.Setting.PROVIDER_ID,
                MediaStore.Streaming.Setting.MAX_BANDWIDTH,
                MediaStore.Streaming.Setting.MIN_UDP_PORT,
                MediaStore.Streaming.Setting.MAX_UDP_PORT,
                MediaStore.Streaming.Setting.TO_PROXY,
                MediaStore.Streaming.Setting.TO_NAPID,
                MediaStore.Streaming.Setting.NETINFO,
                MediaStore.Streaming.Setting.SIM_ID,
                MediaStore.Streaming.Setting.RTSP_PROXY_HOST,
                MediaStore.Streaming.Setting.RTSP_PROXY_PORT,
                MediaStore.Streaming.Setting.RTSP_PROXY_ENABLED
        };
        String[] value = new String[] {
                extras.getString(KEY_NAME),
                extras.getString(KEY_PROVIDER_ID),
                extras.getString(KEY_MAX_BANDWIDTH),
                extras.getString(KEY_MIN_UDP_PORT),
                extras.getString(KEY_MAX_UDP_PORT),
                catString(extras.getStringArrayList(KEY_TO_PROXY), ","),
                catString(extras.getStringArrayList(KEY_TO_NAPID), ","),
                catString(extras.getStringArrayList(KEY_NETINFO), ";"),
                String.valueOf(extras.getInt(KEY_SIM_ID)),
                proxy,
                port,
                enableProxy ? "1" : "0"
        };
        ContentResolver cr = mContext.getContentResolver();
        int count = 0;
        int size = key.length;
        for (int i = 0; i < size; i++) {
            if (Settings.System.putString(cr, key[i], value[i])) {
                count++;
            }
        }
        MtkLog.v(TAG, "writeSetting() count=" + count);
        if (count > 0) {
            return true;
        }
        return false;
    }

    private String catString(ArrayList<String> list, String seperator) {
        String elements = null;
        int size = list != null ? list.size() : 0;
        if (size > 0) {
            elements = "";
            for (int i = 0; i < size - 1; i++) {
                elements += list.get(i) + seperator;
            }
            elements += list.get(size - 1);
        }
        MtkLog.v(TAG, "catString() return " + elements);
        return elements;
    }
}
