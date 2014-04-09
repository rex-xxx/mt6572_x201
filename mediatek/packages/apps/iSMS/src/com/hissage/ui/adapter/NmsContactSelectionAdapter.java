package com.hissage.ui.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.hissage.R;
import com.hissage.api.NmsIpMessageApiNative;
import com.hissage.contact.NmsContact.NmsContactType;
import com.hissage.contact.NmsUIContact;
import com.hissage.platfrom.NmsPlatformAdapter;
import com.hissage.struct.SNmsSimInfo;
import com.hissage.ui.activity.NmsContactSelectionActivity;
import com.hissage.ui.activity.NmsContactSelectionActivity.SelectionMode;
import com.hissage.util.log.NmsLog;

public class NmsContactSelectionAdapter extends BaseAdapter {

    public String Tag = "ContactListAdapter";

    private SelectionMode mType;
    private Context mContext;
    private ArrayList<NmsUIContact> mContacList;
    private HashMap<String, String> mSelectedContactId;
    public ArrayList<NmsUIContact> mSearchList;
    private String mSearchStr = "";
    private boolean mIsSearch = false;
    private int mCount;
    private boolean mIsActivated = false;
    private int mSimId;
    private final LruCache<Long, Bitmap> mContactAvatarItemCach;

    private static class ViewHolder {
        TextView tvName;
        TextView tvPhoneNum;
        CheckBox cbCheck;
        ImageView ivLog;
        ImageView qcbAvatar;
    }

    public NmsContactSelectionAdapter(Context context, ArrayList<NmsUIContact> contacList,
            SelectionMode mode, int simId) {
        mSelectedContactId = new HashMap<String, String>();
        mContext = context;
        mContacList = contacList;
        mCount = contacList.size();
        mType = mode;
        mSimId = simId;
        mContactAvatarItemCach = new LruCache<Long, Bitmap>(50);
        if (isActivated()) {
            mIsActivated = true;
        }
    }

    public int getSelectedCount() {
        if (mSelectedContactId != null) {
            return mSelectedContactId.size();
        } else {
            return 0;
        }
    }

    public void exitSearch() {
        mIsSearch = false;
        mCount = mContacList.size();
        this.notifyDataSetChanged();
    }

    public int search(String queryString) {
        mIsSearch = true;
        mSearchStr = queryString;
        buildSearchList();
        mCount = mSearchList.size();
        this.notifyDataSetChanged();

        return mCount;
    }

    private void buildSearchList() {
        mSearchList = new ArrayList<NmsUIContact>();
        for (NmsUIContact contact : mContacList) {
            if (isContain(contact.getSortKey()) || contact.getNumberOrEmail().contains(mSearchStr)) {
                mSearchList.add(contact);
            }
        }
    }

    private boolean isContain(String sortKey) {
        if (mSearchStr == null || mSearchStr.trim().length() == 0) {
            return true;
        }

        int index = 0;
        sortKey = sortKey.toUpperCase();
        int length = mSearchStr.trim().length();

        for (int i = 0; i < length; i++) {
            char s = mSearchStr.trim().charAt(i);
            if (index == -1 || !sortKey.substring(index).contains((s + "").toUpperCase())) {
                return false;
            } else {
                index = sortKey.indexOf((s + "").toUpperCase(), index) + 1;
            }
        }

        return true;
    }

    public boolean checkSelection(int index) {
        NmsUIContact c = null;

        if (!mIsSearch) {
            c = mContacList.get(index);
        } else {
            c = mSearchList.get(index);
        }

        // short cId = c.getEngineContactid();
        // String contactId = String.valueOf(cId);
        String phoneNum = c.getNumberOrEmail();

        if (mSelectedContactId.containsKey(phoneNum)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getCount() {

        NmsLog.trace(Tag, "Get all converation count:" + mCount);
        return mCount;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            final LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.contact_item, null);

            holder = new ViewHolder();

            holder.tvName = (TextView) convertView.findViewById(R.id.tv_contact_name);
            holder.tvPhoneNum = (TextView) convertView.findViewById(R.id.tv_contact_phonenumber);
            holder.cbCheck = (CheckBox) convertView.findViewById(R.id.cb_check);
            holder.ivLog = (ImageView) convertView.findViewById(R.id.iv_logo);
            holder.qcbAvatar = (ImageView) convertView.findViewById(R.id.qcb_avatar);
            holder.qcbAvatar.setClickable(false);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mType == SelectionMode.SINGLE) {
            holder.cbCheck.setVisibility(View.GONE);
        } else {
            holder.cbCheck.setVisibility(View.VISIBLE);
        }

        NmsUIContact c = null;
        if (!mIsSearch) {
            c = mContacList.get(position);
        } else {
            c = mSearchList.get(position);
        }

        if (c != null) {
            if (mSelectedContactId.containsKey(String.valueOf(c.getNumberOrEmail()))) {
                holder.cbCheck.setChecked(true);
            } else {
                holder.cbCheck.setChecked(false);
            }

            if (mIsSearch) {
                String lineOne = c.getName();
                String lineTwo = c.getNumberOrEmail();
                int i = 0;
                int j = 0;
                int index = 0;
                Spannable splineOne = new SpannableString(lineOne);
                Spannable splineTwo = new SpannableString(lineTwo);

                for (i = 0; i < mSearchStr.length(); i++) {
                    String s = mSearchStr.substring(i, i + 1);
                    for (j = index; j < lineOne.length(); j++) {
                        String subone = lineOne.substring(j, j + 1);
                        if (s.compareToIgnoreCase(subone) == 0) {
                            splineOne.setSpan(new ForegroundColorSpan(mContext.getResources()
                                    .getColor(R.color.search_highlight)), j, j + 1,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            index = ++j;
                            break;
                        }
                    }
                }

                if (lineTwo.contains(mSearchStr)) {
                    index = lineTwo.indexOf(mSearchStr);
                    splineTwo.setSpan(
                            new ForegroundColorSpan(mContext.getResources().getColor(
                                    R.color.search_highlight)), index, index + mSearchStr.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                holder.tvName.setText(splineOne);
                holder.tvPhoneNum.setText(c.getNumberOrEmailType(mContext) + " " + splineTwo);
            } else {
                holder.tvName.setText(c.getName());
                holder.tvPhoneNum.setText(c.getNumberOrEmailType(mContext) + " " + c.getNumberOrEmail());
            }

            Bitmap avatar = mContactAvatarItemCach.get(c.getSystemContactId());
            if (avatar == null) {
                avatar = c.getAvatar(mContext);
                if (avatar == null) {
                    avatar = BitmapFactory.decodeResource(mContext.getResources(),
                            R.drawable.ic_contact_picture);
                }
                mContactAvatarItemCach.put(c.getSystemContactId(), avatar);
            }
            holder.qcbAvatar.setImageBitmap(avatar);

            if ((c.getType() == NmsContactType.HISSAGE_USER) && mIsActivated) {
                holder.ivLog.setVisibility(View.VISIBLE);
            } else {
                holder.ivLog.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private boolean isActivated() {
        boolean isActivated = false;
        if(mSimId == NmsContactSelectionActivity.DEFAUT_SIMID){
            mSimId = (int)NmsPlatformAdapter.getInstance(mContext).getCurrentSimId();
        }
        
        if (NmsIpMessageApiNative.nmsGetActivationStatus(mSimId) == SNmsSimInfo.NmsSimActivateStatus.NMS_SIM_STATUS_ACTIVATED) {
            isActivated = true;
        }
        return isActivated;
    }
    public void check(int index) {

        NmsUIContact c = null;

        if (!mIsSearch) {
            c = mContacList.get(index);
        } else {
            c = mSearchList.get(index);
        }

        short cId = c.getEngineContactId();
        String contactId = String.valueOf(cId);
        String phoneNum = c.getNumberOrEmail();

        if (mSelectedContactId.containsKey(phoneNum)) {
            mSelectedContactId.remove(phoneNum);
        } else {
            mSelectedContactId.put(phoneNum, contactId);
        }

        this.notifyDataSetChanged();
    }

    public long selectSingleContact(int index) {

        NmsUIContact c = null;

        if (!mIsSearch) {
            c = mContacList.get(index);
        } else {
            c = mSearchList.get(index);
        }

        long cId = c.getSystemContactId();

        return cId;
    }

    public int checkAll(boolean check) {
        if (check) {
            for (NmsUIContact c : mContacList) {
                short cId = c.getEngineContactId();
                String contactId = String.valueOf(cId);
                String phoneNum = c.getNumberOrEmail();
                mSelectedContactId.put(phoneNum, contactId);
            }
        } else {
            mSelectedContactId.clear();
        }

        this.notifyDataSetChanged();

        return mSelectedContactId.size();
    }

    public String[] getSelectContactId() {
        String[] contactId = new String[mSelectedContactId.size()];
        mSelectedContactId.values().toArray(contactId);

        return contactId;
    }

    public String getSelectPhoneNumber() {
        String phone = "";
        List<String> phoneList = new ArrayList<String>(mSelectedContactId.keySet());
        for (int i = 0; i < phoneList.size(); i++) {
            phone += phoneList.get(i) + ";";
        }

        if (!TextUtils.isEmpty(phone)) {
            phone = phone.substring(0, phone.length() - 1);
        }

        return phone;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }
}
