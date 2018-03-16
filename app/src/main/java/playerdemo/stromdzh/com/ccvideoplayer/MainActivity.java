package playerdemo.stromdzh.com.ccvideoplayer;

import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.tongxue.videoplayer.controller.IPlayerImpl;
import com.tongxue.videoplayer.util.OrientationUtil;
import com.tongxue.videoplayer.widget.TxVideoPlayer;

public class MainActivity extends AppCompatActivity {

    private TxVideoPlayer mTxVideoPlayer;
    private String url = "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8";
//    private String url = "http://cdn2.txbimg.com/test/video/275220270.ts";
//    private String url = "http://v.cctv.com/flash/mp4video6/TMS/2011/01/05/cf752b1c12ce452b3040cab2f90bc265_h264818000nero_aac32-1.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTxVideoPlayer = findViewById(R.id.mTxVideoPlayer);

        mTxVideoPlayer.setmHostActivity(this);
        mTxVideoPlayer.audoPlay(true);
        Log.i("test", "init video=>" + url);
        mTxVideoPlayer.setUrl(url);
        mTxVideoPlayer.setDefaultVertical(true);
        mTxVideoPlayer.setTitle("");
        mTxVideoPlayer.setPlayerController(new IPlayerImpl() {
            @Override
            public void onBack() {
                super.onBack();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mTxVideoPlayer.onHostPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTxVideoPlayer.onHostResume();
    }

    @Override
    protected void onDestroy() {
        mTxVideoPlayer.onHostDestroy();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mTxVideoPlayer != null) {
            mTxVideoPlayer.updateActivityOrientation();
            if (newConfig.orientation == OrientationUtil.HORIZONTAL) {
                OrientationUtil.forceOrientation(this, OrientationUtil.VERTICAL);
            }
        }
    }
}
