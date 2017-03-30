package com.sw.degree360;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;
import com.google.vr.sdk.widgets.video.VrVideoEventListener;
import com.google.vr.sdk.widgets.video.VrVideoView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_IS_PAUSED = "isPaused";
    private static final String STATE_PROGRESS_TIME = "progressTime";
    private static final String STATE_VIDEO_DURATION = "videoDuration";
    public static final int LOAD_VIDEO_STATUS_UNKNOWN = 0;
    public static final int LOAD_VIDEO_STATUS_SUCCESS = 1;
    public static final int LOAD_VIDEO_STATUS_ERROR = 2;
    private int loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;
    protected VrVideoView videoWidgetView;
    private VrPanoramaView panoWidgetView;
    public boolean loadImageSuccessful;
    private com.google.vr.sdk.widgets.pano.VrPanoramaView.Options panoOptions = new com.google.vr.sdk.widgets.pano.VrPanoramaView.Options();
    private SeekBar seekBar;

    private ImageButton volumeToggle;
    private boolean isMuted;

    private boolean isPaused = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBarListener());
        videoWidgetView = (VrVideoView) findViewById(R.id.video_view);
        videoWidgetView.setEventListener(new VideoCallback());
        panoWidgetView = (VrPanoramaView) findViewById(R.id.pano_view);
        panoWidgetView.setEventListener(new ImageCallback());
        volumeToggle = (ImageButton) findViewById(R.id.volume_toggle);
        volumeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsMuted(!isMuted);
            }
        });
        loadVideoStatus = LOAD_VIDEO_STATUS_UNKNOWN;
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void setIsMuted(boolean isMuted) {
        this.isMuted = isMuted;
        volumeToggle.setImageResource(isMuted ? R.mipmap.volume_off : R.mipmap.volume_on);
        videoWidgetView.setVolume(isMuted ? 0.0f : 1.0f);
    }


    private void handleIntent(Intent intent) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // load video
                try {
                    VrVideoView.Options options = new VrVideoView.Options();
                    options.inputType = VrVideoView.Options.TYPE_MONO;
                    //TYPE_STEREO_OVER_UNDER
                    String fileName = "mongo.mp4";
                    videoWidgetView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    videoWidgetView.loadVideoFromAsset("mongo.mp4", options);
                    videoWidgetView.playVideo();
//                    videoWidgetView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                } catch (IOException e) {
                    loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
                }
                // load image
                AssetManager assetManager = getAssets();
                try (InputStream istr = assetManager.open("test.jpg")) {
                    panoOptions = new com.google.vr.sdk.widgets.pano.VrPanoramaView.Options();
                    panoOptions.inputType = VrPanoramaView.Options.TYPE_MONO;
                    panoWidgetView.setDisplayMode(VrPanoramaView.DisplayMode.FULLSCREEN_STEREO);
                    panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);
//                    videoWidgetView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    istr.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    public int getLoadVideoStatus() {
        return loadVideoStatus;
    }
    public boolean isMuted() {
        return isMuted;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, videoWidgetView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, videoWidgetView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        videoWidgetView.seekTo(progressTime);
        seekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
        seekBar.setProgress((int) progressTime);
        isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isPaused) {
            videoWidgetView.pauseVideo();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoWidgetView.pauseRendering();
        panoWidgetView.pauseRendering();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume the 3D rendering.
        videoWidgetView.resumeRendering();
        panoWidgetView.resumeRendering();
    }

    @Override
    protected void onDestroy() {
        // Free memory.
        videoWidgetView.shutdown();
        panoWidgetView.shutdown();
        super.onDestroy();
    }

    private void togglePause() {
        if (isPaused) {
            videoWidgetView.playVideo();
        } else {
            videoWidgetView.pauseVideo();
        }
        isPaused = !isPaused;
    }


    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                videoWidgetView.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    private class VideoCallback extends VrVideoEventListener {

        @Override
        public void onLoadSuccess() {
            loadVideoStatus = LOAD_VIDEO_STATUS_SUCCESS;
            seekBar.setMax((int) videoWidgetView.getDuration());
        }

        @Override
        public void onLoadError(String errorMessage) {
            loadVideoStatus = LOAD_VIDEO_STATUS_ERROR;
        }

        @Override
        public void onClick() {
            togglePause();
        }

        @Override
        public void onNewFrame() {
            seekBar.setProgress((int) videoWidgetView.getCurrentPosition());
        }

        @Override
        public void onCompletion() {
            videoWidgetView.seekTo(0);
        }
    }

    private class ImageCallback extends VrPanoramaEventListener {
        @Override
        public void onLoadSuccess() {
            loadImageSuccessful = true;
        }

        @Override
        public void onLoadError(String errorMessage) {
            loadImageSuccessful = false;
        }
    }
}