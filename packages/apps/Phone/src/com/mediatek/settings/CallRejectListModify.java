package com.mediatek.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;  
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.android.phone.R;

import java.util.ArrayList;

public class CallRejectListModify extends Activity implements CallRejectListAdapter.CheckSelectCallBack {
    private static final String TAG = "CallRejectListModify";

    private static final Uri URI = Uri.parse("content://reject/list");
    private static final int ID_INDEX = 0;
    private static final int NUMBER_INDEX = 1;
    private static final int TYPE_INDEX = 2;
    private static final int NAME_INDEX = 3;

    private static final int CALL_LIST_DIALOG_WAIT = 2;

    private ListView mListView;
    private CallRejectListAdapter mCallRejectListAdapter;

    private String mType;

    private static final int MENU_ID_SELECT_ALL = Menu.FIRST;
    private static final int MENU_ID_UNSELECT_ALL = Menu.FIRST + 1;
    private static final int MENU_ID_TRUSH = Menu.FIRST + 2;

    private ArrayList<CallRejectListItem> mCRLItemArray = new ArrayList<CallRejectListItem>();
    private AddContactsTask mAddContactsTask = null;            

    class AddContactsTask extends AsyncTask<Integer, String, Integer> {  
        @Override  
        protected void onPreExecute() {  
            super.onPreExecute();  
        }  
          
        @Override  
        protected Integer doInBackground(Integer... params) {  
            int index = params[0];
            int size = params[1];
            while ((index < size) && !isCancelled()) {
                CallRejectListItem callrejectitem = mCRLItemArray.get(index);
                if (callrejectitem.getIsChecked()) {
                    String id = callrejectitem.getId();
                    mCRLItemArray.remove(index);
                    if (isCurTypeVtAndVoice(id)) {
                        updateRowById(id);
                    } else {
                        deleteRowById(id);
                    }
                    size--;
                } else {
                    index++;    
                }
            }
            return size;  
        }  
  
        @Override  
        protected void onProgressUpdate(String... id) {  
            super.onProgressUpdate(id);  
        }  
  
        @Override  
        protected void onPostExecute(Integer size) {  
            if (!this.isCancelled()) {
                dismissDialog(CALL_LIST_DIALOG_WAIT);
                mListView.invalidateViews();
                if (size == 0) {
                    CallRejectListModify.this.finish();
                }
                updateSelectedItemsView(getString(R.string.selected_item_count, 0));
            }
            super.onPostExecute(size);  
        }  
    }  

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_reject_list_modify);

        mType = getIntent().getStringExtra("type");

        getCallRejectListItems();
        mListView = (ListView)findViewById(android.R.id.list);
        mCallRejectListAdapter = new CallRejectListAdapter(this, mCRLItemArray);
        if (mListView != null) {
            mListView.setAdapter(mCallRejectListAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    CheckBox checkboxView = (CheckBox)view.findViewById(R.id.call_reject_contact_check_btn);
                    if (checkboxView != null) {
                        checkboxView.setChecked(!checkboxView.isChecked());
                    }
                }    
            });
        }

        mCallRejectListAdapter.setCheckSelectCallBack(this);
        mType = getIntent().getStringExtra("type");
        configureActionBar();
        updateSelectedItemsView(getString(R.string.selected_item_count, 0));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == CALL_LIST_DIALOG_WAIT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.call_reject_please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_SELECT_ALL, 0, R.string.select_all)
                .setIcon(R.drawable.ic_menu_contact_select_all)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_UNSELECT_ALL, 0, R.string.select_clear)
                .setIcon(R.drawable.ic_menu_contact_clear_select)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_TRUSH, 0, R.string.select_trash)
                .setIcon(R.drawable.ic_menu_contact_trash)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ID_SELECT_ALL:
            selectAll();
            break;
        case MENU_ID_UNSELECT_ALL:
            unSelectAll();
            break;
        case MENU_ID_TRUSH:
            deleteSelection();
            break;
        case android.R.id.home:
            finish();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();    
        if (mAddContactsTask != null) {
            mAddContactsTask.cancel(true);
        }
    }

    private void getCallRejectListItems() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(URI, new String[] {
                "_id", "Number", "type", "Name"}, null, null, null);

            if (cursor == null) {
                return;
            }
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {
                String id = cursor.getString(ID_INDEX);
                String numberDB = cursor.getString(NUMBER_INDEX);
                String type = cursor.getString(TYPE_INDEX);
                String name = cursor.getString(NAME_INDEX);
                Log.d(TAG, "id=" + id);
                Log.d(TAG, "numberDB=" + numberDB);
                Log.d(TAG, "type=" + type);
                Log.d(TAG, "name=" + name);
                if ("3".equals(type)
                    || ("2".equals(type) && "video".equals(mType))
                    || ("1".equals(type) && "voice".equals(mType))) {
                    CallRejectListItem crli = new CallRejectListItem(name, numberDB, id, false);
                    mCRLItemArray.add(crli);
                }
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
    }
    
    private void selectAll() {
        for (CallRejectListItem callrejectitem : mCRLItemArray) {
            callrejectitem.setIsChecked(true);    
        }
        updateSelectedItemsView(getString(R.string.selected_item_count, mCRLItemArray.size()));
        mListView.invalidateViews();
    }

    private void unSelectAll() {
        for (CallRejectListItem callrejectitem : mCRLItemArray) {
            callrejectitem.setIsChecked(false);    
        }
        updateSelectedItemsView(getString(R.string.selected_item_count, 0));
        mListView.invalidateViews();
    }

    private void deleteSelection() {
        Log.i(TAG, "Enter deleteSecection Function");
        boolean isSelected = false;
        for (CallRejectListItem callrejectitem : mCRLItemArray) {
            isSelected |= callrejectitem.getIsChecked();
        }
        if (isSelected) {
            showDialog(CALL_LIST_DIALOG_WAIT);
            mAddContactsTask  = new AddContactsTask();
            mAddContactsTask.execute(0, mCRLItemArray.size());
        }
    }

    private void updateRowById(String id) {
        ContentValues contentValues = new ContentValues();
        if ("video".equals(mType)) {
            contentValues.put("Type", "1");
        } else {
            contentValues.put("Type", "2");
        }

        try {
            Uri existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
            int result = getContentResolver().update(existNumberURI, contentValues, null, null);
            Log.i(TAG, "result is " + result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the index is " + id);
        }
    }

    private void deleteRowById(String id) {
        try {
            Uri existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
            Log.i(TAG, "existNumberURI is " + existNumberURI);
            int result = getContentResolver().delete(existNumberURI, null, null);
            Log.i(TAG, "result is " + result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the index is " + id);
        }
    }

    private boolean isCurTypeVtAndVoice(String id) {
        Uri existNumberURI = null;
        try {
            existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
        } catch (NumberFormatException e) {
                Log.e(TAG, "parseInt failed, the index is " + id);
        }

        Cursor cursor = getContentResolver().query(existNumberURI, new String[] {
                "_id", "Number", "Type", "Name"}, null, null, null);
        if (cursor == null) {
            return false;
        }
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            String type = cursor.getString(TYPE_INDEX);
            if ("3".equals(type)) {
                cursor.close();
                return true;
            }
            cursor.moveToNext();
        }
        cursor.close(); 
        return false;
    }

    private void updateSelectedItemsView(String checkedItemsCount) {
        TextView selectedItemsView = (TextView) getActionBar().getCustomView().findViewById(R.id.select_items);
        if (selectedItemsView == null) {
            return;
        }
        selectedItemsView.setText(checkedItemsCount);
    }

    private void configureActionBar() {
        Log.v(TAG, "configureActionBar()");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(R.layout.call_reject_list_modify_action_bar, null);
        ImageButton doneMenuItem = (ImageButton) customActionBarView.findViewById(R.id.done_menu_item);
        doneMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_SHOW_TITLE);
                actionBar.setCustomView(customActionBarView);
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void setChecked(boolean isChecked) {
        int count = 0;
        for (CallRejectListItem callrejectitem : mCRLItemArray) {
            if (callrejectitem.getIsChecked()) {
                count++;    
            }
        }
        updateSelectedItemsView(getString(R.string.selected_item_count, count));
    }
}
