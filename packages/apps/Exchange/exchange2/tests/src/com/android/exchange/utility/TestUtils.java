
package com.android.exchange.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import android.net.Uri;
import android.os.Environment;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.emailcommon.Logging;

/**
 * Utility methods used only by tests.
 */
@LargeTest
public class TestUtils extends TestCase /* It tests itself */{

    public static final long WAIT_FOR_SERVER_START = 5000;
    public static final long WAIT_FOR_LONG_LOADING = 2500;
    public static final long WAIT_FOR_DB_OPERATION = 1000;
    public static final long WAIT_FOR_SHORT_OPERATION = 500;

    public interface Condition {
        public boolean isMet();
    }

    /**
     * Run {@code runnable} and fails if it doesn't throw a {@code
     * expectedThrowable} or a subclass of it.
     */
    public static void expectThrowable(Runnable runnable,
            Class<? extends Throwable> expectedThrowable) {
        try {
            runnable.run();
            fail("Expected throwable not thrown.");
        } catch (Throwable th) {
            if (expectedThrowable.isAssignableFrom(th.getClass())) {
                return; // Expcted. OK
            }
            fail("Cought unexpected throwable " + th.getClass().getName());
        }
    }

    public void testExpectThrowable() {
        try {
            expectThrowable(new Runnable() {
                @Override public void run() {
                    // Throwing no exception
                }
            }, Throwable.class);
            fail();
        } catch (AssertionFailedError ok) {
        }

        try {
            expectThrowable(new Runnable() {
                @Override public void run() {
                    // Throw RuntimeException, which is not a subclass of Error.
                    throw new RuntimeException();
                }
            }, Error.class);
            fail();
        } catch (AssertionFailedError ok) {
        }

        expectThrowable(new Runnable() {
            @Override public void run() {
                throw new RuntimeException();
            }
        }, Exception.class);
    }

    /**
     * Wait until a {@code Condition} is met.
     */
    public static void waitUntil(Condition condition, int timeoutSeconds) {
        waitUntil("", condition, timeoutSeconds);
    }

    /**
     * Wait until a {@code Condition} is met.
     */
    public static void waitUntil(String message, Condition condition,
            long timeoutSeconds) {
        Log.d(Logging.LOG_TAG, message + ": Waiting...");
        final long timeout = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < timeout) {
            if (condition.isMet()) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
        }
        fail(message + ": Timeout");
    }

    public void testWaitUntil() {
        // Shouldn't fail.
        waitUntil("message", new Condition() {
            @Override public boolean isMet() {
                return true;
            }
        }, 1000000L);

        expectThrowable(new Runnable() {
            @Override public void run() {
                // Condition never meets, should fail.
                waitUntil("message", new Condition() {
                    @Override public boolean isMet() {
                        return false;
                    }
                }, 0);
            }
        }, AssertionFailedError.class);
    }

    public static void sleepAndWait() {
        sleepAndWait(WAIT_FOR_SHORT_OPERATION);
    }

    public static void sleepAndWait(long times) {
        try {
            Thread.sleep(times);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public static Uri createAvailableUri() {
        String mockMessage = "From: quaner85@gmail.com\r\n"
                + "To:A Group(Some people)\r\n"
                + "     :Chris Jones <c@public.example>,\r\n"
                + "         dddd@gmail.com,\r\n"
                + "  John <test@mediatek.com> (my dear friend); (the end of the group)\r\n"
                + "Cc:(Empty list)(start)Undisclosed recipients  :(nobody(that I know))  ;\r\n"
                + "Date: Thu,\r\n" + "      13\r\n" + "        Feb\r\n"
                + "          2015\r\n" + "      23:32\r\n"
                + "               -0330 (Newfoundland Time)\r\n"
                + "Message-ID:              <testabcd.1234@silly.test>\r\n"
                + "Content-Type:                \r\n"
                + "          TEXT/hTML \r\n"
                + "       ; x-blah=\"y-blah\" ; \r\n"
                + "       CHARSET=\"us-ascii\" ; (comment)\r\n" + "\r\n"
                + "<html><body>Testing.........</body></html>\r\n";
        File directory = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        directory.mkdirs();
        File f = new File(directory, "mockMessage.eml");
        try {
            if (!f.exists()) {
                f.createNewFile();
            } else {
                f.delete();
                f.createNewFile();
            }
            FileOutputStream fi = new FileOutputStream(f);
            fi.write(mockMessage.getBytes("us-ascii"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(f);
    }

    public static Uri createUnAvailableUri() {
        File directory = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        directory.mkdirs();
        File f = new File(directory, "mockMessage22784302.eml");
        return Uri.fromFile(f);
    }
}
