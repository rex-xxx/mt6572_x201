package com.mediatek.calloption;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.sip.SipManager;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.util.Log;

import com.mediatek.calloption.SimPickerAdapter.ItemHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class SimPickerDialog {

    private static final String TAG = "SimPickerDialog";
    public static final int DEFAULT_SIM_NOT_SET = -5;

    private SimPickerDialog() {
    }

    /*public static Dialog create(Context context, String title,
                                DialogInterface.OnClickListener listener,
                                SimPickerAdapter simAdapter, final boolean isInternet,
                                final boolean isMultiSim) {
        return create(context, title, DEFAULT_SIM_NOT_SET, 
                      createItemHolder(context, isInternet), listener, simAdapter);
    }*/

    /*public static Dialog create(Context context, String title, long suggestedSimId,
                                DialogInterface.OnClickListener listener,
                                SimPickerAdapter simAdapter, final boolean isInternet,
                                final boolean isMultiSim) {
        return create(context, title, suggestedSimId, createItemHolder(context, isInternet), listener, simAdapter);
    }*/

    public static Dialog create(Context context, String title, /*long suggestedSimId, List<ItemHolder> items,*/
                                   DialogInterface.OnClickListener listener, SimPickerAdapter simPickerAdpater,
                                   boolean addInternet, boolean only3GItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //simPickerAdpater.init(context, items, suggestedSimId);
        simPickerAdpater.setItems(createItemHolder(context, addInternet, only3GItem));
        builder.setSingleChoiceItems(simPickerAdpater, -1, listener)
               .setTitle(title);
        return builder.create();
    }

    /*protected static List<ItemHolder> createItemHolder(Context context, boolean internet) {
        return createItemHolder(context, null, internet, null);
    }*/

    protected static List<ItemHolder> createItemHolder(Context context,
            /*String phone,*/ boolean addInternet/*, ArrayList<Account> accounts*/, boolean only3GItem) {

        List<SIMInfo> simInfos = SIMInfo.getInsertedSIMList(context);
        ArrayList<ItemHolder> itemHolders = new ArrayList<ItemHolder>();
        ItemHolder temp = null;

        /// M: sort the SIMInfo list by slot id
        Collections.sort(simInfos, new Comparator<SIMInfo>() {
            @Override
            public int compare(SIMInfo arg0, SIMInfo arg1) {
                return (arg0.mSlot - arg1.mSlot);
            }
        });

        for (SIMInfo simInfo : simInfos) {
            if (simInfo != null && simInfo.mSlot >= 0) {
                Log.d(TAG, "[createItemHolder] for simId: " + simInfo.mSimId + ", slotId: " + simInfo.mSlot + ", color: "
                        + simInfo.mColor + ", displayName: " + simInfo.mDisplayName);
                temp = new ItemHolder(simInfo, SimPickerAdapter.ITEM_TYPE_SIM);
                if (!only3GItem) {
                    itemHolders.add(temp);
                } else if(CallOptionUtils.get3GCapabilitySIMBySlot(simInfo.mSlot)) {
                    itemHolders.add(temp);
                }
            }
        }

        /*if (!TextUtils.isEmpty(phone)) {
            temp = new ItemHolder(phone, SimPickerAdapter.ITEM_TYPE_TEXT);
            itemHolders.add(temp);
        }*/

        int enabled = Settings.System.getInt(context.getContentResolver(),
                                             Settings.System.ENABLE_INTERNET_CALL, 0);
        if (!only3GItem && addInternet && SipManager.isVoipSupported(context) && enabled == 1) {
            temp = new ItemHolder("Internet"/*context.getResources().getText(R.string.internet)*/,
                    SimPickerAdapter.ITEM_TYPE_INTERNET);
            itemHolders.add(temp);
        }

        /*if (accounts != null) {
            for (Account account : accounts) {
                temp = new ItemHolder(account, SimPickerAdapter.ITEM_TYPE_ACCOUNT);
                itemHolders.add(temp);
            }
        }*/
        return itemHolders;
    }

    protected abstract SimPickerAdapter getSimPickerAdapter(Context context,
                                                            List<ItemHolder> items,
                                                            long suggestedSimId);
}
