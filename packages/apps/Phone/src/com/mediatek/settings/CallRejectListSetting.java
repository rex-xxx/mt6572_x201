package com.mediatek.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;  
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.android.phone.R;

public class CallRejectListSetting extends PreferenceActivity implements 
    Button.OnClickListener, OnItemClickListener {
    private static final String TAG = "CallRejectListSetting";
    private static final int CALL_LIST_DIALOG_EDIT = 0;
    private static final int CALL_LIST_DIALOG_SELECT = 1; 
    private static final int CALL_LIST_DIALOG_WAIT = 2;

    private static final int EVENT_HANDLER_MESSAGE_WAIT = 0;

    private static final int ID_INDEX = 0;
    private static final int NUMBER_INDEX = 1;
    private static final int TYPE_INDEX = 2;
    private static final int NAME_INDEX = 3;

    private static final Uri URI = Uri.parse("content://reject/list");
    private static final Uri CONTACT_URI = Data.CONTENT_URI;
    private static final Uri CALLLOG_URI = Uri.parse("content://call_log/calls");

    private static final String CONTACTS_ADD_ACTION = "android.intent.action.contacts.list.PICKMULTIPHONES";
    private static final String CONTACTS_ADD_ACTION_RESULT = "com.mediatek.contacts.list.pickdataresult";
    private static final String CALL_LOG_SEARCH = "android.intent.action.SEARCH";
    
    private static final int CALL_REJECT_CONTACTS_REQUEST = 125; 
    private static final int CALL_REJECT_LOG_REQUEST = 126; 

    private static final int MENU_ID_DELETE = Menu.FIRST;
    private static final int MENU_ID_ADD = Menu.FIRST + 2;

    private static final String[] CALLER_ID_PROJECTION = new String[] {
            Phone._ID,                      // 0
        Phone.NUMBER,                   // 1
        Phone.LABEL,                    // 2
        Phone.DISPLAY_NAME,             // 3
    };

    private static final int PHONE_ID_COLUMN = 0;
    private static final int PHONE_NUMBER_COLUMN = 1;
    private static final int PHONE_LABEL_COLUMN = 2;
    private static final int CONTACT_NAME_COLUMN = 3;

    public static final String[] CALL_LOG_PROJECTION = new String[] {
        Calls._ID,                       // 0
        Calls.NUMBER,                    // 1
        Calls.DATE,                      // 2
        Calls.DURATION,                  // 3
        Calls.TYPE,                      // 4
        Calls.COUNTRY_ISO,               // 5
        Calls.VOICEMAIL_URI,             // 6
        Calls.GEOCODED_LOCATION,         // 7
        Calls.CACHED_NAME,               // 8
        Calls.CACHED_NUMBER_TYPE,        // 9
        Calls.CACHED_NUMBER_LABEL,       // 10
        Calls.CACHED_LOOKUP_URI,         // 11
        Calls.CACHED_MATCHED_NUMBER,     // 12
        Calls.CACHED_NORMALIZED_NUMBER,  // 13
        Calls.CACHED_PHOTO_ID,           // 14
        Calls.CACHED_FORMATTED_NUMBER,   // 15
        Calls.IS_READ,                   // 16
    };

    public static final int ID = 0;
    public static final int NUMBER = 1;
    public static final int DATE = 2;
    public static final int DURATION = 3;
    public static final int CALL_TYPE = 4;
    public static final int COUNTRY_ISO = 5;
    public static final int VOICEMAIL_URI = 6;
    public static final int GEOCODED_LOCATION = 7;
    public static final int CACHED_NAME = 8;
    public static final int CACHED_NUMBER_TYPE = 9;
    public static final int CACHED_NUMBER_LABEL = 10;
    public static final int CACHED_LOOKUP_URI = 11;
    public static final int CACHED_MATCHED_NUMBER = 12;
    public static final int CACHED_NORMALIZED_NUMBER = 13;
    public static final int CACHED_PHOTO_ID = 14;
    public static final int CACHED_FORMATTED_NUMBER = 15;
    public static final int IS_READ = 16;

    private static final String[] CALLER_DATA_ID_PROJECTION = new String[] {
        Data._ID, // 0
        Data.DATA1,// 1
        Data.DATA2, // 2
        Data.RAW_CONTACT_ID,
    };
    private static final int DATA_ID_COLUMN = 0;
    private static final int DATA_DATA1_COLUMN = 1;
    private static final int DATA_DATA2_COLUMN = 2;
    private static final int DATA_RAW_CONTACT_ID_COLUMN = 3;

    private static final Uri mCallRejectViewCallLogUri = Uri.parse("content://call_log/callsjoindataview");
    public static final String[] CALL_PROJECTION_CALLS_JOIN_DATAVIEW = new String[] {
        Calls._ID,// 0
        Calls.NUMBER,// 1
        Calls.DATA_ID,// 2
    };

    private static final int CALLSVIEW_ID_COLUMN = 0;
    private static final int CALLSVIEW_NUMBER_COLUMN = 1;
    private static final int CALLSVIEW_DATA_ID_COLUMN = 2;

    private ListView mListView;
    private Button mDeleteBtn;
    private Button mAddBtn;
    private Button mDialogSaveBtn;
    private Button mDialogCancelBtn;
    private ImageButton mAddContactsBtn;
    private EditText mNumberEditText;

    private String mType;
    private String mPhoneNumberFromContacts;
    private Intent mResultIntent;

    private AddContactsTask mAddContactsTask = null;
    private UpdateUIContactsTask mUpdatUIContactsTask = null;
    private boolean addFlag = false;

    class AddContactsTask extends AsyncTask<Integer, Integer, String> {  

        @Override  
        protected void onPreExecute() {  
            showDialog(CALL_LIST_DIALOG_WAIT);
            invalidateOptionsMenu();
            super.onPreExecute();  
        }  
          
        @Override  
        protected String doInBackground(Integer... params) {  
            updataCallback(params[0], params[1], mResultIntent);
            return "";  
        }  
  
        @Override  
        protected void onProgressUpdate(Integer... progress) {  
            super.onProgressUpdate(progress);  
        }  
  
        @Override  
        protected void onPostExecute(String result) {  
            if (!this.isCancelled()) {
                dismissDialog(CALL_LIST_DIALOG_WAIT);
                showNumbers();
            }
            super.onPostExecute(result);  
        }  

        @Override
        protected void onCancelled(String result) {
            super.onCancelled(result);
        }
    }  

    /*sync the name begine*/
    class UpdateUIContactsTask extends AsyncTask<Integer, Integer, String>{

        @Override  
        protected void onPreExecute() {
            showDialog(CALL_LIST_DIALOG_WAIT);
            super.onPreExecute();
        }
          
        @Override  
        protected String doInBackground(Integer... params) {
            updateUICallback(params[0], params[1]);
            return "";
        }
  
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }
  
        @Override
        protected void onPostExecute(String result) {
            if(!this.isCancelled()){
                dismissDialog(CALL_LIST_DIALOG_WAIT);
                showNumbers();
            }
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled(String result){
            super.onCancelled(result);
        }
    }

    private void updateUICallback(int requestCode, int resultCode){
        Cursor cursor = getContentResolver().query(URI, new String[] {
                "_id", "Number", "type", "Name"}, null, null, null);
        if(cursor == null){
            return;
        }
        cursor.moveToFirst();
        try {
            while (!cursor.isAfterLast()) {
                String number = cursor.getString(NUMBER_INDEX);
                String name = cursor.getString(NAME_INDEX);
                String type = cursor.getString(TYPE_INDEX);
                if("3".equals(type)
		            || ("2".equals(type) && "video".equals(mType))
		            || ("1".equals(type) && "voice".equals(mType))) {
                    if (number == null || number.isEmpty()) {
                        Log.v(TAG, "updateUICallback0-------[number:"+number+"]--------");  
                    }
                    else {
                        Cursor cursorName = getContentResolver().query(
                            Phone.CONTENT_URI,
                            new String[] {Phone.DISPLAY_NAME, Phone.NUMBER},
                            null,
                            null,
                            null);
                        String tempNumber = "";
                        String tempName = "";
                        String newName = name;
                        boolean isNeedUpdateName = false;//spec the name
                        boolean isNoEsit = false;//the no. esit?
                        cursorName.moveToFirst();
                        try {
                                /* seek all name and number*/
                                while(!cursorName.isAfterLast()) {
                                    //modified for ALPS00567899
                                    //tempName = allWhite(cursorName.getString(0));
                                    tempName = cursorName.getString(0);
                                    tempNumber = allWhite(cursorName.getString(1));
                                    Log.v(TAG, "updateUICallback1..tempName:" + tempName + " tempNumber:" + tempNumber);
                                    if (number.equals(tempNumber)) {
                                        if (name.equals(tempName)) {
                                        } else {
                                            newName = tempName;//update the same
                                            isNeedUpdateName = true;
                                        }
                                        isNoEsit = true;
                                    }
                                    cursorName.moveToNext();
                                }
                        } finally {
                            cursorName.close();
                        }
                        //no. esit, and need name update
                        if (isNoEsit && isNeedUpdateName) {
                            Log.v(TAG, "updateUICallback2..newName1:" + newName + " number:" + number);
                            insertNumbers(number, newName);
                        }
                        //no. is not esit
                        if (!isNoEsit) {
                            newName = getResources().getString(R.string.call_reject_no_name);
                            Log.v(TAG, "updateUICallback2..newName2:" + newName + " number:" + number);
                            insertNumbers(number, newName);
                        }
                    }
                }
                cursor.moveToNext();
            }
         } finally {
            cursor.close();
         }
    }

    private void updateShowNumbers(){
        mUpdatUIContactsTask = new UpdateUIContactsTask();
        mUpdatUIContactsTask.execute(RESULT_OK, RESULT_OK);
    }
    /*sync the name end*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_reject_list);

        PreferenceScreen preference = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(preference);

        mListView = (ListView)findViewById(R.id.list);
        mType = getIntent().getStringExtra("type");

        if ("voice".equals(mType)) {
            setTitle(getResources().getString(R.string.voice_call_reject_list_title));
        } else {
            setTitle(getResources().getString(R.string.video_call_reject_list_title));
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        /*showNumbers(); use next instead*/
        if ((mAddContactsTask !=null) && (mAddContactsTask.getStatus() == AsyncTask.Status.RUNNING)) {
            Log.v(TAG, "onResume-------no update again--------");  
        }
        else {
	updateShowNumbers();
            Log.v(TAG, "onResume---------------");  
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();    
        if (mAddContactsTask != null) {
            mAddContactsTask.cancel(true);
        }
        if (mUpdatUIContactsTask !=null) {
            mUpdatUIContactsTask.cancel(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_DELETE, 0, R.string.call_reject_list_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENU_ID_ADD, 1, R.string.call_reject_list_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.v(TAG, "[preference count=" + getPreferenceScreen().getPreferenceCount() + "]");
        menu.getItem(0).setEnabled(getPreferenceScreen().getPreferenceCount() != 0);
        menu.getItem(1).setEnabled(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ID_ADD:
            addFlag = true;
            showDialog(CALL_LIST_DIALOG_EDIT);
            break;
        case MENU_ID_DELETE:
            Intent it = new Intent(this, CallRejectListModify.class);
            it.putExtra("type", mType);
            startActivity(it);
            break;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showNumbers() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();

        Cursor cursor = getContentResolver().query(URI, new String[] {
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
            if ("3".equals(type)
                    || ("2".equals(type) && "video".equals(mType))
                    || ("1".equals(type) && "voice".equals(mType))) {
                Preference preference = new Preference(this);
                preference.setTitle(name);
                preference.setSummary(numberDB);
                preferenceScreen.addPreference(preference);
            }
            cursor.moveToNext();
        }
        cursor.close();
        invalidateOptionsMenu();
    }
        
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == CALL_LIST_DIALOG_EDIT) {
            if (addFlag == false) return null;
            addFlag = false;
            LayoutInflater inflater;
            inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            Dialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        dismissDialog(CALL_LIST_DIALOG_EDIT);
                        if (mNumberEditText == null 
                                || mNumberEditText.getText().toString().isEmpty()
                                || mType == null) {
                            return;    
                        }
                        String rejectNumbers = allWhite(mNumberEditText.getText().toString());
                        ////////////////////
                        showDialog(CALL_LIST_DIALOG_WAIT);
                        Cursor cursorName = getContentResolver().query(
                            Phone.CONTENT_URI,
                            new String[] {Phone.DISPLAY_NAME, Phone.NUMBER},
                            null,
                            null,
                            null);
                        String tempNumber = "";
                        String tempName = getResources().getString(R.string.call_reject_no_name);
                        String newName = "";
                        cursorName.moveToFirst();
                        try {
                            /* seek all name and number*/
                            while(!cursorName.isAfterLast()) {
                                //modified for ALPS00567899
                                //newName = allWhite(cursorName.getString(0));
                                newName = cursorName.getString(0);
                                tempNumber = allWhite(cursorName.getString(1));
                                Log.v(TAG, "updateUICallback1..tempName:" + tempName + " tempNumber:" + tempNumber);
                                if (rejectNumbers.equals(tempNumber)) {
                                    tempName = newName;//update the same
                                }
                                cursorName.moveToNext();
                            }
                        } finally {
                            cursorName.close();
                        }
                        dismissDialog(CALL_LIST_DIALOG_WAIT);
                        insertNumbers(rejectNumbers, tempName);
                        ////////////////////
                        //insertNumbers(rejectNumbers, getResources().getString(R.string.call_reject_no_name));
                        showNumbers();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setTitle(R.string.add_call_reject_number)
                .setView(inflater.inflate(R.layout.call_reject_dialog, null))
                .create();
            dialog.show();
            mAddContactsBtn = (ImageButton)dialog.findViewById(R.id.select_contact);
            if (mAddContactsBtn != null) {
                mAddContactsBtn.setOnClickListener(this);
            }
            mNumberEditText = (EditText)dialog.findViewById(R.id.EditNumber);
            return dialog;
        } else if (id == CALL_LIST_DIALOG_SELECT) {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.call_reject_dialog_contact);
            dialog.setTitle(getResources().getString(R.string.select_from));
            ListView listview = (ListView)dialog.findViewById(R.id.list);
            listview.setOnItemClickListener(this);
            return dialog;
        } else if (id == CALL_LIST_DIALOG_WAIT) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.call_reject_please_wait));
            dialog.setCancelable(false);
            dialog.setIndeterminate(true);
            return dialog;
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch(id) {
        case CALL_LIST_DIALOG_EDIT:
            if(addFlag == false) break;
            if(mNumberEditText != null) {
                mNumberEditText.setText("");
            }
            break;
        default:
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mDeleteBtn) {
            Intent it = getIntent();
            it.setClass(this, CallRejectListModify.class);
            it.putExtra("type", mType);
            startActivity(it);
        } else if (v == mAddBtn) {
            addFlag = true;
            showDialog(CALL_LIST_DIALOG_EDIT);
        } else if (v == mAddContactsBtn) {
            dismissDialog(CALL_LIST_DIALOG_EDIT);
            showDialog(CALL_LIST_DIALOG_SELECT);
        }
    }

    @Override
    public void onItemClick(AdapterView<?>arg0, View arg1, int arg2, long arg3) {
        if (arg2 == 0) {
            Intent intent = new Intent(CONTACTS_ADD_ACTION);            
            intent.setType(Phone.CONTENT_TYPE);
            try {
                startActivityForResult(intent, CALL_REJECT_CONTACTS_REQUEST);
                dismissDialog(CALL_LIST_DIALOG_SELECT);
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, e.toString());
            }
        } else if (arg2 == 1) {
            Intent intent = new Intent(CALL_LOG_SEARCH);            
            intent.setClassName("com.android.contacts", 
                "com.mediatek.contacts.activities.CallLogMultipleChoiceActivity");
            try {
                startActivityForResult(intent, CALL_REJECT_LOG_REQUEST);
                dismissDialog(CALL_LIST_DIALOG_SELECT);
            } catch (ActivityNotFoundException e) {
                Log.d(TAG, e.toString());
            }
        }
    }
    
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        mAddContactsTask = new AddContactsTask();
        mResultIntent = data;
        mAddContactsTask.execute(requestCode, resultCode);
    }

    private void updataCallback(int requestCode, int resultCode, Intent data) {
        switch(resultCode) {
        case RESULT_OK:
            if (requestCode == CALL_REJECT_CONTACTS_REQUEST) {
                final long[] contactId = data.getLongArrayExtra(CONTACTS_ADD_ACTION_RESULT);
                if (contactId == null || contactId.length < 0) {
                    break;
                }
                for (int i = 0; i < contactId.length && !mAddContactsTask.isCancelled(); i++) {
                    updateContactsNumbers((int)contactId[i]);
                }
            } else if (requestCode == CALL_REJECT_LOG_REQUEST) {
                final String callLogId = data.getStringExtra("calllogids");
                updateCallLogNumbers(callLogId);
            }
            break;
        default:
            break;
        }

    }

    private void updateCallLogNumbers(String callLogId) {
        Log.v(TAG, "---------[" + callLogId + "]----------");
        if (callLogId == null || callLogId.isEmpty()) {
            return;
        }    
        if (!callLogId.startsWith("_id")) {
            return;
        }
        String ids = callLogId.substring(8, callLogId.length() - 1);
        String [] idsArray = ids.split(",");
        for (int i = 0; i < idsArray.length && !mAddContactsTask.isCancelled(); i++) {
            try {
                int id = Integer.parseInt(idsArray[i].substring(1, idsArray[i].length() - 1));
                updateCallLogNumbers(id);
                Log.i(TAG, "id is " + id);
            } catch (NumberFormatException e) {
                Log.e(TAG, "parseInt failed, the id is " + e);
            }
        }
    }

    private void updateCallLogNumbers(int id) {
        Uri existNumberURI = ContentUris.withAppendedId(CALLLOG_URI, id);
        Cursor cursor = getContentResolver().query(existNumberURI, CALL_LOG_PROJECTION, null, null, null);
        cursor.moveToFirst();
        Log.v(TAG, "----updateCallLogNumbers---[calllogid"+id+"]-------");
        String data_id = "";
        try {
            if (!cursor.isAfterLast()) {
                String number = allWhite(cursor.getString(NUMBER));
                String name = cursor.getString(CACHED_NAME);/*name not right, need update*/
                if (name == null || name.isEmpty()) {
                    name = getResources().getString(R.string.call_reject_no_name);
                }
                Log.v(TAG, "updateCallLogNumbers----[number:"+number+"]---[name"+name+"]-[calllogid"+id+"]-----");
                /*use to update the call log username begin. use calllog id seek data_id*/
                Uri viewexistNumberURI = ContentUris.withAppendedId(mCallRejectViewCallLogUri, id);
                Cursor viewcursor = getContentResolver().query(
                viewexistNumberURI, CALL_PROJECTION_CALLS_JOIN_DATAVIEW, null, null, null);
                viewcursor.moveToFirst();
                try {
                    if (!viewcursor.isAfterLast()) {
                        number = allWhite(viewcursor.getString(CALLSVIEW_NUMBER_COLUMN));
                        data_id = allWhite(viewcursor.getString(CALLSVIEW_DATA_ID_COLUMN)); 
                        Log.v(TAG, "updateCallLogNumbers1----[number:"+number+"]----[data_id"+data_id+"]----");
                    }
                } finally {
                    viewcursor.close();
                }
                /*use data_id seek raw_contact_id*/
                String raw_id = "";
                if (data_id == null || data_id.isEmpty()) {
                    name = getResources().getString(R.string.call_reject_no_name);
                    Log.v(TAG, "updateCallLogNumbers:-------[data_id"+data_id+"]--------");
                }
                else {
                    Cursor cursorData_id = getContentResolver().query(
                        CONTACT_URI,
                        CALLER_DATA_ID_PROJECTION,
                        Data._ID + "=?",
                        new String[] {data_id},
                        null);
                    cursorData_id.moveToFirst();
                    try {
                        if (!cursorData_id.isAfterLast()) {
                            raw_id = allWhite(cursorData_id.getString(DATA_RAW_CONTACT_ID_COLUMN));
                            String data1 = cursorData_id.getString(DATA_DATA1_COLUMN);
                            String data2 = cursorData_id.getString(DATA_DATA2_COLUMN);
                            Log.v(TAG, "updateCallLogNumbers2:data_id:" + data_id + "raw_id:" + raw_id + "data1:" + data1 + "data2:" + data2);
                        }
                    } finally {
                        cursorData_id.close();
                    }
                }
                /*use raw_contact_id and mimetype, seek the displayname, if not ready, use the old value*/
                if (raw_id == null || raw_id.isEmpty()) {
                    name = getResources().getString(R.string.call_reject_no_name);
                    Log.v(TAG, "updateCallLogNumbers::-------[raw_id"+raw_id+"]--------");  
                }
                else {
                    Cursor cursorName = getContentResolver().query(
                        Data.CONTENT_URI,
                        new String[] {Data._ID, StructuredName.DISPLAY_NAME, Data.RAW_CONTACT_ID},
                        Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "='" + StructuredName.CONTENT_ITEM_TYPE +"'",
                        new String[] {raw_id},
                        null);
                    cursorName.moveToFirst();
                    Log.v(TAG, "updateCallLogNumbers3..raw_id:" + raw_id + "--MIMETYPE:" + StructuredName.CONTENT_ITEM_TYPE);
                    try {
                        while (!cursorName.isAfterLast()) {
                            String data_idt = cursorName.getString(0);
                            name = allWhite(cursorName.getString(1));
                            Log.v(TAG, "TestModify3..name:" + "name:" + name + "data_idt:" + data_idt);
                            cursorName.moveToNext();
                        }
                    } finally {
                        cursorName.close();
                    }
                }
                /*use to update the call log username end*/
                insertNumbers(number, name);
                cursor.moveToNext();
            }
       } finally {
           cursor.close();
       }
    }

    private void updateContactsNumbers(int id) {
        Uri existNumberURI = ContentUris.withAppendedId(CONTACT_URI, id);
        Cursor cursor = getContentResolver().query(
            existNumberURI, CALLER_ID_PROJECTION, null, null, null);
        cursor.moveToFirst();
        try {
            while (!cursor.isAfterLast()) {
                String number = allWhite(cursor.getString(PHONE_NUMBER_COLUMN));
                String name = cursor.getString(CONTACT_NAME_COLUMN);
                insertNumbers(number, name);
                cursor.moveToNext();
           }
        } finally {
            cursor.close();
        }
    }

    private void insertNumbers(String number, String name) {
        Cursor cursor = getContentResolver().query(URI, new String[] {
                "_id", "Number", "type", "Name"}, null, null, null);
        if (cursor == null) {
            return;
        }
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            String id = cursor.getString(ID_INDEX);
            String numberDB = cursor.getString(NUMBER_INDEX);
            String type = cursor.getString(TYPE_INDEX);

            if (equalsNumber(number, numberDB)) {
                cursor.close();
                update(id, number, name, type);
                return;
            }
            cursor.moveToNext();
        }
        cursor.close(); 
        insert(number, name);
    }
    
    private boolean equalsNumber(String number1, String number2) {
        if (number1 == null || number2 == null) {
            return false;
        }
        boolean isEquals = false;

        if (number1.equals(number2)) {
            isEquals = true;
        } else {
            isEquals = false;
        }
        return isEquals;
    }

    private void insert(String number, String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("Number", number);
        if (mType.equals("video")) {
            contentValues.put("Type", "2");
        } else {
            contentValues.put("Type", "1");
        }
        contentValues.put("Name", name);
        getContentResolver().insert(URI, contentValues);
    }

    private void update(String id, String number, String name, String type) {
        if (id == null) {
            return;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("Number", number);

        int typeInt = 0;

        try {
            typeInt = Integer.parseInt(type);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the typeInt is " + typeInt);
        }

        if (mType.equals("video")) {
            contentValues.put("Type", String.valueOf(typeInt | 0x2));
        } else {
            contentValues.put("Type", String.valueOf(typeInt | 0x1));
        }

        if (!getResources().getString(R.string.call_reject_no_name).equals(name)) {
            contentValues.put("Name", name);
        }
    
        try {
            Uri existNumberURI = ContentUris.withAppendedId(URI, Integer.parseInt(id));
            int result = getContentResolver().update(existNumberURI, contentValues, null, null);
            Log.i(TAG, "result is " + result);
        } catch (NumberFormatException e) {
            Log.e(TAG, "parseInt failed, the index is " + id);
        }
    }

    private String allWhite(String str) {
        if (str != null) {
            str = str.replaceAll(" ", "");
        }
        return str;
    }
}
