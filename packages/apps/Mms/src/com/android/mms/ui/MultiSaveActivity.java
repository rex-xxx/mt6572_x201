/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.net.Uri;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.model.FileAttachmentModel;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.mediatek.encapsulation.MmsLog;
import com.android.mms.ui.PduBodyCache;
import com.android.mms.ui.CustomMenu.DropDownMenu;
import com.mediatek.encapsulation.android.os.storage.EncapsulatedStorageManager;

//add for attachment enhance

import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;

import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;

import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduPart;
import com.android.mms.model.SlideshowModel;
import com.android.mms.MmsPluginManager;
import com.google.android.mms.MmsException;
import android.widget.Toast;
import android.os.SystemProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/** M:
 * This activity provides a list view of existing conversations.
 */
public class MultiSaveActivity extends Activity {
    private static final String TAG = "Mms/MultiSaveActivity";
    private MultiSaveListAdapter mListAdapter;
    private ListView mMultiSaveList;
    private ContentResolver mContentResolver;
    private boolean needQuit = false;
    private Button mActionBarText;
    /// M: new feature, MultiSaveActivity redesign UI
    private MenuItem mSelectionItem;
    private DropDownMenu mSelectionMenu;
    
   /// M: @{
    private long smode = -1;
   /// @}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   /// M: @{
       IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 
   /// @}
        setTitle(R.string.save);
        mContentResolver = getContentResolver();
        setContentView(R.layout.multi_save);
        mMultiSaveList = (ListView) findViewById(R.id.item_list);
        mMultiSaveList
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        if (view != null) {
                            ((MultiSaveListItem) view).clickListItem();
                            mListAdapter.changeSelectedState(position);
                            updateActionBarText();
                        }
                    }
                });
        
        Intent i = getIntent();
        long msgId = -1;
        if (i != null && i.hasExtra("msgid")) {
            msgId = i.getLongExtra("msgid", -1);
        /// M: @{ 

        //add for attachment enhance

            if (mMmsAttachmentEnhancePlugin != null) {
                smode = mMmsAttachmentEnhancePlugin.getSaveAttachMode(i);
            }
        /// @}
        }

        initListAdapter(msgId); 
        initActivityState(savedInstanceState);
        setUpActionBar();
    }
    
    private void initActivityState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            boolean selectedAll = savedInstanceState.getBoolean("is_all_selected");
            if (selectedAll) {
                mListAdapter.setItemsValue(true, null);
                return;
            } 
            
            int [] selectedItems = savedInstanceState.getIntArray("select_list");
            if (selectedItems != null) {
                mListAdapter.setItemsValue(true, selectedItems);
            }
            
        } else {
            MmsLog.i(TAG, "initActivityState, fresh start select all");
            mListAdapter.setItemsValue(true, null);
            markCheckedState(true);
        }
    }
    
    private void initListAdapter(long  msgId) {
        //PduBody body = ComposeMessageActivity.getPduBody(MultiSaveActivity.this, msgId);
        PduBody body = PduBodyCache.getPduBody(MultiSaveActivity.this, msgId);
    /// M: @{

        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        SlideshowModel mSlideshow = null;
    /// @}
        if (body == null) {
            MmsLog.e(TAG, "initListAdapter, oops, getPduBody returns null");
            return;
        }
        int partNum = body.getPartsNum();
        
        ArrayList<MultiSaveListItemData> attachments = new ArrayList<MultiSaveListItemData>(partNum);
   /// M: @{
        try{
             mSlideshow = SlideshowModel.createFromPduBody(this, body);
        } catch (MmsException e){
             MmsLog.v(TAG, "Create from pdubody exception!");
             return;
        }

        ArrayList<FileAttachmentModel> attachmentList = mSlideshow.getAttachFiles();
   /// @}
         for(int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            byte[] cl = part.getContentLocation();
            byte[] name = part.getName();
            byte[] ci = part.getContentId();
            byte[] fn = part.getFilename();
            String filename = null;
            String mSrc = null;
            if (cl != null) {
            	filename = new String(cl);
            } else if (name != null){
            	filename = new String(name);
            } else if (ci != null){
            	filename = new String(ci);
            } else if (fn != null){
            	filename = new String(fn);
            } else {
                MmsLog.v(TAG, "initListAdapter: filename = null,continue"); 
            	continue;
            }

            mSrc = filename;

            String PartUri = null;

            if (part.getDataUri() != null) {
                   MmsLog.e(TAG, "part Uri = " + part.getDataUri().toString());
                   PartUri = part.getDataUri().toString();
            } else {
                   MmsLog.v(TAG, "PartUri = null");
                   continue;
            }
            final String type =  MessageUtils.getContentType(new String(part.getContentType()),mSrc);
        // part.setContentType(type.getBytes());
     /// M: @{
            if (mMmsAttachmentEnhancePlugin != null && mSrc != null) {

                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                    //OP01

                     MmsLog.v(TAG, "In multisave initList" );
                 MmsLog.v(TAG, "smode = " + smode );
                if (smode == IMmsAttachmentEnhance.MMS_SAVE_ALL_ATTACHMENT) {
                    //save all attachment including slides
                    MmsLog.v(TAG, "save all attachment including slides" );
                       if ((EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) || "application/ogg".equalsIgnoreCase(type) ||
                            EncapsulatedContentType.isAudioType(type) || FileAttachmentModel.isSupportedFile(part) ) &&
                           !type.equals(EncapsulatedContentType.TEXT_PLAIN) && !type.equals(EncapsulatedContentType.TEXT_HTML)) {
                       attachments.add(new MultiSaveListItemData(this, part, msgId));
                       MmsLog.v(TAG, "smode = " + smode);
                   }
                }else if(smode == IMmsAttachmentEnhance.MMS_SAVE_OTHER_ATTACHMENT) {
                    //Only save attachment files no including slides
                            MmsLog.v(TAG, "Only save attachment files no including slides" );                            
                            for (int k=0; k<attachmentList.size(); k++) {  
                                if (PartUri != null && attachmentList.get(k).getUri() != null) {
                                    if (PartUri.compareTo(attachmentList.get(k).getUri().toString()) == 0) {
                                        //MmsLog.v(TAG, "part.getFilename() = "+part.getFilename());
                                        attachments.add(new MultiSaveListItemData(this, part, msgId));
                                    }
                                }                   
                            }
                    }
                    // add text and html attachment
                    for (int k=0; k<attachmentList.size(); k++) {
                        if (mSrc.equals(attachmentList.get(k).getSrc()) && (type.equals(EncapsulatedContentType.TEXT_PLAIN) ||
                               type.equals(EncapsulatedContentType.TEXT_HTML))) {
                            int flag = 0;
                            attachments.add(new MultiSaveListItemData(this, part, msgId, flag));
                        }
                    }
                }else {
                    //Not OP01
            part.setContentType(type.getBytes());
            if (EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) || "application/ogg".equalsIgnoreCase(type) ||
                    EncapsulatedContentType.isAudioType(type) || FileAttachmentModel.isSupportedFile(part)
                    /// M: fix bug ALPS00446644, support dcf (0ct-stream) file to save
                    || (mSrc != null && mSrc.toLowerCase().endsWith(".dcf"))) {
                attachments.add(new MultiSaveListItemData(this, part, msgId));
            }
        }
            } else {//common
            part.setContentType(type.getBytes());
            if (EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) || "application/ogg".equalsIgnoreCase(type) ||
                    EncapsulatedContentType.isAudioType(type) || FileAttachmentModel.isSupportedFile(part)
                    /// M: fix bug ALPS00446644, support dcf (0ct-stream) file to save
                    || (mSrc != null && mSrc.toLowerCase().endsWith(".dcf"))) {
                attachments.add(new MultiSaveListItemData(this, part, msgId));
            }
        }
        }//for
        attachments.trimToSize();
        mListAdapter = new MultiSaveListAdapter(this, attachments);
        mMultiSaveList.setAdapter(mListAdapter);
    }
    
    private void setUpActionBar() {
        ActionBar actionBar = getActionBar();
        
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                | ActionBar.DISPLAY_SHOW_TITLE);

        CustomMenu customMenu = new CustomMenu(this);
        View customView = LayoutInflater.from(this).inflate(
                R.layout.multi_save_list_actionbar, null);

        /// M: fix bug ALPS00441681, re-layout for landscape
        actionBar.setCustomView(customView,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                        ActionBar.LayoutParams.MATCH_PARENT,
                        Gravity.FILL));

        mActionBarText = (Button) customView.findViewById(R.id.selection_menu);
        mSelectionMenu = customMenu.addDropDownMenu(mActionBarText, R.menu.selection);
        mSelectionItem = mSelectionMenu.findItem(R.id.action_select_all);

        customMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                // TODO Auto-generated method stub
                if (mListAdapter.isAllSelected()) {
                    cancelSelectAll();
                } else {
                    selectAll();
                }
                return false;
            }
        });

        Button quit = (Button) customView.findViewById(R.id.selection_cancel);
        quit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                finish();
            }
        });

        Button save = (Button) customView.findViewById(R.id.selection_done);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub
                save();
            }
        });

        updateActionBarText();
    }

    private void selectAll() {
        if (mListAdapter != null) {
            markCheckedState(true);
            mListAdapter.setItemsValue(true, null);
            updateActionBarText();
        }
    }

    private void cancelSelectAll() {
        if (mListAdapter != null) {
            markCheckedState(false);
            mListAdapter.setItemsValue(false, null);
            updateActionBarText();
        }
    }

    private void save() {
        if (mListAdapter.getSelectedNumber() > 0) {
            boolean succeeded = false;
            succeeded = copyMedia();
            Intent i = new Intent();
            i.putExtra("multi_save_result", succeeded);
            setResult(RESULT_OK, i);
            finish();

            /// M: @{

            //add for attachment enhance
            IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
            /// @}
            MmsLog.v(TAG, "mMmsAttachmentEnhancePlugin = " + mMmsAttachmentEnhancePlugin);
            if (mMmsAttachmentEnhancePlugin!= null) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                    MmsLog.v(TAG, "OUT MMS_SAVE_OTHER_ATTACHMENT");
                    if (smode == IMmsAttachmentEnhance.MMS_SAVE_OTHER_ATTACHMENT) {
                        MmsLog.v(TAG, "IN MMS_SAVE_OTHER_ATTACHMENT");
                        int resId = succeeded ? R.string.copy_to_sdcard_success : R.string.copy_to_sdcard_fail;
                            Toast.makeText(MultiSaveActivity.this, resId, Toast.LENGTH_SHORT).show();                        
                    }
                }
            }
        }
    }

    private void updateActionBarText() {
        if (mListAdapter != null && mActionBarText != null) {
            mActionBarText.setText(getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count,
                    mListAdapter.getSelectedNumber(),
                    mListAdapter.getSelectedNumber()));
        }

        if (mSelectionItem != null && mListAdapter != null) {
            if (mListAdapter.isAllSelected()) {
                mSelectionItem.setChecked(true);
                mSelectionItem.setTitle(R.string.unselect_all);
            } else {
                mSelectionItem.setChecked(false);
                mSelectionItem.setTitle(R.string.select_all);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        MmsLog.v(TAG, "onSaveInstanceState, with bundle " + outState);
        super.onSaveInstanceState(outState);      
        if (mListAdapter != null) {
            if (mListAdapter.isAllSelected()) {
                outState.putBoolean("is_all_selected", true);
            } else if (mListAdapter.getSelectedNumber() == 0) {
                return;
            } else { 
                int[] checkedArray = new int[mListAdapter.getSelectedNumber()];
                ArrayList<MultiSaveListItemData> list = mListAdapter.getItemList();
                for (int i = 0; i < checkedArray.length; i++) {
                    if (list.get(i).isSelected()) {
                        checkedArray[i] = i;
                    }
                }
                outState.putIntArray("select_list", checkedArray);
            }
        }
    }
    
    private void markCheckedState(boolean checkedState) {
        int count = mMultiSaveList.getChildCount();     
        MmsLog.v(TAG, "markCheckState count is " + count + ", state is " + checkedState);
        MultiSaveListItem item = null;
        for (int i = 0; i < count; i++) {
            item = (MultiSaveListItem) mMultiSaveList.getChildAt(i);
            item.selectItem(checkedState);
        }
    }
  
    /**
     * Copies media from an Mms to the "download" directory on the SD card
     * @param msgId
     */
    private boolean copyMedia() {
        boolean result = true;

        ArrayList<MultiSaveListItemData> list = mListAdapter.getItemList();
        int size = list.size();
        for(int i = 0; i < size; i++) {
            if (!list.get(i).isSelected()) {
                continue;
            }
            PduPart part = list.get(i).getPduPart();
            final String filename = list.get(i).getName();
            final String type = new String(part.getContentType());
            if (EncapsulatedContentType.isImageType(type) || EncapsulatedContentType.isVideoType(type) || EncapsulatedContentType.isAudioType(type)
                    || "application/ogg".equalsIgnoreCase(type)
                    || FileAttachmentModel.isSupportedFile(part)
                    /// M: fix bug ALPS00446644, support dcf (0ct-stream) file to save
                    || (filename != null && filename.toLowerCase().endsWith(".dcf"))) {
                result &= copyPart(part, filename);   // all parts have to be successful for a valid result.
            } else if (type.equals(EncapsulatedContentType.TEXT_PLAIN) || type.equals(EncapsulatedContentType.TEXT_HTML)) {
                // for text attachment or html attachment
                result &= copyPartNoUri(part, filename);
            }
        }
        return result;
    }
    //add for attachment

    ///M: @{ add for attachment enhance, save text/plain, text/html attachment files

    private boolean copyPartNoUri(PduPart part, String filename) {
        FileOutputStream fout = null;      
        try {
            File file = MessageUtils.getStorageFile(filename, getApplicationContext());
            if (file == null) {
                MmsLog.e(TAG, "default file is null");
                return false;
            }
            fout = new FileOutputStream(file);
            fout.write(part.getData(),0,part.getData().length);
        } catch(IOException e){
            MmsLog.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
             if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    MmsLog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    ///@}

    private boolean copyPart(PduPart part, String filename) {
        Uri uri = part.getDataUri();
        MmsLog.i(TAG, "copyPart, copy part into sdcard uri " + uri);

        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;
                // Depending on the location, there may be an
                // extension already on the name or not
                File file = MessageUtils.getStorageFile(filename, getApplicationContext());
                if (file == null) {
                    return false;
                }
                fout = new FileOutputStream(file);
                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size=fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            MmsLog.e(TAG, "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    MmsLog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    MmsLog.e(TAG, "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }
}
