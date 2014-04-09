package com.android.camera.manager;

import android.view.View;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;

public class InfoManager extends ViewManager {
    private static final String TAG = "InfoManager";
    private static final boolean LOG = Log.LOGV;
    
    private TextView mInfoView;
    private String mInfoText;
    
    public InfoManager(Camera context) {
        super(context);
    }
    
    @Override
    protected View getView() {
        View view = inflate(R.layout.onscreen_info);
        mInfoView = (TextView)view.findViewById(R.id.info_view);
        return view;
    }
    
    public void showText(String text) {
        if (LOG) {
            Log.v(TAG, "showText(" + text + ")");
        }
        mInfoText = text;
        show();
    }
    
    @Override
    protected void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mInfoView=" + mInfoView + ", mInfoText=" + mInfoText);
        }
        if (mInfoView != null) {
            mInfoView.setText(mInfoText);
            int visibility = mInfoText != null ? View.VISIBLE : View.INVISIBLE;
            mInfoView.setVisibility(visibility);
        }
    }
}
