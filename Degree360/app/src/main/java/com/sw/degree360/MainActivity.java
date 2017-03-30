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

/**
 * Main View Class
 *
 * @author SimonWong
 * @see AppCompatActivity
 */
public class MainActivity extends AppCompatActivity {

    // instance passing parameters
    private static final String STATE_IS_PAUSED = "ISPAUSED";
    private static final String STATE_PROGRESS_TIME = "PROGRESSTIME";
    private static final String STATE_VIDEO_DURATION = "VIDEODURATION";

    // view variable
    private VrVideoView vrVideoView;
    private VrPanoramaView vrImageView;
    private SeekBar vrVideoViewSeekBar;
    private ImageButton vrVideoViewVolumeToggleIb;

    // state variable
    private boolean isVideoVolumeMuted;
    private boolean isVideoPaused = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vrVideoViewSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        vrVideoViewSeekBar.setOnSeekBarChangeListener(new SeekBarListener());
        vrVideoView = (VrVideoView) findViewById(R.id.video_view);
        vrVideoView.setEventListener(new VideoCallback());
        vrImageView = (VrPanoramaView) findViewById(R.id.pano_view);
        vrImageView.setEventListener(new ImageCallback());
        vrVideoViewVolumeToggleIb = (ImageButton) findViewById(R.id.volume_toggle);
        vrVideoViewVolumeToggleIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setIsMuted(!isVideoVolumeMuted);
            }
        });
        initialize();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // intent launching triggered event #ignoring the passing parameters
        setIntent(intent);
        initialize();
    }

    /**
     * Setting Volume of video is muted or not
     *
     * @param isMuted - boolean indicating current selected muted state
     */
    private void setIsMuted(boolean isMuted) {
        this.isVideoVolumeMuted = isMuted;
        vrVideoViewVolumeToggleIb.setImageResource(isMuted ? R.mipmap.volume_off : R.mipmap.volume_on);
        vrVideoView.setVolume(isMuted ? 0.0f : 1.0f);
    }

    /**
     * Initialize VrVideoView and VrPanoramaView
     */
    private void initialize() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // load video
                try {
                    VrVideoView.Options options = new VrVideoView.Options();
                    options.inputType = VrVideoView.Options.TYPE_MONO;
                    vrVideoView.setDisplayMode(VrVideoView.DisplayMode.FULLSCREEN_STEREO);
                    vrVideoView.loadVideoFromAsset("mongo.mp4", options);
                    vrVideoView.playVideo();
                } catch (IOException ignored) {
                }
                // load image
                AssetManager assetManager = getAssets();
                try (InputStream istr = assetManager.open("test.jpg")) {
                    VrPanoramaView.Options panoOptions = new VrPanoramaView.Options();
                    panoOptions.inputType = VrPanoramaView.Options.TYPE_MONO;
                    vrImageView.setDisplayMode(VrPanoramaView.DisplayMode.FULLSCREEN_STEREO);
                    vrImageView.loadImageFromBitmap(BitmapFactory.decodeStream(istr),
                            panoOptions);
                    istr.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(STATE_PROGRESS_TIME, vrVideoView.getCurrentPosition());
        savedInstanceState.putLong(STATE_VIDEO_DURATION, vrVideoView.getDuration());
        savedInstanceState.putBoolean(STATE_IS_PAUSED, isVideoPaused);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long progressTime = savedInstanceState.getLong(STATE_PROGRESS_TIME);
        vrVideoView.seekTo(progressTime);
        vrVideoViewSeekBar.setMax((int) savedInstanceState.getLong(STATE_VIDEO_DURATION));
        vrVideoViewSeekBar.setProgress((int) progressTime);
        isVideoPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
        if (isVideoPaused)
            vrVideoView.pauseVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the 3D Rendering
        vrVideoView.pauseRendering();
        vrImageView.pauseRendering();
        isVideoPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume the 3D rendering.
        vrVideoView.resumeRendering();
        vrImageView.resumeRendering();
    }

    @Override
    protected void onDestroy() {
        // Free memory
        vrVideoView.shutdown();
        vrImageView.shutdown();
        super.onDestroy();
    }

    /**
     * Video Play/Pause toggle function
     */
    private void togglePause() {
        if (isVideoPaused)
            vrVideoView.playVideo();
        else
            vrVideoView.pauseVideo();
        isVideoPaused = !isVideoPaused;
    }

    /**
     * OnSeekBarListener handling vrVideoViewSeekBar onTouch triggering events
     */
    private class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser)
                vrVideoView.seekTo(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    /**
     * VrVideoEventListener handling VrVideoView onDIsplay triggering events
     */
    private class VideoCallback extends VrVideoEventListener {

        @Override
        public void onLoadSuccess() {
            // customize vrVideoViewSeekBar's length (max value) according to video duration
            vrVideoViewSeekBar.setMax((int) vrVideoView.getDuration());
        }

        @Override
        public void onLoadError(String errorMessage) {
            // do something when the video fails to be loaded, and with error Message
        }

        @Override
        public void onClick() {
            // event triggered when vrVideoView is clicked
            togglePause();
        }

        @Override
        public void onNewFrame() {
            // update vrVideoViewSeekBar's progress during the video is playing
            vrVideoViewSeekBar.setProgress((int) vrVideoView.getCurrentPosition());
        }

        @Override
        public void onCompletion() {
            // event triggered after the video is finished playing
            vrVideoView.seekTo(0);
        }
    }

    /**
     * VrPanoaramaView Image Loading Listener
     */
    private class ImageCallback extends VrPanoramaEventListener {
        @Override
        public void onLoadSuccess() {
            // do something to indicate image loading success
        }

        @Override
        public void onLoadError(String errorMessage) {
            // do something to indicate image loading failure with error message
        }
    }
}