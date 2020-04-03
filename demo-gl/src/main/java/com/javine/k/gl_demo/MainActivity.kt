package com.javine.k.gl_demo

import android.app.Activity
import android.media.MediaDataSource
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import java.lang.IllegalStateException

class MainActivity : Activity() {

    private val DEFAULT_MEDIA_URI =
        "https://storage.googleapis.com/exoplayer-test-media-1/mkv/android-screens-lavf-56.36.100-aac-avc-main-1280x720.mkv"

    private var mPlayerView : PlayerView? = null
    private var mVideoGLSurfaceView : VideoProcessingGLSurfaceView? = null
    private var player : SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPlayerView = findViewById(R.id.player_view)
        mVideoGLSurfaceView = VideoProcessingGLSurfaceView(this, false, BitmapOverlayVideoProcessor(applicationContext))
        val contentFrame = findViewById<FrameLayout>(R.id.exo_content_frame)
        contentFrame.addView(mVideoGLSurfaceView)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        mPlayerView?.onResume()
    }

    override fun onResume() {
        super.onResume()
        if (player == null) {
            initializePlayer()
            mPlayerView?.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        mPlayerView?.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        mPlayerView?.onPause()
        releasePlayer()
    }

    fun initializePlayer() {
        val drmSessionManager = DrmSessionManager.DUMMY
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "JavinePlayerDemo"))

        val uri = Uri.parse(DEFAULT_MEDIA_URI)
        val type = Util.inferContentType(uri)
        var mediaSource: MediaSource? = null
        if (type == C.TYPE_DASH) {
            mediaSource = DashMediaSource.Factory(dataSourceFactory)
                .setDrmSessionManager(drmSessionManager)
                .createMediaSource(uri)
        } else if (type == C.TYPE_OTHER) {
            mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .setDrmSessionManager(drmSessionManager)
                .createMediaSource(uri)
        } else {
            throw IllegalStateException("video type error")
        }

        player = SimpleExoPlayer.Builder(this).build()
        player!!.run {
            repeatMode = SimpleExoPlayer.REPEAT_MODE_ALL
            prepare(mediaSource!!)
            playWhenReady = true
            addAnalyticsListener(EventLogger(null))
        }
        mVideoGLSurfaceView?.setVideoComponent(player?.videoComponent)
        mPlayerView?.player = player
    }

    private fun releasePlayer() {
        mPlayerView?.player = null
        player?.release()
        mVideoGLSurfaceView?.setVideoComponent(null)
        player = null
    }
}
