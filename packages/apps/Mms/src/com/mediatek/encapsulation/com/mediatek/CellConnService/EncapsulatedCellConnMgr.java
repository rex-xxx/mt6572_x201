
package com.mediatek.encapsulation.com.mediatek.CellConnService;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedCellConnMgr {
    /** M:MTK ADD */
    CellConnMgr mCellConnMgr;

    private static final String TAG = "EncapsulatedCellConnMgr";

    public static final int RESULT_UNKNOWN = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_UNKNOWN
            : -1;

    public static final int RESULT_OK = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_OK
            : 0;

    public static final int RESULT_WAIT = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_WAIT
            : 1;

    public static final int RESULT_ABORT = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_ABORT
            : 2;

    public static final int RESULT_EXCEPTION = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_EXCEPTION
            : 3;

    public static final int RESULT_STATE_NORMAL = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.RESULT_STATE_NORMAL
            : 4;

    public static final int REQUEST_TYPE_UNKNOWN = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.REQUEST_TYPE_UNKNOWN
            : 300;

    public static final int REQUEST_TYPE_SIMLOCK = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.REQUEST_TYPE_SIMLOCK
            : 302;

    public static final int REQUEST_TYPE_FDN = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.REQUEST_TYPE_FDN
            : 304;

    public static final int REQUEST_TYPE_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.REQUEST_TYPE_ROAMING
            : 306;

    public static final int FLAG_SUPPRESS_CONFIRMDLG = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.FLAG_SUPPRESS_CONFIRMDLG
            : 0x80000000;

    public static final int FLAG_REQUEST_NOPREFER = EncapsulationConstant.USE_MTK_PLATFORM ? CellConnMgr.FLAG_REQUEST_NOPREFER
            : 0x40000000;

    public EncapsulatedCellConnMgr(Runnable r) {
        mCellConnMgr = new CellConnMgr(r);

    }

    public EncapsulatedCellConnMgr() {
        mCellConnMgr = new CellConnMgr();

    }

    /** M:MTK ADD */
    public static String resultToString(int ret) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return CellConnMgr.resultToString(ret);
        } else {
            if (RESULT_OK == ret) {
                return "RESULT_OK";
            } else if (RESULT_WAIT == ret) {
                return "RESULT_WAIT";
            } else if (RESULT_ABORT == ret) {
                return "RESULT_ABORT";
            } else if (RESULT_UNKNOWN == ret) {
                return "RESULT_UNKNOWN";
            } else if (RESULT_EXCEPTION == ret) {
                return "RESULT_EXCEPTION";
            } else if (RESULT_STATE_NORMAL == ret) {
                return "RESULT_STATE_NORMAL";
            }
            return "null";
        }
    }

    /** M:MTK ADD */
    public void register(Context ctx) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mCellConnMgr.register(ctx);
        } else {
            /** M: Can not complete for this branch. */
        }
    }

    /** M:MTK ADD */
    public void unregister() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mCellConnMgr.unregister();
        } else {
            /** M: Can not complete for this branch. */
        }
    }

    /** M:MTK ADD */
    public int getResult() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mCellConnMgr.getResult();
        } else {
            /** M: Can not complete for this branch. */
            return RESULT_OK;
        }
    }

    /** M:MTK ADD */
    public int getPreferSlot() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mCellConnMgr.getPreferSlot();
        } else {
            /** M: Can not complete for this branch. */
            return 0;
        }
    }

    /*
     * Handle the request for Cell Connection Return: RESULT_WAIT - need to wait
     * the service callback RESULT_OK - the current cell connection state is
     * normal and can continue to next action RESULT_EXCEPTION - exception
     * occurred at the handling action
     */
    /** M:MTK ADD */
    public int handleCellConn(int slot, int reqType) {

        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mCellConnMgr.handleCellConn(slot, reqType);
        } else {
            /** M: Can not complete for this branch. */
            return RESULT_OK;
        }
    }

    /** M:MTK ADD */
    public int handleCellConn(int slot, int reqType, Runnable r) {

        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mCellConnMgr.handleCellConn(slot, reqType, r);
        } else {
            /** M: Can not complete for this branch. */
            return RESULT_OK;
        }
    }

}
