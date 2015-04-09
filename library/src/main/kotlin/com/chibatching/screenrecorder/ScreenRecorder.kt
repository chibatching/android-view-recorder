package com.chibatching.screenrecorder

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.util.Config
import android.util.Log
import android.view.View
import com.chibatching.screenrecorder.encoder.gif.AnimatedGifEncoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


public class ScreenRecorder(
        val context: Context, val view: View,
        val duration: Int = 5000, val frameRate: Int = 15, val scale: Double = 0.5) {

    var isRecording: Boolean = false

    public fun start() {
        Log.d(javaClass<ScreenRecorder>().getSimpleName(), "start")
        isRecording = true

        val fileDirPath = context.getFilesDir().getAbsolutePath().plus("/screen_record_temp")
        val fileDir = File(fileDirPath)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        } else if (!fileDir.isDirectory()) {
            fileDir.delete()
            fileDir.mkdirs()
        } else {
            fileDir.listFiles().forEach { it.delete() }
        }

        Thread (Runnable {
            Log.d(javaClass<ScreenRecorder>().getSimpleName(), "start thread")
            var frameCount = 0
            while (frameCount < frameRate * duration / 1000) {
                Log.d(javaClass<ScreenRecorder>().getSimpleName(), "frame: $frameCount")
                view.setDrawingCacheEnabled(true)
                var orgBitmap = view.getDrawingCache()
                val flag = if (orgBitmap == null) {
                    orgBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(orgBitmap)
                    view.draw(canvas)
                    true
                } else { false }

                val bitmap =
                        Bitmap.createScaledBitmap(
                                orgBitmap,
                                (orgBitmap.getWidth() * scale).toInt(),
                                (orgBitmap.getHeight() * scale).toInt(),
                                false)

                if (flag) {
                    orgBitmap.recycle()
                }

                object: AsyncTask<Void, Void, Void>() {
                    override protected fun doInBackground(vararg args: Void?): Void? {
                        val frameFile = File(fileDirPath.plus(java.lang.String.format("/%08d.png", frameCount.toInt())))
                        ByteArrayOutputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            frameFile.createNewFile()
                            frameFile.writeBytes(it.toByteArray())
                        }
                        return null
                    }

                    override protected fun onPostExecute(result: Void?) {
                    }
                }.execute()

                frameCount++
                view.destroyDrawingCache()
                Thread.sleep(1000 / frameRate.toLong())
            }

            val outputFile = File(context.getFilesDir().getAbsolutePath().plus("/output.gif"))
            outputFile.createNewFile()

            FileOutputStream(outputFile).use { fos ->
                val encoder = AnimatedGifEncoder()
                encoder.start(fos)
                encoder.setRepeat(0)
                fileDir.listFiles().forEach {
                    encoder.setDelay(frameRate)
                    encoder.addFrame(BitmapFactory.decodeFile(it.getAbsolutePath()))
                }
                encoder.finish()
            }

            isRecording = false
        }).start()
    }
}