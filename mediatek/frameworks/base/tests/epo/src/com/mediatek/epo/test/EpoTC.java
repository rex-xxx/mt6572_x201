package com.mediatek.epo.test;

import android.test.InstrumentationTestCase;
import android.util.Log;
import android.content.Context;
import android.os.Bundle;

import com.mediatek.common.epo.MtkEpoClientManager;
import com.mediatek.common.epo.MtkEpoFileInfo;
import com.mediatek.common.epo.MtkEpoStatusListener;

public class EpoTC extends InstrumentationTestCase {
    
    private final static String TAG = "epo_test";
    private MtkEpoClientManager mEpoMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        Context context = getInstrumentation().getTargetContext();
        mEpoMgr = (MtkEpoClientManager)context.getSystemService(Context.MTK_EPO_CLIENT_SERVICE);
        assertNotNull(mEpoMgr);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mEpoMgr = null;
        msleep(50);
    }

    public void test01Enable() {
        log("test01Enable ++");
        mEpoMgr.enable();
        assertTrue(mEpoMgr.getStatus());
        log("test01Enable --");
    }
    public void test02Disable() {
        log("test02Disable ++");
        mEpoMgr.disable();
        assertFalse(mEpoMgr.getStatus());
        log("test02Disable --");
    }
    public void test03EnableDisable() {
        log("test03EnableDisable ++");
        mEpoMgr.enable();
        assertTrue(mEpoMgr.getStatus());
        mEpoMgr.disable();
        assertFalse(mEpoMgr.getStatus());
        log("test03EnableDisable --");
    }
    public void test04EnableDisableStress() {
        log("test04EnableDisableStress ++");
        for(int i = 0; i < 10; i++) {
            mEpoMgr.enable();
            assertTrue(mEpoMgr.getStatus());
            mEpoMgr.disable();
            assertFalse(mEpoMgr.getStatus());
        }
        log("test04EnableDisableStress --");
    }
    public void test05AutoEnable() {
        log("test05AutoEnable ++");
        mEpoMgr.enableAutoDownload(true);
        assertTrue(mEpoMgr.getAutoDownloadStatus());
        log("test05AutoEnable --");
    }
    public void test06AutoDisable() {
        log("test06AutoDisable ++");
        mEpoMgr.enableAutoDownload(false);
        assertFalse(mEpoMgr.getAutoDownloadStatus());
        log("test06AutoDisable --");
    }
    public void test07AutoEnableDisable() {
        log("test07AutoEnableDisable ++");
        mEpoMgr.enableAutoDownload(true);
        assertTrue(mEpoMgr.getAutoDownloadStatus());
        mEpoMgr.enableAutoDownload(false);
        assertFalse(mEpoMgr.getAutoDownloadStatus());
        log("test07AutoEnableDisable --");
    }
    public void test08AddListener() {
        log("test08AddListener ++");
        try {
            mEpoMgr.addStatusListener(null);
        } catch (IllegalArgumentException e) {
        }
        mEpoMgr.addStatusListener(mEpoStatusListener);
        log("test08AddListener --");
    }
    public void test09RemoveListener() {
        log("test09RemoveListener ++");
        try {
            mEpoMgr.removeStatusListener(null);
        } catch (IllegalArgumentException e) {
        }
        mEpoMgr.removeStatusListener(mEpoStatusListener);
        log("test09RemoveListener --");
    }
    public void test10StartEpoDownload() {
        log("test10StartEpoDownload++");
        mEpoMgr.enable();
        mEpoMgr.startDownload();
        log("test10StartEpoDownload --");
    }
    public void test11StopEpoDownload() {
        log("test11StopEpoDownload ++");
        mEpoMgr.stopDownload();
        log("test11StopEpoDownload --");
    }
    public void test12GetProgress() {
        log("test12GetProgress ++");
        mEpoMgr.getProgress();
        log("test12GetProgress --");
    }
    public void test13GetEpoFileInfo() {
        log("test13GetEpoFileInfo ++");
        mEpoMgr.getEpoFileInfo();
        log("test13GetEpoFileInfo --");
    }
    public void test14SetUpdatePeriod10() {
        log("test14SetUpdatePeriod10 ++");
        long backup = mEpoMgr.getUpdatePeriod();
        mEpoMgr.setUpdatePeriod(10);
        assertEquals(10, mEpoMgr.getUpdatePeriod());
        mEpoMgr.setUpdatePeriod(backup);
        log("test14SetUpdatePeriod10 --");
    }
    public void test15SetUpdatePeriod123456() {
        log("test15SetUpdatePeriod123456 ++");
        long backup = mEpoMgr.getUpdatePeriod();
        mEpoMgr.setUpdatePeriod(123456);
        assertEquals(123456, mEpoMgr.getUpdatePeriod());
        mEpoMgr.setUpdatePeriod(backup);
        log("test15SetUpdatePeriod123456 --");
    }
    public void test16ExtraUsingXml() {
        log("test16ExtraUsingXml ++");
        mEpoMgr.extraCommand("USING_XML", null);
        log("test16ExtraUsingXml --");
    }
    public void test17ExtraUndefined() {
        log("test17ExtraUndefined ++");
        mEpoMgr.extraCommand("UNDEFINED", null);
        log("test17ExtraUndefined --");
    }

    public void test18SetProfile() {
        log("test18SetProfile ++");
        mEpoMgr.setProfile("epo.mediatek.com" , 21, "epo_alps", "epo_alps");
        log("test18SetProfile --");
    }

    public void test19SetTimeout() {
        log("test19SetTimeout ++");
        mEpoMgr.setTimeout(10*1000);
        log("test19SetTimeout --");
    }

    public void test20SetRetryTimes() {
        log("test20SetRetryTimes ++");
        mEpoMgr.setRetryTimes(5);
        log("test20SetRetryTimes --");
    }

    public void test21ExtraCommand() {
        log("test21ExtraCommand ++");
        Bundle extra = new Bundle();
        extra.putInt("TYPE", 0);
        mEpoMgr.extraCommand("EXTRA", extra);
        log("test21ExtraCommand --");
    }
    
    public void test18Combination() {
        log("test18Combination ++");
        mEpoMgr.extraCommand("RESET_COVERAGE_DATA", null);
        test02Disable();
        test06AutoDisable();
        
        test14SetUpdatePeriod10();
        test15SetUpdatePeriod123456();
        
        test01Enable();
        test05AutoEnable();
        test08AddListener();
        test10StartEpoDownload();
        test12GetProgress();
        test13GetEpoFileInfo();

        test11StopEpoDownload();
        test09RemoveListener();
        test02Disable();
        test06AutoDisable();

        test16ExtraUsingXml();
        test17ExtraUndefined();

        test18SetProfile();
        test19SetTimeout();
        test20SetRetryTimes();
        test21ExtraCommand();
        
        mEpoMgr.extraCommand("DUMP_COVERAGE_DATA", null);
        log("test18Combination --");
    }

    /* Template
    public void test() {
        log("test ++");
        
        log("test --");
    }
    */


    private MtkEpoStatusListener mEpoStatusListener = new MtkEpoStatusListener() {
        public void onStatusChanged(int status) {
        }
    };


    private void log(String msg) {
        Log.d(TAG, msg);
    }
    
    private void msleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
        }
    }
    
}
