package com.mediatek.appwidget.worldclock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.appwidget.worldclock.*;

public class SelectedCityListAdapter extends BaseAdapter {
    private static final String TAG = "MTKWORLDCHOOSE";
    private ArrayList<HashMap<String, Object>> data;
    private Context mContext;
    private LayoutInflater mFlater;
    private int mWidgetId;

    public SelectedCityListAdapter(Context ctx,
            ArrayList<HashMap<String, Object>> data, int id) {
        // TODO Auto-generated constructor stub
        this.data = data;
        this.mContext = ctx;
        this.mWidgetId = id;
        mFlater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data == null ? 0 : data.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int pos, View view, ViewGroup arg2) {
        // TODO Auto-generated method stub
        TextView cityNameView = null, weatherIdView = null;
        RadioButton currentCityButton = null;
        ViewHolder viewHolder = null;
        if (view == null) {
            viewHolder = new ViewHolder();
            view = mFlater.inflate(R.layout.chooselistview, null);
            viewHolder.cityNameView = (TextView) view
                    .findViewById(R.id.chooseactivitytextviewid);
            viewHolder.weatherIdView = (TextView) view
                    .findViewById(R.id.chooselistviewweatherid);
            viewHolder.currentCityButton = (RadioButton) view
                    .findViewById(R.id.current_city);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        HashMap<String, Object> item = data.get(pos);
        String cityName = (String) item.get("chooselistviewtextview");
        String weatherId = (String) item.get("chooselistviewweatherid");
        String currentCity = ClockCityUtils.getCityName(mContext, mWidgetId);
        Log.v(TAG, "currentCity = " + currentCity + "  cityname=" + cityName);
        viewHolder.cityNameView.setText(cityName);
        viewHolder.weatherIdView.setText(weatherId);
        if(currentCity != null && cityName != null){
            viewHolder.currentCityButton.setChecked(currentCity.split(",")[0].equals(cityName.split(",")[0]));
        }
        return view;
    }

    private class ViewHolder {
        TextView cityNameView;
        TextView weatherIdView;
        RadioButton currentCityButton;
    }
}