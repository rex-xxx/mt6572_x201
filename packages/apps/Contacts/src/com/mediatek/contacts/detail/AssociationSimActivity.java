package com.mediatek.contacts.detail;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity.NamedContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.Telephony.SIMInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.activities.ContactDetailActivity;
import com.android.contacts.util.NotifyingAsyncQueryHandler;

import com.mediatek.contacts.widget.SimPickerDialog; 
import com.mediatek.phone.SIMInfoWrapper;

import java.util.ArrayList;
import java.util.List;

public class AssociationSimActivity extends ContactsActivity implements
        NotifyingAsyncQueryHandler.AsyncQueryListener, OnClickListener,
        NotifyingAsyncQueryHandler.AsyncUpdateListener {
    
    public static final String INTENT_DATA_KEY_SEL_DATA_ID = "sel_data_id";
    public static final String INTENT_DATA_KEY_SEL_SIM_ID = "sel_sim_id";
    public static final String INTENT_DATA_KEY_CONTACT_DETAIL_INFO = "contact_detail_info";
    private static final String TAG = "AssociationSimActivity";
    
    
    private ActionBar                  mActionBar        = null;
    private ImageView                  mContactPhoto     = null;
    private ListView                   mListView         = null;
    private Button                     mBtnSave          = null;
    private Button                     mBtnCancel        = null;
    private NotifyingAsyncQueryHandler mHandler          = null;
    private SimInfoMgr                 mSimInfoMgr       = new SimInfoMgr();
    private NumberInfoMgr              mNumberInfoMgr    = new NumberInfoMgr();
    
    private long                       mInitDataId       = -1;
    private int                        mInitSimId        = -1;
    private int                        mAdapterChildSize = 2;
    private AlertDialog                mSelectDialog     = null;
    private int                        mSelectDialogType = 0;
    private ContactDetailInfo          mContactDetailInfo;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Intent intent = this.getIntent();
        this.mInitDataId = intent.getLongExtra(INTENT_DATA_KEY_SEL_DATA_ID, -1);
        this.mInitSimId = intent.getIntExtra(INTENT_DATA_KEY_SEL_SIM_ID, -1);
        mContactDetailInfo = intent.getParcelableExtra(INTENT_DATA_KEY_CONTACT_DETAIL_INFO);
        if (mContactDetailInfo == null) {
            Log.e(TAG, "[onCreate]intent did not carry ContactDetailInfo: " + intent);
            throw new IllegalArgumentException(
                    "A ContactDetailInfo object should be put in the intent to launch this Activity");
        }
        this.setContentView(R.layout.association_sim);
        this.initView();

        this.mHandler = new NotifyingAsyncQueryHandler(this, this);
        mHandler.setUpdateListener(this);
    }
        
    public void initView() {
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_HOME);
            this.mActionBar.setTitle(mContactDetailInfo.mDisplayTitle);
            /*
             * Bug Fix by Mediatek Begin. Original Android's code:
             * this.mActionBar.setSubtitle(sContactDetailInfo.mDisplaySubtitle);
             * CR ID: ALPS00115576
             */
            if (mContactDetailInfo.mDisplaySubtitle != null
                    && !mContactDetailInfo.mDisplaySubtitle.isEmpty()) {
                this.mActionBar.setSubtitle(mContactDetailInfo.mDisplaySubtitle);
            }
            /*
             * Bug Fix by Mediatek End.
             */
        }

        this.mContactPhoto = (ImageView) this.findViewById(R.id.contact_photo);
        if (this.mContactPhoto != null) {
            mContactDetailInfo.setPhoto(this.mContactPhoto);
            /** M: Bug Fix for ALPS00335357 */
            this.mContactPhoto.setClickable(false);
        }

        this.mListView = (ListView) findViewById(R.id.association_list);
        this.mBtnSave = (Button) findViewById(R.id.btn_done);
        this.mBtnCancel = (Button) findViewById(R.id.btn_discard);

        this.mBtnSave.setOnClickListener(this);
        this.mBtnCancel.setOnClickListener(this);
    } 
   
    @Override
    protected void onResume() {
        super.onResume();

        this.mNumberInfoMgr.initNumberInfo();
        this.mSimInfoMgr.initSimInfo(this);
        this.mNumberInfoMgr.setShowingNumberNameByDataId(this.mInitDataId);
        this.mSimInfoMgr.setShowingIndexBySimId(this.mInitSimId);

        this.mListView.setAdapter(new ListViewAdapter(this));
        this.mListView.setOnItemClickListener(this.mOnListViewItemClick);

        this.mBtnSave.setEnabled(this.isSimInfoChanged());
    }
    
    /*
     * Bug Fix by Mediatek Begin.
     *   Original Android's code:
     *   
     *   CR ID: ALPS00114099
     */
    @Override
    protected void onDestroy() {
        Intent intent = this.getIntent();
        intent.putExtra(INTENT_DATA_KEY_SEL_DATA_ID, this.mInitDataId);
        intent.putExtra(INTENT_DATA_KEY_SEL_SIM_ID, this.mInitSimId);
        super.onDestroy();
    }
    /*
     * Bug Fix by Mediatek End.
     */
    
    private boolean isSimInfoChanged() {
        if (mNumberInfoMgr.mNumberInfoList.size() > 0 && mSimInfoMgr.mSimInfoList.size() > 0) {
            return mNumberInfoMgr.getShowingNumberSimId() != mSimInfoMgr.getShowingSimId();
        }
        return false;
    }
    
    public void closeSelectDialog() {
        if (mSelectDialog != null && !this.isFinishing()) {
            mSelectDialog.dismiss();
            mSelectDialog = null;
        }
    }
    
    public void saveAssociationSimInfo() {
        ContentValues values = new ContentValues();
        values.put(Data.SIM_ASSOCIATION_ID, mSimInfoMgr.getShowingSimId());
        this.mHandler.startUpdate(0, this, Data.CONTENT_URI, values, Data._ID + "=? ",
                new String[] {
                    String.valueOf(mNumberInfoMgr.getShowingNumberDataId())
                });
    }
    
    public void setListViewChildText(View view, String text1, String text2) {
        TextView txtTitle = (TextView) view.findViewById(R.id.text1);
        txtTitle.setText(text1);
        TextView txtData = (TextView) view.findViewById(R.id.text2);
        txtData.setText(text2);
    }
    
    private void returnToContactDetail(boolean isNewIntent) {
        /** M: Bug Fix for ALPS00405416 @{ */
        //do not start ContactDetailActivity. it will finish when user click ok or cancel.
        Log.i(TAG, "returnToContactDetail : " + isNewIntent);
//        if (isNewIntent) {
//            final Intent intent = new Intent(Intent.ACTION_VIEW, sContactDetailInfo.mLookupUri);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
//        }
        /** @} */
        finish();
    }

    private OnItemClickListener mOnListViewItemClick = new AdapterView.OnItemClickListener() {

        public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
            if (position < 2) {
                closeSelectDialog();
                if (position == 1 && mSimInfoMgr.mSimInfoList.size() == 0) {
                    Log.i(TAG, "[onListViewItemClick]: mSimInfoList.size() = 0");
                    return;
                }
                mSelectDialogType = position;
                final NumberInfoAdapter adapter;

                final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectDialogType == 0) {
                            /*
                             * Bug Fix by Mediatek Begin. Original Android's
                             * code: mNumberInfoMgr.setShowingIndex(which); } CR
                             * ID: ALPS00115837
                             */
                            if (!mNumberInfoMgr.setShowingIndex(which)) {
                                closeSelectDialog();
                                return;
                            }
                            /*
                             * Bug Fix by Mediatek End.
                             */
                            mInitDataId = mNumberInfoMgr.getShowingNumberDataId();
                            mSimInfoMgr.setShowingIndexBySimId(mNumberInfoMgr
                                    .getShowingNumberSimId());
                        } else {
                            final ListAdapter listAdapter = mSelectDialog.getListView()
                                    .getAdapter();
                            final int slot = ((Integer) listAdapter.getItem(which)).intValue();
                            mSimInfoMgr.setShowingSlot(slot);
                            /*
                             * Bug Fix by Mediatek Begin. Original Android's
                             * code: mInitSimId = mSimInfoMgr.getShowingSimId();
                             * } CR ID: ALPS00114099
                             */
                        }
                        mInitSimId = mSimInfoMgr.getShowingSimId();
                        /*
                         * Bug Fix by Mediatek End.
                         */
                        mListView.setAdapter(new ListViewAdapter(AssociationSimActivity.this));
                        mBtnSave.setEnabled(isSimInfoChanged());
                        closeSelectDialog();
                    }
                };

                if (position == 0) {
                    adapter = new NumberInfoAdapter(AssociationSimActivity.this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            AssociationSimActivity.this);
                    builder.setSingleChoiceItems(adapter, -1, onClickListener).setTitle(
                            mContactDetailInfo.mDisplayTitle).setIcon(
                            android.R.drawable.ic_menu_more);
                    mSelectDialog = builder.create();
                } else {
                    /*
                     * Bug Fix by Mediatek Begin. Original Android's code:
                     * mSelectDialog =
                     * SimPickerDialog.create(AssociationSimActivity.this,
                     * getResources().getString(R.string.associate_SIM),
                     * onClickListener); CR ID: ALPS00115742
                     */
                    mSelectDialog = SimPickerDialog.create(AssociationSimActivity.this,
                            getResources().getString(R.string.associate_SIM), false,
                            onClickListener, false);
                    /*
                     * Bug Fix by Mediatek End.
                     */
                }
                mSelectDialog.show();
            }
        }

    };
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.returnToContactDetail(true);
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
  
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_done:
                this.saveAssociationSimInfo();
                this.returnToContactDetail(true);
                break;

            case R.id.btn_discard:
                this.returnToContactDetail(false);
                break;

            default:
                break;
        }
    }    

    public void onQueryComplete(int token, Object cookie, Cursor cursor) {

    }

    private class ListViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        ListViewAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mAdapterChildSize;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.association_sim_item, null);
            ImageView moreImage = (ImageView) convertView.findViewById(R.id.more_icon);
            moreImage.setImageResource(R.drawable.ic_btn_round_more_normal);

            if (position == 0) {
                setListViewChildText(convertView, mNumberInfoMgr.getShowingNumberTypeName(),
                        mNumberInfoMgr.getShowingNumberContent());
            } else {
                setListViewChildText(convertView, getResources().getString(
                        R.string.associatiated_SIM), mSimInfoMgr.getShowingSimName());
            }
            convertView.setTag(position);

            return convertView;
        }

    }
        
    private class NumberInfoAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        NumberInfoAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mNumberInfoMgr.mNumberInfoList.size();
        }

        public Object getItem(int position) {
            return mNumberInfoMgr.mNumberInfoList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.number_list_item, null);
            setListViewChildText(convertView, mNumberInfoMgr.getTypeName(position), mNumberInfoMgr
                    .getContent(position));
            convertView.setTag(position);
            return convertView;
        }

    }

    public final class NumberInfoMgr {
        public List<NamedContentValues> mNumberInfoList = null;

        private int mShowingIndex = -1;

        public void clear() {
            if (this.mNumberInfoList != null) {
                this.mNumberInfoList.clear();
            }
            this.mShowingIndex = -1;
        }

        public boolean initNumberInfo() {
            this.clear();
            this.mNumberInfoList = new ArrayList<NamedContentValues>();
            for (NamedContentValues values : mContactDetailInfo.mNumberInfoList) {
                this.mNumberInfoList.add(values);
            }
            return !this.mNumberInfoList.isEmpty();
        }

        public boolean setShowingIndex(int showingIndex) {
            if (this.mShowingIndex != showingIndex) {
                if (showingIndex > -1 && showingIndex < this.mNumberInfoList.size()) {
                    this.mShowingIndex = showingIndex;
                    return true;
                }
            }
            return false;
        }

        public String getShowingNumberContent() {
            return getContent(this.mShowingIndex);
        }

        public String getShowingNumberTypeName() {
            return getTypeName(this.mShowingIndex);
        }

        public String getContent(int index) {
            if (index > -1 && index < this.mNumberInfoList.size()) {
                NamedContentValues subValues = this.mNumberInfoList.get(index);
                return subValues.values.getAsString(Data.DATA1);
            }
            return "";
        }

        public String getTypeName(int index) {
            if (index > -1 && index < this.mNumberInfoList.size()) {
                NamedContentValues subValues = this.mNumberInfoList.get(index);
                int type = Integer.parseInt(subValues.values.getAsString(Data.DATA2));
                return (String) CommonDataKinds.Phone.getTypeLabel(getResources(), type, null);
            }
            return "";
        }

        public boolean setShowingNumberNameByDataId(long id) {
            for (int i = 0; i < this.mNumberInfoList.size(); i++) {
                NamedContentValues subValues = this.mNumberInfoList.get(i);
                if (id == subValues.values.getAsLong(Data._ID)) {
                    this.mShowingIndex = i;
                    return true;
                }
            }
            return false;
        }

        public int getShowingNumberSimId() {
            if (this.mShowingIndex > -1) {
                NamedContentValues subValues = this.mNumberInfoList.get(this.mShowingIndex);
                return subValues.values.getAsInteger(Data.SIM_ASSOCIATION_ID);
            }
            return -1;
        }

        public long getShowingNumberDataId() {
            if (this.mShowingIndex > -1) {
                NamedContentValues subValues = this.mNumberInfoList.get(this.mShowingIndex);
                return subValues.values.getAsLong(Data._ID);
            }
            return -1;
        }
    }
           
    public final class SimInfoMgr {
        public List<SIMInfo> mSimInfoList = null;

        private int mShowingIndex = -1;

        public void clear() {
            if (this.mSimInfoList != null) {
                /** M: The static reference will change the default slot information @ { */
                //this.mSimInfoList.clear();
                this.mSimInfoList = null;
                /** @ } */
            }
            this.mShowingIndex = -1;
        }

        public boolean initSimInfo(Context context) {
            this.clear();
            this.mSimInfoList = SIMInfoWrapper.getDefault().getInsertedSimInfoList();
            return !this.mSimInfoList.isEmpty();
        }

        public boolean setShowingIndex(int showingIndex) {
            if (this.mShowingIndex != showingIndex) {
                this.mShowingIndex = showingIndex;
                return true;
            }
            return false;
        }

        public boolean setShowingSlot(int slot) {
            if (slot < 0) {
                return this.setShowingIndex(-1);
            }
            for (int i = 0; i < this.mSimInfoList.size(); i++) {
                SIMInfo simInfo = this.mSimInfoList.get(i);
                if (simInfo.mSlot == slot) {
                    this.mShowingIndex = i;
                    return true;
                }
            }
            return false;
        }

        public boolean setShowingIndexBySimId(int simId) {
            if (simId < 0) {
                return this.setShowingIndex(-1);
            }
            for (int i = 0; i < this.mSimInfoList.size(); i++) {
                SIMInfo simInfo = this.mSimInfoList.get(i);
                if (simInfo.mSimId == simId) {
                    this.mShowingIndex = i;
                    return true;
                }
            }
            return false;
        }

        public String getShowingSimName() {
            if (this.mShowingIndex > -1) {
                return this.mSimInfoList.get(this.mShowingIndex).mDisplayName;
            }
            return getResources().getString(R.string.unassociated);
        }

        public int getShowingSimId() {
            if (this.mShowingIndex > -1) {
                return (int) this.mSimInfoList.get(this.mShowingIndex).mSimId;
            }
            return -1;
        }
    }

    public static final class ContactDetailInfo implements Parcelable {
        public String mDisplayTitle = "";

        public String mDisplaySubtitle = "";

        public Uri mLookupUri = null;

        public List<NamedContentValuesDecorator> mNumberInfoList = new ArrayList<NamedContentValuesDecorator>();
        public String mPhotoUri = null;

        public ContactDetailInfo(String displayTitle, String displaySubtitle, Uri lookupUri,
                List<NamedContentValues> numberInfoList, String photoUri) {
            mDisplayTitle = displayTitle;
            mDisplaySubtitle = displaySubtitle;
            mLookupUri = lookupUri;
            for (NamedContentValues values : numberInfoList) {
                mNumberInfoList.add(new NamedContentValuesDecorator(values));
            }
            mPhotoUri = photoUri;

        }

        public void setPhoto(ImageView photoView) {
            if (null != mPhotoUri) {
                photoView.setImageURI(Uri.parse(mPhotoUri));
            } else {
                photoView.setImageResource(R.drawable.ic_contact_picture_180_holo_light);
            }
        }

        /**
         * M: [ALPS00578150]ContactDetailInfo should be a Parcel, so that it can be carried by intent,
         * and saved & restore when Process be killed. @{
         */
        public static Parcelable.Creator<ContactDetailInfo> CREATOR = new Creator<ContactDetailInfo>() {

            @Override
            public ContactDetailInfo createFromParcel(Parcel source) {
                ClassLoader classLoader = getClass().getClassLoader();
                /**
                 * M: keep the sequence: 
                 * displayTitle -> displaySubtitle -> lookupUri -> numberInfoList -> photoUri
                 */
                String displayTitle = source.readString();
                String displaySubTitle = source.readString();
                Uri lookupUri = source.readParcelable(classLoader);
                List<NamedContentValues> numberInfoList = new ArrayList<NamedContentValues>();
                source.readList(numberInfoList, classLoader);
                String photoUri = source.readString();
                return new ContactDetailInfo(displayTitle, displaySubTitle, lookupUri, numberInfoList, photoUri);
            }

            @Override
            public ContactDetailInfo[] newArray(int size) {
                return new ContactDetailInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flag) {
            /**
             * M: keep the sequence: 
             * displayTitle -> displaySubtitle -> lookupUri -> numberInfoList -> photoUri
             */
            dest.writeString(mDisplayTitle);
            dest.writeString(mDisplaySubtitle);
            dest.writeParcelable(mLookupUri, 0);
            dest.writeList(mNumberInfoList);
            dest.writeString(mPhotoUri);
        }

        /**
         * M: use a Decorator to wrapper NamedContentValues, in order to make the NamedContentValues content
         * to be Parcelable, and can be carried by Intent
         */
        private static class NamedContentValuesDecorator extends NamedContentValues implements Parcelable {

            public NamedContentValuesDecorator(Uri uri, ContentValues values) {
                super(uri, values);
            }

            public NamedContentValuesDecorator(NamedContentValues namedContentValues) {
                super(namedContentValues.uri, namedContentValues.values);
            }

            public static Creator<NamedContentValuesDecorator> CREATOR = new Creator<NamedContentValuesDecorator>() {

                @Override
                public NamedContentValuesDecorator[] newArray(int size) {
                    return new NamedContentValuesDecorator[size];
                }

                @Override
                public NamedContentValuesDecorator createFromParcel(Parcel source) {
                    ClassLoader classLoader = getClass().getClassLoader();
                    Uri uri = source.readParcelable(classLoader);
                    ContentValues values = source.readParcelable(classLoader);
                    return new NamedContentValuesDecorator(uri, values);
                }
            };

            @Override
            public int describeContents() {
                return 0;
            }

            @Override
            public void writeToParcel(Parcel dest, int flag) {
                dest.writeParcelable(uri, 0);
                dest.writeParcelable(values, 0);
            }

            @Override
            public String toString() {
                return "[NamedContentValuesDecorator]uri = " + uri + ", ContentValues = " + values + ", this = " + this;
            }

        }
        /**
         * M: [ALPS00578150]Parcelable @}
         */

    }

    public void onUpdateComplete(int token, Object cookie, int result) {
        if (token == 0) {
            this.sendBroadcast(new Intent("com.android.contacts.associate_changed"));
        }
    }
}