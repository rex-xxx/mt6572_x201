
package com.mediatek.lockscreensettings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BounceCoverFlow;
import android.widget.Button;
import android.widget.ImageView; 
import android.widget.TextView;

import com.android.settings.R;
import com.mediatek.common.policy.IKeyguardLayer;
import com.mediatek.common.policy.KeyguardLayerInfo;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class LockScreenStyleSettings extends Activity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    public static final String CURRENT_KEYGURAD_LAYER_KEY = "mtk_current_keyguard_layer";
    
    private static final String TAG = "LockScreenStyleSettings";
    private static final boolean DBG = true; 
    
    private static final String CONTACT_STRING = "&";
    private static final int REQUEST_CONFIG_KEYGUARD_LAYER = 1;
    private static final int MAX_ZOOM_OUT = 120;

    /**
     * The key guard layer position current used by user.
     */
    private int mCurrentLayerIndex; 

    /**
     * The position of current selected in cover flow.
     */
    private int mSelectedPosition;

    private int mPreviewWidth;
    private int mPreviewHeight;
    private String mCurrentSuffix;

    private PackageManager mPackageManager;
    private ContentResolver mResolver;

    private TextView mCurrentLayerName;
    private TextView mCurrentLayerDesc;
    private Button mBtnApply;
    private BounceCoverFlow mCoverFlow;
    
    private KeyguardLayerAdapter mKeyguardLayerAdapter;
    private KeyguardLayerInfo mDefaultKeyguardLayer;

    /**
     * ArrayList to store all of the Scene datas and bitmap images.
     */
    private ArrayList<KeyguardLayerInfo> mKeyguardLayers = new ArrayList<KeyguardLayerInfo>();

    private final BroadcastReceiver mPackageActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                final String packageName = intent.getData().getSchemeSpecificPart();
                final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                if (DBG) {
                    Xlog.d(TAG, "onReceive: action = " + action + ",packageName = " + packageName + ",replacing = "
                            + replacing);
                }
                // If there is key guard layer plug-in removed, reload key guard
                // layers since there is only a few plug-ins. Or we can just
                // delete the specify one in data.
                if (isKeyguardLayerProvider(packageName)) {
                    loadKeyguardLayers();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.lockscreen_style_settings);

        mPackageManager = getPackageManager();
        mResolver = getContentResolver();

        mPreviewWidth = (int) getResources().getDimension(R.dimen.lockscreen_preview_display_width);
        mPreviewHeight = (int) getResources().getDimension(R.dimen.lockscreen_preview_display_height);
        mCurrentSuffix = getResources().getString(R.string.current_suffix);
        if (DBG) {
            Xlog.d(TAG, "onCreate: mPreviewWidth = " + mPreviewWidth + ",mPreviewHeight = " + mPreviewHeight
                    + ",mCurrentSuffix = " + mCurrentSuffix);
        }

        initViews();
        loadKeyguardLayers();

        // Register package change broadcast receiver to deal with the situation
        // if the changed package is a key guard layer provider.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);

        // TODO: whether we need to monitor package added broadcast or just make
        // it simple to query all package to get key guard layer providers.
        // filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        registerReceiver(mPackageActionReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (mKeyguardLayerAdapter != null) {
            mKeyguardLayerAdapter.clearBitmapCache();
            mKeyguardLayerAdapter = null;
        }

        unregisterReceiver(mPackageActionReceiver);
        super.onDestroy();
    }

    private void initViews() {
        mCurrentLayerName = (TextView) findViewById(R.id.lockscreen_name);
        mCurrentLayerDesc = (TextView) findViewById(R.id.lockscreen_desc);
        mBtnApply = (Button) findViewById(R.id.set_lock_screen);
        mBtnApply.setOnClickListener(this);

        mCoverFlow = (BounceCoverFlow) findViewById(R.id.gallery);
        mCoverFlow.setCallbackDuringFling(false);
        mCoverFlow.setMaxZoomOut(MAX_ZOOM_OUT);
        mCoverFlow.setSelection(mCurrentLayerIndex);
        mCoverFlow.setOnItemSelectedListener(this);
    }

    /**
     * Load key guard layer informations from system and set the current layer index.
     */
    private void loadKeyguardLayers() {
        mKeyguardLayers.clear();
        mDefaultKeyguardLayer = generateDefaultLayer();
        mKeyguardLayers.add(mDefaultKeyguardLayer);
        addPluginKeyguardLayers();

        final int curLayer = getCurrentLayerIndex();
        // Not find the previous set layer, set default as the current.
        if (curLayer < 0) {
            saveCurrentLayer(0);
        } else {
            mCurrentLayerIndex = curLayer;
        }

        if (mKeyguardLayerAdapter != null) {
            mKeyguardLayerAdapter.clearBitmapCache();
        }

        mKeyguardLayerAdapter = new KeyguardLayerAdapter(this, mKeyguardLayers);
        mKeyguardLayerAdapter.setImageDispSize(mPreviewWidth, mPreviewHeight);
        mKeyguardLayerAdapter.setIndicatorBitmapResource(R.drawable.ic_current_stamp);
        mKeyguardLayerAdapter.setChosenItem(mCurrentLayerIndex);
        mCoverFlow.setAdapter(mKeyguardLayerAdapter);
        mCoverFlow.setSelection(mCurrentLayerIndex);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mBtnApply.getId()) {
            setLockScreen(mSelectedPosition);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (DBG) {
            Xlog.d(TAG, "onItemSelected: position = " + position + ",mSelectedPosition = " + mSelectedPosition
                    + ",mCurrentSuffix = " + mCurrentSuffix);
        }
        mSelectedPosition = position;
        final KeyguardLayerInfo layer = mKeyguardLayers.get(position);
        if (mSelectedPosition == mCurrentLayerIndex) {
            mCurrentLayerName.setText(getStringFromPackage(layer.layerPackage, layer.nameResId) + mCurrentSuffix);
        } else {
            mCurrentLayerName.setText(getStringFromPackage(layer.layerPackage, layer.nameResId));
        }
        mCurrentLayerDesc.setText(getStringFromPackage(layer.layerPackage, layer.descResId));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Xlog.d(TAG, "onActivityResult: requestCode = " + requestCode + ",resultCode = " + resultCode + ",data = " + data
                + ",mSelectedPosition = " + mSelectedPosition);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CONFIG_KEYGUARD_LAYER) {
            saveCurrentLayer(mSelectedPosition);
            finish();
        }
    }

    /**
     * Start an configure activity or set the current selected item as the key guard layer.
     */
    private void setLockScreen(final int index) {
        final Intent configIntent = mKeyguardLayers.get(index).configIntent;
        if (configIntent != null) {
            startActivityForResult(configIntent, REQUEST_CONFIG_KEYGUARD_LAYER);
        } else {
            saveCurrentLayer(index);
            finish();
        }
    }

    /**
     * Save current layer information to system settings provider.
     * 
     * @param index
     */
    private void saveCurrentLayer(final int index) {
        if (DBG) {
            Xlog.d(TAG, "saveCurrentLayer: mCurrentLayerIndex = " + mCurrentLayerIndex + ",index = " + index);
        }
        if (mCurrentLayerIndex != index) {
            mCurrentLayerIndex = index;
            final String combinedStr = generateCombinedIdentString(index);
            android.provider.Settings.System.putString(mResolver, CURRENT_KEYGURAD_LAYER_KEY, combinedStr);
        }
    }

    /**
     * Get current key guard layer information from system settings provider, find it in layer list and set the index as the
     * current layer position.
     * 
     * @return
     */
    private int getCurrentLayerIndex() {
        final String combinedLayerStr = android.provider.Settings.System.getString(mResolver, CURRENT_KEYGURAD_LAYER_KEY);
        if (DBG) {
            Xlog.d(TAG, "getCurrentLayer: combinedLayerStr = " + combinedLayerStr + ",mCurrentLayerPosition = "
                    + mCurrentLayerIndex);
        }

        int ret = -1;
        if (combinedLayerStr != null) {
            final StringTokenizer tokenizer = new StringTokenizer(combinedLayerStr, CONTACT_STRING);
            final String packageName = tokenizer.nextToken();
            if (packageName != null) {
                for (int i = 0, sz = mKeyguardLayers.size(); i < sz; i++) {
                    if (DBG) {
                        Xlog.d(TAG, "getCurrentLayer: i = " + i + ", packageName = " + packageName + ",layerPackage = "
                                + mKeyguardLayers.get(i).layerPackage);
                    }
                    if (packageName.equals(mKeyguardLayers.get(i).layerPackage)) {
                        ret = i;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Generate an identity string which combines package name and resource id of key guard layer name string.
     * 
     * @param pos
     * @return
     */
    private String generateCombinedIdentString(final int pos) {
        return mKeyguardLayers.get(pos).layerPackage + CONTACT_STRING + String.valueOf(mKeyguardLayers.get(pos).nameResId);
    }

    /**
     * Get Bitmap resource from the given package.
     * 
     * @param pkgName
     * @param resId
     * @return
     */
    private Bitmap getBitmapFromPackage(final String pkgName, final int resId) {
        Resources res = null;
        try {
            res = mPackageManager.getResourcesForApplication(pkgName);
        } catch (PackageManager.NameNotFoundException e) {
            Xlog.w(TAG, "getBitmapFromPackage can not find pakcage: " + pkgName);
        }

        Bitmap ret = null;
        if (res != null) {
            ret = BitmapFactory.decodeResource(res, resId);
        }
        return ret;
    }

    /**
     * Get string resource from the given package.
     * 
     * @param pkgName
     * @param resId
     * @return
     */
    private String getStringFromPackage(final String pkgName, final int resId) {
        Resources res = null;
        try {
            res = mPackageManager.getResourcesForApplication(pkgName);
        } catch (PackageManager.NameNotFoundException e) {
            Xlog.w(TAG, "getStringFromPackage can not find pakcage: " + pkgName);
        }

        String ret = null;
        if (res != null) {
            ret = res.getString(resId);
        }
        return ret;
    }

    /**
     * Whether the package is a key guard layer provider.
     * 
     * @param pkgName
     * @return
     */
    private boolean isKeyguardLayerProvider(final String pkgName) {
        for (KeyguardLayerInfo layerInfo : mKeyguardLayers) {
            if (pkgName.equals(layerInfo.layerPackage)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate default key guard layer.
     * 
     * @return
     */
    private KeyguardLayerInfo generateDefaultLayer() {
        final KeyguardLayerInfo layerInfo = new KeyguardLayerInfo();
        layerInfo.layerPackage = getPackageName();
        layerInfo.nameResId = R.string.default_name;
        layerInfo.descResId = R.string.lockscreen_style_default_desc;
        layerInfo.previewResId = R.drawable.lockscreen_default_preview;
        return layerInfo;
    }

    /**
     * Get key guard layers from system, a key guard layer should implement IKeyguardLayer interface. Plugin app should make
     * sure the data is valid.
     */
    private void addPluginKeyguardLayers() {
        try {
            final PluginManager plugManager = PluginManager.<IKeyguardLayer> create(this, IKeyguardLayer.class.getName());
            final int pluginCount = plugManager.getPluginCount();
            if (DBG) {
                Xlog.d(TAG, "getKeyguardLayers: pluginCount = " + pluginCount);
            }
            if (pluginCount != 0) {
                Plugin<IKeyguardLayer> plugin;
                IKeyguardLayer keyguardLayer;
                for (int i = 0; i < pluginCount; i++) {
                    plugin = plugManager.getPlugin(i);
                    keyguardLayer = (IKeyguardLayer) plugin.createObject();
                    final KeyguardLayerInfo info = keyguardLayer.getKeyguardLayerInfo();
                    if (DBG) {
                        Xlog.d(TAG, "getKeyguardLayers: i = " + i + ",keyguardLayer = " + keyguardLayer + ",info = " + info);
                    }
                    if (info != null) {
                        mKeyguardLayers.add(info);
                    }
                }
            }
        } catch (Exception e) {
            Xlog.e(TAG, "getPluginKeyguardLayers exception happens: e = " + e.getMessage());
        }
    }

    private class KeyguardLayerAdapter extends ReflectionDecorAdapter {
        private LayoutInflater mLayoutInflater;
        private ArrayList<KeyguardLayerInfo> mLayerList;

        KeyguardLayerAdapter(Context context, ArrayList<KeyguardLayerInfo> list) {
            super(context);
            mLayoutInflater = LayoutInflater.from(context);
            mLayerList = list;
        }

        @Override
        public int getCount() {
            return mLayerList.size();
        }

        @Override
        public Object getItem(int position) {
            return mLayerList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getViewInner(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mLayoutInflater.inflate(R.layout.lockscreen_style_item, parent, false);
                holder.mPreviewView = (ImageView) convertView.findViewById(R.id.lockscreen_preview_image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            return convertView;
        }

        @Override
        public int getContainerId() {
            return R.id.lockscreen_preview_image;
        }

        @Override
        public Bitmap getContentAt(final int position) {
            return getBitmapFromPackage(mLayerList.get(position).layerPackage, mLayerList.get(position).previewResId);
        }
    }

    private static class ViewHolder {
        ImageView mPreviewView;
    }
}
