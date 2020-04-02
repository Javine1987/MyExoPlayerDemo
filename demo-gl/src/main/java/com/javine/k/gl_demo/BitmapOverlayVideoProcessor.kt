package com.javine.k.gl_demo

import android.content.Context
import android.graphics.Paint

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 20-3-31
 */
class BitmapOverlayVideoProcessor(var context: Context) : VideoProcessingGLSurfaceView.VideoProcessor {
    var paint: Paint

    init {
        paint = Paint()
        paint.textSize = 64f
    }

    override fun initialize(): Void {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSurfaceSize(width: Int, height: Int): Void {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun draw(frameTexture: Int, frameTimestampUs: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}