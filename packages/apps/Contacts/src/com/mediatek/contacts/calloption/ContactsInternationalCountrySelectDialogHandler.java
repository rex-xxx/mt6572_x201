package com.mediatek.contacts.calloption;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.contacts.R;
import com.mediatek.calloption.InternationalCountrySelectDialogHandler;

public class ContactsInternationalCountrySelectDialogHandler extends InternationalCountrySelectDialogHandler {

    private static final String TAG = "ContactsInternationalCountrySelectDialogHandler";

    public ContactsInternationalCountrySelectDialogHandler(Context context,
                                                        OnCountrySelectListener countrySelectListener) {
        super(context, countrySelectListener);
    }

    protected void initDialogView() {
        mDialogView = LayoutInflater.from(mContext).inflate(R.layout.international_country_select_list, null, false);

        ListView countryListView = (ListView) mDialogView.findViewById(R.id.list);
        mListAdapter = new SimpleAdapter(mContext, mCountryInfoMapList,
                                         R.layout.international_country_select_list_item,
                                         new String[] { MAP_KEY_NAME_COUNTRY_NAME, MAP_KEY_NAME_COUNTRY_CODE },
                                         new int[] { R.id.country_name, R.id.country_code });
        countryListView.setAdapter(mListAdapter);
        countryListView.setOnItemClickListener(this);

        mSearchView = (SearchView) mDialogView.findViewById(R.id.search);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setQueryHint(mContext.getString(com.android.internal.R.string.search_go));
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextListener(this);

        mNoSearchResult = (TextView) mDialogView.findViewById(R.id.no_search_result);
        mCountryListView = (ListView) mDialogView.findViewById(R.id.list);

        mMoreButton = (Button) mDialogView.findViewById(R.id.more);
        mMoreButton.setText(mContext.getString(com.android.internal.R.string.more_item_label));
        mMoreButton.setOnClickListener(this);
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
