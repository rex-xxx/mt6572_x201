package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.widget.Toast;

import com.android.contacts.R;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.NfcShareContactsHandler;
import com.mediatek.contacts.list.MultiContactsBasePickerAdapter.PickListItemCache.PickListItemData;
import com.mediatek.contacts.util.LogUtils;

public class MultiContactsShareFragment extends MultiContactsPickerBaseFragment {

    private static final String TAG = "MultiContactsShareFragment";
    private static final int MAX_DATA_SIZE = 400000;

    @Override
    public void onOptionAction() {
        final int selectedCount = getCheckedItemIds().length;
        if (selectedCount == 0) {
            Toast.makeText(getContext(), R.string.multichoice_no_select_alert, Toast.LENGTH_SHORT).show();
            return;
        } else if (selectedCount > AbstractPickerFragment.ALLOWED_ITEMS_MAX) {
            Toast.makeText(getContext(), R.string.share_contacts_limit, Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] uriArray = getLoopUriArray();
        final Intent retIntent = new Intent();
        retIntent.putExtra(RESULTINTENTEXTRANAME, uriArray);
        boolean result = doShareVisibleContacts("Multi_Contact", null, uriArray);
        if (result) {
            getActivity().setResult(Activity.RESULT_OK, retIntent);
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (FeatureOption.MTK_BEAM_PLUS_SUPPORT) {
//            NfcShareContactsHandler.register(getActivity(), this);
//        }
    }

    private boolean doShareVisibleContacts(String type, Uri uri, String[] idArrayUriLookUp) {
        if (idArrayUriLookUp == null || idArrayUriLookUp.length == 0) {
            return true;
        }

        StringBuilder uriListBuilder = new StringBuilder();
        int index = 0;
        for (int i = 0; i < idArrayUriLookUp.length; i++) {
            if (index != 0) {
                uriListBuilder.append(":");
            }
            // find lookup key
            uriListBuilder.append(idArrayUriLookUp[i]);
            index++;
        }
        int dataSize = uriListBuilder.length();
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(uriListBuilder.toString()));
        final Intent intent = new Intent(Intent.ACTION_SEND);
        //intent.setDataAndType(shareUri, Contacts.CONTENT_VCARD_TYPE);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        //intent.putExtra("contactId", String.valueOf(mContactData.getContactId()));
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);

        /** M: Bug Fix for CR: ALPS00395378 @{ 
         * Original Code:
         * intent.putExtra("LOOKUPURIS", uriListBuilder.toString());
         */
        /** @} M: Bug fix for CR: ALPS00395378 */

        // Launch chooser to share contact via
        final CharSequence chooseTitle = getText(R.string.share_via);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            Log.i(TAG, "doShareVisibleContacts dataSize : " + dataSize);
            if (dataSize < MAX_DATA_SIZE) {
                startActivity(chooseIntent);
                return true;
            } else {
                Toast.makeText(getContext(), R.string.share_too_large, Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getContext(), R.string.share_error, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    public Uri getUri() {
        final long[] checkArray = getCheckedItemIds();
        final int selectedCount = checkArray.length;
        if (selectedCount == 0) {
            Toast.makeText(getContext(), R.string.multichoice_no_select_alert, Toast.LENGTH_SHORT).show();
            return null;
        } else if (selectedCount > AbstractPickerFragment.ALLOWED_ITEMS_MAX) {
            Toast.makeText(getContext(), R.string.share_contacts_limit, Toast.LENGTH_SHORT).show();
            return null;
        }

        final String[] uriArray = getLoopUriArray();

        StringBuilder uriListBuilder = new StringBuilder();
        boolean isFirstItem = true;
        for (String uri : uriArray) {
            if (isFirstItem) {
                isFirstItem = false;
            } else {
                uriListBuilder.append(":");
            }
            // find lookup key
            uriListBuilder.append(uri);
        }
        Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(uriListBuilder.toString()));

        return shareUri;
    }
    
    private String[] getLoopUriArray() {
        final long[] checkArray = getCheckedItemIds();
        final int selectedCount = checkArray.length;
        final MultiContactsBasePickerAdapter adapter = (MultiContactsBasePickerAdapter) getAdapter();

        int curArray = 0;
        String[] uriArray = new String[selectedCount];

        for (long id : checkArray) {
            if (curArray > selectedCount) {
                break;
            }
            PickListItemData item = adapter.getListItemCache().getItemData(id);
            if(item != null){
                uriArray[curArray++] = item.lookupUri;
            }else{
                LogUtils.e(TAG, "#getLoopUriArray(),the item is null. may some error happend.curArray:" + curArray
                        + ",id:" + id + ",checkArray.length:" + selectedCount + ",ListViewCheckedCount:"
                        + getListView().getCheckedItemCount());
            }
        }

        return uriArray;
    }
}
