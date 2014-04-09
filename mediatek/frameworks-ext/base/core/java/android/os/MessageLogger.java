package android.os;

import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Printer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Date;
import java.text.SimpleDateFormat;


/**
 * @hide
 */
public class MessageLogger implements Printer {
    private static final String TAG = "MessageLogger";
    private static boolean mEnableLooperLog;
    private static LinkedList mMessageHistoryRecord = new LinkedList();
    private static LinkedList mLongTimeMessageHistoryRecord = new LinkedList();
    private static LinkedList mMessageTimeRecord = new LinkedList();
    private static LinkedList mNonSleepMessageTimeRecord = new LinkedList();
    private static LinkedList mNonSleepLongTimeRecord = new LinkedList();
    private static LinkedList mElapsedLongTimeRecord = new LinkedList();
    final static int MESSAGE_SIZE = 20 * 2;// One for dispatching, one for
    // finished
    final static int LONGER_TIME_MESSAGE_SIZE = 40 * 2;// One for dispatching, one for
    // finished
    final static int LONGER_TIME = 200; //ms
    final static int FLUSHOUT_SIZE = 1024*2; //ms
    private static String mLastRecord = null;
    private static long mLastRecordKernelTime;            //Unir: Milli
    private static long mNonSleepLastRecordKernelTime;    //Unit: Milli
    private static long mLastRecordDateTime;            //Unir: Micro
    private static int mState = 0;
    private static long mMsgCnt = 0;
    private static String messageInfo = "";

    public MessageLogger() {
    }

    public long wallStart;                 //Unit:Micro
    public long wallTime;                 //Unit:Micro
    public long nonSleepWallStart;        //Unit:Milli
    public long nonSleepWallTime;        //Unit:Milli
    public static void addTimeToList(LinkedList mList, long startTime, long durationTime) {
        mList.add(startTime);
        mList.add(durationTime);
        return;
    }

    public void println(String s) {
        synchronized (mMessageHistoryRecord) {

            mState++;
            int size = mMessageHistoryRecord.size();
            if (size > MESSAGE_SIZE) {
                mMessageHistoryRecord.removeFirst();
                mMessageTimeRecord.removeFirst();
                mNonSleepMessageTimeRecord.removeFirst();
            }
            s = "Msg#:" + mMsgCnt + " " + s;
            mMsgCnt++;

            mMessageHistoryRecord.add(s);
            mLastRecordKernelTime = SystemClock.elapsedRealtime();
            mNonSleepLastRecordKernelTime = SystemClock.uptimeMillis();
            mLastRecordDateTime = SystemClock.currentTimeMicro();
            if( mState%2 == 0) {
                mState = 0;
                wallTime = SystemClock.currentTimeMicro() - wallStart;
                nonSleepWallTime = SystemClock.uptimeMillis() - nonSleepWallStart;
                addTimeToList(mMessageTimeRecord, wallStart, wallTime);
                addTimeToList(mNonSleepMessageTimeRecord, nonSleepWallStart, nonSleepWallTime);

                if(nonSleepWallTime >= LONGER_TIME) {
                    if(mLongTimeMessageHistoryRecord.size() >= LONGER_TIME_MESSAGE_SIZE)
                    {
                        mLongTimeMessageHistoryRecord.removeFirst();
                        for(int i = 0; i < 2 ;i++)
                        {
                            mNonSleepLongTimeRecord.removeFirst();
                            mElapsedLongTimeRecord.removeFirst();
                        }
                    }

                    mLongTimeMessageHistoryRecord.add(s);
                    addTimeToList(mNonSleepLongTimeRecord,wallStart,nonSleepWallTime);
                    addTimeToList(mElapsedLongTimeRecord,wallStart,wallTime);
                   
                }
            } else {
                wallStart = SystemClock.currentTimeMicro();
                nonSleepWallStart = SystemClock.uptimeMillis();

                    /*
                       Test Longer History Code.
                     */
                    /*
                       if(mMsgCnt%3 == 0 && nonSleepWallStart > 2*LONGER_TIME) {
                       nonSleepWallStart -= 2*LONGER_TIME;
                       }
                     */

                    /*
                       Test Longer History Code
                       ================================.
                     */

            }

            if (mEnableLooperLog) {
                if (s.contains(">")) {
                    Log.d(TAG,"Debugging_MessageLogger: " + s + " start");
                } else {
                    Log.d(TAG,"Debugging_MessageLogger: " + s + " spent " + wallTime / 1000 + "ms");
                }
            }
        }
    }

    private static  int sizeToIndex( int size) {
            return --size;
    }

    private static void flushedOrNot(StringBuilder sb, boolean bl ) {
        if(sb.length() > FLUSHOUT_SIZE && !bl) {
            //Log.d(TAG, "After Flushed, Current Size Is:" + sb.length() + ",bool" + bl);
            sb.append("***Flushing, Current Size Is:" + sb.length() + ",bool" + bl +"***TAIL\n");
            bl = true;
            /// M: Add message history/queue to _exp_main.txt 
            messageInfo = messageInfo + sb.toString();
            Log.d(TAG, sb.toString());
            //Why New the one, not Clear the one? -> http://stackoverflow.com/questions/5192512/how-to-clear-empty-java-stringbuilder
            //Performance  is better for new object allocation.
            //sb = new StringBuilder(1024);
            sb.delete(0,sb.length());
        }
        else if(bl) {
            bl = false;
        }
            /*
               Test Longer History Code.
             */
            /*
               sb.append("***Current Size Is:" + sb.length() + "***\n");
             */
            /*
               Test Longer History Code.
               ================
             */
    }
        /*
           Test Longer History Code.
         */
        /*
           private static int DumpRound = 0;
         */
        /*
           Test Longer History Code.
           ================
         */


    public static String dump() {        
        synchronized (mMessageHistoryRecord) {
            StringBuilder history = new StringBuilder(1024);
                /*
                   Test Longer History Code.
                 */
                /*
                   history.append("=== DumpRound:" + DumpRound + " ===\n");
                   DumpRound++;
                 */
                /*
                   Test Longer History Code.
                   ================
                 */
            history.append("MSG HISTORY IN MAIN THREAD:\n");
            history.append("Current kernel time : " + SystemClock.elapsedRealtime() + "ms\n");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            //State = 1 means the current dispatching message has not been finished
            int sizeForMsgRecord = mMessageHistoryRecord == null ? 0 : mMessageHistoryRecord.size();
            if (mState == 1) {
                Date date = new Date((long)mLastRecordDateTime/1000);
                long spent = SystemClock.elapsedRealtime() - mLastRecordKernelTime;
                long nonSleepSpent = SystemClock.uptimeMillis()- mNonSleepLastRecordKernelTime;

                history.append("Last record : " + mMessageHistoryRecord.getLast());
                history.append("\n");
                history.append("Last record dispatching elapsedTime:" + spent + " ms/upTime:"+ nonSleepSpent +" ms\n");
                history.append("Last record dispatching time : " + simpleDateFormat.format(date));
                history.append("\n");
                sizeForMsgRecord --;
            }

            String msg = null;
            Long time = null;
            Long nonSleepTime = null;
            StringBuilder longerHistory = new StringBuilder(1024);
            boolean flushed = false;
            for (;sizeForMsgRecord > 0; sizeForMsgRecord--) {
                msg = (String)mMessageHistoryRecord.get(sizeToIndex(sizeForMsgRecord));
                time = (Long)mMessageTimeRecord.get(sizeToIndex(sizeForMsgRecord));
                nonSleepTime = (Long)mNonSleepMessageTimeRecord.get(sizeToIndex(sizeForMsgRecord));
                if (msg.contains(">")) {
                    Date date = new Date((long)time.longValue()/1000);
                    history.append(msg + " from " + simpleDateFormat.format(date));
                    history.append("\n");
                } else {
                    history.append(msg + " elapsedTime:" + time/1000 + " ms/upTime:" + nonSleepTime +" ms");
                    history.append("\n");
                }

                flushedOrNot(history, flushed);
            }

            if(!flushed) {
               /// M: Add message history/queue to _exp_main.txt 
               messageInfo = messageInfo + history.toString();
               Log.d(TAG, history.toString());
            }

            /*Dump for LongerTimeMessageRecord*/
            flushed = false;
            longerHistory.append("=== LONGER MSG HISTORY IN MAIN THREAD ===\n");
            sizeForMsgRecord = mLongTimeMessageHistoryRecord.size();
            int indexForTimeRecord = mNonSleepLongTimeRecord.size() - 1;
            for ( ;sizeForMsgRecord > 0; sizeForMsgRecord--, indexForTimeRecord-=2) {
                msg = (String)mLongTimeMessageHistoryRecord.get(sizeToIndex(sizeForMsgRecord));
                nonSleepTime = (Long) mNonSleepLongTimeRecord.get(indexForTimeRecord);
                time = (Long) mNonSleepLongTimeRecord.get(indexForTimeRecord-1);
                Date date = new Date((long)time.longValue()/1000);
                longerHistory.append(msg + " from " + simpleDateFormat.format(date) + " elapsedTime:"+ (((Long)(mElapsedLongTimeRecord.get(indexForTimeRecord))).longValue()/1000)+" ms/upTime:" + nonSleepTime +"ms");
                longerHistory.append("\n");

                flushedOrNot(longerHistory, flushed);
            }
            if(!flushed) {
                /// M: Add message history/queue to _exp_main.txt 
                messageInfo = messageInfo + longerHistory.toString();
                Log.d(TAG, longerHistory.toString());
            }
            // Dump message queue
            
        }
        /// M: Add message history/queue to _exp_main.txt 
        String retMessageInfo = messageInfo + Looper.getMainLooper().getQueue().dumpMessageQueue();
        messageInfo = "";
        return retMessageInfo;
    }
}

