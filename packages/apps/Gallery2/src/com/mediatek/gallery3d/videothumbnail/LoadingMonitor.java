package com.mediatek.gallery3d.videothumbnail;

//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.Random;
//
//import android.util.Log;

// comment out this class for emma coverage purpose
// this class is loading handling related but never used at present
public class LoadingMonitor {
//
//    private static final String TAG = "vt/Loading";
//    // get CPU usage interval time.
//    private static final int INTERVAL_CPU_TIME = 500;
//    // calculate times of getting CPU usage
//    private static final int TIMES_PARAMETER = 2;
//    // flag. whether need get CPU usage thread run.
//    private boolean mIsThreadRun = true;
//    // interval time to calculate CPU usage.
//    private int mIntervalTime = 2;
//
//    private LoadingListener mListener = null;
//
//
//    public interface LoadingListener {
//        /**
//         * loading changed
//         */
//        void loadingChanged(int count);
//    }
//
//    public LoadingMonitor(LoadingListener listener) {
//        try {
//            mListener = listener;
//        } catch (ClassCastException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * set interval time to calculate CPU usage.
//     * @param seconds, default is 2.
//     */
//    public void setInervalTime(int seconds) {
//        this.mIntervalTime = seconds;
//    }
//
//    /**
//     * get CPU usage according to interval time.
//     */
//    public void startCheck() {
//        Thread mGetCPUStateThread = new Thread() {
//            int count = 0;
//            int average = 0;
//            int targetCount = mIntervalTime * TIMES_PARAMETER;
//            public void run() {
//                while (mIsThreadRun) {
//                    int usage = readUsage();
//                    average += (usage > 0 ? usage : 0);
//                    ++count;
//                    if (count == targetCount) {
//                        average = average / targetCount;
//                        mListener.loadingChanged(average);
//                        average = 0;
//                        count = 0;
//                    }
//
//                    /*
//                     * test
//                     */
////                    mListener.loadingChanged(getCpu());
////                    notifyHostAP(TestListener.SDCARD_STATE_CHANGED);
//                    try {
//                        System.out.println("sleep------");
//                        Thread.sleep(INTERVAL_CPU_TIME);
//                    } catch (Exception e) {
//                        // TODO: handle exception
//                    }
//                }
//            }
//        };
//        mGetCPUStateThread.start();
//    }
//
//    private int readUsage() {
//        int result = 0;
//        try {
//            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
//            String load = reader.readLine();
//            String[] toks = load.split(" ");
//            long idle1 = Long.parseLong(toks[5]);
//            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
//            + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
//                    + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
//            try {
//                Thread.sleep(360);
//            } catch (Exception e) {
//            }
//            reader.seek(0);
//            load = reader.readLine();
//            reader.close();
//            toks = load.split(" ");
//            long idle2 = Long.parseLong(toks[5]);
//            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
//            + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
//            + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
//            result = (int) (100 * (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)));
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        return result;
//    }
//
//    public int getCpu() {
//        int[] cpuState = new int[] {20, 50, 70, 90, 95};
//        Random random = new Random();
//        int r = random.nextInt(5);
//        return cpuState[r];
//    }
}
