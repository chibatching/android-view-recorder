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
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import rx.subscriptions.Subscriptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.concurrent.TimeUnit


public class ViewRecorder {

    val context: Context
    val view: View
    val duration: Int
    val frameRate: Int
    val scale: Double
    val loopCount: Int

    private var mIsEncording: Boolean = false
    private var mIsRecording: Boolean = false
    private var mSubscriptions: CompositeSubscription = CompositeSubscription()

    private val mMaxCount: Int
    private val mInterval: Int

    private var mData: MutableList<Pair<Int, ByteArray?>>

    init {
    }

    protected constructor(context: Context, view: View, duration: Int = 5000,
                          frameRate: Int = 12, scale: Double = 0.3, loopCount: Int = 0) {

        this.context = context
        this.view = view
        this.duration = duration
        this.frameRate = frameRate
        this.scale = scale
        this.loopCount = loopCount

        mMaxCount = (frameRate * duration / 1000).toInt()
        mInterval = 1000 / frameRate
        mData = ArrayList(mMaxCount)
    }

    public fun recorderBuilder(): ViewRecorder {
        return this
    }

    public fun start() {
        Log.d(javaClass<ViewRecorder>().getSimpleName(), "start")
        mIsRecording = true

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "maxCount: $mMaxCount, interval: $mInterval")

        var savedFrame = 0

        mSubscriptions.add(Observable.create<Pair<Int, ByteArray?>> {subscriber ->
            // When stop recording, start encode
            subscriber.add(Subscriptions.create{
                mIsRecording = false
                encode(mData)
            })
            // Don't use Observable.interval etc.., it is too slow to repeat recording.
            Thread (Runnable {
                Log.d(javaClass<ViewRecorder>().getSimpleName(), "start thread")

                var i = 0
                while ((i < mMaxCount) and mIsRecording) {
                    val startTime = System.currentTimeMillis()
                    val currentFrame = i
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "frame: $currentFrame")

                    mSubscriptions.add(getViewDrawingCache()
                            .observeOn(Schedulers.newThread())
                            .map {
                                subscriber.onNext(Pair(currentFrame, convertBitmapToByteArray(it)))
                                ++savedFrame
                            }
                            .filter { it >= mMaxCount }
                            .subscribe { subscriber.onCompleted() })

                    Thread.sleep(
                            if ((System.currentTimeMillis() - startTime) < mInterval.toLong()) {
                                mInterval.toLong() - (System.currentTimeMillis() - startTime)
                            } else {
                                0
                            })

                    i++
                }
            }).start()
        }
        .subscribeOn(Schedulers.newThread())
        .buffer(1000.toLong(), TimeUnit.MILLISECONDS)
        .subscribe { mData.addAll(it) })
    }

    public fun stop() {
        mSubscriptions.unsubscribe()
    }

    private fun encode(data: List<Pair<Int, ByteArray?>>) {
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

    private fun getViewDrawingCache(): Observable<Bitmap> {
        return Observable.create<Bitmap> {
            view.setDrawingCacheEnabled(true)
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
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray? {
        var byteArray: ByteArray? = null
        ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            byteArray = it.toByteArray()
        }
        bitmap.recycle()
        return byteArray
    }
}