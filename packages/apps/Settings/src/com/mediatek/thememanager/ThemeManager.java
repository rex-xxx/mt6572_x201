package com.mediatek.thememanager;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

/**
 * List all themes in system, and user can choose one of them.
 */
public class ThemeManager extends SettingsPreferenceFragment implements
        OnItemClickListener,DialogCreatable {
    private static final String TAG = "ThemeManager";
    
    static final String DEFAULT_THEME_PACKAGE_NAME = "android";

    private ArrayList<ThemeData> mThemeDatas = new ArrayList<ThemeData>();

    /**
     * The position of current used theme.
     */
    private int mCurrentPosition; 

    /**
     * The number of theme in the system.
     */
    private int mThemeCount;

    /**
     * The PreferenceFragment's ListView
     */
    private ListView mThemeList; 
    private LayoutInflater mInflater;
    private Object mLock = new Object();
    private ProgressDialog mProgressDialog;
    private StatusBarManager mStatusBarManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Xlog.d(TAG, "onCreate");
        mInflater = (LayoutInflater) this.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        loadThemesFromDatabase();
    }

    @Override
    public void onStart() {
        super.onStart();
        mThemeList = getListView();
        mThemeList.setAdapter(new ThemeAdapter(this.getActivity(), mThemeDatas));
        mThemeList.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
        if (mCurrentPosition != position) {
            new SetThemeTask(this.getActivity()).execute(position);
            mCurrentPosition = position;
        } else {
            mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
        }
    }

    /**
     * Get all theme informations from database.
     */
    private void loadThemesFromDatabase() {
        int position = 0;
        int columnIndex = 0;

        if (null != mThemeDatas) {
            mThemeDatas.clear();
        }

        Configuration config = getResources().getConfiguration();
        Cursor cursor = this.getActivity().getContentResolver().query(
                Themes.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Xlog.w(TAG, "There is no theme in database.");
            return;
        }

        try {
            String packageName = null;
            String themePath = null;
            int themeNameId = 0;
            String themeName = null;
            for (cursor.moveToFirst(); (!cursor.isAfterLast()); cursor.moveToNext()) {
                columnIndex = cursor.getColumnIndex(Themes.PACKAGE_NAME);
                packageName = cursor.getString(columnIndex);

                columnIndex = cursor.getColumnIndex(Themes.THEME_PATH);
                themePath = cursor.getString(columnIndex);

                columnIndex = cursor.getColumnIndex(Themes.THEME_NAME_ID);
                themeNameId = cursor.getInt(columnIndex);

                if (config != null && config.skin.equals(themePath)) {
                    mCurrentPosition = position;
                }
            
                if (packageName.equals(DEFAULT_THEME_PACKAGE_NAME)) {
                    themeName = getResources().getString(R.string.default_name);
                } else {
                    themeName = getThemeName(this.getActivity(), packageName,
                            themeNameId);
                }

                if (themeName != null) {
                    ThemeData themeData = new ThemeData();
                    themeData.setPackageName(packageName);
                    themeData.setThemePath(themePath);
                    themeData.setThemeName(themeName);
                    mThemeDatas.add(position++, themeData);

                    Xlog.d(TAG, "position = " + position + " ThemePath = "
                            + themePath + " ThemeNameId = " + themeNameId);
                } else {
                    this.getActivity().getContentResolver().delete(
                            Themes.CONTENT_URI, Themes.PACKAGE_NAME + " = ?",
                            new String[] { packageName });
                    Xlog.d(TAG, "delete record whose package name is = "
                            + packageName);
                }
            }
            mThemeCount = position;
        } finally {
            cursor.close();
        }
        
    }

    private void showSetThemeDialog() {
        Xlog.d(TAG, "showSetThemeDialog()");
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this.getActivity(), null,
                    getString(R.string.loading), true, false);
        } else {
            Xlog.d(TAG, "showSetThemeDialog mProgressDialog != null.");
        }
    }

    private void finishSetThemeDialog() {
        Xlog.d(TAG, "finishSetThemeDialog()");
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        Xlog.d(TAG,"onDestroy");
        finishSetThemeDialog();
        super.onDestroy();
    }

     /**
      * Get theme name from theme resource package.
      * If it hasn't specify theme name in manifest, will get the last part of 
      * it's package name as theme name.
      * 
      * @param context Context to get package manager.
      * @param packageName The theme resource package name.
      * @param resId The resource id of theme name.
      * 
      * @return Theme package name.
      */
    static String getThemeName(Context context, String packageName, int resId) {
        PackageManager pm = context.getPackageManager();
        String themeName = null;
        try {
            //log theme name id and text
            Resources r = pm.getResourcesForApplication(packageName);
            themeName = String.valueOf(r.getText(resId));
            Xlog.d(TAG, "get theme name " + themeName + " from id " + Integer.toHexString(resId));
        } catch (NameNotFoundException e) {
            Xlog.d("TAG", "PackageManager cann't find resources for " + packageName);
        } catch (NotFoundException e) {
            Xlog.d("TAG", "Cann't find string in theme package, use default name.");
            themeName = context.getResources().getString(R.string.custom_theme_name, getPackageNameLastPart(packageName));
        }
        return themeName;
    }
    
    private static String getPackageNameLastPart(String packageName) {
        int index = packageName.lastIndexOf('.');
        String last = String.valueOf(packageName.subSequence(index + 1, packageName.length()));
        return last;
    }
    /**
     * Return current theme summary string.
     * DisplaySettings get theme summary from here.
     */
    public static String getThemeSummary(Context context) {
        String curThemePath = context.getResources().getConfiguration().skin;
        Cursor cursor = context.getContentResolver().query(Themes.CONTENT_URI, 
                new String[] {Themes.PACKAGE_NAME, Themes.THEME_NAME_ID},
                Themes.THEME_PATH + "=?", new String[]{ curThemePath }, null);
        String packageName = null;
        String themeSummary = null;
        int themeNameId;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    packageName = cursor.getString(0);
                    themeNameId = cursor.getInt(1);
                    if (packageName.equals(DEFAULT_THEME_PACKAGE_NAME)) {
                        themeSummary = context.getString(R.string.default_name);
                    } else {
                        themeSummary = getThemeName(context, packageName, themeNameId);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return themeSummary;
    }
    
    private class SetThemeTask extends AsyncTask<Integer, Void, Void> {
        public SetThemeTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Integer... types) {
            int position = types[0];
            IActivityManager am = ActivityManagerNative.getDefault();
            try {
                // The mThemeDatas list will be cleared when onDestroy is called,
                // so null pointer exception would happens when calling mThemeDatas.get(position),
                // synchronized set theme and destroy to fix this issue.
                synchronized (mLock) {
                    if (mThemeDatas == null) {
                        Xlog.e(TAG, "doInBackground error occured, mThemeDatas becomes null.");
                    }
                    Configuration config = am.getConfiguration();
                    config.skin = mThemeDatas.get(position).getThemePath()
                            .toString();
                    Xlog.d(TAG, "doInBackground() am.updateConfiguration() config.skin = "
                            + config.skin);
                    am.updateConfiguration(config);
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "Update configuration for theme changed failed.");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            showSetThemeDialog();
        }

        @Override
        protected void onPostExecute(Void unused) {
            mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
        }

        private Context mContext;
    }

    class ThemeAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<ThemeData> mThemeDatas;

        public ThemeAdapter(Context context, ArrayList<ThemeData> themeDatas) {
            this.mContext = context;
            this.mThemeDatas = themeDatas;
        }

        @Override
        public int getCount() {
            return mThemeDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mThemeDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.theme_item, null);
                holder = new ViewHolder();
                holder.mText = (TextView) convertView
                        .findViewById(R.id.theme_name);
                holder.mIcon = (ImageView) convertView
                        .findViewById(R.id.theme_preview);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ThemeData themeData = mThemeDatas.get(position);
            holder.mText.setText(themeData.getThemeName());
            if ((DEFAULT_THEME_PACKAGE_NAME).equals(themeData.getPackageName())) {
                // default theme
                Bitmap mDefaultPreviewImage = BitmapFactory.decodeStream(
                        getResources().openRawResource(R.drawable.ic_default_theme));
                holder.mIcon.setImageBitmap(mDefaultPreviewImage);
            } else {
                // other themes
                Bitmap previewImage = getResources().getThemePreview(
                        themeData.getThemePath());
                holder.mIcon.setImageBitmap(previewImage);
            }
            return convertView;
        }
    }

    static class ViewHolder {
        TextView mText;
        ImageView mIcon;
    }
}
