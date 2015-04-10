package com.chibatching.screenrecorder

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.util.Log
import android.view.View
import com.chibatching.screenrecorder.encoder.gif.AnimatedGifEncoder
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Comparator
import java.util.HashMap
import java.util.concurrent.TimeUnit


public class ScreenRecorder(
        val context: Context, val view: View, val duration: Int = 5000,
        val frameRate: Int = 12, val scale: Double = 0.3, val loopCount: Int = 0) {

    var isRecording: Boolean = false

    public fun start() {
        Log.d(javaClass<ScreenRecorder>().getSimpleName(), "start")
        isRecording = true

        val fileDirPath = context.getExternalCacheDir().getAbsolutePath().plus("/screen_record_temp")
        val fileDir = File(fileDirPath)
        if (!fileDir.exists()) {
            fileDir.mkdirs()
        } else if (!fileDir.isDirectory()) {
            fileDir.delete()
            fileDir.mkdirs()
        } else {
            fileDir.listFiles().forEach { it.delete() }
        }

        val maxCount = (frameRate * duration / 1000).toInt()
        val interval = 1000 / frameRate

        Log.d(javaClass<ScreenRecorder>().getSimpleName(), "macCount: $maxCount, interval: $interval")

        var savedFrame = 0
        view.setDrawingCacheEnabled(true)

        Observable.create<Pair<Int, ByteArray?>> {subscriber ->
            Thread (Runnable {
                Log.d(javaClass<ScreenRecorder>().getSimpleName(), "start thread")
                var frameCount = 0
                while (frameCount < maxCount) {
                    val currentCount = frameCount
                    val startTime = System.currentTimeMillis()
                    Log.d(javaClass<ScreenRecorder>().getSimpleName(), "frame: $currentCount")
                    var orgBitmap = view.getDrawingCache()
                    val bitmap =
                            Bitmap.createScaledBitmap(
                                    orgBitmap,
                                    (orgBitmap.getWidth() * scale).toInt(),
                                    (orgBitmap.getHeight() * scale).toInt(),
                                    false)

                    object: AsyncTask<Void, Void, ByteArray>() {
                        override protected fun doInBackground(vararg args: Void?): ByteArray? {
                            var result: ByteArray? = null
                            ByteArrayOutputStream().use {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                                result = it.toByteArray()
                            }
                            return result
                        }

                        override protected fun onPostExecute(result: ByteArray?) {
                            subscriber?.onNext(Pair(currentCount, result))
                            savedFrame++
                            if (savedFrame >= maxCount) {
                                subscriber.onCompleted()
                            }
                        }
                    }.execute()

                    view.destroyDrawingCache()

                    frameCount++
                    Thread.sleep(
                            if ((System.currentTimeMillis() - startTime) < interval.toLong()) {
                                interval.toLong() - (System.currentTimeMillis() - startTime)
                            } else {
                                0
                            })
                }
            }).start()
        }
        .buffer(maxCount + 1)
        .subscribe { data ->
            val outputFile = File(context.getExternalFilesDir(null).getAbsolutePath().plus("/output.gif"))
            outputFile.createNewFile()

            FileOutputStream(outputFile).use { fos ->
                val encoder = AnimatedGifEncoder()
                encoder.start(fos)
                encoder.setRepeat(loopCount)
                encoder.setFrameRate(frameRate.toFloat())
                var prev = 0
                data.sortBy { it.first }
                data.forEach {
                    val current = it.first
                    Log.d(javaClass<ScreenRecorder>().getSimpleName(), "current: $current, prev: $prev")
                    encoder.addFrame(BitmapFactory.decodeByteArray(it.second, 0, it.second!!.size()))
                    prev = current
                }
                encoder.finish()
            }
        }
    }
}