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

package com.android.mms.model;


import com.android.mms.ContentRestrictionException;
import com.android.mms.ExceedMessageSizeException;
import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.dom.smil.parser.SmilXmlSerializer;
import com.android.mms.layout.LayoutManager;
import com.mediatek.encapsulation.com.google.android.mms.EncapsulatedContentType;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.ContentType;

//add for attachment enhance
//import packages
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;
import com.android.mms.MmsPluginManager;
import com.android.mms.model.FileModel;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.smil.SMILDocument;
import org.w3c.dom.smil.SMILElement;
import org.w3c.dom.smil.SMILLayoutElement;
import org.w3c.dom.smil.SMILMediaElement;
import org.w3c.dom.smil.SMILParElement;
import org.w3c.dom.smil.SMILRegionElement;
import org.w3c.dom.smil.SMILRootLayoutElement;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/// M: Code analyze 001, new feature, import some useful classes @{
import android.view.Display;
import android.view.WindowManager;
import android.content.ContentResolver;
import android.content.res.Configuration;
import com.mediatek.encapsulation.android.drm.EncapsulatedDrmManagerClient;
import android.drm.mobile1.DrmException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import com.android.mms.UnsupportContentTypeException;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;


import com.mediatek.encapsulation.MmsLog;
/// @}

public class SlideshowModel extends Model
        implements List<SlideModel>, IModelChangedObserver {
    private static final String TAG = "Mms/slideshow";

    private final LayoutModel mLayout;
    private final ArrayList<SlideModel> mSlides;
    private SMILDocument mDocumentCache;
    private PduBody mPduBodyCache;
    /// M: Code analyze 002, new feature, we abandon this two members @{
    private int mCurrentMessageSize;    // This is the current message size, not including
                                        // attachments that can be resized (such as photos)
    private int mTotalMessageSize;      // This is the computed total message size
    /// @}
    private Context mContext;

    // amount of space to leave in a slideshow for text and overhead.
    public static final int SLIDESHOW_SLOP = 1024;
    
    /// M: Code analyze 002, new feature, CurrentSlideshow Size include all attachment size @{
    private static final int MMS_HEADER_SIZE                  = 128;
    private static final int MMS_CONTENT_TYPE_HEAER_LENGTH    = 128; 
    private static final int SMILE_HEADER_SIZE                = 128;
    private int mCurrentSlideshowSize = MMS_HEADER_SIZE + MMS_CONTENT_TYPE_HEAER_LENGTH 
                                        + SMILE_HEADER_SIZE + SLIDESHOW_SLOP;
    /// @}

    /// M: Code analyze 003, fix bug ALPS00261194, let recorder auto stop when the messaging reach limit @{
    public static final int MMS_SLIDESHOW_INIT_SIZE = MMS_HEADER_SIZE + MMS_CONTENT_TYPE_HEAER_LENGTH
        + SMILE_HEADER_SIZE + SLIDESHOW_SLOP + MediaModel.SMILE_TAG_SIZE_ATTACH + MediaModel.SMILE_TAG_SIZE_PAGE
        + MediaModel.SMILE_TAG_SIZE_IMAGE;
    /// @}

    /// M: Code Analyze 004, fix bug ALPS00065646,
    /// Make the size of message when attachment size (299,300k) @{
    public static final int mReserveSize = MMS_HEADER_SIZE + MMS_CONTENT_TYPE_HEAER_LENGTH
                                            + SMILE_HEADER_SIZE + SLIDESHOW_SLOP;
    /// @}

    public static final String VCARD = "BEGIN:VCARD";
    public static final String VCALENDAR = "BEGIN:VCALENDAR";
    public static final String VCARD_DESCRIPTION = ".vcf";
    public static final String VCALENDAR_DESCRIPTION = ".vcs";

    /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
    private final ArrayList<FileAttachmentModel> mAttachFiles;
    /// @}

    /// M: Code analyze 006, new feature, Drm support @{
    private static boolean mHasDrmContent;
    private static boolean mHasDrmRight;
    /// @}

    /// M: Code analyze 007, new feature, par.dur set 5 sec if smil par.dur==0 @{
    private static final float DEFAULT_DUR_SEC = 5.0F;
    /// @}

    private SlideshowModel(Context context) {
        mLayout = new LayoutModel();
        mSlides = new ArrayList<SlideModel>();
        /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
        mAttachFiles = new ArrayList<FileAttachmentModel>();
        /// @}
        mContext = context;
    }

    private SlideshowModel (
            LayoutModel layouts, ArrayList<SlideModel> slides,
            /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
            ArrayList<FileAttachmentModel> attachFiles,
            SMILDocument documentCache, PduBody pbCache,
            Context context) {
        /// M:
        MmsLog.d(TAG, "SlideshowModel.init");
        mLayout = layouts;
        mSlides = slides;
        /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
        mAttachFiles = attachFiles;
        /// @}
        mContext = context;
        mDocumentCache = documentCache;
        mPduBodyCache = pbCache;
        for (SlideModel slide : mSlides) {
            /// M: Code analyze 002, new feature
            increaseSlideshowSize(slide.getSlideSize());
            slide.setParent(this);
        }
        /// M: External attachment size should be add to the whole size
        //add for attachment enhance
        int attachSize = 0;

        for (int i = 0; i < attachFiles.size(); i++) {
            attachSize += attachFiles.get(i).getAttachSize();
        }
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                increaseSlideshowSize(attachSize);
            }
        }
        /// M: Code analyze 008, fix bug ALPS00339457, init textLayout type when create @{
        initLayoutType();
        /// @}
    }

    public static SlideshowModel createNew(Context context) {
        return new SlideshowModel(context);
    }

    public static SlideshowModel createFromMessageUri(
            Context context, Uri uri) throws MmsException {
        return createFromPduBody(context, getPduBody(context, uri));
    }

    public static SlideshowModel createFromPduBody(Context context, PduBody pb) throws MmsException {
        /// M: Code analyze 006, new feature, Drm support @{
        int partNum = pb.getPartsNum();
        mHasDrmContent = false;
        for(int i = 0; i < partNum; i++) {
            PduPart part = pb.getPart(i);
            byte[] cl = part.getContentLocation();
            String path = null ;
            if (cl != null) {
            	path = new String(cl);
            }
            MmsLog.i(TAG, "SlideshowModel path:" + path);
            if (EncapsulatedFeatureOption.MTK_DRM_APP) {
                if (path != null) {
                    String extName = path.substring(path.lastIndexOf('.') + 1);
                    if (extName.equals("dcf")) {
                	    MmsLog.i(TAG, "SlideshowModel Has DrmContent") ;
                	    mHasDrmContent = true;
                    }
                }
            }
        }
        /// @}

        SMILDocument document = SmilHelper.getDocument(context, pb);

        // Create root-layout model.
        SMILLayoutElement sle = document.getLayout();
        SMILRootLayoutElement srle = sle.getRootLayout();
        /// M: Code analyze 009, new feature, w/h must adjust according to orientation @{
        // int w = srle.getWidth();
        // int h = srle.getHeight();
        WindowManager windowM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Configuration config = context.getResources().getConfiguration();
        Display defDisplay = windowM.getDefaultDisplay();
        int w = 0;
        int h = 0;
        if( config.orientation == Configuration.ORIENTATION_PORTRAIT){
            w = defDisplay.getWidth();
            h = defDisplay.getHeight();
        }else{
            w = defDisplay.getHeight();
            h = defDisplay.getWidth();
        }
        /// @}
        if ((w == 0) || (h == 0)) {
            w = LayoutManager.getInstance().getLayoutParameters().getWidth();
            h = LayoutManager.getInstance().getLayoutParameters().getHeight();
            srle.setWidth(w);
            srle.setHeight(h);
        }
        RegionModel rootLayout = new RegionModel(
                null, 0, 0, w, h);

        // Create region models.
        ArrayList<RegionModel> regions = new ArrayList<RegionModel>();
        NodeList nlRegions = sle.getRegions();
        int regionsNum = nlRegions.getLength();
        /// M: Code analyze 010, fix bug ALPS00267318,
        /// resolve mms slideshow text layout problem(unknown) @{
        SMILRegionElement[] smils = new SMILRegionElement[regionsNum];
        for (int i = 0;i<regionsNum;i++) {
            /// M: change google default. @{
            smils[i] = (SMILRegionElement) nlRegions.item(i);
        }
        Arrays.sort(smils,new Comparator<SMILRegionElement>(){

            public int compare(SMILRegionElement object1,
                    SMILRegionElement object2) {
                if (object1.getTop() < object2.getTop()) {
                    return -1;
                } else if(object1.getTop() > object2.getTop()) {
                    return 1;
                }
                return 0;
            }});
        int itemHeight = 0;
        if (regionsNum != 0) {
            itemHeight = h / regionsNum;
        }
        MmsLog.d(TAG, "SlideshowModel.createFromPduBody(): RootLayout.Width=" + srle.getWidth()
                + ", RootLayout.Height=" + srle.getHeight() + ", Windows.w=" + w + ", Windows.h=" + h);
        /// @}
        
        for (int i = 0; i < regionsNum; i++) {
            /// M: Code analyze 010, fix bug ALPS00267318,
            /// resolve mms slideshow text layout problem(unknown) @{
            SMILRegionElement sre = smils[i];
            RegionModel r;
            if (regionsNum == 1) {
    	    	r = new RegionModel(sre.getId(), sre.getFit(), 0, sre.getTop(), w, itemHeight, sre.getBackgroundColor());
    	    } else {
                /// M: Code analyze 011, fix bug ALPS00273298, handle Arithmetic Exception @{
    	        int left = 0;
    	        int width = 0;
    	        int top = 0;
    	        int height = 0;
    	        if (srle.getWidth() != 0) {
                    left = (defDisplay.getWidth() * sre.getLeft()) / srle.getWidth();
                    width = (defDisplay.getWidth() * sre.getWidth()) / srle.getWidth();
    	        } else {
    	            left = 0;
    	            width = w;
    	        }
    	        if (srle.getHeight() != 0) {
                    top = (defDisplay.getHeight() * sre.getTop()) / srle.getHeight();
                    height = (defDisplay.getHeight() * sre.getHeight()) / srle.getHeight();
    	        } else {
    	            top = sre.getTop();
    	            height = itemHeight;
    	        }
                r = new RegionModel(sre.getId(), sre.getFit(), left, top, width, height, sre.getBackgroundColor());
                MmsLog.d(TAG, "SlideshowModel.createFromPduBody(): " + r.toString());
                /// @}
            }
            /// @}
            
            regions.add(r);
        }
        LayoutModel layouts = new LayoutModel(rootLayout, regions);

        // Create slide models.
        SMILElement docBody = document.getBody();
        NodeList slideNodes = docBody.getChildNodes();
        int slidesNum = slideNodes.getLength();
        ArrayList<SlideModel> slides = new ArrayList<SlideModel>(slidesNum);
        /// M:
        MmsLog.i(TAG, "SlideshowModel slidesNum:" + slidesNum);
        for (int i = 0; i < slidesNum; i++) {           
            // FIXME: This is NOT compatible with the SMILDocument which is
            // generated by some other mobile phones.
            /// M: Code analyze 012, new feature, catch ClassCastException @{
            SMILParElement par = null;
            try { 
                par = (SMILParElement) slideNodes.item(i);
            } catch (ClassCastException cce) {
                MmsLog.e(TAG, cce.getMessage());
                continue;
            }
            /// @}

            // Create media models for each slide.
            NodeList mediaNodes = par.getChildNodes();
            int mediaNum = mediaNodes.getLength();
            ArrayList<MediaModel> mediaSet = new ArrayList<MediaModel>(mediaNum);

            for (int j = 0; j < mediaNum; j++) {
                /// M: Code analyze 012, new feature, catch ClassCastException @{
                SMILMediaElement sme = null;
                try { 
                    sme = (SMILMediaElement) mediaNodes.item(j);
                } catch (ClassCastException cce) {
                    MmsLog.e(TAG, cce.getMessage());
                    continue;
                }
                /// @}
                try {
                    MediaModel media = MediaModelFactory.getMediaModel(
                            context, sme, layouts, pb);

                    /*
                    * This is for slide duration value set.
                    * If mms server does not support slide duration.
                    */
                    /// M: Code analyze 013, fix bug ALPS00116755,
                    /// change the duration time according to the smil par duration time @{
                    MmsLog.i(TAG, "MmsConfig.getSlideDurationEnabled(): " + MmsConfig.getSlideDurationEnabled());
                    if (MmsConfig.getSlideDurationEnabled()) {
                        int mediadur = media.getDuration();
                        /// M:
                        MmsLog.i(TAG, "media.getDuration(): " + media.getDuration());
                        float dur = par.getDur();
                        MmsLog.i(TAG, "dur = par.getDur():" + dur);
                        if (dur == 0) {
                            mediadur = MmsConfig.getMinimumSlideElementDuration() * 1000;
                            media.setDuration(mediadur);
                        }

                        if ((int)mediadur / 1000 != dur) {
                        	/// M:
                        	MmsLog.i(TAG, "mediadur / 1000 != dur");
                            String tag = sme.getTagName();

                            if (EncapsulatedContentType.isVideoType(media.mContentType)
                              || tag.equals(SmilHelper.ELEMENT_TAG_VIDEO)
                              || EncapsulatedContentType.isAudioType(media.mContentType)
                              || tag.equals(SmilHelper.ELEMENT_TAG_AUDIO)) {
                                /// M: Code analyze 014, fix bug ALPS00111711,
                                /// The duration time shouldn't add one second by default @{
                                /// M: google default. if need according media file duration , use like this :
                                // par.setDur((float)mediadur / 1000);

                                /// M: if need according slide setting duration , use like this:
                                media.setDuration((int)dur * 1000);
                                MmsLog.i(TAG, "par.setDur:" + dur);
                                /// @}
                            } else {
                                /*
                                * If a slide has an image and an audio/video element
                                * and the audio/video element has longer duration than the image,
                                * The Image disappear before the slide play done. so have to match
                                * an image duration to the slide duration.
                                */
                                if ((int)mediadur / 1000 < dur) {
                                    media.setDuration((int)dur * 1000);
                                    /// M:
                                    MmsLog.i(TAG, "media.setDuration:" + (int)dur * 1000);
                                } else {
                                    if ((int)dur != 0) {
                                        media.setDuration((int)dur * 1000);
                                        /// M:
                                        MmsLog.i(TAG, "media.setDuration:" + (int)dur * 1000);
                                    } else {
                                        par.setDur((float)mediadur / 1000);
                                        /// M:
                                        MmsLog.i(TAG, "media.setDuration:" + (float)mediadur / 1000);
                                    }
                                }
                            }
                        }
                    }
                    SmilHelper.addMediaElementEventListeners(
                            (EventTarget) sme, media);
                    mediaSet.add(media);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, e.getMessage(), e);
                } 
                /// M: @{
                catch (UnsupportContentTypeException e) {
                    MmsLog.e(TAG, e.getMessage(), e);
                    continue;
                }
                }
            /// M: Code analyze 007, new feature, par.dur set 5 sec if smil par.dur==0 @{
            try {
                float durSec = par.getDur();
                if (durSec <= 0) {
                    par.setDur(DEFAULT_DUR_SEC);
            }
            /// @}

            SlideModel slide = new SlideModel((int) (par.getDur() * 1000), mediaSet);
            slide.setFill(par.getFill());
            SmilHelper.addParElementEventListeners((EventTarget) par, slide);
            slides.add(slide);
            /// M: set page index for each model.
            slide.setCurrentPage(slides.size());
            }
            /// M: @{
            catch (ClassCastException cce) {
                MmsLog.e(TAG, cce.getMessage());
            }
            /// @}
        }
        /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
        ArrayList<FileAttachmentModel> attachFiles = new ArrayList<FileAttachmentModel>();
        for (int i = 0; i < partNum; i++) {
            /// M: Code analyze 015, fix bug ALPS00245377,
            /// Need check the content_id content_name content_filename not only location @{
            PduPart part = pb.getPart(i);
            byte[] cl = part.getContentLocation();
            byte[] name = part.getName();
            byte[] ci = part.getContentId();
            byte[] fn = part.getFilename();
            byte[] data = part.getData();
            String filename = null;
            if (cl != null) {
            	filename = new String(cl);
            } else if (name != null){
            	filename = new String(name);
            } else if (ci != null){
            	filename = new String(ci);
            } else if (fn != null){
            	filename = new String(fn);
            } else {
            	continue;
            }
            /// @}
            /// M: For attachment enhance

           //add for attachment enhance

            String PartUri = null;
            if (part.getDataUri() != null) {
                    MmsLog.e(TAG, "YF: part Uri = " + part.getDataUri().toString());
                    PartUri = part.getDataUri().toString();
            }
           /// @}

            final String type = new String(part.getContentType());
            /// M: Code analyze 016, fix bug ALPS00107176,
            /// incompatible MMSCs change vCard ContentType etc, use TimeStamp as FileName @{
            if (FileAttachmentModel.isVCard(part)) {
                FileAttachmentModel fam = new VCardModel(context, EncapsulatedContentType.TEXT_VCARD,
                        filename, part.getDataUri());
                attachFiles.add(fam);
            /// M: Code analyze 017, new feature(ALPS00249336), add vCalendar support @{
            } else if (FileAttachmentModel.isVCalendar(part)) {
                FileAttachmentModel fam = new VCalendarModel(context, EncapsulatedContentType.TEXT_VCALENDAR,
                        filename, part.getDataUri());
                attachFiles.add(fam);
            } else if (data != null && ( (new String(data)).startsWith(VCARD)
                      || (new String(data)).startsWith(VCALENDAR)) && !FileAttachmentModel.isTxtType(part)) {
                String dataContent = new String(data);
                if (dataContent.startsWith(VCARD)) {
                    if (filename == null) {
                        filename = "vcf";
                    }
                    FileAttachmentModel fam = new VCardModel(context,
                            ContentType.TEXT_VCARD, filename+VCARD_DESCRIPTION,
                            part.getDataUri());
                    attachFiles.add(fam);
                } else if (dataContent.startsWith(VCALENDAR)) {
                    if (filename == null) {
                        filename = "vcs";
                    }
                    FileAttachmentModel fam = new VCalendarModel(context,
                            ContentType.TEXT_VCALENDAR, filename+VCALENDAR_DESCRIPTION,
                            part.getDataUri());
                    attachFiles.add(fam);
                }
            } else {
  /// M: @{

            //add for attachment enhance
             IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

             Log.w(TAG, "In add slideshow attach mMmsAttachmentEnhancePlugin = "+ mMmsAttachmentEnhancePlugin);
 
  /// @}
            if (mMmsAttachmentEnhancePlugin != null) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                    Log.w(TAG, "In add slideshow attach");
                    Log.w(TAG, "isOtherAttachment(part) = " + FileAttachmentModel.isOtherAttachment(part));
                   // Log.w(TAG, "isInSmil(filename,slides) = " + isInSmil(filename,slides));
                    if (EncapsulatedContentType.isImageType(new String(part.getContentType())) && !isInSmil(filename,slides,PartUri)) { //support image attachment
                        FileAttachmentModel fam = new FileModel(context, new String(part.getContentType()), filename, part.getDataUri());
                        attachFiles.add(fam);
                        Log.w(TAG, "In add attachment: Add image");
                    } else if (EncapsulatedContentType.isVideoType(new String(part.getContentType())) && !isInSmil(filename,slides,PartUri)) {
                        FileAttachmentModel fam = new FileModel(context, new String(part.getContentType()),
                        filename, part.getDataUri());
                        attachFiles.add(fam);
                        Log.w(TAG, "In add attachment: add video" );
                    } else if (EncapsulatedContentType.isAudioType(new String(part.getContentType()))&& !isInSmil(filename,slides,PartUri)) {
                        FileAttachmentModel fam = new FileModel(context, new String(part.getContentType()),
                        filename, part.getDataUri());
                        attachFiles.add(fam);
                        Log.w(TAG, "In add attachment: add audio");
                    } else if (FileAttachmentModel.isOtherAttachment(part) && !isInSmil(filename,slides,PartUri)){
                        FileAttachmentModel fam = new FileModel(context, new String(part.getContentType()),
                        filename, part.getDataUri());
                        attachFiles.add(fam);
                        Log.w(TAG, "In add attachment: Add flie");
                    } else {
                        String cid = null;

                        if (ci != null)
                            cid = new String(ci); //get cid

                        if (FileAttachmentModel.isTxtType(part) && isInSmil(filename,cid,slides) == false){
                        FileAttachmentModel fam = new FileModel(context, EncapsulatedContentType.TEXT_PLAIN,
                            filename, part.getData());
                        attachFiles.add(fam);
                        } else if (FileAttachmentModel.isHtmType(part) && isInSmil(filename,cid,slides) == false){
                        FileAttachmentModel fam = new FileModel(context, EncapsulatedContentType.TEXT_HTML,
                            filename, part.getData());
                        attachFiles.add(fam);
                    } 
                }
            }
          }
        }
        }
        /// @}

        /// M:
        SlideshowModel slideshow = new SlideshowModel(layouts, slides, attachFiles, document, pb, context);
        slideshow.registerModelChangedObserver(slideshow);
        return slideshow;
    }

/// M: @{

        //add for attachment enhance
        /* This function is for justify if this attachment is in slides */
  private static boolean isInSmil(String filename, ArrayList<SlideModel> slides, String PartUri) {
         for (SlideModel slide : slides) {
               for (int j=0; j<slide.size(); j++) {
                   MmsLog.e(TAG, "filename_1 = " + filename);
                   //MmsLog.e(TAG, "filename_2 = " + slide.get(j).getSrc());
                   if (slide.get(j).getUri() != null && PartUri != null) {
                       MmsLog.e(TAG, "slide model Uri = " + slide.get(j).getUri().toString());
                       if (slide.get(j).getUri().toString().compareTo(PartUri) == 0) {
                           return true;
                       }
                   }
                    //Modify for consistence test case
                    if (filename != null && filename.compareTo(slide.get(j).getSrc()) == 0) {
                        MmsLog.e(TAG, "This media is in smil");
                        return true;
                    }
               }
           }
         MmsLog.e(TAG, "This media is NOT in smil !! ");
         return false;
     }

    /* This function is for justify if the text file is in slides */
    /*
    private static boolean isInSmil(String filename, ArrayList<SlideModel> slides) {
        for (SlideModel slide : slides) {
            for (int j=0; j<slide.size(); j++) {
               if (filename.compareTo(slide.get(j).getSrc()) == 0) {
                    MmsLog.e(TAG, "This media is in smil");
                    return true;
                }
            }
        }
        MmsLog.e(TAG, "This media is NOT in smil !! ");
        return false;
    }*/

      //Justify if the text or html is in slide or not
    private static boolean isInSmil(String filename, String cid, ArrayList<SlideModel> slides) {
        for (SlideModel slide : slides) {
            for (int j=0; j<slide.size(); j++) {
               MmsLog.e(TAG, "text html filename_1 = " + filename);
               //MmsLog.e(TAG, "text html filename_2 = " + slide.get(j).getSrc());

                //The src in MediaModel is content location or filename
            if (filename != null && slide.get(j) != null) {
               if (filename.compareTo(slide.get(j).getSrc()) == 0) {
                    MmsLog.e(TAG, "This media is in smil (1)");
                    return true;
               }
             }

               //The src in MediaModel is content id
               String cidSrc_1 = null;
               if (slide.get(j) != null) {
                   cidSrc_1 = unescapeXML(slide.get(j).getSrc()); //src in MediaModel
               }
               String cidSrc_2 =  null;
               if (cidSrc_1 != null && cid != null) {
                   if (cidSrc_1.startsWith("cid:")) {
                       cidSrc_2 = "<" + cidSrc_1.substring("cid:".length()) + ">" ; //<XXX>

                       if (cidSrc_2.compareTo(cid) == 0) {
                          MmsLog.e(TAG, "This media is in smil (2)");
                          return true;
                       }else {
                           cidSrc_2 = "<" + cidSrc_1 + ">"  ;        //<cid:XXX>
                           if (cidSrc_2.compareTo(cid) == 0) {
                                MmsLog.e(TAG, "This media is in smil (3)");
                                return true;
                            }
                       }
                   }
               }
            }
        }
        MmsLog.e(TAG, "This media is NOT in smil !! ");
        return false;
    }
      private static String unescapeXML(String str) {
          return str.replaceAll("&lt;","<")
              .replaceAll("&gt;", ">")
              .replaceAll("&quot;","\"")
              .replaceAll("&apos;","'")
              .replaceAll("&amp;", "&");
      }
/// @}


    public PduBody toPduBody() {
        if (mPduBodyCache == null) {
            mDocumentCache = SmilHelper.getDocument(this);
            mPduBodyCache = makePduBody(mDocumentCache);
        }
        return mPduBodyCache;
    }

    private PduBody makePduBody(SMILDocument document) {
        PduBody pb = new PduBody();

        boolean hasForwardLock = false;
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                PduPart part = new PduPart();

                if (media.isText()) {
                    TextModel text = (TextModel) media;
                    // Don't create empty text part.
                    if (TextUtils.isEmpty(text.getText())) {
                        continue;
                    }
                    // Set Charset if it's a text media.
                    part.setCharset(text.getCharset());
                }

                // Set Content-Type.
                part.setContentType(media.getContentType().getBytes());

                String src = media.getSrc();
                String location;
                boolean startWithContentId = src.startsWith("cid:");
                if (startWithContentId) {
                    location = src.substring("cid:".length());
                } else {
                    location = src;
                }

                // Set Content-Location.
                part.setContentLocation(location.getBytes());
    /// M: @{
              //add for attachment enhance

                IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
                  (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
 
                if (mMmsAttachmentEnhancePlugin != null) {
                      if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                          // Set part name and filename
                          part.setName(location.getBytes());
                          part.setFilename(location.getBytes());
                      }
                }
  ///@}

                // Set Content-Id.
                if (startWithContentId) {
                    //Keep the original Content-Id.
                    part.setContentId(location.getBytes());
                }
                else {
                    int index = location.lastIndexOf(".");
                    String contentId = (index == -1) ? location
                            : location.substring(0, index);
                    part.setContentId(contentId.getBytes());
                }

                if (media.isText()) {
                    /// M: Code Analyze 018, fix bug ALPS00074082,
                    /// set correct charset (not default UTF-8) when persister and load pduPart @{
                	try {
                        String charsetName = CharacterSets.getMimeName(part.getCharset());
                        part.setData(((TextModel) media).getText().getBytes(charsetName));
                    } catch (UnsupportedEncodingException e) {
                        MmsLog.e(TAG, "Unsupported encoding: ", e);
                        part.setData(((TextModel) media).getText().getBytes());
                    }
                    /// @}
                } else if (media.isImage() || media.isVideo() || media.isAudio()) {
                    part.setDataUri(media.getUri());
                } else {
                    Log.w(TAG, "Unsupport media: " + media);
                }

                pb.addPart(part);
            }
        }

        // Create and insert SMIL part(as the first part) into the PduBody.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SmilXmlSerializer.serialize(document, out);
        PduPart smilPart = new PduPart();
        smilPart.setContentId("smil".getBytes());
        smilPart.setContentLocation("smil.xml".getBytes());
        smilPart.setContentType(EncapsulatedContentType.APP_SMIL.getBytes());
        smilPart.setData(out.toByteArray());
        pb.addPart(0, smilPart);

        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
        for (FileAttachmentModel fileAttachment : mAttachFiles) {
            PduPart part = new PduPart();

            /// M: Code analyze 019, fix bug ALPS00241756, Fix vCard Compatibility Issue @{
            part.setContentType(fileAttachment.getContentType().toLowerCase().getBytes());
            /// @}

            String src = fileAttachment.getSrc();
            String location;
            boolean startWithContentId = src.startsWith("cid:");
            if (startWithContentId) {
                location = src.substring("cid:".length());
            } else {
                location = src;
            }

            part.setContentLocation(location.getBytes());

            if (startWithContentId) {
                part.setContentId(location.getBytes());
            } else {
                int index = location.lastIndexOf(".");
                String contentId = (index == -1) ? location
                        : location.substring(0, index);
                part.setContentId(contentId.getBytes());
            }
            part.setName(fileAttachment.getSrc().getBytes());
            part.setFilename(fileAttachment.getSrc().getBytes());

            if (fileAttachment.isSupportedFile()) {
                part.setDataUri(fileAttachment.getUri());
            } else { // add for attachment enhance
                if (mMmsAttachmentEnhancePlugin != null) {
                    if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                        //OP01
                        if (fileAttachment.getContentType().equals("text/plain")
                            || fileAttachment.getContentType().equals("text/html")) {
                        part.setData(fileAttachment.getData());  //add for attachment enhance
                        }
                    }
            } else {
                MmsLog.w(TAG, "Unsupport file attachment: " + fileAttachment);
            }

            }/* else {
                MmsLog.w(TAG, "Unsupport file attachment: " + fileAttachment);
            }*/

            pb.addPart(part);
        }
        /// @}
        return pb;
    }

    public PduBody makeCopy() {
        return makePduBody(SmilHelper.getDocument(this));
    }

    public SMILDocument toSmilDocument() {
        if (mDocumentCache == null) {
            mDocumentCache = SmilHelper.getDocument(this);
        }
        return mDocumentCache;
    }

    public static PduBody getPduBody(Context context, Uri msg) throws MmsException {
        PduPersister p = PduPersister.getPduPersister(context);
        GenericPdu pdu = p.load(msg);

        int msgType = pdu.getMessageType();
        if ((msgType == PduHeaders.MESSAGE_TYPE_SEND_REQ)
                || (msgType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF)) {
            return ((MultimediaMessagePdu) pdu).getBody();
        } else {
            throw new MmsException();
        }
    }

    /// M: Code analyze 002, new feature
    public void setCurrentSlideshowSize(int size) {
    	mCurrentSlideshowSize = size;
    }

    // getCurrentMessageSize returns the size of the message, not including resizable attachments
    // such as photos. mCurrentMessageSize is used when adding/deleting/replacing non-resizable
    // attachments (movies, sounds, etc) in order to compute how much size is left in the message.
    // The difference between mCurrentMessageSize and the maxSize allowed for a message is then
    // divided up between the remaining resizable attachments. While this function is public,
    // it is only used internally between various MMS classes. If the UI wants to know the
    // size of a MMS message, it should call getTotalMessageSize() instead.
    /// M: Code analyze 002, new feature
    public int getCurrentSlideshowSize() {
        return mCurrentSlideshowSize;
    }

    /// M: Code analyze 002, new feature
    public void increaseSlideshowSize(int increaseSize) {
        if (increaseSize > 0) {
        	mCurrentSlideshowSize += increaseSize;
        }
    }

    /// M: Code analyze 002, new feature
    public void decreaseSlideshowSize(int decreaseSize) {
        if (decreaseSize > 0) {
        	mCurrentSlideshowSize -= decreaseSize;
        }
    }
    
    public LayoutModel getLayout() {
        return mLayout;
    }

    //
    // Implement List<E> interface.
    //
    public boolean add(SlideModel object) {
        int increaseSize = object.getSlideSize();
        checkMessageSize(increaseSize);

        if ((object != null) && mSlides.add(object)) {
            /// M: Code analyze 002, new feature
            increaseSlideshowSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean addAll(Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public void clear() {
        if (mSlides.size() > 0) {
            for (SlideModel slide : mSlides) {
                slide.unregisterModelChangedObserver(this);
                for (IModelChangedObserver observer : mModelChangedObservers) {
                    slide.unregisterModelChangedObserver(observer);
                }
            }
            /// M: Code analyze 002, new feature
            resetSlideshowSize();
            mSlides.clear();
            notifyModelChanged(true);
        }
    }

    public boolean contains(Object object) {
        return mSlides.contains(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return mSlides.containsAll(collection);
    }

    public boolean isEmpty() {
        return mSlides.isEmpty();
    }

    public Iterator<SlideModel> iterator() {
        return mSlides.iterator();
    }

    public boolean remove(Object object) {
        if ((object != null) && mSlides.remove(object)) {
            SlideModel slide = (SlideModel) object;
            /// M: Code analyze 002, new feature
            decreaseSlideshowSize(slide.getSlideSize());
            slide.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public int size() {
        /// M: Code analyze 002, new feature
        return mSlides == null ? 0 : mSlides.size();
    }

    public Object[] toArray() {
        return mSlides.toArray();
    }

    public <T> T[] toArray(T[] array) {
        return mSlides.toArray(array);
    }

    public void add(int location, SlideModel object) {
        if (object != null) {
            int increaseSize = object.getSlideSize();
            checkMessageSize(increaseSize);

            mSlides.add(location, object);
            /// M: Code analyze 002, new feature
            increaseSlideshowSize(increaseSize);
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
        }
    }

    public boolean addAll(int location,
            Collection<? extends SlideModel> collection) {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    public SlideModel get(int location) {
        return (location >= 0 && location < mSlides.size()) ? mSlides.get(location) : null;
    }

    public int indexOf(Object object) {
        return mSlides.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        return mSlides.lastIndexOf(object);
    }

    public ListIterator<SlideModel> listIterator() {
        return mSlides.listIterator();
    }

    public ListIterator<SlideModel> listIterator(int location) {
        return mSlides.listIterator(location);
    }

    public SlideModel remove(int location) {
        SlideModel slide = mSlides.remove(location);
        if (slide != null) {
            /// M: Code analyze 002, new feature
            decreaseSlideshowSize(slide.getSlideSize());
            slide.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
        }
        return slide;
    }

    public SlideModel set(int location, SlideModel object) {
        SlideModel slide = mSlides.get(location);
        if (null != object) {
            int removeSize = 0;
            int addSize = object.getSlideSize();
            if (null != slide) {
                removeSize = slide.getSlideSize();
            }
            if (addSize > removeSize) {
                checkMessageSize(addSize - removeSize);
                /// M: Code analyze 002, new feature
                increaseSlideshowSize(addSize - removeSize);
            } else {
                /// M: Code analyze 002, new feature
                decreaseSlideshowSize(removeSize - addSize);
            }
        }

        slide =  mSlides.set(location, object);
        if (slide != null) {
            slide.unregisterAllModelChangedObservers();
        }

        if (object != null) {
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
        }

        notifyModelChanged(true);
        return slide;
    }

    public List<SlideModel> subList(int start, int end) {
        return mSlides.subList(start, end);
    }

    @Override
    protected void registerModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.registerModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.registerModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterModelChangedObserverInDescendants(
            IModelChangedObserver observer) {
        mLayout.unregisterModelChangedObserver(observer);

        for (SlideModel slide : mSlides) {
            slide.unregisterModelChangedObserver(observer);
        }
    }

    @Override
    protected void unregisterAllModelChangedObserversInDescendants() {
        mLayout.unregisterAllModelChangedObservers();

        for (SlideModel slide : mSlides) {
            slide.unregisterAllModelChangedObservers();
        }
    }

    public void onModelChanged(Model model, boolean dataChanged) {
        if (dataChanged) {
            mDocumentCache = null;
            mPduBodyCache = null;
        }
    }

    public void sync(PduBody pb) {
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                PduPart part = pb.getPartByContentLocation(media.getSrc());
                if (part != null) {
                    media.setUri(part.getDataUri());
                }
            }
        }
        /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
        for (FileAttachmentModel fileAttach : mAttachFiles) {
            PduPart part = pb.getPartByContentLocation(fileAttach.getSrc());
            if (part != null) {
                fileAttach.setUri(part.getDataUri());
                fileAttach.setData(part.getData());
            }
        }
        /// @}
    }

    public void checkMessageSize(int increaseSize) throws ContentRestrictionException {
        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        /// M: Code analyze 002, new feature
        cr.checkMessageSize(mCurrentSlideshowSize, increaseSize, mContext.getContentResolver());
    }

    /**
     * Determines whether this is a "simple" slideshow.
     * Criteria:
     * - Exactly one slide
     * - Exactly one multimedia attachment, M:del[but no audio]
     * - It can optionally have a caption
    */
    /// M: Code analyze 020, fix bug ALPS00085982, SimpleSlide should support audio @{
    public boolean isSimple() {
        // There must be one (and only one) slide.
        if (size() != 1) {
            /// M:
            MmsLog.i(TAG, "size() != 1, isSimple return false");
            return false;
        }

        SlideModel slide = get(0);
        // The slide must have either an audio image or video, but not both.
        /// M: change google default @{
        if (!(slide.hasAudio() ^ slide.hasImage() ^ slide.hasVideo())) {
        	MmsLog.i(TAG, "isSimple return false");
            return false;
        }
        MmsLog.i(TAG, "isSimple return true");
        /** M: comment it
        // No audio allowed.
        if (slide.hasAudio())
            return false;
        */
        /// @}
        
        /// M: will return true when there is only one audio/video/image.
        return true;
    }
    /// @}

    /**
     * Make sure the text in slide 0 is no longer holding onto a reference to the text
     * in the message text box.
     */
    public void prepareForSend() {
        if (size() == 1) {
            TextModel text = get(0).getText();
            if (text != null) {
                text.cloneText();
            }
        }
    }

    /// M: Code analyze 005, new feature(ALPS00104088), Add vCard support @{
    public FileAttachmentModel removeAttachFile(int location) {
        FileAttachmentModel attach = mAttachFiles.remove(location);
        int attachSize = attach.getAttachSize();
        int currentSlideshowSize = getCurrentSlideshowSize();
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        if (mMmsAttachmentEnhancePlugin != null) {
            if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == true) {
                if (currentSlideshowSize >= attachSize) {
                      setCurrentSlideshowSize(currentSlideshowSize - attachSize);
                }
            }
        }
        if (attach != null) {
            /// M: Code analyze 021, fix bug ALPS00242250,
            // vCard and slide won't co-exist, so it should not occupy quota @{
            // decreaseSlideshowSize(attach.getAttachSize());
            /// @}
            attach.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
        }
        return attach;
    }

    // Because there are extra things to do when remove, so we call #removeAttachFile()
    // rather than use List#clear();
    public void removeAllAttachFiles() {
        final int size = mAttachFiles.size();
        for (int i = (size - 1); i >= 0; i--) {
            removeAttachFile(i);
        }
    }

    public boolean addFileAttachment(FileAttachmentModel object) {
        if ((object != null) && mAttachFiles.add(object)) {
            /// M: Code analyze 021, fix bug ALPS00242250,
            // vCard and slide won't co-exist, so it should not occupy quota @{
            // increaseSlideshowSize(increaseSize);
            /// @}
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public ArrayList<FileAttachmentModel> getAttachFiles() {
        return mAttachFiles;
    }

    public boolean removeAttachFile(Object object){
        if ((object != null) && mAttachFiles.remove(object)) {
            FileAttachmentModel attach = (FileAttachmentModel) object;
            /// M: Code analyze 021, fix bug ALPS00242250,
            // vCard and slide won't co-exist, so it should not occupy quota @{
            // decreaseSlideshowSize(attach.getAttachSize());
            /// @}
            attach.unregisterAllModelChangedObservers();
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

    public int sizeOfFilesAttach() {
        return mAttachFiles == null ? 0 : mAttachFiles.size();
    }
    /// @}

    /// M: Code analyze 022, fix bug ALPS00278013, resolve duplicate names problem @{
    public enum MediaType{IMAGE,AUDIO,VIDEO};
    
    public String[] getAllMediaNames(MediaType mediaType) {
        if (mSlides == null || mSlides.size() < 1) {
            return null;
        }
        String[] names = new String[mSlides.size()];
        int mIndex = 0;
        switch (mediaType) {
            case IMAGE:
                for (SlideModel sm : mSlides) {
                    if (sm.hasImage()) {
                        names[mIndex] = sm.getImage().getSrc();
                        mIndex++;
                    }
                }
                return names;
            case AUDIO:
                for (SlideModel sm : mSlides) {
                    if (sm.hasAudio()) {
                        names[mIndex] = sm.getAudio().getSrc();
                        mIndex++;
                    }
                }
                return names;
            case VIDEO:
                for (SlideModel sm : mSlides) {
                    if (sm.hasVideo()) {
                        names[mIndex] = sm.getVideo().getSrc();
                        mIndex++;
                    }
                }
                return names;
            default:
                return null;
        }
    }
    /// @}

    /// M: Code analyze 006, new feature, Drm support @{
    public boolean checkDrmContent() {
    	return mHasDrmContent;
    }
    
    public boolean checkDrmRight() {
    	return mHasDrmRight;
    }
    
    public void setDrmContentFlag(boolean hasDrmContent) {
    	mHasDrmContent = hasDrmContent;
    }
    
    public void setDrmRightFlag(boolean hasDrmRight) {
    	mHasDrmRight = hasDrmRight;
    }
    /// @}

    /// M: Code analyze 002, new feature
    public void resetSlideshowSize() {
    	mCurrentSlideshowSize = MMS_HEADER_SIZE + MMS_CONTENT_TYPE_HEAER_LENGTH 
    	                        + SMILE_HEADER_SIZE + SLIDESHOW_SLOP;
    }
    
    /**
     * only for SlideshowEditActivity to swap slides. 
     * In this case, We don't check the message size
     */
    public void addNoCheckSize(int location, SlideModel object) {
        if (object != null) {
            mSlides.add(location, object);
            /// M: Code analyze 023, fix bug ALPS00243328, should increase size,
            /// because remove slide and decrease size when move slide position @{
            increaseSlideshowSize(object.getSlideSize());
            /// @}
            object.registerModelChangedObserver(this);
            for (IModelChangedObserver observer : mModelChangedObservers) {
                object.registerModelChangedObserver(observer);
            }
            notifyModelChanged(true);
        }
    }

    /// M: Code analyze 024, fix bug ALPS00242250, check size when add vCard @{
    public void checkAttachmentSize(int newSize, boolean append) throws ContentRestrictionException {
        int currentSize = 0;

        //add for attachment enhance
        IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin =
              (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);
        if (mMmsAttachmentEnhancePlugin != null &&
                   mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance()) {
            // OP01 support attachment enhance
            // Because slides and vcard can co-exsit in compose, the currentSize
            // should add mCurrentSlideshowSize
            currentSize = mCurrentSlideshowSize;
        }

        if (mAttachFiles != null) {
            for (FileAttachmentModel file : mAttachFiles) {
                currentSize += file.mSize;
            }
        }

        int added = append ? newSize : newSize + SlideshowModel.mReserveSize - currentSize;
        if (added < 0) {
            return;
        }

        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        cr.checkMessageSize(currentSize, added, mContext.getContentResolver());
    }
    /// @}

    /// M: Code analyze 008, fix bug ALPS00339457, init textLayout type when create @{
    private void initLayoutType(){
        if (mLayout == null) {
            return;
        }
        RegionModel textRegionModel = mLayout.getTextRegion();
        int textRegionModelTop = 0;
        if (textRegionModel != null) {
            textRegionModelTop = textRegionModel.getTop();
            if(textRegionModelTop>0){
                this.getLayout().setLayoutType(LayoutModel.LAYOUT_BOTTOM_TEXT);
            } else {
                this.getLayout().setLayoutType(LayoutModel.LAYOUT_TOP_TEXT);
            }
        } else {
            return;
        }
    }
    /// @}
        /**
     * Resize all the resizeable media objects to fit in the remaining size of the slideshow.
     * This should be called off of the UI thread.
     *
     * @throws MmsException, ExceedMessageSizeException
     */
    public void finalResize(Uri messageUri) throws MmsException, ExceedMessageSizeException {

        // Figure out if we have any media items that need to be resized and total up the
        // sizes of the items that can't be resized.
        int resizableCnt = 0;
        int fixedSizeTotal = 0;
        for (SlideModel slide : mSlides) {
            for (MediaModel media : slide) {
                if (media.getMediaResizable()) {
                    ++resizableCnt;
                } else {
                    fixedSizeTotal += media.getMediaSize();
                }
            }
        }
        Log.v(TAG, "finalResize: original message size: " + getCurrentSlideshowSize() +
                " getMaxMessageSize: " + MmsConfig.getMaxMessageSize() +
                " fixedSizeTotal: " + fixedSizeTotal);
        if (resizableCnt > 0) {
            int remainingSize = MmsConfig.getMaxMessageSize() - fixedSizeTotal - SLIDESHOW_SLOP;
            if (remainingSize <= 0) {
                throw new ExceedMessageSizeException("No room for pictures");
            }
            long messageId = ContentUris.parseId(messageUri);
            int bytesPerMediaItem = remainingSize / resizableCnt;
            // Resize the resizable media items to fit within their byte limit.
            for (SlideModel slide : mSlides) {
                for (MediaModel media : slide) {
                    if (media.getMediaResizable()) {
                        String contentType = media.getContentType();
                        if (contentType != null && contentType.equalsIgnoreCase("image/x-ms-bmp")) {
                            media.resizeMedia(bytesPerMediaItem, messageId);
                        }
                    }
                }
            }
            // One last time through to calc the real message size.
            int totalSize = 0;
            for (SlideModel slide : mSlides) {
                for (MediaModel media : slide) {
                    totalSize += media.getMediaSize();
                }
            }
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                Log.v(TAG, "finalResize: new message size: " + totalSize);
            }

            if (totalSize > MmsConfig.getMaxMessageSize()) {
                throw new ExceedMessageSizeException("After compressing pictures, message too big");
            }
            setCurrentSlideshowSize(totalSize);

            onModelChanged(this, true);     // clear the cached pdu body
            PduBody pb = toPduBody();
            // This will write out all the new parts to:
            //      /data/data/com.android.providers.telephony/app_parts
            // and at the same time delete the old parts.
            PduPersister.getPduPersister(mContext).updateParts(messageUri, pb, null);
        }
    }
}
