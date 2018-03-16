package com.tongxue.videoplayer.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bokecc.sdk.mobile.play.DWMediaPlayer;
import com.tongxue.videoplayer.R;
import com.tongxue.videoplayer.constant.PlayState;
import com.tongxue.videoplayer.controller.AnimationImpl;
import com.tongxue.videoplayer.controller.IPlayerImpl;
import com.tongxue.videoplayer.controller.OnScreenChangeListener;
import com.tongxue.videoplayer.util.DensityUtil;
import com.tongxue.videoplayer.util.Formatter;
import com.tongxue.videoplayer.util.OrientationUtil;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;

/**
 * author : dzh .
 * date   : 2018/1/26
 * desc   : 视屏播放器
 */
public class TxVideoPlayer extends RelativeLayout implements View.OnClickListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener {

    private final int CODE_VIDEO_PROGRESS = 0; //更新进度条
    private final int CODE_VIDEO_AUTO_HIDE = 1; //自动隐藏控制控件
    private final int CODE_VIDEO_AUTO_PLAY_EVENT = 2; //自动播放事件
    private static final int TIME_AUTO_HIDE_BARS_DELAY = 5000;
    private int iconPause = R.drawable.zz_player_pause;
    private int iconPlay = R.drawable.zz_player_play;
    int iconShrink = R.drawable.zz_player_shrink;
    int iconExpand = R.drawable.zz_player_expand;
    private Animation mEnterFromTop;
    private Animation mEnterFromBottom;
    private Animation mExitFromTop;
    private Animation mExitFromBottom;

    private ImageView iv_video_default;
    private ImageView iv_center_play;
    private ImageView mLoadingView;
    private DWMediaPlayer mPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TextView positionTxt;
    private TextView durationTxt;
    private CustomSeekBar progressBar;
    private ImageView iv_play_pause;
    private ImageView iv_toggle_expandable;
    private RelativeLayout mController;
    private LinearLayout mTitleBar;
    private TextView tv_title;
    private String mUrl = null;
    private boolean inSeek = false;
    private WeakReference<Activity> mHostActivity;
    private int mLastPlayingPos = -1;//onPause时的播放位置
    private int mDuration;
    private boolean isActivityStop = false;
    private IPlayerImpl mIPlayerImpl;
    private boolean mShowVerticalTitleBar = false;
    private int mPlayState = PlayState.IDLE; //记录当前视屏的播放状态
    private boolean isFirstOccur; //是否是第一次触发播放到50%事件
    private MyHandler mHandler;
    private boolean mDefaultVertical = true;
    private boolean mAudoPlay = false;
    private int mVerticalHeight = 0; //竖屏本空间的高度
    private int mVerticalWidth = 0; //竖屏本空间的宽度
    @SuppressWarnings("all")
    private int mVideoHeight = 0;  //视屏的高度
    @SuppressWarnings("all")
    private int mVideoWidth = 0;   //视屏的宽度

    private boolean isResume = false;

    public TxVideoPlayer(Context context) {
        this(context, null);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TxVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        inflate(getContext(), R.layout.view_tx_player, this);
        mHandler = new MyHandler(this);
        mSurfaceView = findViewById(R.id.mSurfaceView);
        positionTxt = findViewById(R.id.tv_current_time);
        durationTxt = findViewById(R.id.tv_total_time);
        progressBar = findViewById(R.id.csb);
        findViewById(R.id.rl_play_pause).setOnClickListener(this);
        iv_play_pause = findViewById(R.id.iv_play_pause);
        findViewById(R.id.rl_toggle_expandable).setOnClickListener(this);
        iv_toggle_expandable = findViewById(R.id.iv_toggle_expandable);
        mController = findViewById(R.id.mController);
        findViewById(R.id.mControllerContent).setOnClickListener(this);
        mTitleBar = findViewById(R.id.ll_video_title);
        findViewById(R.id.rl_back).setOnClickListener(this);
        tv_title = findViewById(R.id.tv_title);
        mLoadingView = findViewById(R.id.loadingImageView);
        iv_video_default = findViewById(R.id.iv_video_default);
        iv_center_play = findViewById(R.id.iv_center_play);
        iv_center_play.setOnClickListener(this);
        mSurfaceView.setOnClickListener(this);
        initAnimation();
        initSeekBar();
        initSurfaceView();
    }

    /**
     * 初始化SurfaceView
     */
    private void initSurfaceView() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //activity pause  就会调动 这里。。。。
                Log.i("test", "surfaceDestroyed======================>");
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i("test", "surfaceCreated======================>");
                mSurfaceCreated();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i("test", "surfaceChanged======================>");
            }
        });
    }

    private void mSurfaceCreated() {
        Log.i("test", "mSurfaceCreated====》" + isActivityStop);
        if (isActivityStop) {  //播放着被强制暂停的
            try {
                mPlayer.setDisplay(mSurfaceHolder);
                mPlayer.start();
                setPlayState(PlayState.PLAY);
                isActivityStop = false;
                startUpdateTimer();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("test", "Exception   mSurfaceCreated====》" + isActivityStop);
            }
        } else {
            if (mPlayState == PlayState.IDLE || mPlayState == PlayState.STOP || mPlayState == PlayState.COMPLETE || mPlayState == PlayState.ERROR)
                initVodPlayer();
            mPlayer.setDisplay(mSurfaceHolder);
            Log.i("test", "mSurfaceCreated   mAudoPlay====》" + mAudoPlay);
            Log.i("test", "mSurfaceCreated   mPlayState====》" + mPlayState);
            if (mPlayState == PlayState.IDLE || mPlayState == PlayState.COMPLETE || mPlayState == PlayState.ERROR) {
                if (mAudoPlay) {
                    setCenterPlayStata(false);
                    startLoadingAnimation();
                    mHandler.sendEmptyMessageDelayed(CODE_VIDEO_AUTO_PLAY_EVENT, 2000);
                } else {
                    setBgPlayDefault(false);
                }
            }
        }
        //加载到最后一帧
        reloadLastFrame();
    }

    /**
     * 加载最后一帧
     */
    private void reloadLastFrame() {
        if (mPlayState == PlayState.PREPARE || mPlayState == PlayState.PAUSE || mPlayState == PlayState.STOP) {
            if (mPlayer != null) {
                int lastFramePos = mLastPlayingPos - (3 * 1000) <= 0 ? 0 : mLastPlayingPos - (3 * 1000);
                mPlayer.seekTo(lastFramePos);
            }
        }
    }

    /**
     * 初始化标题栏/控制栏显隐动画效果
     */
    private void initAnimation() {
        mEnterFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_top);
        mEnterFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.enter_from_bottom);
        mExitFromTop = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_top);
        mExitFromBottom = AnimationUtils.loadAnimation(getContext(), R.anim.exit_from_bottom);

        mEnterFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(VISIBLE);
            }
        });
        mEnterFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(VISIBLE);
            }
        });
        mExitFromTop.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mTitleBar.setVisibility(GONE);
            }
        });
        mExitFromBottom.setAnimationListener(new AnimationImpl() {
            @Override
            public void onAnimationEnd(Animation animation) {
                super.onAnimationEnd(animation);
                mController.setVisibility(GONE);
            }
        });
    }

    /**
     * 初始化进度
     */
    private void initSeekBar() {

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (fromUser && mPlayer != null) {
                    try {
                        mPlayer.seekTo(seekBar.getProgress());
                        mLastPlayingPos = mPlayer.getCurrentPosition();
                        positionTxt.setText(Formatter.formatTime(mLastPlayingPos));
                        durationTxt.setText(Formatter.formatTime(mDuration));
                        progressBar.setMax(mDuration);
                        progressBar.setProgress(mLastPlayingPos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopUpdateTimer();
                inSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startUpdateTimer();
                inSeek = false;
            }
        });
    }

    /**
     * 初始化视屏播放器
     */
    private synchronized void initVodPlayer() {
        //创建player对象
        if (mPlayer != null) {
            mPlayer.reset();
        }
        mPlayer = new DWMediaPlayer();
//        mPlayer =  DWMediaPlayer.create(this,mUrl);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnSeekCompleteListener(this);
    }

    /**
     * 点击事件
     *
     * @param v 控件
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rl_play_pause) { //点击播放暂停
            if (isShowtopDefault()) return;
            playPause();
        } else if (id == R.id.iv_center_play) { //首次播放，中间的播放按钮
            setCenterPlayStata(false);
            startLoadingAnimation();
            startNew();
        } else if (id == R.id.rl_toggle_expandable) { //缩放
            if (isShowtopDefault()) return;
            OrientationUtil.changeOrientation(mHostActivity.get(), mScreenListener);
        } else if (id == R.id.rl_back) { //返回
            vvBack();
        } else if (id == R.id.mSurfaceView) { //控制条
            controlBar();
        }
    }

    private boolean isShowtopDefault() {
        return iv_video_default == null || iv_video_default.getVisibility() == VISIBLE;
    }

    /**
     * @param show 显示中间播放按钮
     */
    @SuppressWarnings("all")
    private void setCenterPlayStata(boolean show) {
        if (iv_center_play == null) return;
        if (show) {
            if (iv_center_play.getVisibility() != VISIBLE)
                iv_center_play.setVisibility(View.VISIBLE);
        } else {
            if (iv_center_play.getVisibility() != GONE)
                iv_center_play.setVisibility(View.GONE);
        }
    }

    /**
     * 横竖屏切换监听
     */
    private OnScreenChangeListener mScreenListener = new OnScreenChangeListener() {
        @Override
        public void OnScreenChange(int state) {

            if (state == OrientationUtil.VERTICAL) {
                resetSurfaceViewSize(true);
            } else {
                resetSurfaceViewSize(false);
            }
        }
    };

    /**
     * 控制条
     */
    private void controlBar() {
        if (mController.getVisibility() == VISIBLE) {
            animateShowOrHideBars(false);
        } else {
            animateShowOrHideBars(true);
        }
    }

    /**
     * 返回
     */
    private void vvBack() {
        if (mIPlayerImpl != null) {
            mIPlayerImpl.onBack();
        } else {
            if (mHostActivity.get() != null)
                mHostActivity.get().finish();
        }
    }

    /**
     * 播放暂停
     */
    private void playPause() {
        inSeek = false;
        if (mPlayer == null) return;
        if (mPlayer.isPlaying()) {
            Log.i("test", "playPause==============>" + 1);
            pause();
            setPlayState(PlayState.PAUSE);
        } else {
            resume();
        }
    }

    /**
     * 带动画效果的显隐标题栏和控制栏
     */
    private void animateShowOrHideBars(boolean show) {
        mController.clearAnimation();
        mTitleBar.clearAnimation();

        if (show) {
            if (mController.getVisibility() != VISIBLE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mEnterFromTop);
                mController.startAnimation(mEnterFromBottom);
            }
            sendAutoHideBarsMsg();
        } else {
            if (mController.getVisibility() != GONE) {
                if (isVerticalShow())
                    mTitleBar.startAnimation(mExitFromTop);
                else
                    mTitleBar.setVisibility(GONE);
                mController.startAnimation(mExitFromBottom);
            }
        }
    }

    /**
     * 竖屏时候是否需要显示标题栏
     *
     * @return 返回true 需要显示 false：不需要显示
     */
    private boolean isVerticalShow() {
        Activity activity = mHostActivity.get();
        if (activity == null) return false;
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());
        //竖屏返回mShowVerticalTitleBar  横屏返回true
        return (orientation != OrientationUtil.VERTICAL || mShowVerticalTitleBar);
    }

    /**
     * 直接显隐标题栏和控制栏
     */
    private void forceShowOrHideBars(boolean show) {
        if (mController == null || mTitleBar == null) return;
        mTitleBar.clearAnimation();
        mController.clearAnimation();
        if (show) {
            mController.setVisibility(VISIBLE);
            if (isVerticalShow())
                mTitleBar.setVisibility(VISIBLE);
            else
                mTitleBar.setVisibility(GONE);
        } else {
            mController.setVisibility(GONE);
            mTitleBar.setVisibility(GONE);
        }
    }

    /**
     * 点击按钮播放
     */
    private void resume() {
        if (mPlayer == null) return;
        Log.i("test", "resume mPlayState====》" + mPlayState);
        if (mPlayState == PlayState.PREPARE || mPlayState == PlayState.PAUSE) {
            startHasPrepare();
        } else {
            startNew();
        }
    }

    private void startHasPrepare() {
        if (mPlayer == null) return;
        try {
            mPlayer.start();
            mPlayState = PlayState.PLAY;
            setPlayState(PlayState.PLAY);
            startUpdateTimer();
            Log.i("test", "startHasPrepare====》" + mPlayState);
        } catch (Exception e) {
            Log.i("test", "Exception startHasPrepare====》" + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 设置地址并且播放
     */
    private void startNew() {
        if (mPlayer == null) return;
        try {
            mPlayer.clearMediaData();
            mPlayer.reset();
            mPlayer.setDataSource(mUrl);
            mPlayer.prepareAsync();
            Log.i("test", "startNew====》" + mPlayState);
        } catch (Exception e) {
            Log.i("test", "Exception  startNew====》" + mPlayState);
            e.printStackTrace();
        }
    }

    /**
     * 发送message给handler,自动隐藏标题栏
     */
    private void sendAutoHideBarsMsg() {
        //  初始自动隐藏标题栏和控制栏
        mHandler.removeMessages(CODE_VIDEO_AUTO_HIDE);
        mHandler.sendEmptyMessageDelayed(CODE_VIDEO_AUTO_HIDE, TIME_AUTO_HIDE_BARS_DELAY);
    }

    /**
     * 设置播放按钮的状态
     *
     * @param curPlayState 是不是播放状态
     */
    private void setPlayState(int curPlayState) {

        switch (curPlayState) {
            case PlayState.PLAY:
                iv_play_pause.setImageResource(iconPause);
                break;
            case PlayState.IDLE:
            case PlayState.PAUSE:
            case PlayState.STOP:
            case PlayState.COMPLETE:
            case PlayState.ERROR:
                iv_play_pause.setImageResource(iconPlay);
                break;
        }
    }

    /**
     * 暂停
     */
    private void pause() {
        if (mPlayer == null) return;
        try {
            mPlayer.pause();
            mPlayState = PlayState.PAUSE;
            setPlayState(PlayState.PAUSE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始进度
     */
    private void startUpdateTimer() {
        mHandler.removeMessages(CODE_VIDEO_PROGRESS);
        mHandler.sendEmptyMessageDelayed(CODE_VIDEO_PROGRESS, 1000);
    }

    /**
     * 结束进度
     */
    private void stopUpdateTimer() {
        mHandler.removeMessages(CODE_VIDEO_PROGRESS);
    }

    //MyHandler
    private static class MyHandler extends Handler {

        private final WeakReference<TxVideoPlayer> layout;

        private MyHandler(TxVideoPlayer layout) {
            this.layout = new WeakReference<>(layout);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (layout.get() == null) return;
            TxVideoPlayer view = layout.get();
            if (view == null) return;
            if (msg.what == view.CODE_VIDEO_AUTO_HIDE) { //控制栏
                view.animateShowOrHideBars(false);
            } else if (msg.what == view.CODE_VIDEO_PROGRESS) { //进度条
                try {
                    if (view.mPlayer != null)
                        view.mLastPlayingPos = view.mPlayer.getCurrentPosition();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                view.showVideoProgressInfo(view.mLastPlayingPos);
                if (view.isHalfProgress(view.mLastPlayingPos) && view.mIPlayerImpl != null) {
                    view.mIPlayerImpl.onFinishTask();
                }
            } else if (msg.what == view.CODE_VIDEO_AUTO_PLAY_EVENT) { //自动播放
                view.mAudoPlay = false;
                Log.i("test", "CODE_VIDEO_AUTO_PLAY_EVENT====》");
                view.resume();
            }
        }
    }

    /**
     * 判断播放到50%
     *
     * @param mLastPlayingPos 当前播放的进度
     * @return 是不是在50%
     */
    private boolean isHalfProgress(int mLastPlayingPos) {
        if (mLastPlayingPos <= 0 || mDuration <= 0) return false;
        if ((mLastPlayingPos * 100 / mDuration >= 50)) {
            if (isFirstOccur) {
                mIPlayerImpl.onFinishTask();
                isFirstOccur = false;
            }
        }
        return false;
    }

    /**
     * 放大 缩小 控件样式
     *
     * @param orientation 方向
     */
    private void setOrientation(int orientation) {
        //更新全屏图标
        if (orientation == OrientationUtil.HORIZONTAL) {
            iv_toggle_expandable.setImageResource(iconShrink);
        } else {
            iv_toggle_expandable.setImageResource(iconExpand);
        }
    }

    /**
     * 开始loading动画
     */
    private void startLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.VISIBLE);
            ((AnimationDrawable) mLoadingView.getDrawable()).start();
        }
    }

    /**
     * 结束loading动画
     */
    private void stopLoadingAnimation() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.GONE);
            ((AnimationDrawable) mLoadingView.getDrawable()).stop();
        }
    }

    /**
     * 显示视屏播放进度信息
     *
     * @param curPosition 当前播放的进度
     */
    private void showVideoProgressInfo(int curPosition) {
        if ((mPlayer.isPlaying())
                && !inSeek) {
            positionTxt.setText(Formatter.formatTime(curPosition));
            durationTxt.setText(Formatter.formatTime(mDuration));
            progressBar.setMax(mDuration);
            progressBar.setProgress(curPosition);
        }
        mLastPlayingPos = curPosition;
        startUpdateTimer();
    }

    /**
     * 隐藏默认背景
     */
    private void setBgPlayDefault(boolean show) {
        if (show) {
            if (iv_video_default.getVisibility() != VISIBLE)
                iv_video_default.setVisibility(View.VISIBLE);
            setCenterPlayStata(true);
        } else {
            if (iv_video_default.getVisibility() != GONE)
                iv_video_default.setVisibility(View.GONE);
            setCenterPlayStata(false);
            stopLoadingAnimation();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!isResume) return;
        isFirstOccur = true;
        mPlayState = PlayState.PREPARE;
        mDuration = mp.getDuration();
        mLastPlayingPos = 0;
        mPlayer.start();
        setProgressBarState(true);
        resetSurfaceViewSize(mDefaultVertical);
        setBgPlayDefault(false);
        mPlayState = PlayState.PLAY;
        setPlayState(PlayState.PLAY);
        startUpdateTimer();
    }

    /**
     * 播放结束和播放错误 隐藏进度条
     *
     * @param show true:显示进度条
     */
    private void setProgressBarState(boolean show) {
        if (progressBar == null) return;
        if (show) {
            if (progressBar.getVisibility() != VISIBLE)
                progressBar.setVisibility(VISIBLE);
        } else {
            if (progressBar.getVisibility() != INVISIBLE)
                progressBar.setVisibility(INVISIBLE);
        }
    }

    private int getVideoHei() {
        if (mPlayer == null || mPlayer.getVideoHeight() <= 0)
            return DensityUtil.dip2px(getContext(), 200);
        return mPlayer.getVideoHeight();
    }

    private int getVideoWid() {
        if (mPlayer == null || mPlayer.getVideoHeight() <= 0) {
            int widthInPx = (int) DensityUtil.getWidthInPx(getContext());
            return DensityUtil.dip2px(getContext(), widthInPx);
        }
        return mPlayer.getVideoWidth();
    }

    /**
     * 横屏设置视屏宽高
     */
    private void resetSurfaceViewSize(boolean isVertical) {

        mVideoHeight = getVideoHei();
        mVideoWidth = getVideoWid();

        if (isVertical) { //竖屏
            int widthInPx = (int) DensityUtil.getWidthInPx(getContext());

            if (mVerticalHeight <= 0)
                mVerticalHeight = DensityUtil.dip2px(getContext(), 200);
            if (mVerticalWidth <= 0)
                mVerticalWidth = DensityUtil.dip2px(getContext(), widthInPx);
            Log.i("test", "videoHeight ==>" + mVideoHeight);
            Log.i("test", "videoWidth ==>" + mVideoWidth);
            RelativeLayout.LayoutParams layoutParams = (LayoutParams) mSurfaceView.getLayoutParams();

            double videop = (double) mVideoHeight / (double) mVideoWidth;
            double kp = (double) mVerticalHeight / (double) mVerticalWidth;
            if (videop > kp) { //视屏高宽比比控件大
                layoutParams.width = mVideoWidth * mVerticalHeight / mVideoHeight;
                layoutParams.height = DensityUtil.dip2px(getContext(), widthInPx);
            } else {
                layoutParams.width = DensityUtil.dip2px(getContext(), widthInPx);
                layoutParams.height = mVideoHeight * mVerticalWidth / mVideoWidth;
            }
            mSurfaceView.setLayoutParams(layoutParams);
        } else { //横屏
            RelativeLayout.LayoutParams layoutParams = (LayoutParams) mSurfaceView.getLayoutParams();
            layoutParams.width = LayoutParams.MATCH_PARENT;
            layoutParams.height = LayoutParams.MATCH_PARENT;
            mSurfaceView.setLayoutParams(layoutParams);
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mLastPlayingPos >= mDuration * 0.99) { //进度等于长度在完成
            setBgPlayDefault(true);
            mPlayer.reset();
            stopUpdateTimer();
            setProgressBarState(false);
            mLastPlayingPos = 0;
            mPlayState = PlayState.COMPLETE;
            setPlayState(PlayState.COMPLETE);
            progressBar.setProgress(0);
            if (mIPlayerImpl != null) {
                mIPlayerImpl.onComplete();
            }
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        setBgPlayDefault(true);
        stopLoadingAnimation();
        setProgressBarState(false);
        mPlayState = PlayState.ERROR;
        setPlayState(PlayState.ERROR);
//        if (mIPlayerImpl != null) {
//            mIPlayerImpl.onError();
//        }
        Log.i("test", "onError==>what:" + what + "   extra:" + extra);
        return false;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    //----------------------------------以下方法可供外部调用------------------------------------

    /**
     * 播放器横屏
     */
    public void updateActivityOrientation() {
        int orientation = OrientationUtil.getOrientation(mHostActivity.get());

        //更新播放器宽高
        float width = DensityUtil.getWidthInPx(mHostActivity.get());
        float height = DensityUtil.getHeightInPx(mHostActivity.get());
        if (orientation == OrientationUtil.HORIZONTAL) {
            getLayoutParams().height = (int) height;
            getLayoutParams().width = (int) width;
            resetSurfaceViewSize(false);
        } else {
            width = DensityUtil.getWidthInPx(mHostActivity.get());
            height = DensityUtil.dip2px(mHostActivity.get(), 200f);
            resetSurfaceViewSize(true);
        }
        getLayoutParams().height = (int) height;
        getLayoutParams().width = (int) width;

        //需要强制显示再隐藏控制条,不然若切换为横屏时控制条是隐藏的,首次触摸显示时,会显示在200dp的位置
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
        //更新全屏图标
        setOrientation(orientation);
    }

    /**
     * 播放器控制功能对外开放接口,包括返回按钮,播放等...
     */
    public void setPlayerController(IPlayerImpl IPlayerImpl) {
        mIPlayerImpl = IPlayerImpl;
    }

    /**
     * 设置视屏标题
     *
     * @param title 标题
     */
    public void setTitle(String title) {
        if (mHostActivity == null)
            throw new IllegalArgumentException("没有绑定Activity");
        if (tv_title != null)
            tv_title.setText(title);
    }

    /**
     * 设置默认是横屏尺寸 还是竖屏尺寸
     *
     * @param isVertical true：竖屏样式  false：横屏样式
     */
    @SuppressWarnings("all")
    public void setDefaultVertical(boolean isVertical) {
        mDefaultVertical = isVertical;
    }

    /**
     * 设置视屏播放地址
     *
     * @param url 播放地址
     */
    public void setUrl(String url) {
        if (mHostActivity == null)
            throw new IllegalArgumentException("没有绑定Activity");
        this.mUrl = url;
    }

    /**
     * 在竖屏时候是否需要标题栏
     */
    @SuppressWarnings("unused")
    public void showTitleBarVertical(boolean showVerticalTitleBar) {
        mShowVerticalTitleBar = showVerticalTitleBar;
    }

    /**
     * 绑定Activity
     *
     * @param activity 绑定Activity
     */
    public void setmHostActivity(Activity activity) {
        mHostActivity = new WeakReference<>(activity);
    }

    /**
     * 自动播放
     *
     * @param isAuto true：自动播放  false：不会自动播放
     */
    @SuppressWarnings("all")
    public void audoPlay(boolean isAuto) {
        mAudoPlay = isAuto;
    }

    /**
     * 宿主页面onResume的时候从上次播放位置继续播放
     */
    public void onHostResume() {
        isResume = true;
        if (mHostActivity == null)
            return;
        setPlayState(PlayState.PAUSE);
//        if (isActivityStop) {
//            mPlayer.start();
//            isActivityStop = false;
//        }
        //强制弹出标题栏和控制栏
        forceShowOrHideBars(true);
        sendAutoHideBarsMsg();
    }

    /**
     * 宿主页面onPause的时候记录播放位置，好在onResume的时候从中断点继续播放
     * 如果在宿主页面onStop的时候才来记录位置,则取到的都会是0
     */
    public void onHostPause() {
        isResume = false;
        if (mHostActivity == null)
            return;
        if (mPlayer != null && mPlayer.isPlaying()) {
            isActivityStop = true;
            mPlayer.pause();
            mPlayState = PlayState.PAUSE;
        }
        stopUpdateTimer();
        mHandler.removeMessages(CODE_VIDEO_AUTO_HIDE);
        forceShowOrHideBars(false);
    }

    /**
     * 宿主页面destroy的时候页面恢复成竖直状态
     */
    public void onHostDestroy() {
        try {
            if (mPlayer != null)
                mPlayer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mPlayer != null) {
                mPlayer.reset();
                mPlayer.release();
            }
        }
        mPlayer = null;
        mIPlayerImpl = null;
//        if (mHostActivity == null)
//            throw new IllegalArgumentException("没有绑定Activity");
//        OrientationUtil.forceOrientation(mHostActivity.get(), OrientationUtil.VERTICAL);
    }
}
