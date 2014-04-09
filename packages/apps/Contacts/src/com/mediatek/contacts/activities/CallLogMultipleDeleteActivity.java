/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.contacts.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.android.contacts.R;
import com.mediatek.contacts.calllog.CallLogMultipleDeleteFragment;
import com.mediatek.contacts.list.DropMenu;
import com.mediatek.contacts.list.DropMenu.DropDownMenu;
import com.mediatek.contacts.util.SetIndicatorUtils;

/**
 * Displays a list of call log entries.
 */
public class CallLogMultipleDeleteActivity extends Activity {
    private static final String TAG = "CallLogMultipleDeleteActivity";

    protected CallLogMultipleDeleteFragment mFragment;

    public StatusBarManager mStatusBarMgr;

    //the dropdown menu with "Select all" and "Deselect all"
    private DropDownMenu mSelectionMenu;
    private boolean mIsSelectedAll = false;
    private boolean mIsSelectedNone = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.call_log_multiple_delete_activity);

        // Typing here goes to the dialer
        //setDefaultKeyMode(DEFAULT_KEYS_DIALER);

        mFragment = (CallLogMultipleDeleteFragment) getFragmentManager().findFragmentById(
                R.id.call_log_fragment);
        configureActionBar();
        updateSelectedItemsView(0);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // This action deletes all elements in the group from the call log.
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        

        SetIndicatorUtils.getInstance().showIndicator(true, this);
    }

    @Override
    protected void onPause() {

        SetIndicatorUtils.getInstance().showIndicator(false, this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.finish();
    }
    
    protected void onStopForSubClass() {
        super.onStop();  
    }
    
    private void configureActionBar() {
        log("configureActionBar()");
        // Inflate a custom action bar that contains the "done" button for
        // multi-choice
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBarView = inflater.inflate(R.layout.call_log_multiple_delete_custom_action_bar, null);
        ImageButton doneMenuItem = (ImageButton) customActionBarView.findViewById(R.id.done_menu_item);
        doneMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        selectView.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.dropdown_normal_holo_dark));
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View parent = (View) v.getParent();
                updateSelectionMenu(parent);
                mSelectionMenu.show();
                return;
            }
        });
        
        //dispaly the "CANCEL" button.
        Button cancelView = (Button) customActionBarView
                .findViewById(R.id.cancel);
        String cancelText = cancelView.getText().toString();
        if ("Cancel".equalsIgnoreCase(cancelText)) {
            cancelText = cancelText.toUpperCase();
            cancelView.setText(cancelText);
        }
        cancelView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        //dispaly the "OK" button.
        Button deleteView = (Button) customActionBarView
                .findViewById(R.id.delete);
        if (mIsSelectedNone) {
            // if there is no item selected, the "OK" button is disable.
            deleteView.setEnabled(false);
            deleteView.setTextColor(Color.GRAY);
        } else {
            deleteView.setEnabled(true);
            deleteView.setTextColor(Color.WHITE);
        }
        deleteView.setOnClickListener(getClickListenerOfActionBarOKButton());
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                            | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }
    }



    public void updateSelectedItemsView(final int checkedItemsCount) {
        Button selectedItemsView = (Button) getActionBar().getCustomView().findViewById(R.id.select_items);
        if (selectedItemsView == null) {
            log("Load view resource error!");
            return;
        }
        selectedItemsView.setText(getString(R.string.selected_item_count, checkedItemsCount));
        //if no item selected, the "OK" button is disable.
        Button optionView = (Button) getActionBar().getCustomView()
        .findViewById(R.id.delete);
        if (checkedItemsCount == 0) {
            optionView.setEnabled(false);
            optionView.setTextColor(Color.GRAY);
        } else {
            optionView.setEnabled(true);
            optionView.setTextColor(Color.WHITE);
        }
        
        
    }

    private void log(final String log) {
        Log.i(TAG, log);
    }
    
    private void showDeleteDialog() {
        AlertDialog delDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.deleteCallLogConfirmation_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.deleteCallLogConfirmation_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mFragment.deleteSelectedCallItems();
                                updateSelectedItemsView(0);
                            }
                        });
        delDialog = builder.create();
        delDialog.show();
    }
    
    /**
     * add dropDown menu on the selectItems.The menu is "Select all" or "Deselect all"
     * @param customActionBarView
     */
    public void updateSelectionMenu(View customActionBarView) {
        DropMenu dropMenu = new DropMenu(this);
        // new and add a menu.
        mSelectionMenu = dropMenu.addDropDownMenu((Button) customActionBarView
                .findViewById(R.id.select_items), R.menu.selection);
        // new and add a menu.
        Button selectView = (Button) customActionBarView
                .findViewById(R.id.select_items);
        selectView.setBackgroundDrawable(getResources().getDrawable(
                R.drawable.dropdown_normal_holo_dark));
        selectView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View parent = (View) v.getParent();
                updateSelectionMenu(parent);
                mSelectionMenu.show();
                return;
            }
        });        
        MenuItem item = mSelectionMenu.findItem(R.id.action_select_all);
        mIsSelectedAll = mFragment.isAllSelected();
        // if select all items, the menu is "Deselect all"; else the menu is "Select all".
        if (mIsSelectedAll) {
            item.setChecked(true);
            item.setTitle(R.string.menu_select_none);
            // click the menu, deselect all items
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    configureActionBar();
                    mFragment.unSelectAllItems();
                    updateSelectedItemsView(0);
                    return false;
                }
            });
        } else {
            item.setChecked(false);
            item.setTitle(R.string.menu_select_all);
            //click the menu, select all items.
            dropMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    configureActionBar();
                    updateSelectedItemsView(mFragment.selectAllItems());
                    return false;
                }
            });
        }
       return;
    }

    protected OnClickListener getClickListenerOfActionBarOKButton() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFragment.getSelectedItemCount() == 0) {
                    Toast.makeText(v.getContext(), R.string.multichoice_no_select_alert,
                                 Toast.LENGTH_SHORT).show();                
                  return;
              }
              showDeleteDialog();
              return;
            }
        };
    }
}
