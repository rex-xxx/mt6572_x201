package com.mediatek.ipmsg.ui;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mms.R;
import com.mediatek.ipmsg.util.IpMessageUtils;
import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.IpMessageConsts.RemoteActivities;

public class ConversationEmptyView extends LinearLayout {

    private LinearLayout mBackground;
    private Context mContext;
    private View mConvertView;
    private TextView mContent;
    private LinearLayout mActivate;
    private LinearLayout mImportant;
    private RelativeLayout mSpam;
    private RelativeLayout mGroupChat;
    private Button mBtnActivate;

    public ConversationEmptyView(Context context) {
        super(context);
    }

    public ConversationEmptyView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        mConvertView = inflater.inflate(R.layout.conversation_empty, this, true);
        mBackground = (LinearLayout)mConvertView.findViewById(R.id.background);
        mContent = (TextView)mConvertView.findViewById(R.id.tv_empty_content);
        mImportant = (LinearLayout)mConvertView.findViewById(R.id.ll_empty_important);
        mSpam = (RelativeLayout)mConvertView.findViewById(R.id.ll_empty_spam);
        mGroupChat = (RelativeLayout)mConvertView.findViewById(R.id.ll_empty_groupchat);
        mActivate = (LinearLayout)mConvertView.findViewById(R.id.ll_empty_activate);
        mBtnActivate = (Button)mConvertView.findViewById(R.id.btn_activate);
        mBtnActivate.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Intent intent = new Intent(RemoteActivities.ACTIVITION);
                // do to sim_id update.
                intent.putExtra(RemoteActivities.KEY_SIM_ID, 0); // need put int type SIM id
                IpMessageUtils.startRemoteActivity(context, intent);
            }});
    }

    public void setSpamEmpty(boolean isActivate) {
        mSpam.setVisibility(View.VISIBLE);
        mImportant.setVisibility(View.GONE);
        mGroupChat.setVisibility(View.GONE);
        mBackground.setBackgroundResource(R.color.empty_background);
        mContent.setText(IpMessageUtils.getResourceManager(mContext)
            .getSingleString(IpMessageConsts.string.ipmsg_spam_empty));
        setActivate(isActivate);

    }

    public void setImportantEmpty(boolean isActivate) {
        mSpam.setVisibility(View.GONE);
        mImportant.setVisibility(View.VISIBLE);
        mGroupChat.setVisibility(View.GONE);
        mBackground.setBackgroundResource(R.color.empty_background);
        mContent.setText(IpMessageUtils.getResourceManager(mContext)
            .getSingleString(IpMessageConsts.string.ipmsg_important_empty));
        setActivate(isActivate);
    }
    public void setGroupChatEmpty(boolean isActivate) {
        mSpam.setVisibility(View.GONE);
        mImportant.setVisibility(View.GONE);
        mGroupChat.setVisibility(View.VISIBLE);
        mBackground.setBackgroundResource(R.color.empty_background);
        mContent.setText(IpMessageUtils.getResourceManager(mContext)
            .getSingleString(IpMessageConsts.string.ipmsg_groupchat_empty));
        setActivate(isActivate);
    }

    public void setAllChatEmpty() {
        mBackground.setBackgroundResource(R.color.transparent);
        mContent.setText(IpMessageUtils.getResourceManager(mContext)
            .getSingleString(IpMessageConsts.string.ipmsg_allchat_empty));
        mSpam.setVisibility(View.GONE);
        mImportant.setVisibility(View.GONE);
        mGroupChat.setVisibility(View.GONE);
    }

    private void setActivate(boolean isActivate) {
        if (isActivate) {
            mActivate.setVisibility(View.GONE);
        } else {
            mActivate.setVisibility(View.VISIBLE);
        }
    }
}
