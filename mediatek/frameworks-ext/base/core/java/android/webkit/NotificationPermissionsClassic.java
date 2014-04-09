package android.webkit;

import android.os.Handler;
import android.os.Message;

import java.util.Vector;

/**
 * M: HTML5 web notification
 *
 * This class is the Java counterpart of the WebKit C++ NotificationPermissions
 * class. It simply marshals calls from the UI thread to the WebKit thread.
 *
 */
final class NotificationPermissionsClassic extends NotificationPermissions {
    private static final String LOGTAG = "NotificationPermissionsClassic";

    private Handler mHandler;
    private Handler mUIHandler;

    // Message ids
    private static final int CLEAR_ALL = 0;

    // A queue to store messages until the handler is ready
    private Vector<Message> mQueuedMessages;

    private static NotificationPermissionsClassic sInstance;

    // Global instance
    public static NotificationPermissionsClassic getInstance() {
        if (sInstance == null) {
            sInstance = new NotificationPermissionsClassic();
        }
        return sInstance;
    }

    /**
     * Create the message handler. Must be called on WebKit Thread.
     *
     * @hide
     */
    public synchronized void createHandler() {
        if (mHandler != null) {
            return;
        }
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case CLEAR_ALL:
                        nativeClearAll();
                        break;
                    default:
                        break;
                }
            }
        };

        if (mQueuedMessages != null) {
            while (!mQueuedMessages.isEmpty()) {
                mHandler.sendMessage(mQueuedMessages.remove(0));
            }
            mQueuedMessages = null;
        }
    }

    /**
     * Utility function to send a message to our handler
     */
    private synchronized void postMessage(Message msg) {
        if (mHandler == null) {
            if (mQueuedMessages == null) {
                mQueuedMessages = new Vector<Message>();
            }
            mQueuedMessages.add(msg);
        } else {
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void clearAll() {
        // Called on UI thread
        postMessage(Message.obtain(null, CLEAR_ALL));
    }

    NotificationPermissionsClassic() {}

    // Native function run on WebKit thread
    private static native void nativeClearAll();
}
