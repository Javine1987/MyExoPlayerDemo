package com.javine.k.gl_demo

import android.content.Context
import android.opengl.GLSurfaceView
import com.google.android.exoplayer2.Player

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 20-3-31
 */
class VideoProcessingGLSurfaceView(context: Context?) : GLSurfaceView(context) {

    interface VideoProcessor {
        fun initialize() : Void
        fun setSurfaceSize(width:Int, height:Int) : Void
    }

    constructor(context: Context?, requeireSecureContext: Boolean, videoProcessor : VideoProcessor ) : this(context) {

    }

    public fun setVideoComponent(videoComponent: Player.VideoComponent?) {

    }

}