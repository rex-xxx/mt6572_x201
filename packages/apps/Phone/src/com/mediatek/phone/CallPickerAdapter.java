package com.mediatek.phone;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.R;

import com.mediatek.phone.gemini.GeminiUtils;

import java.util.List;

public class CallPickerAdapter extends BaseAdapter {
    List<Call> mItems;
    Context mContext;
    private String mOperatorNameFirstCall;
    private String mOperatorNameSecondCall;
    private String mFirstCallerInfoName;
    private String mSecondCallerInfoName;
    private int mFirstSimColor;
    private int mSecondSimColor;
    private int mSimIndicatorPaddingLeft;
    private int mSimIndicatorPaddingRight;

    private int[] mSimDarkBorderMap = {
            R.drawable.sim_dark_blue,
            R.drawable.sim_dark_orange,
            R.drawable.sim_dark_green,
            R.drawable.sim_dark_purple,
        };

    public CallPickerAdapter(Context context, List<Call> items) {
        mContext = context;
        mItems = items;
        mSimIndicatorPaddingLeft = mContext.getResources().getDimensionPixelSize(
                R.dimen.call_banner_sim_indicator_padding_left);
        mSimIndicatorPaddingRight = mContext.getResources().getDimensionPixelSize(
                R.dimen.call_banner_sim_indicator_padding_right);
    }

    public int getCount() {
        // TODO Auto-generated method stub
        return mItems.size();
    }
    
    public Object getItem(int position) {
        return mItems.get(position);
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.call_select_list_item, null);
            holder.mDisplayName = (TextView) view.findViewById(R.id.displayName);
            holder.mOperator = (TextView) view.findViewById(R.id.operator);
            holder.mPhoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
            view.setTag(holder);
        }

        holder = (ViewHolder) view.getTag();
        Call call = mItems.get(position);

        String operatorName = getOperatorName(position);
        String displayName = getCallerInfoName(position);
        String address = null;
        if (null != call && null != call.getLatestConnection()) {
            address = call.getLatestConnection().getAddress();
        }

        if (null != address && null != displayName && tripHyphen(displayName).equals(address)) {
            holder.mDisplayName.setText(displayName);
            holder.mPhoneNumber.setVisibility(View.GONE);
        } else {
            if (null != displayName) {
                holder.mDisplayName.setText(displayName);
            }
            if (null != address) {
                holder.mPhoneNumber.setVisibility(View.VISIBLE);
                holder.mPhoneNumber.setText(address);
            }
        }
        if (null != operatorName) {
            updateCallOperatorBackground(call, holder.mOperator, getOperatorColor(position));
            holder.mOperator.setText(operatorName);
        }

        return view;
    }

    public void setOperatorName(String operator1, String operator2) {
        mOperatorNameFirstCall = operator1;
        mOperatorNameSecondCall = operator2;
    }

    public String getOperatorName(int position) {
        if (0 == position) {
            return mOperatorNameFirstCall;
        } else {
            return mOperatorNameSecondCall;
        }
    }

    public void setCallerInfoName(String callerName1, String callerName2) {
        mFirstCallerInfoName = callerName1;
        mSecondCallerInfoName = callerName2;
    }

    public String getCallerInfoName(int position) {
        if (0 == position) {
            return mFirstCallerInfoName;
        } else {
            return mSecondCallerInfoName;
        }
    }

    public void setOperatorColor(int simColor1, int simColor2) {
        mFirstSimColor = simColor1;
        mSecondSimColor = simColor2;
    }

    public int getOperatorColor(int position) {
        if (0 == position) {
            return mFirstSimColor;
        } else {
            return mSecondSimColor;
        }
    }

    private class ViewHolder {
        TextView mDisplayName;
        TextView mOperator;
        TextView mPhoneNumber;
    }

    private String tripHyphen(String number) {
        if (TextUtils.isEmpty(number)) {
            return number;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c != '-' && c != ' ') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void updateCallOperatorBackground(Call call, TextView operator, int simColor) {
        if (GeminiUtils.isGeminiSupport()) {
            final int phoneType = call.getPhone().getPhoneType();
            if (PhoneConstants.PHONE_TYPE_SIP == phoneType) {
                if (null != operator && operator.getVisibility() == View.VISIBLE) {
                    operator.setBackgroundResource(R.drawable.sim_dark_internet_call);
                }
            } else {
                if (null == mSimDarkBorderMap || simColor < 0
                        || simColor >= mSimDarkBorderMap.length) {
                    return;
                }
                if (null != operator && operator.getVisibility() == View.VISIBLE) {
                    operator.setBackgroundResource(mSimDarkBorderMap[simColor]);
                }
            }
        } else {
            if (null != operator && operator.getVisibility() == View.VISIBLE) {
                operator.setBackgroundResource(R.drawable.sim_dark_purple);
            }
        }
        if (null != operator && operator.getVisibility() == View.VISIBLE) {
            operator.setPadding(mSimIndicatorPaddingLeft, 0, mSimIndicatorPaddingRight, 0);
        }
    }
    
}
