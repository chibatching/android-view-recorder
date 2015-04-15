package com.chibatching.viewrecorder

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.os.Debug
import android.util.Log
import android.view.View
import com.chibatching.viewrecorder.encoder.gif.AnimatedGifEncoder
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Comparator
import java.util.HashMap
import java.util.concurrent.TimeUnit


public class ViewRecorder(
        val context: Context, val view: View, val duration: Int = 5000,
        val frameRate: Int = 12, val scale: Double = 0.3, val loopCount: Int = 0) {

    var isRecording: Boolean = false

    public fun start() {
        Log.d(javaClass<ViewRecorder>().getSimpleName(), "start")
        isRecording = true

        val maxCount = (frameRate * duration / 1000).toInt()
        val interval = 1000 / frameRate

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "maxCount: $maxCount, interval: $interval")

        var savedFrame = 0
        view.setDrawingCacheEnabled(true)

        Observable.create<Pair<Int, ByteArray?>> {subscriber ->
            Thread (Runnable {
                Log.d(javaClass<ViewRecorder>().getSimpleName(), "start thread")

                for (i in 0..maxCount - 1) {
                    val startTime = System.currentTimeMillis()
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "frame: $i")

                    Observable.create<Bitmap> {
                        var orgBitmap = view.getDrawingCache()
                        val bitmap =
                                Bitmap.createScaledBitmap(
                                        orgBitmap,
                                        (orgBitmap.getWidth() * scale).toInt(),
                                        (orgBitmap.getHeight() * scale).toInt(),
                                        false)
                        it.onNext(bitmap)
                        it.onCompleted()
                        view.destroyDrawingCache()
                    }
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.newThread())
                    .subscribe { bitmap ->
                        ByteArrayOutputStream().use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                            subscriber.onNext(Pair(i, it.toByteArray()))
                            savedFrame++
                            if (savedFrame >= maxCount) {
                                subscriber.onCompleted()
                            }
                        }
                    }

                    Thread.sleep(
                            if ((System.currentTimeMillis() - startTime) < interval.toLong()) {
                                interval.toLong() - (System.currentTimeMillis() - startTime)
                            } else {
                                0
                            })
                }
            }).start()
        }
        .subscribeOn(Schedulers.newThread())
        .buffer(maxCount + 1)
        .observeOn(Schedulers.newThread())
        .subscribe { data ->
            val outputFile = File(context.getExternalFilesDir(null).getAbsolutePath().plus("/output.gif"))
            outputFile.createNewFile()

            FileOutputStream(outputFile).use { fos ->
                val encoder = AnimatedGifEncoder(fos)
                encoder.start()
                encoder.repeat = loopCount
                encoder.setFrameRate(frameRate.toFloat())
                var prev = 0
                data.sortBy { it.first }.forEach {
                    val current = it.first
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "current: $current, prev: $prev")
                    encoder.addFrame(BitmapFactory.decodeByteArray(it.second, 0, it.second!!.size()))
                    prev = current
                }
                encoder.finish()
            }
        }
    }
}