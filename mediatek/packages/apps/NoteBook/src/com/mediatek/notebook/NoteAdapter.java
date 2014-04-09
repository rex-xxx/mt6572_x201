package com.mediatek.notebook;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;

import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.notebook.NotePad.Notes;

import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends BaseAdapter {

	private static final String TAG = "NoteAdapter";
	private Context mContext;
	public Cursor cur;
	public List<NoteItem> list = new ArrayList<NoteItem>();
	private Resources resource;
	private ColorStateList colorWork;
	private ColorStateList colorPersonal;
	private ColorStateList colorFamily;
	private ColorStateList colorStudy;
	private String groupWork;
	private String groupPersonal;
	private String groupFamily;
	private String groupStudy;

	public class NoteItem {
		public int id;
		public String note;
		public String create_time;
		public boolean isselect;
		public String notegroup;
		public String modify_time;
	}

	public NoteAdapter(NotesList context, Cursor cursor, int token) {
		mContext = context;
		cur = cursor;
		setDataOfCurrentActivity();
	}

	private void setDataOfCurrentActivity() {
		resource = (Resources) mContext.getResources();
		colorWork = (ColorStateList) resource.getColorStateList(R.color.work);
		colorPersonal = (ColorStateList) resource
				.getColorStateList(R.color.personal);
		colorFamily = (ColorStateList) resource
				.getColorStateList(R.color.family);
		colorStudy = (ColorStateList) resource.getColorStateList(R.color.study);
		groupWork = (String) resource.getString(R.string.menu_work);
		groupPersonal = (String) resource.getString(R.string.menu_personal);
		groupFamily = (String) resource.getString(R.string.menu_family);
		groupStudy = (String) resource.getString(R.string.menu_study);
	}

	public int getCount() {
		return cur.getCount();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public static class ViewClass {
		TextView title;
		TextView createTime;
		TextView groupColor;
		TextView notegroup;
		LinearLayout noteItemHole;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewClass view;
		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(
					R.layout.noteslist_item_context, null);
			view = new ViewClass();
			view.title = (TextView) convertView.findViewById(R.id.title);
			view.createTime = (TextView) convertView
					.findViewById(R.id.create_time);
			view.groupColor = (TextView) convertView
					.findViewById(R.id.groupcolor);
			view.notegroup = (TextView) convertView.findViewById(R.id.group);
			view.noteItemHole = (LinearLayout) convertView.findViewById(R.id.linearLayout1);
			convertView.setTag(view);
		} else {
			view = (ViewClass) convertView.getTag();
		}

		NoteItem item = list.get(position);
		
		if (item.isselect == true) {
			view.noteItemHole.setBackgroundResource(R.color.select);
		} else {
			view.noteItemHole.setBackgroundResource(R.color.unselect);
		}
		view.title.setText(item.note);
		view.createTime.setText(item.create_time);
		if (item.notegroup.equals(groupPersonal)) {
			view.notegroup.setTextColor(colorPersonal);
			view.groupColor.setBackgroundResource(R.color.personal);
		} else if (item.notegroup.equals(groupWork)) {
			view.notegroup.setTextColor(colorWork);
			view.groupColor.setBackgroundResource(R.color.work);
		} else if (item.notegroup.equals(groupFamily)) {
			view.notegroup.setTextColor(colorFamily);
			view.groupColor.setBackgroundResource(R.color.family);
		} else if (item.notegroup.equals(groupStudy)) {
			view.notegroup.setTextColor(colorStudy);
			view.groupColor.setBackgroundResource(R.color.study);
		} else {
			view.groupColor.setBackgroundResource(R.color.none);
		}
		view.notegroup.setText(item.notegroup);
		return convertView;
	}

	public void checkboxClickAction(int position) {
         NoteItem item = list.get(position);
         item.isselect = !item.isselect;
         Notes.sDeleteNum = selectedNumber();
         this.notifyDataSetChanged();      
     }

	public void addList(NoteItem item) {
		list.add(item);
	}
	
	public String getFilter() {
		selectedNumber();
		StringBuilder filter = new StringBuilder("_id in ");
		filter.append("(");
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isselect) {
				filter.append("\'");
				filter.append(String.valueOf(list.get(i).id));
				filter.append("\'");
			    filter.append(",");
			}
		}
		filter.deleteCharAt(filter.length() - 1);
		filter.append(")");
		return String.valueOf(filter);
	}

	public void selectAllOrNoCheckbox(boolean userSelect) {			
		for (int i = 0; i < list.size(); i++) {
			list.get(i).isselect = userSelect;			
		}
		this.notifyDataSetChanged();
		Notes.sDeleteNum = selectedNumber();
	}

	public int selectedNumber() {
		int count = 0;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).isselect) {
				count++;
			}
		}
		return count;
	}
	
}