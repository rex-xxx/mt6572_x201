package com.android.soundrecorder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;

/** M: use DialogFragment to show Dialog */
public class SelectDialogFragment extends DialogFragment implements
        DialogInterface.OnClickListener, OnMultiChoiceClickListener {

    private static final String TAG = "SR/SelectDialogFragment";
    private static final String KEY_ITEM_ARRAY = "itemArray";
    private static final String KEY_SUFFIX_ARRAY = "suffixArray";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DEFAULT_SELECT = "nowSelect";
    private static final String KEY_DEFAULT_SELECTARRAY = "nowSelectArray";
    private static final String KEY_SINGLE_CHOICE = "singleChoice";
    private DialogInterface.OnClickListener mClickListener = null;
    private DialogInterface.OnMultiChoiceClickListener mMultiChoiceClickListener = null;

    /**
     * M: create a instance of SelectDialogFragment
     * 
     * @param itemArrayID
     *            the resource id array of strings that show in list
     * @param sufffixArray
     *            the suffix array at the right of list item
     * @param titleID
     *            the resource id of title string
     * @param nowSelect
     *            the current select item index
     * @return the instance of SelectDialogFragment
     */
    public static SelectDialogFragment newInstance(int[] itemArrayID, CharSequence[] sufffixArray,
            int titleID, boolean singleChoice, int nowSelect, boolean[] nowSelectArray) {
        SelectDialogFragment frag = new SelectDialogFragment();
        Bundle args = new Bundle();
        args.putIntArray(KEY_ITEM_ARRAY, itemArrayID);
        args.putCharSequenceArray(KEY_SUFFIX_ARRAY, sufffixArray);
        args.putInt(KEY_TITLE, titleID);
        args.putBoolean(KEY_SINGLE_CHOICE, singleChoice);
        if (singleChoice) {
            args.putInt(KEY_DEFAULT_SELECT, nowSelect);
        } else {
            args.putBooleanArray(KEY_DEFAULT_SELECTARRAY, nowSelectArray.clone());
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    /**
     * M: create a select dialog
     */
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtils.i(TAG, "<onCreateDialog>");
        Bundle args = getArguments();
        final String title = getString(args.getInt(KEY_TITLE));
        final int[] itemArrayID = args.getIntArray(KEY_ITEM_ARRAY);
        int arraySize = itemArrayID.length;
        CharSequence[] itemArray = new CharSequence[arraySize];
        CharSequence[] suffixArray = args.getCharSequenceArray(KEY_SUFFIX_ARRAY);
        if (null == suffixArray) {
            for (int i = 0; i < arraySize; i++) {
                itemArray[i] = getString(itemArrayID[i]);
            }
        } else {
            for (int i = 0; i < arraySize; i++) {
                itemArray[i] = getString(itemArrayID[i]) + suffixArray[i];
            }
        }

        final boolean singleChoice = args.getBoolean(KEY_SINGLE_CHOICE);
        AlertDialog.Builder builder = null;
        if (singleChoice) {
            int nowSelect = args.getInt(KEY_DEFAULT_SELECT);
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).setSingleChoiceItems(itemArray, nowSelect, this)
                    .setNegativeButton(getString(R.string.cancel), null);
        } else {
            boolean[] nowSelectArray = args.getBooleanArray(KEY_DEFAULT_SELECTARRAY);
            builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(title).setMultiChoiceItems(itemArray, nowSelectArray, this)
                    .setNegativeButton(getString(R.string.cancel), null).setPositiveButton(
                            getString(R.string.ok), this);
        }
        return builder.create();
    }

    @Override
    /**
     * M: the process of select an item
     */
    public void onClick(DialogInterface arg0, int arg1) {
        if (null != mClickListener) {
            mClickListener.onClick(arg0, arg1);
        }
    }

    @Override
    public void onClick(DialogInterface arg0, int arg1, boolean arg2) {
        if (null != mMultiChoiceClickListener) {
            mMultiChoiceClickListener.onClick(arg0, arg1, arg2);
        }
    }

    /**
     * M: set listener of click items
     * 
     * @param listener
     *            the listener to be set
     */
    public void setOnClickListener(DialogInterface.OnClickListener listener) {
        mClickListener = listener;
    }

    public void setOnMultiChoiceListener(DialogInterface.OnMultiChoiceClickListener listener) {
        mMultiChoiceClickListener = listener;
    }
}