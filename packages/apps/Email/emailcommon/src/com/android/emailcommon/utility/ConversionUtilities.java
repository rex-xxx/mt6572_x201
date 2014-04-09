/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.emailcommon.utility;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeHeader;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.AttachmentColumns;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ConversionUtilities {
    /**
     * Values for HEADER_ANDROID_BODY_QUOTED_PART to tag body parts
     */
    public static final String BODY_QUOTED_PART_REPLY = "quoted-reply";
    public static final String BODY_QUOTED_PART_FORWARD = "quoted-forward";
    public static final String BODY_QUOTED_PART_INTRO = "quoted-intro";

    private static final String ATTACHMENT_NO_NAME = "noname";
    /**
     * Helper function to append text to a StringBuffer, creating it if necessary.
     * Optimization:  The majority of the time we are *not* appending - we should have a path
     * that deals with single strings.
     */
    private static StringBuffer appendTextPart(StringBuffer sb, String newText) {
        if (newText == null) {
            return sb;
        }
        else if (sb == null) {
            sb = new StringBuffer(newText);
        } else {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(newText);
        }
        return sb;
    }

    /**
     * Copy body text (plain and/or HTML) from MimeMessage to provider Message
     */
    public static boolean updateBodyFields(EmailContent.Body body,
            EmailContent.Message localMessage, ArrayList<Part> viewables)
    throws MessagingException {

        body.mMessageKey = localMessage.mId;

        StringBuffer sbHtml = null;
        StringBuffer sbText = null;
        StringBuffer sbHtmlReply = null;
        StringBuffer sbTextReply = null;
        StringBuffer sbIntroText = null;

        for (Part viewable : viewables) {
            String text = MimeUtility.getTextFromPart(viewable);
            String[] replyTags = viewable.getHeader(MimeHeader.HEADER_ANDROID_BODY_QUOTED_PART);
            String replyTag = null;
            if (replyTags != null && replyTags.length > 0) {
                replyTag = replyTags[0];
            }
            // Deploy text as marked by the various tags
            boolean isHtml = "text/html".equalsIgnoreCase(viewable.getMimeType());

            if (replyTag != null) {
                boolean isQuotedReply = BODY_QUOTED_PART_REPLY.equalsIgnoreCase(replyTag);
                boolean isQuotedForward = BODY_QUOTED_PART_FORWARD.equalsIgnoreCase(replyTag);
                boolean isQuotedIntro = BODY_QUOTED_PART_INTRO.equalsIgnoreCase(replyTag);

                if (isQuotedReply || isQuotedForward) {
                    if (isHtml) {
                        sbHtmlReply = appendTextPart(sbHtmlReply, text);
                    } else {
                        sbTextReply = appendTextPart(sbTextReply, text);
                    }
                    // Set message flags as well
                    localMessage.mFlags &= ~EmailContent.Message.FLAG_TYPE_MASK;
                    localMessage.mFlags |= isQuotedReply
                        ? EmailContent.Message.FLAG_TYPE_REPLY
                            : EmailContent.Message.FLAG_TYPE_FORWARD;
                    continue;
                }
                if (isQuotedIntro) {
                    sbIntroText = appendTextPart(sbIntroText, text);
                    continue;
                }
            }

            // Most of the time, just process regular body parts
            if (isHtml) {
                sbHtml = appendTextPart(sbHtml, text);
            } else {
                sbText = appendTextPart(sbText, text);
            }
        }

        // write the combined data to the body part
        boolean needConvert = false;
        if (!TextUtils.isEmpty(sbText)) {
            String text = sbText.toString();
            body.mTextContent = text;
            localMessage.mSnippet = TextUtilities.makeSnippetFromPlainText(text);
        } else {
            needConvert = true;
        }
        if (!TextUtils.isEmpty(sbHtml)) {
            String text = sbHtml.toString();
            body.mHtmlContent = text;
            if (localMessage.mSnippet == null) {
                localMessage.mSnippet = TextUtilities.makeSnippetFromHtmlText(text);
            }
            if (needConvert) {
                body.mTextContent = Utility.HtmlToText(text);
            }
        }
        if (sbHtmlReply != null && sbHtmlReply.length() != 0) {
            body.mHtmlReply = sbHtmlReply.toString();
        }
        if (sbTextReply != null && sbTextReply.length() != 0) {
            body.mTextReply = sbTextReply.toString();
        }
        if (sbIntroText != null && sbIntroText.length() != 0) {
            body.mIntroText = sbIntroText.toString();
        }
        return true;
    }

    /** M: Move to ConversionUtilities to be common
     * Copy attachments from MimeMessage to provider Message.
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param attachments the attachments to add
     * @throws IOException
     */
    public static void updateAttachments(Context context, EmailContent.Message localMessage,
            ArrayList<Part> attachments) throws MessagingException, IOException {
        localMessage.mAttachments = null;
        int partLocation = 1;
        for (Part attachmentPart : attachments) {
            partLocation += 1;
            Attachment localAtt = addOneAttachment(localMessage, attachmentPart, partLocation);
            saveOneAttachment(context, localMessage, attachmentPart, localAtt);
        }
    }

    /** M: Move to ConversionUtilities to support Exchange either.
     * Copy attachments from MimeMessage to provider Message.
     *
     * @param localMessage the attachments will be built against this message
     * @param attachments the attachments to add
     * @throws IOException
     */
    public static void updateAttachments(EmailContent.Message localMessage,
            ArrayList<Part> attachments) throws MessagingException, IOException {
        localMessage.mAttachments = null;
        int partLocation = 1;
        for (Part attachmentPart : attachments) {
            partLocation += 1;
            addOneAttachment(localMessage, attachmentPart, partLocation);
        }
    }

    /**
     * M : Add a single attachment part to the message
     * Move to ConversionUtilities to support Exchange either.
     * This will skip adding attachments if they are already found in the attachments table.
     * The heuristic for this will fail (false-positive) if two identical attachments are
     * included in a single POP3 message.
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param part a single attachment part from POP or IMAP
     * @throws IOException
     */
    private static Attachment addOneAttachment(EmailContent.Message localMessage,
            Part part, int partLocation) throws MessagingException, IOException {

        Attachment localAttachment = new Attachment();

        // Transfer fields from mime format to provider format
        String contentType = MimeUtility.unfoldAndDecode(part.getContentType());
        String name = MimeUtility.getHeaderParameter(contentType, "name");
        if (name == null) {
            String contentDisposition = MimeUtility.unfoldAndDecode(part.getDisposition());
            name = MimeUtility.getHeaderParameter(contentDisposition, "filename");
        }

        // Incoming attachment: Try to pull size from disposition (if not downloaded yet)
        long size = 0;
        String disposition = part.getDisposition();
        if (disposition != null) {
            String s = MimeUtility.getHeaderParameter(disposition, "size");
            if (s != null) {
                size = Long.parseLong(s);
            }
        }

        // Get partId for unloaded IMAP attachments (if any)
        // This is only provided (and used) when we have structure but not the actual attachment
        String[] partIds = part.getHeader(MimeHeader.HEADER_ANDROID_ATTACHMENT_STORE_DATA);
        String partId = partIds != null ? partIds[0] : null;
        /** M: Avoid attachement's name is null.  @{ */
        if (name == null) {
            name = ATTACHMENT_NO_NAME;
        }
        /** @} */
        localAttachment.mFileName = name;
        String mimeTypeString = part.getMimeType();
        if (TextUtils.isEmpty(mimeTypeString)
                || "name/plain".equalsIgnoreCase(mimeTypeString)
                || "/".equalsIgnoreCase(mimeTypeString)) {
            localAttachment.mMimeType = AttachmentUtilities.inferMimeType(name, null);
            Logging.w(Logging.LOG_TAG, " update attachment's mimetype from name, result "
                    + localAttachment.mMimeType);
        } else {
            localAttachment.mMimeType = mimeTypeString;
        }
        localAttachment.mSize = size;           // May be reset below if file handled
        localAttachment.mContentId = part.getContentId();
        localAttachment.mContentUri = null;     // Will be rewritten by saveAttachmentBody
        localAttachment.mMessageKey = localMessage.mId;
        localAttachment.mLocation = partId;

        if (localAttachment.mLocation == null) {
            localAttachment.mLocation = String.valueOf(partLocation);
        }
        String[] encoding = part.getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING);
        if (encoding != null) {
            // get attachment encodings
            localAttachment.mEncoding = encoding[0];
        } else {
            // set default value 'base64'
            localAttachment.mEncoding = "base64";
        }
        localAttachment.mAccountKey = localMessage.mAccountKey;
        if (localMessage.mAttachments == null) {
            localMessage.mAttachments = new ArrayList<Attachment>();
        }
        localMessage.mAttachments.add(localAttachment);
        localMessage.mFlagAttachment = true;
        Logging.d(Logging.LOG_TAG, "Add attachment " + localAttachment);
        return localAttachment;
    }

    /**
     * M: Save a single attachment to a Message
     *
     * @param context a context for file operations
     * @param localMessage the attachments will be built against this message
     * @param part a single attachment part from POP or IMAP
     * @throws IOException
     */
    public static void saveOneAttachment(Context context, EmailContent.Message localMessage,
            Part part, Attachment localAttachment)
            throws MessagingException, IOException {

        // To prevent duplication - do we already have a matching attachment?
        // The fields we'll check for equality are:
        //  mFileName, mMimeType, mContentId, mMessageKey, mLocation
        // NOTE:  This will false-positive if you attach the exact same file, twice, to a POP3
        // message.  We can live with that - you'll get one of the copies.
        Uri uri = ContentUris.withAppendedId(Attachment.MESSAGE_ID_URI, localMessage.mId);
        Cursor cursor = context.getContentResolver().query(uri, Attachment.CONTENT_PROJECTION,
                null, null, null);
        boolean attachmentFoundInDb = false;
        try {
            while (cursor.moveToNext()) {
                Attachment dbAttachment = new Attachment();
                dbAttachment.restore(cursor);
                // We test each of the fields here (instead of in SQL) because they may be
                // null, or may be strings.
                if (stringNotEqual(dbAttachment.mLocation, localAttachment.mLocation)) {
                    continue;
                }
                if (stringNotEqual(dbAttachment.mFileName, localAttachment.mFileName)) {
                    continue;
                }
                if (stringNotEqual(dbAttachment.mMimeType, localAttachment.mMimeType)) {
                    continue;
                }
                if (stringNotEqual(dbAttachment.mContentId, localAttachment.mContentId)) {
                    continue;
                }
                // We found a match, so use the existing attachment id, and stop looking/looping
                attachmentFoundInDb = true;
                localAttachment.mId = dbAttachment.mId;
                Log.d(Logging.LOG_TAG, "Skipped, found db attachment " + dbAttachment);
                break;
            }
        } finally {
            cursor.close();
        }

        // Save the attachment (so far) in order to obtain an id
        if (!attachmentFoundInDb) {
            localAttachment.save(context);
        }

        // If an attachment body was actually provided, we need to write the file now
        saveAttachmentBody(context, part, localAttachment, localMessage.mAccountKey);
//
//        if (localMessage.mAttachments == null) {
//            localMessage.mAttachments = new ArrayList<Attachment>();
//        }
//        localMessage.mAttachments.add(localAttachment);
//        localMessage.mFlagAttachment = true;
    }

    /**
     * Helper for addOneAttachment that compares two strings, deals with nulls, and treats
     * nulls and empty strings as equal.
     */
    /* package */
    static boolean stringNotEqual(String a, String b) {
        if (a == null && b == null) {
            return false; // fast exit for two null strings
        }
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return !a.equals(b);
    }

    /** M: Move this function to ConversionUtilities to be a common method
     * Save the body part of a single attachment, to a file in the attachments directory.
     */
    public static void saveAttachmentBody(Context context, Part part, Attachment localAttachment,
            long accountId) throws MessagingException, IOException {
        if (part.getBody() != null) {
            long attachmentId = localAttachment.mId;

            InputStream in = null;
            try {
                in = part.getBody().getInputStream();
            } catch (MessagingException me) {
                Logging.e("saveAttachmentBody get part body Exception: " + me);
                throw new IOException(me.getMessage());
            }

            File saveAs = null;
            FileOutputStream out = null;
            long copySize = -1;
            try {
                File saveIn = AttachmentUtilities.getAttachmentDirectory(context, accountId);
                if (!saveIn.exists()) {
                    if (!saveIn.mkdirs()) {
                        Logging.w("saveAttachmentBody mkdirs failed.");
                    }
                }
                saveAs = AttachmentUtilities.getAttachmentFilename(context, accountId,
                        attachmentId);
                if (!saveAs.createNewFile()) {
                    Logging.w("saveAttachmentBody createNewFile failed.");
                }
                out = new FileOutputStream(saveAs);
                copySize = IOUtils.copy(in, out);
            } catch (IOException ioe) {
                Logging.e("saveAttachmentBody Exception: " + ioe);
                if (saveAs != null) {
                    if (!saveAs.delete()) {
                        Logging.w("saveAttachmentBody delete file failed.");
                    }
                }
                throw ioe;
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }

            // update the attachment with the extra information we now know
            String contentUriString = AttachmentUtilities.getAttachmentUri(
                    accountId, attachmentId).toString();
            Logging.d("saveAttachmentBody Description size:" + localAttachment.mSize
                    + ",Actual size:" + copySize);

            localAttachment.mSize = copySize;
            localAttachment.mContentUri = contentUriString;

            // update the attachment in the database as well
            ContentValues cv = new ContentValues();
            cv.put(AttachmentColumns.SIZE, copySize);
            cv.put(AttachmentColumns.CONTENT_URI, contentUriString);
            Uri uri = ContentUris.withAppendedId(Attachment.CONTENT_URI, attachmentId);
            context.getContentResolver().update(uri, cv, null, null);
        }
    }
}
