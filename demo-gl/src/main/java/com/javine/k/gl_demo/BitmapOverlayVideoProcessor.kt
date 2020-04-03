package com.javine.k.gl_demo

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.opengl.GLES10
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.util.GlUtil
import com.google.android.exoplayer2.util.Util
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.*
import javax.microedition.khronos.opengles.GL10

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 20-3-31
 */
class BitmapOverlayVideoProcessor(context: Context) : VideoProcessingGLSurfaceView.VideoProcessor {
    private val OVERLAY_WIDTH = 512
    private val OVERLAY_HEIGHT = 256

    private var context: Context
    private var paint: Paint
    private var mOverlayBitmap: Bitmap
    private var mLogoBitmap: Bitmap
    private var mOverlayCanvas: Canvas
    private var mTexture: IntArray

    private var mBitmapScaleX: Float = 1f
    private var mBitmapScaleY: Float = 1f

    private var mProgramHandle: Int = 0
    private var mAttributes: Array<GlUtil.Attribute>? = null
    private var mUniforms: Array<GlUtil.Uniform>? = null

    init {
        this.context = context.applicationContext
        paint = Paint()
        paint.textSize = 64f
        paint.isAntiAlias = true
        paint.setARGB(0xff, 0xff, 0xff, 0xff)
        mTexture = IntArray(1)
        mOverlayBitmap = Bitmap.createBitmap(OVERLAY_WIDTH, OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888)
        mOverlayCanvas = Canvas(mOverlayBitmap)
        try {
            mLogoBitmap = (context.packageManager.getApplicationIcon(context.packageName) as BitmapDrawable).bitmap
        } catch (e : PackageManager.NameNotFoundException) {
            throw IllegalStateException(e)
        }
    }

    override fun initialize() {
        val vertexShaderCode = loadAssertAsString("video_processor_vertex.glsl")
        val fragmentShaderCode = loadAssertAsString("video_processor_fragment.glsl")
        mProgramHandle = GlUtil.compileProgram(vertexShaderCode, fragmentShaderCode)
        mAttributes = GlUtil.getAttributes(mProgramHandle)
        mUniforms = GlUtil.getUniforms(mProgramHandle)
        mAttributes?.forEach {attribute ->
             if (attribute.name.equals("a_position")) {
                 attribute.setBuffer(floatArrayOf(
                     -1.0f, -1.0f, 0f, 1.0f,
                     1.0f, -1.0f, 0f, 1.0f,
                     -1.0f, 1.0f, 0f, 1.0f,
                     1.0f, 1.0f, 0f, 1.0f
                 ), 4)
             } else if (attribute.name.equals("a_texcoord")) {
                 attribute.setBuffer(floatArrayOf(
                     0f, 1.0f, 1.0f,
                     1.0f, 1.0f, 1.0f,
                     0f, 0f, 1.0f,
                     1.0f, 0f, 1.0f
                 ), 3)
             }
        }
        GLES20.glGenTextures(1, mTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[0])
        GLES20.glTexParameterf(GLES10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST.toFloat())
        GLES20.glTexParameterf(GLES10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat())
        GLES20.glTexParameterf(GLES10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())
        GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, mOverlayBitmap, 0)
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        mBitmapScaleX = (width/OVERLAY_WIDTH).toFloat()
        mBitmapScaleY = (height/OVERLAY_HEIGHT).toFloat()
        Log.d("Javine", "bitmap scaleXY = " + mBitmapScaleX + ", " + mBitmapScaleY)
    }

    override fun draw(frameTexture: Int, frameTimestampUs: Long) {
        val text = String.format(Locale.US, "%.02f", frameTimestampUs/ C.MICROS_PER_SECOND.toFloat())
        mOverlayBitmap.eraseColor(Color.TRANSPARENT)
        mOverlayCanvas.drawBitmap(mLogoBitmap, 32f, 32f, paint)
        mOverlayCanvas.drawText(text, 200f, 130f, paint)
        GLES20.glBindTexture(GLES10.GL_TEXTURE_2D, mTexture[0])
        GLUtils.texSubImage2D(GLES10.GL_TEXTURE_2D, 0, 0, 0, mOverlayBitmap)
        GlUtil.checkGlError()

        GLES20.glUseProgram(mProgramHandle)
        mUniforms?.forEach {uniform ->
            when(uniform.name) {
                "tex_sampler_0" -> uniform.setSamplerTexId(frameTexture, 0)
                "tex_sampler_1" -> uniform.setSamplerTexId(mTexture[0], 1)
                "scaleX" -> uniform.setFloat(mBitmapScaleX)
                "scaleY" -> uniform.setFloat(mBitmapScaleY)
            }
        }

        mAttributes?.forEach { attribute -> attribute.bind() }
        mUniforms?.forEach { uniform -> uniform.bind() }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GlUtil.checkGlError()
    }

    fun loadAssertAsString(fileName: String): String {
        var inputStream: InputStream? = null
        try {
            inputStream = context.assets.open(fileName)
            return Util.fromUtf8Bytes(Util.toByteArray(inputStream))
        } catch (e : IOException) {
            throw IllegalStateException(e)
        } finally {
            Util.closeQuietly(inputStream)
        }
    }
}