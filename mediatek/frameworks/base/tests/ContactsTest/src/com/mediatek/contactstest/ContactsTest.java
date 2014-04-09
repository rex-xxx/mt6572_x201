package com.mediatek.contactstest;

/*import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;*/

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
//import android.view.KeyEvent;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
//import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity;

import com.jayway.android.robotium.solo.Solo;

/**
 * @author MTK54140
 */
public class ContactsTest extends
    ActivityInstrumentationTestCase2<PeopleActivity> {
    
    private Solo solo;  
    private Activity mActivity = null;
    private Context mContext = null;
    final String PACKAGE_UNDER_TEST_CONTACTS = "com.android.contacts";
    private static final String TAG = "ContactsTest";
    private int id;
    private int id2;
    private String USIM;
    private String SIM;
    private String Done;
    private String DeleteContacts;
    private View AddContacts;
    private View SelectAll;    
    private View DeleteIcon;    
    private String DefaultPath;
    private String Sdcard2 = "/mnt/sdcard2";

    /**
     * constructor
     */
    public ContactsTest() {
        super(PeopleActivity.class);
    
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
        mActivity = getActivity();
        solo = new Solo(getInstrumentation(), getActivity());
        id = mActivity.getResources().getIdentifier("menu_add_contact", "id",
                PACKAGE_UNDER_TEST_CONTACTS);
        AddContacts = solo.getView(id);
        id = mActivity.getResources().getIdentifier("menu_delete_contact", "string",
                PACKAGE_UNDER_TEST_CONTACTS);
        DeleteContacts = mContext.getString(id);    
        id = mActivity.getResources().getIdentifier("account_type_usim_label", "string",
                PACKAGE_UNDER_TEST_CONTACTS);
        USIM = mContext.getString(id);    
        id2 = mActivity.getResources().getIdentifier("account_type_sim_label", "string",
                PACKAGE_UNDER_TEST_CONTACTS);
        SIM = mContext.getString(id2);    
        id = mActivity.getResources().getIdentifier("menu_done", "string",
                PACKAGE_UNDER_TEST_CONTACTS);
        Done = mContext.getString(id);    
        
    }

    public void tearDown() throws Exception {
        SystemClock.sleep(2000);
        super.tearDown();
    }
  
    public void AddContacts(String SIMCard, String Name, int index){
        solo.clickOnView(AddContacts);
        SystemClock.sleep(2000);  
        if (!solo.searchText(SIMCard)){
        solo.clickOnView(AddContacts);
            SystemClock.sleep(2000); 
        }
        
        //Click on USIM/SIM item
        if(solo.searchText(SIMCard)) {
            solo.clickOnText(SIMCard, index);
            SystemClock.sleep(1000);
            solo.enterText(0, Name);
            SystemClock.sleep(1000);
            solo.enterText(1,"13511113232");
            SystemClock.sleep(500);
            solo.clickOnText(Done);
            assertTrue(TAG + "Can not save contact", solo.waitForText("Contact saved"));
            SystemClock.sleep(3000);
            assertTrue(TAG + "Can not save contact", solo.searchText(Name));        
            solo.goBack();
            SystemClock.sleep(1000);
            assertTrue(TAG + "Can not save contact", solo.searchText(Name));    
        } else {
            solo.goBack();
            SystemClock.sleep(1000);            
        }
    }


    public void test_contactsUsim() throws Exception {
        final String TEST_NAME = "test_contacts";
        final String MESSAGE = "[" + TEST_NAME + "] ";
        Log.v(TAG, MESSAGE + "test start");
        solo.sendKey(KeyEvent.KEYCODE_MENU);
        SystemClock.sleep(2000);
        solo.clickOnMenuItem(DeleteContacts);
        SystemClock.sleep(2000);
          
        //Click on selected menu button
        View view = solo.getButton(0);
        solo.clickOnView(view);
      
        //Get select all string
        id = mActivity.getResources().getIdentifier("menu_select_all", "string", PACKAGE_UNDER_TEST_CONTACTS);
        String SelectAllStr = mActivity.getResources().getString(id);
        Log.v(TAG, MESSAGE + "id=" + id + ", SelectAllStr=" + SelectAllStr);

        //If there is select all button, delete all contacts. otherwise, 
        if (solo.searchText(SelectAllStr)){
            solo.clickOnText(SelectAllStr);
            SystemClock.sleep(2000);
            String okStr = mActivity.getResources().getString(android.R.string.ok);
            solo.clickOnText(okStr);
            SystemClock.sleep(1000);

            okStr = solo.getCurrentActivity().getResources().getString(android.R.string.ok);
            Log.v(TAG, MESSAGE + "okStr=" + okStr);
            solo.clickOnButton(okStr);
            SystemClock.sleep(25000);
        } else {
            //Back to delete activity(cancel seleted menu)
            solo.goBack();
            //Back to contact list activity
            solo.goBack();
            SystemClock.sleep(1000);
        }
        
        //Add USIM contacts
        if(solo.searchText("USIM", 1)) {
            //Insert one USIM
            AddContacts("USIM", "test_usim", 1);
            SystemClock.sleep(1000); 
        } else if(solo.searchText("USIM", 2)) {
            //Insert two USIM
            AddContacts("USIM", "sim1_test_usim", 1);
            SystemClock.sleep(1000); 
            AddContacts("USIM", "sim2_test_usim",2);
            SystemClock.sleep(1000); 
        } 
        Log.v(TAG, MESSAGE + "test end");
    }
    
    public void test_contactsSim() throws Exception {
        final String TEST_NAME = "test_contacts";
        final String MESSAGE = "[" + TEST_NAME + "] ";
        Log.v(TAG, MESSAGE + "test start");
        solo.sendKey(KeyEvent.KEYCODE_MENU);
        SystemClock.sleep(2000);
        solo.clickOnMenuItem(DeleteContacts);
        SystemClock.sleep(2000);
          
        //Click on selected menu button
        View view = solo.getButton(0);
        solo.clickOnView(view);
      
        //Get select all string
        id = mActivity.getResources().getIdentifier("menu_select_all", "string", PACKAGE_UNDER_TEST_CONTACTS);
        String SelectAllStr = mActivity.getResources().getString(id);
        Log.v(TAG, MESSAGE + "id=" + id + ", SelectAllStr=" + SelectAllStr);

        //If there is select all button, delete all contacts. otherwise, 
        if (solo.searchText(SelectAllStr)){
            solo.clickOnText(SelectAllStr);
            SystemClock.sleep(2000);
            String okStr = mActivity.getResources().getString(android.R.string.ok);
            solo.clickOnText(okStr);
            SystemClock.sleep(1000);

            okStr = solo.getCurrentActivity().getResources().getString(android.R.string.ok);
            Log.v(TAG, MESSAGE + "okStr=" + okStr);
            solo.clickOnButton(okStr);
            SystemClock.sleep(25000);
        } else {
            //Back to delete activity(cancel seleted menu)
            solo.goBack();
            //Back to contact list activity
            solo.goBack();
            SystemClock.sleep(1000);
        }
        
        //Add SIM contacts
        if(solo.searchText("SIM", 1)) {
            //Insert one SIM
            AddContacts("USIM", "test_sim",1);
            SystemClock.sleep(1000); 
        } else if (solo.searchText("SIM", 2)) {
            //Insert two SIM
            AddContacts("SIM", "sim1_test_sim",1);
            SystemClock.sleep(1000); 
            AddContacts("SIM", "sim2_test_sim",2);
            SystemClock.sleep(1000);             
        } 
        
        Log.v(TAG, MESSAGE + "test end");
    }    
}
