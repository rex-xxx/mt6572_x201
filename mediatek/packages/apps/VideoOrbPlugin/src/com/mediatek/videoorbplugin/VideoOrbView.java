package com.mediatek.videoorbplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import com.mediatek.ngin3d.Container;
import com.mediatek.ngin3d.Image;
import com.mediatek.ngin3d.Point;
import com.mediatek.ngin3d.Rotation;
import com.mediatek.ngin3d.Scale;
import com.mediatek.ngin3d.Stage;
import com.mediatek.ngin3d.Transaction;
import com.mediatek.ngin3d.Video;
import com.mediatek.ngin3d.android.StageTextureView;
import com.mediatek.common.policy.IKeyguardLayer;
import com.mediatek.common.policy.KeyguardLayerInfo;
import com.mediatek.ngin3d.animation.Animation;
import com.mediatek.ngin3d.animation.AnimationGroup;
import com.mediatek.ngin3d.animation.AnimationLoader;
import com.mediatek.ngin3d.animation.BasicAnimation;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/*
 * The plug-in view life-cycle order is
 * Constructor(), Create(), onAttachedToWindow(), Destroy()
 */
public class VideoOrbView extends StageTextureView
        implements IKeyguardLayer, SensorEventListener {
    private static final String TAG = "vo.view";
    private static final String PACKAGE_NAME = "com.mediatek.videoorbplugin";

    private static final Point sAnimationPos[] = new Point[] {
            new Point(0.4916f, 0.5362f, 428, true), new Point(0.4916f, 0.2775f, 500, true),
            new Point(0.4916f, 0.62875f, 406, true), new Point(0.4916f, 0.4150f, 428, true),
            new Point(0.4916f, 0.89375f, 388, true), new Point(0.4816f, 0.53600f, 385, true),
            new Point(0.4816f, 0.3525f, 385, true), new Point(0.4816f, 0.72000f, 385, true) };

    private static final Point sVideoPos[] = new Point[] {
            new Point(0f, 0f, -500), new Point(0f, 0f, -500),
            new Point(0f, 0f, -500), new Point(0f, 0f, -400),
            new Point(0f, 0f, -500), new Point(0f, 0f, -500),
            new Point(0f, 0f, -500), new Point(0f, 0f, -400)
    };

    private Point mVideoPos[];
    final static int sAnimationId[] = new int[] {
            R.raw.video_memory_movieclip00, R.raw.video_memory_movieclip01,
            R.raw.video_memory_movieclip02, R.raw.video_memory_movieclip03,
            R.raw.video_memory_movieclip04, R.raw.video_memory_movieclip05,
            R.raw.video_memory_movieclip06, R.raw.video_memory_movieclip07
    };

    private final static int sVideoTextureTag = 99;
    private final Context mContext;
    private final Container mRoot = new Container();
    private final AnimationGroup mPlay = new AnimationGroup();
    private final Vector<Container> mTargets = new Vector<Container>();

    /**
     * Dynamically detect profiling setting.
     */
    private static final String PROFILING_PROP = "vo.profile";
    private boolean mIsProfiling;
    private void detectProfilingSetting() {
        mIsProfiling = SystemProperties.getBoolean(PROFILING_PROP, false);
        Log.i(TAG, "profile : " + PROFILING_PROP + ", value : " + mIsProfiling);
    }

    /**
     * Constructor.
     * The setCacheDir() is a necessary call for multiple ngin3d-supported plugins
     */
    public VideoOrbView(Context context) {
        super(context);
        mContext = context;
        setCacheDir(mContext, "/data/system/videoorbplugin");
        detectProfilingSetting();
        LayoutManager.setDisplayMetrics(context);
        Log.v(TAG, "VideoOrbView() is called.");
    }

    /**
     * Dynamically detect whether videoOrbPlugin is set as lockscreen layer.
     * The detection method is from "Setting" application.
     */
    public static final String CURRENT_KEYGURAD_LAYER_KEY = "mtk_current_keyguard_layer";
    private boolean checkIfCurrentLayer() {
        String currentLayer = android.provider.Settings.System.getString(
                mContext.getContentResolver(), CURRENT_KEYGURAD_LAYER_KEY);
        Log.v(TAG, "current layer : " + currentLayer);
        return (currentLayer != null) && currentLayer.contains(PACKAGE_NAME);
    }

    IPOBootReceiver mIPOBootReceiver;
    /**
     * Called by LockScreen to create VideoOrbView.
     * VideoOrbView has to detect whether it has been set as current style.
     * In order to speed up launching, only set up necessary actors in create().
     * Requested in IKeyguardLayer
     */
    public View create() {
        if (!checkIfCurrentLayer()) {
            Log.v(TAG, "Package : " + PACKAGE_NAME + " is disabled");
            return null;
        }

        long real_start = SystemClock.elapsedRealtime();
        long thread_start = SystemClock.currentThreadTimeMillis();
        setupStage();
        setupBackground();
        if (mIsProfiling) {
            long real_end = SystemClock.elapsedRealtime();
            long thread_end = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "create() in reality : " + (real_end - real_start) +
                    ", in thread : " + (thread_end - thread_start));
        }
        Log.v(TAG, "create() is called : Configuration : " + getResources().getConfiguration());
        return this;
    }

    /**
     * Called by LockScreen to destroy VideoOrbView.
     * Requested in IKeyguardLayer
     */
    public void destroy() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        if (mIPOBootReceiver != null) {
            getContext().unregisterReceiver(mIPOBootReceiver);
            mIPOBootReceiver = null;
        }
        cleanup();
        Log.v(TAG, "destroy() is called");
    }

    /**
     * Called by LockScreen to get VideoOrbView layer information.
     * Requested in IKeyguardLayer
     */
    public KeyguardLayerInfo getKeyguardLayerInfo() {
        KeyguardLayerInfo info = new KeyguardLayerInfo();
        info.layerPackage = PACKAGE_NAME; // package name
        info.nameResId = R.string.plugin_name;
        info.descResId = R.string.plugin_description;
        info.previewResId = R.drawable.preview;
        info.configIntent = new Intent("com.mediatek.action.VIDEOORB_TRANSCODE_VIDEO");
        return info;
    }

    protected void onAttachedToWindow() {
        long real_start = SystemClock.elapsedRealtime();
        long thread_start = SystemClock.currentThreadTimeMillis();
        super.onAttachedToWindow();

        setupActors();
        if (mIPOBootReceiver == null) {
            mIPOBootReceiver = new IPOBootReceiver();
        }
        getContext().registerReceiver(mIPOBootReceiver, new IntentFilter(IPO_BOOT_INTENT));
        if (mIsProfiling) {
            long real_end = SystemClock.elapsedRealtime();
            long thread_end = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "onAttachedToWindow() in reality : " + (real_end - real_start) +
                    ", in thread : " + (thread_end - thread_start));
        }
        Log.i(TAG, "onAttachedToWindow() was called. Context : " + getContext());
    }

    private void setupStage() {
        mStage.add(mRoot);
        mStage.setProjection(Stage.UI_PERSPECTIVE_LHC, 2.0f, 3000.0f, -1111.0f);
    }

    ScheduledExecutorService mScheduler = Executors.newSingleThreadScheduledExecutor();
    private void setupActors() {
        mScheduler.schedule(new ActorLoader(), 200, TimeUnit.MICROSECONDS);
    }

    private void reloadIfNoContent() {
        // Only reload while there is no content in previous round
        if (mVideoCount == 0) {
            mScheduler.schedule(new Reloader(), 200, TimeUnit.MICROSECONDS);
        }
    }

    private void cleanup() {
        mStage.unrealize();
        mStage.removeAll();
        removeRules();
    }

    /**
     * Populate a video texture in stage and bind animation on it.
     * @param Point anchor point position
     * @param Point video texture relative position to anchor point.
     * @param int video resource id.
     * @param int animation resource id.
     */
    private BasicAnimation prepareVideoTextureAndBindAnimation(Point parentPos, Point actorPos, int animationId) {
        Scale frameScale = new Scale(LayoutManager.getScaleFactor(), LayoutManager.getScaleFactor());
        // Setup the frame image to avoid alias artifact.
        Image frontFrame = Image.createFromResource(getResources(), R.drawable.video_frame);
        frontFrame.setDoubleSided(true);
        frontFrame.setPosition(new Point(actorPos.x, actorPos.y, actorPos.z - 0.2f));
        frontFrame.setScale(frameScale);

        // To avoid back side alias artifact, add back frame into container.
        Image backFrame = Image.createFromResource(getResources(), R.drawable.video_frame);
        backFrame.setDoubleSided(true);
        backFrame.setPosition(new Point(actorPos.x, actorPos.y, actorPos.z + 0.2f));
        backFrame.setScale(frameScale);

        Container parent = new Container();
        parent.setPosition(parentPos);
        parent.add(frontFrame, backFrame);
        parent.setVisible(false); // Default make the whole container as invisible.
        mTargets.add(parent);
        mRoot.add(parent);
        return (BasicAnimation)AnimationLoader.loadAnimation(
                mContext, animationId).setLoop(true).setTarget(parent).
                disableOptions(Animation.SHOW_TARGET_ON_STARTED);
    }

    private int getMaxAnimatedVideo() {
        return mSource.getMediaCount();
    }

    private IMediaSource mSource;
    private int mVideoCount;
    private void setupMediaSource() {
        mSource = MediaSourceFactory.getInstance(getContext().getContentResolver());
        mVideoCount = getMaxAnimatedVideo();
    }

    public void setupVideoActors() {
        long real_start = SystemClock.elapsedRealtime();
        long thread_start = SystemClock.currentThreadTimeMillis();
        int count = getMaxAnimatedVideo();
        final int[] measurement = LayoutManager.getIntArray(getResources(), R.array.video_measure, true);
        Log.i(TAG, "measurement width : " + measurement[0] + ", height : " + measurement[1]);
        for (int i = 0 ; i < count; ++i) {
            Video videoTexture = mSource.getMedia(mContext,
                    i, measurement[0], measurement[1]);
            if (videoTexture == null) {
                break;
            }
            videoTexture.setPosition(mVideoPos[i]);
            videoTexture.setTag(sVideoTextureTag);
            Container parent = mTargets.get(i);
            if (parent != null) {
                parent.add(videoTexture);
            }
        }
        mSource.close();
        if (mIsProfiling) {
            long real_end = SystemClock.elapsedRealtime();
            long thread_end = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "setupVideoActors in reality : " + (real_end - real_start) +
                    ", in thread : " + (thread_end - thread_start));
        }
        Log.v(TAG, "setupVideoActors ");
    }

    private class ActorLoader implements Runnable {
        public void run() {
            long real_start = SystemClock.elapsedRealtime();
            long thread_start = SystemClock.currentThreadTimeMillis();
            setupMediaSource();
            setupLights();
            setupVideoActorAnimations();
            setupVideoActors();
            firstAnimationControlRules();
            setupAcceleratorSensor();
            if (mIsProfiling) {
                long real_end = SystemClock.elapsedRealtime();
                long thread_end = SystemClock.currentThreadTimeMillis();
                Log.i(TAG, "ActorLoader in reality : " + (real_end - real_start) +
                        ", in thread : " + (thread_end - thread_start));
            }
        }
    }

    private class Reloader implements Runnable {
        public void run() {
            long real_start = SystemClock.elapsedRealtime();
            long thread_start = SystemClock.currentThreadTimeMillis();
            setupVideoActorAnimations();
            setupVideoActors();
            firstAnimationControlRules();
            if (mIsProfiling) {
                long real_end = SystemClock.elapsedRealtime();
                long thread_end = SystemClock.currentThreadTimeMillis();
                Log.i(TAG, "ActorLoader in reality : " + (real_end - real_start) +
                        ", in thread : " + (thread_end - thread_start));
            }
        }
    }

    public void setupVideoActorAnimations() {
        mVideoPos = LayoutManager.getActorPos();
        Point[] animationPos = LayoutManager.getAnimationPos();
        for (int i = 0; i < getMaxAnimatedVideo(); ++i) {
            mPlay.add(prepareVideoTextureAndBindAnimation(
                    animationPos[i], mVideoPos[i], sAnimationId[i]));
        }
        mPlay.start();

        // Also setup the animation in front of window table.
        mWindow.setupTable();
    }

    /**
     * Setup background.
     * The reason why separate background setup from lighting is
     * We try to show up background as soon as possible.
     * Therefore, lighting and relative animation are setup in later stage.
     */
    Image mBg;
    private void setupBackground() {
        mBg = Image.createFromResource(getResources(), R.drawable.bg);
        mBg.setPosition(LayoutManager.getBackgroundPos());
        mBg.setScale(new Scale(6.0f, 10.0f));
        mStage.add(mBg);
    }

    /**
     * Setup lights image and setup lighting and background animations.
     */
    AnimationGroup mPlay2 = new AnimationGroup();
    Image mLightOne;
    Image mLightTwo;
    private void setupLights() {
        mLightOne = Image.createFromResource(getResources(), R.drawable.light_01);
        mLightOne.setPosition(LayoutManager.getLightPos());
        mStage.add(mLightOne);

        mLightTwo = Image.createFromResource(getResources(), R.drawable.light_02);
        mLightTwo.setPosition(LayoutManager.getLightPos());
        mStage.add(mLightTwo);

        mPlay2.add( AnimationLoader.loadAnimation(mContext, R.raw.video_memory_light_01).setLoop(true).setTarget(mLightOne) );
        mPlay2.add( AnimationLoader.loadAnimation(mContext, R.raw.video_memory_light_02).setLoop(true).setTarget(mLightTwo) );
        mPlay2.add( AnimationLoader.loadAnimation(mContext, R.raw.video_memory_cover_02).setLoop(true).setTarget(mBg)       );
        mPlay2.start();
    }

    private PlayWindow mWindow = new PlayWindow();
    private class PlayWindow {
        private ArrayList<Pair<Float, Float>> progressMaps = new ArrayList<Pair<Float, Float>>();
        public void setupTable() {
            progressMaps.add(new Pair<Float, Float>(new Float(0.875f), new Float(0.125f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.000f), new Float(0.250f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.125f), new Float(0.375f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.264f), new Float(0.514f)));

            progressMaps.add(new Pair<Float, Float>(new Float(0.375f), new Float(0.525f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.500f), new Float(0.750f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.625f), new Float(0.875f)));
            progressMaps.add(new Pair<Float, Float>(new Float(0.750f), new Float(1.000f)));
        }

        public boolean isInFrontWindow(float p, int i) {
            if (i >= progressMaps.size())
                return false;

            Pair<Float, Float> boundary = progressMaps.get(i);
            if (boundary.first < boundary.second)
                return (p >= boundary.first && p <= boundary.second);
            else
                return (p >= boundary.first && p <= 1) || (p >= 0 && p <= boundary.second);
        }
    }

    /**
     * Pause background videos playback, and start foreground videos.
     */
    class Rules implements Runnable {
        public void run() {
            int count = mPlay.getAnimationCount();
            for (int i = 0; i < count; ++i) {
                BasicAnimation animation = (BasicAnimation) mPlay.getAnimation(i);
                Container parent = (Container) animation.getTarget();
                Video video = (Video) ((Container) animation.getTarget()).findChildByTag(sVideoTextureTag);
                if (video == null || parent == null) {
                    continue;
                }

                if (mWindow.isInFrontWindow(animation.getProgress(), i) ) {
                    video.play();
                } else {
                    video.pause();
                }
            }
            setupAnimationControlRules();
        }
    }


    Handler mHandler = new Handler();
    Rules mRule = new Rules();
    /**
     * Apply animation controls.
     */
    private void setupAnimationControlRules() {
        if (mHandler == null)
            return;
        mHandler.postDelayed(mRule, 2000);
    }

    private void firstAnimationControlRules() {
        if (mHandler == null)
            return;
        mHandler.post(mRule);
    }

    private void removeRules() {
        if (mHandler == null)
            return;
        mHandler.removeCallbacks(mRule);
    }

    /**
     * Acceleration detection.
     */
    SensorManager mSensorManager;
    Sensor mAccelerator;

    /**
     * Setup acceleration sensor initialization.
     */
    public void setupAcceleratorSensor(){
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerator = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mAccelerator, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    final static Rotation[] rotLevels = new Rotation[] {
            new Rotation(15, 0, 0), new Rotation(10, 0, 0), new Rotation(5, 0, 0),
            new Rotation(0, 0, 0), new Rotation(-5, 0, 0) };

    final static float levelStep = (rotLevels.length / 9.81f); // 9.81 : gravity
    Rotation mPrevRotation = rotLevels[3]; // zero rotation
    float mYAcceleration;

    public void onAccuracyChanged(android.hardware.Sensor sensor, int i) {
        // Not implement by intent
    }

    /**
     * Setup acceleration sensor initialization.
     */
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mYAcceleration = e.values[1]; // only observe acceleration of Y axis.
            if (isChangeLargeEnough()) {
                applyRotationOnRoot();
            }
        }
    }

    /**
     * Check whether phone rotated angle phone is large enough to apply rotation transition.
     */
    private boolean isChangeLargeEnough() {
        int rotLevel = (int)(mYAcceleration / levelStep);
        if (rotLevel >= rotLevels.length || rotLevel < 0) {
            return false;
        }

        Rotation rot = rotLevels[rotLevel];
        // Do not apply same rotation again.
        if (rot == mPrevRotation) {
            return false;
        }
        mPrevRotation = rot;
        return true;
    }

    /**
     * Apply rotation on mRoot container to achieve rotation transition..
     */
    private void applyRotationOnRoot() {
        Transaction.beginImplicitAnimation();
        mRoot.setRotation(mPrevRotation);
        Transaction.commit();
    }

    private static final String IPO_BOOT_INTENT = "android.intent.action.ACTION_BOOT_IPO";
    public class IPOBootReceiver extends BroadcastReceiver {
        public IPOBootReceiver() {}

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(IPO_BOOT_INTENT)) {
                reloadIfNoContent();
                Log.v(TAG, "IPOBootReceiver.onReceive is called.");
            }
        }
    }
}