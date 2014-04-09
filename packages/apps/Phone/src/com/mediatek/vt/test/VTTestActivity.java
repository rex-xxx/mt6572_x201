package com.mediatek.vt.test;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.graphics.PixelFormat;
import android.view.Window;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.phone.R;

public class VTTestActivity extends Activity implements SurfaceHolder.Callback {

	private Camera mCamera;
	private static final String LOG_TAG = "VTTestActivity";
	private SurfaceView mPeerSurfaceView;
	private SurfaceView mLocalSurfaceView;

	void log(String msg) {
		Log.d(LOG_TAG, msg);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vt_test);
		log("VTTestActivity onCreate()");

		mPeerSurfaceView = (SurfaceView) findViewById(R.id.VTEMHighVideo);
		mLocalSurfaceView = (SurfaceView) findViewById(R.id.VTEMLowVideo);
		// mSurfaceHolder.addCallback(this);
		// mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);

	}

	@Override
	public void onDestroy() {
		log("CameraActivity onDestroy()");
		super.onDestroy();
	}

	public Camera getCamera() {
		return mCamera;
	}

	public SurfaceHolder getPeerSurfaceHolder() {
		log("getPeerSurfaceHolder()");
		return mPeerSurfaceView.getHolder();
	}

	public SurfaceHolder getLocalSurfaceHolder() {
		log("getLocalSurfaceHolder()");
		return mLocalSurfaceView.getHolder();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		try {
			Parameters p = mCamera.getParameters();
			p.setPictureSize(width, height);
			mCamera.setParameters(p);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (Exception e) {

		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera = Camera.open();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}

}
