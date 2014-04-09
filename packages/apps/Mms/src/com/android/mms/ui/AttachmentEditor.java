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

import com.android.mms.R;
import com.android.mms.data.WorkingMessage;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.util.ItemLoadedCallback;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
/// M:
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.telephony.SmsMessage;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mms.ExceedMessageSizeException;
import com.android.mms.MmsConfig;
import com.android.mms.model.FileAttachmentModel;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmManagerClient;
import com.mediatek.encapsulation.com.mediatek.internal.EncapsulatedR;

//add for attachment enhance by feng
//import packages
import com.android.mms.MmsPluginManager;
import com.android.mms.model.FileModel;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;

import java.util.List;
import com.mediatek.drm.OmaDrmUiUtils;

/**
 * This is an embedded editor/view to add photos and sound/video clips
 * into a multimedia message.
 */
public class AttachmentEditor extends LinearLayout {
    private static final String TAG = "AttachmentEditor";

    static final int MSG_EDIT_SLIDESHOW   = 1;
    static final int MSG_SEND_SLIDESHOW   = 2;
    static final int MSG_PLAY_SLIDESHOW   = 3;
    static final int MSG_REPLACE_IMAGE    = 4;
    static final int MSG_REPLACE_VIDEO    = 5;
    static final int MSG_REPLACE_AUDIO    = 6;
    static final int MSG_PLAY_VIDEO       = 7;
    static final int MSG_PLAY_AUDIO       = 8;
    static final int MSG_VIEW_IMAGE       = 9;
    static final int MSG_REMOVE_ATTACHMENT = 10;
    /// M: add for attachment enhance
    static final int MSG_REMOVE_EXTERNAL_ATTACHMENT = 11;
    static final int MSG_REMOVE_SLIDES_ATTACHMENT = 12;

    private final Context mContext;
    private Handler mHandler;

    private SlideViewInterface mView;
    private SlideshowModel mSlideshow;
    private Presenter mPresenter;
    private boolean mCanSend;
    private Button mSendButton;

    /// M: add for vCard
    private View mFileAttachmentView;

    public AttachmentEditor(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;
    }

    /**
     * Returns true if the attachment editor has an attachment to show.
     */
    public boolean update(WorkingMessage msg) {
        hideView();
        mView = null;
        /// M: add for vcard @{
        mFileAttachmentView = null;
        mWorkingMessage = msg;
        /// @}
        // If there's no attachment, we have nothing to do.
        if (!msg.hasAttachment()) {
            return false;
        }

        // Get the slideshow from the message.
        mSlideshow = msg.getSlideshow();
        try {
            /// M: for vcard: file attachment view and other views are exclusive to each other
            if (mSlideshow.sizeOfFilesAttach() > 0) {
                mFileAttachmentView = createFileAttachmentView(msg);
                if (mFileAttachmentView != null) {
                    mFileAttachmentView.setVisibility(View.VISIBLE);
                }
            }
            //add for attachment enhance
            if (mSlideshow.size() == 0) {
                //It only has attachment but not slide
                return true;
            }
            mView = createView(msg);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if ((mPresenter == null) || !mSlideshow.equals(mPresenter.getModel())) {
            mPresenter = PresenterFactory.getPresenter(
                    "MmsThumbnailPresenter", mContext, mView, mSlideshow);
        } else {
            mPresenter.setView(mView);
        }

        if ((mPresenter != null) && mSlideshow.size() > 1) {
            mPresenter.present(null);
        } else if (mSlideshow.size() == 1) {
            SlideModel sm = mSlideshow.get(0);
            if ((mPresenter != null) && (sm != null) && (sm.hasAudio() || sm.hasImage() || sm.hasVideo())) {
                mPresenter.present(null);
            }
        }
        return true;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setCanSend(boolean enable) {
        if (mCanSend != enable) {
            mCanSend = enable;
            updateSendButton();
        }
    }

    private void updateSendButton() {
        if (null != mSendButton) {
            mSendButton.setEnabled(mCanSend);
            mSendButton.setFocusable(mCanSend);
        }
    }

    public void hideView() {
        if (mView != null) {
            ((View)mView).setVisibility(View.GONE);
        }
        /// M: add for vcard
        if (mFileAttachmentView != null) {
            mFileAttachmentView.setVisibility(View.GONE);
        }
    }

    private View getStubView(int stubId, int viewId) {
        View view = findViewById(viewId);
        if (view == null) {
            ViewStub stub = (ViewStub) findViewById(stubId);
            view = stub.inflate();
        }
        return view;
    }

    private class MessageOnClick implements OnClickListener {
        private int mWhat;

        public MessageOnClick(int what) {
            mWhat = what;
        }

        public void onClick(View v) {
            Message msg = Message.obtain(mHandler, mWhat);
            msg.sendToTarget();
        }
    }
    /// m: @{
    //    private SlideViewInterface createView() {
    private SlideViewInterface createView(WorkingMessage msg) {
    /// @}
        boolean inPortrait = inPortraitMode();
        if (mSlideshow.size() > 1) {
            /// m: @{
            // return createSlideshowView(inPortrait);
            return createSlideshowView(inPortrait, msg);
            /// @}
        }

        /// M: @{

        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        final int NOT_OP01 = 0;
        final int IS_OP01 = 1;
        int flag = NOT_OP01; // 0 means not OP01, 1 means OP01

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                flag = IS_OP01;
            }
        }

         ///@}
        SlideModel slide = mSlideshow.get(0);
        /// M: before using SlideModel's function,we should make sure it is
        // null or not
        if (null == slide) {
            throw new IllegalArgumentException();
        }
        if (slide.hasImage()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.image_attachment_view_stub,
                    R.id.image_attachment_view,
                    R.id.view_image_button, R.id.replace_image_button, R.id.remove_image_button,
                /// m: @{
                // MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
            }else {
                //OP01
                return createMediaView(
                            R.id.image_attachment_view_stub,
                            R.id.image_attachment_view,
                            R.id.view_image_button, R.id.replace_image_button, R.id.remove_image_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_VIEW_IMAGE, MSG_REPLACE_IMAGE, MSG_REMOVE_SLIDES_ATTACHMENT, msg);
            } 
        } else if (slide.hasVideo()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.video_attachment_view_stub,
                    R.id.video_attachment_view,
                    R.id.view_video_button, R.id.replace_video_button, R.id.remove_video_button,
                /// M: @{
                // MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
            }else  {
                //OP01
                return createMediaView(
                            R.id.video_attachment_view_stub,
                            R.id.video_attachment_view,
                            R.id.view_video_button, R.id.replace_video_button, R.id.remove_video_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_PLAY_VIDEO, MSG_REPLACE_VIDEO, MSG_REMOVE_SLIDES_ATTACHMENT, msg);	
            }
        } else if (slide.hasAudio()) {
            if (flag == NOT_OP01) {
            return createMediaView(
                    R.id.audio_attachment_view_stub,
                    R.id.audio_attachment_view,
                    R.id.play_audio_button, R.id.replace_audio_button, R.id.remove_audio_button,
                /// M: @{
                // MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_ATTACHMENT);
                    R.id.media_size_info, msg.getCurrentMessageSize(),
                        MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_ATTACHMENT, msg);
                /// @}
        } else {
                //OP01
                return createMediaView(
                            R.id.audio_attachment_view_stub,
                            R.id.audio_attachment_view,
                            R.id.play_audio_button, R.id.replace_audio_button, R.id.remove_audio_button,
                            R.id.media_size_info, msg.getCurrentMessageSize(),
                            MSG_PLAY_AUDIO, MSG_REPLACE_AUDIO, MSG_REMOVE_SLIDES_ATTACHMENT, msg);	
            }
        } else {
            throw new IllegalArgumentException();
        }
    }


    /**
     * What is the current orientation?
     */
    private boolean inPortraitMode() {
        final Configuration configuration = mContext.getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private SlideViewInterface createMediaView(
            int stub_view_id, int real_view_id,
            int view_button_id, int replace_button_id, int remove_button_id,
            /// M: @{
            // int viewMessage, int replaceMessage, int removeMessage) {
            int sizeViewId, int msgSize,
            int viewMessage, int replaceMessage, int removeMessage, WorkingMessage msg) {
            /// @}
        LinearLayout view = (LinearLayout)getStubView(stub_view_id, real_view_id);
        view.setVisibility(View.VISIBLE);

        Button viewButton = (Button) view.findViewById(view_button_id);
        Button replaceButton = (Button) view.findViewById(replace_button_id);
        Button removeButton = (Button) view.findViewById(remove_button_id);

        /// M: @{
        /// M: show Mms Size  
        mMediaSize = (TextView) view.findViewById(sizeViewId); 
        int sizeShow = (msgSize - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info); 
        /// @}

        viewButton.setOnClickListener(new MessageOnClick(viewMessage));
        replaceButton.setOnClickListener(new MessageOnClick(replaceMessage));
        removeButton.setOnClickListener(new MessageOnClick(removeMessage));

        /// M: @{
        if (mFlagMini) {
            replaceButton.setVisibility(View.GONE);
        }
        /// @}
        return (SlideViewInterface) view;
    }

    /// M: @{
    // private SlideViewInterface createSlideshowView(boolean inPortrait) {
    private SlideViewInterface createSlideshowView(boolean inPortrait, WorkingMessage msg) {
    /// @}
        LinearLayout view =(LinearLayout) getStubView(
                R.id.slideshow_attachment_view_stub,
                R.id.slideshow_attachment_view);
        view.setVisibility(View.VISIBLE);

        Button editBtn = (Button) view.findViewById(R.id.edit_slideshow_button);
        mSendButton = (Button) view.findViewById(R.id.send_slideshow_button);
       /// M: @{
        mSendButton.setOnClickListener(new MessageOnClick(MSG_SEND_SLIDESHOW));
        /// @}

        updateSendButton();
        final ImageButton playBtn = (ImageButton) view.findViewById(
                R.id.play_slideshow_button);
        /// M: @{
        if (EncapsulatedFeatureOption.MTK_DRM_APP) {
                if (msg.mHasDrmPart) {
                    MmsLog.i(TAG, "mHasDrmPart");
                    Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.mms_play_btn);        
                    Drawable front = mContext.getResources().getDrawable(EncapsulatedR.drawable.drm_red_lock);
                    EncapsulatedDrmManagerClient drmManager = new EncapsulatedDrmManagerClient(mContext);
                    Bitmap drmBitmap = OmaDrmUiUtils.overlayBitmap(drmManager, bitmap, front);
                    playBtn.setImageBitmap(drmBitmap);
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        bitmap = null;
                    }
                }
        }

        /// M: show Mms Size  
        mMediaSize = (TextView) view.findViewById(R.id.media_size_info); 
               int sizeShow = (msg.getCurrentMessageSize() - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        
        mMediaSize.setText(info);
        /// @}

        editBtn.setEnabled(true);
        editBtn.setOnClickListener(new MessageOnClick(MSG_EDIT_SLIDESHOW));
        mSendButton.setOnClickListener(new MessageOnClick(MSG_SEND_SLIDESHOW));
        playBtn.setOnClickListener(new MessageOnClick(MSG_PLAY_SLIDESHOW));

        Button removeButton = (Button) view.findViewById(R.id.remove_slideshow_button);
        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //OP01
                removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_SLIDES_ATTACHMENT));
            } else{
                //not OP01
                removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
            }
        }else {
            //common
        removeButton.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
        }

        return (SlideViewInterface) view;
    }

    /// M: 
    private WorkingMessage mWorkingMessage;
    private TextView mMediaSize;
    private boolean mFlagMini = false;

    public void update(WorkingMessage msg, boolean isMini) {
        mFlagMini = isMini;
        update(msg);
    }

    public void onTextChangeForOneSlide(CharSequence s) throws ExceedMessageSizeException {

        if (null == mMediaSize || (mWorkingMessage.hasSlideshow() && mWorkingMessage.getSlideshow().size() > 1)) {
            return;
        }

        /// M: borrow this method to get the encoding type
        int[] params = SmsMessage.calculateLength(s, false);
        int totalSize = 0;
        if (mWorkingMessage.hasAttachment()) {
            totalSize = mWorkingMessage.getCurrentMessageSize();
        }
        /// M: show
        int sizeShow = (totalSize - 1) / 1024 + 1;
        String info = sizeShow + "K/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K";
        mMediaSize.setText(info);
    }
    /// @}

    /// M: add for vcard
    private View createFileAttachmentView(WorkingMessage msg) {
        List<FileAttachmentModel> attachFiles = mSlideshow.getAttachFiles();
/// M: @{
        //add for attachment enhance

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        if (attachFiles == null) {
            Log.e(TAG, "createFileAttachmentView, oops no attach files found.");
            return null;
        } else {
            if (mMmsAttachmentEnhancePlugin != null) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == false) {
                    //NOT for OP01
                    if (attachFiles.size() != 1) {
                        return null;
                    }
                }
            } else {
                if (attachFiles.size() != 1) {
                    return null;
        }
            }
        }
/// @}
        FileAttachmentModel attach = attachFiles.get(0);
        Log.i(TAG, "createFileAttachmentView, attach " + attach.toString());
        final View view = getStubView(R.id.file_attachment_view_stub, R.id.file_attachment_view);
        view.setVisibility(View.VISIBLE);
        final ImageView thumb = (ImageView) view.findViewById(R.id.file_attachment_thumbnail);
        final TextView name = (TextView) view.findViewById(R.id.file_attachment_name_info);
        String nameText = null;
        int thumbResId = -1;

        int attachSize = 0;
        //get external attachment size
        for (int i = 0; i < attachFiles.size(); i++) {
            attachSize += attachFiles.get(i).getAttachSize();
        }

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //Op01 plugin
    /// M: @{
                //add for attachment enhance
               if (attachFiles.size() > 1) {
                    //multi attachments files
                    MmsLog.i(TAG, "createFileAttachmentView, attachFiles.size() > 1");
                    nameText = mContext.getString(R.string.file_attachment_common_name, String.valueOf(attachFiles.size()));
                    thumbResId = R.drawable.multi_files;
                } else if (attachFiles.size() == 1){
                    //single attachment(file)
                    if (attach.isVCard()) {
                        // vCard
                        nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
                        thumbResId = R.drawable.ic_vcard_attach;
                    } else if (attach.isVCalendar()) {
                        // VCalender
                        nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
                        thumbResId = R.drawable.ic_vcalendar_attach;
                    } else {
                        // other attachment
                        nameText = attach.getSrc();
                        thumbResId = R.drawable.unsupported_file;
                    }
                }
    /// @}
            }else {
            //common
        if (attach.isVCard()) {
            nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcard_attach;
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcalendar_attach;
        }
            }
        }else {
            //common
        if (attach.isVCard()) {
            nameText = mContext.getString(R.string.file_attachment_vcard_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcard_attach;
        } else if (attach.isVCalendar()) {
            nameText = mContext.getString(R.string.file_attachment_vcalendar_name, attach.getSrc());
            thumbResId = R.drawable.ic_vcalendar_attach;
        }
        }
        name.setText(nameText);
        thumb.setImageResource(thumbResId);
        final TextView size = (TextView) view.findViewById(R.id.file_attachment_size_info);
        if (mMmsAttachmentEnhancePlugin != null &&
            mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
            //OP01
            size.setText(MessageUtils.getHumanReadableSize(attachSize));
        } else {
            //Not OP01
        size.setText(MessageUtils.getHumanReadableSize(attach.getAttachSize())
                + "/" + MmsConfig.getUserSetMmsSizeLimit(false) + "K");
        }

        final ImageView remove = (ImageView) view.findViewById(R.id.file_attachment_button_remove);
        final ImageView divider = (ImageView) view.findViewById(R.id.file_attachment_divider);
        divider.setVisibility(View.VISIBLE);
        remove.setVisibility(View.VISIBLE);
        //add for attachment enhance
        //IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE); 

        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                //OP01
                remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_EXTERNAL_ATTACHMENT));
            }else {
                //not OP01
                remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
            }
        }else {
            //not OP01
            remove.setOnClickListener(new MessageOnClick(MSG_REMOVE_ATTACHMENT));
        }
        return view;
    }
}
