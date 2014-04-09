package com.mediatek.settings;

import android.content.Context;
import android.content.Intent;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.xlog.Xlog;

public class PreCheckForRunning {
    private CellConnMgr mCellConnMgr;
    private ServiceComplete mServiceComplete;
    private Context mContext;
    private Intent mIntent;
    private static final String TAG = "Settings/PreCheckForRunning";
    public boolean mByPass = false;
    
    public PreCheckForRunning(Context ctx) {
        mContext = ctx;
        mServiceComplete = new ServiceComplete();
        mCellConnMgr = new CellConnMgr(mServiceComplete);
        mCellConnMgr.register(mContext.getApplicationContext());
    }
    class ServiceComplete implements Runnable {
        public void run() {
            int result = mCellConnMgr.getResult();
            Xlog.d(TAG, "ServiceComplete with the result = " + CellConnMgr.resultToString(result));
            if (CellConnMgr.RESULT_OK == result || CellConnMgr.RESULT_STATE_NORMAL == result) {
                mContext.startActivity(mIntent);
            }
        }
    }
    public void checkToRun(Intent intent, int slotId, int req) {
        if (mByPass) {
            mContext.startActivity(intent);
            return ;
        }
        setIntent(intent);
        int r = mCellConnMgr.handleCellConn(slotId, req);
        Xlog.d(TAG, "The result of handleCellConn = " + CellConnMgr.resultToString(r));
    }
    
    public void setIntent(Intent it) {
        mIntent = it;
    }
    
    public void deRegister() {
        mCellConnMgr.unregister();
    }
}
