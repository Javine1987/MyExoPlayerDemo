package com.javine.k.gl_demo

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaFormat
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.view.Surface
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.GlUtil
import com.google.android.exoplayer2.util.TimedValueQueue
import com.google.android.exoplayer2.video.VideoFrameMetadataListener
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL10

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 20-3-31
 */
class VideoProcessingGLSurfaceView(context: Context?) : GLSurfaceView(context) {
    private val EGL_PROTECTED_CONTENT_EXT = 0x32C0
    private lateinit var mRenderer: VideoRenderer
    private var mVideoComponent: Player.VideoComponent? = null
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    private lateinit var mHandler: Handler

    interface VideoProcessor {
        fun initialize()
        fun setSurfaceSize(width:Int, height:Int)
        fun draw(frameTexture: Int, frameTimestampUs: Long)
    }

    constructor(context: Context?, requeireSecureContext: Boolean, videoProcessor : VideoProcessor ) : this(context) {
        mHandler = Handler()
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8,8,8,8,0,0)
        setEGLContextFactory(object : EGLContextFactory {
            override fun createContext(
                egl: EGL10?,
                display: EGLDisplay?,
                eglConfig: EGLConfig?
            ): EGLContext {
                val glAttributes : IntArray
                if (requeireSecureContext) {
                    glAttributes = intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION,
                        2,
                        EGL_PROTECTED_CONTENT_EXT,
                        EGL14.EGL_TRUE,
                        EGL14.EGL_NONE)
                } else {
                    glAttributes = intArrayOf(
                        EGL14.EGL_CONTEXT_CLIENT_VERSION,
                        2,
                        EGL14.EGL_NONE
                    )
                }
                return egl?.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, glAttributes)!!
            }

            override fun destroyContext(egl: EGL10?, display: EGLDisplay?, context: EGLContext?) {
                egl?.eglDestroyContext(display, context)
            }
        })
        setEGLWindowSurfaceFactory(object : EGLWindowSurfaceFactory{
            override fun createWindowSurface(
                egl: EGL10?,
                display: EGLDisplay?,
                config: EGLConfig?,
                nativeWindow: Any?
            ): EGLSurface {
                val glAttributes: IntArray
                if (requeireSecureContext) {
                    glAttributes = intArrayOf(
                        EGL_PROTECTED_CONTENT_EXT,
                        EGL14.EGL_TRUE,
                        EGL14.EGL_NONE
                    )
                } else {
                    glAttributes = intArrayOf(
                        EGL14.EGL_NONE
                    )
                }
                return egl?.eglCreateWindowSurface(display, config, nativeWindow, glAttributes)!!
            }

            override fun destroySurface(egl: EGL10?, display: EGLDisplay?, surface: EGLSurface?) {
                egl?.eglDestroySurface(display, surface)
            }
        })
        mRenderer = VideoRenderer(videoProcessor).also{renderer ->
            setRenderer(renderer)
            renderMode = RENDERMODE_WHEN_DIRTY
        }
    }

    private fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?) {
        mHandler.post{
            val oldSurfaceTexture = mSurfaceTexture
            val oldSurface = mSurface
            mSurfaceTexture = surfaceTexture
            mSurface = Surface(mSurfaceTexture)
            releaseSurface(oldSurface, oldSurfaceTexture)
            mVideoComponent?.setVideoSurface(mSurface)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.post {
            if (mSurface != null) {
                mVideoComponent?.clearVideoSurface(mSurface)
            }
            mVideoComponent?.clearVideoFrameMetadataListener(mRenderer)
            releaseSurface(mSurface, mSurfaceTexture)
            mSurface = null
            mSurfaceTexture = null
        }
    }

    public fun setVideoComponent(videoComponent: Player.VideoComponent?) {
        if (mVideoComponent == videoComponent) {
            return
        }

        mVideoComponent?.run {
            clearVideoSurface(mSurface)
            clearVideoFrameMetadataListener(mRenderer)
        }

        videoComponent?.run {
            setVideoSurface(mSurface)
            setVideoFrameMetadataListener(mRenderer)
        }
    }

    fun releaseSurface(surface: Surface?, surfaceTexture: SurfaceTexture?) {
        surfaceTexture?.release()
        surface?.release()
    }

    inner class VideoRenderer: Renderer, VideoFrameMetadataListener {
        private var height: Int = -1
        private var width: Int = -1
        private var videoProcessor: VideoProcessor? = null
        private var frameAvailable: AtomicBoolean = AtomicBoolean(false)
        private var sampleTimestampQueue: TimedValueQueue<Long?>? = null
        private var frameTimestampUs: Long = 0
        private var surfaceTexture: SurfaceTexture? = null
        private var texture: Int = -1
        private var initialized: Boolean = false

        constructor(videoProcessor: VideoProcessor) {
            this.videoProcessor = videoProcessor
            sampleTimestampQueue = TimedValueQueue()
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            texture = GlUtil.createExternalTexture()
            surfaceTexture = SurfaceTexture(texture)
            surfaceTexture?.setOnFrameAvailableListener {
                frameAvailable.set(true)
                requestRender()
            }
            onSurfaceTextureAvailable(surfaceTexture)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            this.width = width;
            this.height = height;
        }

        override fun onDrawFrame(gl: GL10?) {
            if (!initialized) {
                videoProcessor?.initialize()
                initialized = true
            }

            if (width != -1 && height != -1) {
                videoProcessor?.setSurfaceSize(width, height)
                width = -1
                height = -1
            }

            if (frameAvailable.compareAndSet(true, false)) {
                surfaceTexture?.updateTexImage()
                frameTimestampUs = sampleTimestampQueue?.poll(surfaceTexture?.timestamp!!)!!
            }
            videoProcessor?.draw(texture, frameTimestampUs)
        }

        override fun onVideoFrameAboutToBeRendered(
            presentationTimeUs: Long,
            releaseTimeNs: Long,
            format: Format,
            mediaFormat: MediaFormat?
        ) {
            sampleTimestampQueue?.add(releaseTimeNs, presentationTimeUs)
        }
    }

}