package com.android.browser;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.NotificationPermissions;
import android.webkit.NotificationPermissions.Callback;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class NotificationPermissionsPrompt extends RelativeLayout {

    private static final String TAG = "NotificationPermissionsPrompt";

    private NotificationPermissions.Callback mCallback;

    private TextView mTitle;
    private TextView mBody;
    private String mOrigin;
    private Button mDenyButton;
    private Button mAllowButton;    

    public NotificationPermissionsPrompt(Context context) {
        this(context, null);
    }

    public NotificationPermissionsPrompt(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void handleButtonClick(boolean allow) {
        Log.v(TAG, "Inside NotificationPermissionsPrompt handleButtonClick");
        hide();
        mCallback.invoke(mOrigin, allow);
    }

    private void init() {
        Log.v(TAG, "Inside NotificationPermissionsPrompt init");
        mTitle = ((TextView)findViewById(R.id.title));
        mBody = ((TextView)findViewById(R.id.body));
        mDenyButton = ((Button)findViewById(R.id.deny_button));
        mAllowButton = ((Button)findViewById(R.id.allow_button));

        mDenyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                handleButtonClick(false);
            }
        });
        mAllowButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                handleButtonClick(true);
            }
        });
    }

    private void setMessage(CharSequence message) {
        Log.v(TAG, "Inside NotificationPermissionsPrompt setMessage" + message);
        mTitle.setText(String.format(getResources().getString(R.string.notification_permissions_prompt_message), new Object[] { message }));
    }

    public void hide() {
        setVisibility(View.GONE);
    }

    protected void onFinishInflate() {
        Log.v(TAG, "Inside NotificationPermissionsPrompt onFinishInflate");
        super.onFinishInflate();
        init();
    }

    public void show(String origin, NotificationPermissions.Callback callback) {
        Log.v(TAG, "Inside NotificationPermissionsPrompt show, origin = " + origin);
        mOrigin = origin;
        mCallback = callback;
        if ("http".equals(Uri.parse(mOrigin).getScheme())){
            setMessage(mOrigin.substring(7));
        } else {
            setMessage(mOrigin);
        }
        
        setVisibility(View.VISIBLE);
    }
}
