package com.mediatek.calloption;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class InternationalCountrySelectDialogHandler implements View.OnClickListener,
                                                                         SearchView.OnQueryTextListener,
                                                                         AdapterView.OnItemClickListener {

    private static final String TAG = "InternationalCountrySelectDialogHandler";

    protected static final String MAP_KEY_NAME_COUNTRY_ISO  = "CountryISO";
    protected static final String MAP_KEY_NAME_COUNTRY_CODE = "CountryCode";
    protected static final String MAP_KEY_NAME_COUNTRY_NAME = "CountryName";
    protected static final String MAP_KEY_NAME_COUNTRY_ENGLISH_NAME = "CountryEnglisName";

    public interface OnCountrySelectListener {
        void onCountrySelected(String countryISO, String countryCode, String countryName);
    }

    protected Context mContext;
    protected OnCountrySelectListener mCountrySelectListener;
    protected ArrayList<String> mDefaultCountryISOList;
    protected SimpleAdapter mListAdapter;
    protected List<Map<String, Object>> mCountryInfoMapList;

    protected Button mMoreButton;
    protected SearchView mSearchView;
    protected TextView mNoSearchResult;
    protected ListView mCountryListView;
    protected View mDialogView;
    protected Dialog mDialog;

    protected boolean mIsInflated;
    protected boolean mIsMoreButtonClicked;

    public InternationalCountrySelectDialogHandler(Context context,
                                                   OnCountrySelectListener countrySelectListener) {
        mContext = context;
        mCountrySelectListener = countrySelectListener;
        mCountryInfoMapList = new ArrayList<Map<String, Object>>();
    }

    public void showCountrySelectDialog(ArrayList<String> defaultCountryISOList) {
        mDefaultCountryISOList = defaultCountryISOList;
        mCountryInfoMapList.removeAll(mCountryInfoMapList);
        addDefaultCountryInfoToMapList(mCountryInfoMapList, mDefaultCountryISOList);
        if (mIsInflated) {
            mMoreButton.setVisibility(View.VISIBLE);
            mSearchView.setQuery("", false);
        } else {
            initDialogView();
            mIsInflated = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setView(mDialogView);
            mDialog = builder.create();
        }
        mSearchView.clearFocus();
        mNoSearchResult.setVisibility(View.GONE);
        mCountryListView.setVisibility(View.VISIBLE);
        mDialog.show();
    }

    protected abstract void initDialogView();

    @Override
    public void onClick(View view) {
        mIsMoreButtonClicked = true;
        view.setVisibility(View.GONE);
        addSupportedCountryInfoToMapList(mCountryInfoMapList);
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String inputText) {
        mCountryInfoMapList.removeAll(mCountryInfoMapList);
        if (!TextUtils.isEmpty(inputText)) {
            List<Map<String, Object>> countryInfoMapListForSearch = new ArrayList<Map<String, Object>>();
            addDefaultCountryInfoToMapList(countryInfoMapListForSearch, mDefaultCountryISOList);
            addSupportedCountryInfoToMapList(countryInfoMapListForSearch);
            searchCountryInfoFromMap(inputText, countryInfoMapListForSearch, mCountryInfoMapList);
            mMoreButton.setVisibility(View.GONE);
        } else {
            addDefaultCountryInfoToMapList(mCountryInfoMapList, mDefaultCountryISOList);
            if (mIsMoreButtonClicked) {
                mMoreButton.setVisibility(View.GONE);
                addSupportedCountryInfoToMapList(mCountryInfoMapList);
            } else {
                mMoreButton.setVisibility(View.VISIBLE);
            }
        }
        if (0 == mCountryInfoMapList.size()) {
            mNoSearchResult.setVisibility(View.VISIBLE);
            mCountryListView.setVisibility(View.GONE);
        } else {
            mNoSearchResult.setVisibility(View.GONE);
            mCountryListView.setVisibility(View.VISIBLE);
        }
        mListAdapter.notifyDataSetChanged();
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(position);
            mCountrySelectListener.onCountrySelected((String)item.get(MAP_KEY_NAME_COUNTRY_ISO),
                                                     // remove "+"
                                                     ((String)item.get(MAP_KEY_NAME_COUNTRY_CODE)).substring(1),
                                                     (String)item.get(MAP_KEY_NAME_COUNTRY_NAME));
            if (null != mDialog) {
                mDialog.dismiss();
            }
    }

    private void searchCountryInfoFromMap(String searchText, List<Map<String, Object>> searchFromList,
                                          List<Map<String, Object>> searchResultList) {
        for (Iterator iterator = searchFromList.iterator(); iterator.hasNext();) {
            Map<String, Object> item = (Map<String, Object>) iterator.next();
            String countryName = (String)item.get(MAP_KEY_NAME_COUNTRY_NAME);
            String countryCode = (String)item.get(MAP_KEY_NAME_COUNTRY_CODE);
            String countryEnglishName = (String)item.get(MAP_KEY_NAME_COUNTRY_ENGLISH_NAME);
            if (countryName.toUpperCase().contains(searchText.toUpperCase())
                    || countryEnglishName.toUpperCase().contains(searchText.toUpperCase())
                    || countryCode.contains(searchText)) {
                Map<String, Object> itemResult = new HashMap<String, Object>();
                itemResult.put(MAP_KEY_NAME_COUNTRY_ISO, item.get(MAP_KEY_NAME_COUNTRY_ISO));
                itemResult.put(MAP_KEY_NAME_COUNTRY_CODE, countryCode);
                itemResult.put(MAP_KEY_NAME_COUNTRY_NAME, countryName);
                itemResult.put(MAP_KEY_NAME_COUNTRY_ENGLISH_NAME, countryEnglishName);
                searchResultList.add(itemResult);
            }
        }
    }

    private List<Map<String, Object>> getCountryInfoMapList(ArrayList<String> defaultCountryISOList,
                                                            boolean onlyDefaultOnes) {
        List<Map<String, Object>> countryInfoMapList = new ArrayList<Map<String, Object>>();
        addDefaultCountryInfoToMapList(countryInfoMapList, defaultCountryISOList);
        if (!onlyDefaultOnes) {
            addSupportedCountryInfoToMapList(countryInfoMapList);
        }
        return countryInfoMapList;
    }

    private void addDefaultCountryInfoToMapList(List<Map<String, Object>> countryInfoMapList,
                                                ArrayList<String> defaultCountryISOList) {
        for (int i = 0; i < defaultCountryISOList.size(); i++) {
            if (!isSameMapItemExits(countryInfoMapList, defaultCountryISOList.get(i))) {
                Locale locale = new Locale(Locale.getDefault().getLanguage(), defaultCountryISOList.get(i));
                Map<String, Object> itemMap = new HashMap<String, Object>();
                itemMap.put(MAP_KEY_NAME_COUNTRY_ISO, defaultCountryISOList.get(i));
                itemMap.put(MAP_KEY_NAME_COUNTRY_CODE, "+"
                        + String.valueOf(PhoneNumberUtil.getInstance().getCountryCodeForRegion(
                                defaultCountryISOList.get(i))));
                itemMap.put(MAP_KEY_NAME_COUNTRY_NAME, locale.getDisplayCountry(Locale.getDefault()));
                itemMap.put(MAP_KEY_NAME_COUNTRY_ENGLISH_NAME, locale.getDisplayCountry(Locale.ENGLISH));
                countryInfoMapList.add(itemMap);
            }
        }
    }

    private void addSupportedCountryInfoToMapList(List<Map<String, Object>> countryInfoMapList) {
        Map<Integer, List<String>> regionMap = PhoneNumberUtilsEx.getCountryCodeToRegionCodeMap();
        Set<Integer> countryCodeKeySet = regionMap.keySet();

        for (Iterator iterator = countryCodeKeySet.iterator(); iterator.hasNext();) {
            Integer countryCode = (Integer) iterator.next();
            List<String> regionCodeList = regionMap.get(countryCode);
            for (Iterator iterator2 = regionCodeList.iterator(); iterator2.hasNext();) {
                String countryISO = (String)iterator2.next();
                if (!isSameMapItemExits(countryInfoMapList, countryISO)) {
                    Locale locale = new Locale(Locale.getDefault().getLanguage(), countryISO);
                    Map<String, Object> itemMap = new HashMap<String, Object>();
                    itemMap.put(MAP_KEY_NAME_COUNTRY_ISO, countryISO);
                    itemMap.put(MAP_KEY_NAME_COUNTRY_CODE, "+" + String.valueOf(countryCode));
                    itemMap.put(MAP_KEY_NAME_COUNTRY_NAME, locale.getDisplayCountry(Locale.getDefault()));
                    itemMap.put(MAP_KEY_NAME_COUNTRY_ENGLISH_NAME, locale.getDisplayCountry(Locale.ENGLISH));
                    countryInfoMapList.add(itemMap);
                }
            }
        }
    }

    private boolean isSameMapItemExits(List<Map<String, Object>> countryInfoMapList, String countryISO) {
        ListIterator<Map<String, Object>> iterator = countryInfoMapList.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> itemMap = (Map<String, Object>)iterator.next();
            if (itemMap.get(MAP_KEY_NAME_COUNTRY_ISO).equals(countryISO)) {
                return true;
            }
        }
        return false;
    }

    public void dismissHandledDialog() {
        if (null != mDialog && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
