
package com.android.exchange.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.http.entity.InputStreamEntity;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;

import com.android.emailcommon.Logging;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailExternalCalls;
import com.android.emailcommon.service.EmailExternalConstants;
import com.android.exchange.EasOutboxService;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.ExchangeService;

/**
 * This EasSyncService support BT-MAP send message.
 * 
 */
public class EmailExternalEas extends EasSyncService {

    private InputStream mInputStream = null;

    private EmailExternalCalls mCallback = null;

    private boolean mSaveInSent = false;

    private Uri mUri = null;
    private static final String TAG = "EmailExternalEas";

    public EmailExternalEas(Context context, Mailbox mailbox, Uri uri,
            EmailExternalCalls callback, boolean save) {
        super(context, mailbox);
        mContentResolver = context.getContentResolver();
        mUri = uri;
        mCallback = callback;
        mSaveInSent = save;
    }

    @Override
    public void run() {
        // Initialize return value
        int resultType = EmailExternalConstants.TYPE_SEND;
        try {
            // Note: not set mDeviceId will cause 449 error.
            mDeviceId = ExchangeService.getDeviceId(mContext);
            setupService();
            sendMessage();
        } catch (IOException e) {
            sendCallback(EmailExternalConstants.RESULT_FAIL, resultType);
            userLog("Exception caught in EmailExternalEas", e);
        } finally {
            userLog(mMailbox != null ? mMailbox.mDisplayName
                    : "EmailExternalEas", ": sync finished");
        }

    }

    public void sendCallback(int result, int resultType) {
        long accountId = mMailbox.mAccountKey;
        try {
            mCallback.sendCallback(result, accountId, resultType);
        } catch (RemoteException e) {
            Logging.d(TAG, "RemoteException in sendCallback method");
        }
    }

    void writToFile(InputStream in, File tmpFile) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(tmpFile);
        OutputStream stream = new BufferedOutputStream(fileOut, 1024);
        Writer writer = new OutputStreamWriter(stream);
        int readChar = 0;
        while ((readChar = in.read()) != -1) {
            writer.write(readChar);
        }
        writer.flush();
        fileOut.flush();
        fileOut.close();
    }

    public void sendMessage() throws IOException, FileNotFoundException {
        Logging.d(TAG, "Start send message ... for Uri " + mUri);
        // Initialize return value
        int resultType = EmailExternalConstants.TYPE_SEND;
        int result = EmailExternalConstants.RESULT_SUCCESS;
        // Create tmp file
        File cacheDir = mContext.getCacheDir();
        File tmpFile = File.createTempFile("eas_", "tmp", cacheDir);

        try {
            mInputStream = mContentResolver.openInputStream(mUri);
            if (null == mInputStream) {
                Logging.w(TAG, "Send Message Failed in sendMessage() method , "
                        + "Can't get InputStream from the given uri: " + mUri);
                result = EmailExternalConstants.RESULT_FAIL;
                sendCallback(result, resultType);
                return;
            }

            // Write the output to a temporary file
            writToFile(mInputStream, tmpFile);
            mInputStream.close();

            // Get an input stream to our temporary file and create an entity
            // with it
            FileInputStream fileInputStream = new FileInputStream(tmpFile);
            InputStreamEntity inputEntity = new InputStreamEntity(
                    fileInputStream, tmpFile.length());

            // Create the appropriate command and POST it to the server
            String cmd = "SendMail&SaveInSent=F";
            if (mSaveInSent) {
                cmd = "SendMail&SaveInSent=T";
            }
            // if (smartSend) {
            // cmd = reply ? "SmartReply" : "SmartForward";
            // cmd += "&ItemId=" + itemId + "&CollectionId=" + collectionId +
            // "&SaveInSent=T";
            // }
            Logging.d(TAG, "Send cmd: " + cmd);
            EasResponse resp = sendHttpClientPost(cmd, inputEntity,
                    EasOutboxService.SEND_MAIL_TIMEOUT);
            fileInputStream.close();

            // Send feedback
            // sendCallback(result, resultType);

            // TODO:How to define the Deliver complete.
            // resultType = EmailExternalConstants.TYPE_DELIVER;
            int code = resp.getStatus();
            if (code == HttpStatus.SC_OK) {
                Logging.d(TAG, "EAS Message sending success, code:" + code);
                result = EmailExternalConstants.RESULT_SUCCESS;
                sendCallback(result, resultType);
                return;
            } else {
                Logging.d(TAG, "EAS Message sending failed, code:" + code);
                result = EmailExternalConstants.RESULT_FAIL;
            }
        } catch (FileNotFoundException e) {
            Logging.e(TAG, "EAS SendMessage FileNotFoundException "
                    + e.getMessage());
            result = EmailExternalConstants.RESULT_FAIL;
        } catch (IOException e) {
            Logging.e(TAG, "EAS SendMessage Exception " + e.getMessage());
            result = EmailExternalConstants.RESULT_FAIL;
        } finally {
            // Clean up the temporary file
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
        Logging.d(TAG, "EAS send Message feedback result = " + result
                + " resultType = " + resultType);
        sendCallback(result, resultType);
    }
}
