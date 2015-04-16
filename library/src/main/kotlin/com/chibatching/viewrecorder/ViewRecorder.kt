package com.chibatching.viewrecorder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import com.chibatching.viewrecorder.encoder.gif.AnimatedGifEncoder
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import rx.subscriptions.Subscriptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.concurrent.TimeUnit


public class ViewRecorder(private val outputFile: File,
                          private val view: View,
                          private val duration: Int = DEFAULT_DURATION,
                          private val frameRate: Int = DEFAULT_FRAME_RATE,
                          private val scale: Double = DEFAULT_SCALE,
                          private val loopCount: Int = DEFAULT_LOOP_COUNT,
                          private val onRecordFinishListener: ViewRecorder.OnRecordFinishListener? = null,
                          private val onEncodeFinishListener: ViewRecorder.OnEncodeFinishListener? = null) {

    private var mRecordingSubscription: Subscription = Subscriptions.empty()
    private var mEncodingSubscription: Subscription = Subscriptions.empty()
    private var mViewObtainSubscriptions: CompositeSubscription = CompositeSubscription()

    private val mMaxCount: Int
    private val mInterval: Int

    private var mData: MutableList<Pair<Int, ByteArray?>>

    var isEncoding: Boolean = false
        private set
    var isRecording: Boolean = false
        private set

    init {
        mMaxCount = if (duration > 0) (frameRate * duration / 1000).toInt() else -1
        mInterval = 1000 / frameRate
        if (mMaxCount > 0) {
            mData = ArrayList(mMaxCount)
        } else {
            mData = ArrayList()
        }
    }

    public fun start(): Boolean {

        if (isRecording) {
            return false
        }

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "start")
        isRecording = true

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "maxCount: $mMaxCount, interval: $mInterval")

        var savedFrame = 0

        mRecordingSubscription = Observable.create<Pair<Int, ByteArray?>> {subscriber ->
            // When stop recording, start encoding
            subscriber.add(Subscriptions.create{
                isRecording = false
                onRecordFinishListener?.onRecordFinish()
                mEncodingSubscription = encode(mData).subscribe {
                    onEncodeFinishListener?.onEncodeFinish(it)
                    mEncodingSubscription.unsubscribe()
                }
            })

            fun loopCheck(count: Int) = isRecording and ((count < mMaxCount) or (mMaxCount < 0))

            // Don't use Observable.interval or other rx emission methods.
            // It is too slow to repeat to high speed.
            Thread (Runnable {
                Log.d(javaClass<ViewRecorder>().getSimpleName(), "start thread")

                var i = 0
                while (loopCheck(i)) {
                    val startTime = System.currentTimeMillis()
                    val currentFrame = i
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "frame: $currentFrame")

                    mViewObtainSubscriptions.add(getViewDrawingCache()
                            .observeOn(Schedulers.newThread())
                            .map {
                                subscriber.onNext(Pair(currentFrame, convertBitmapToByteArray(it)))
                                ++savedFrame
                            }
                            .filter { !loopCheck(it) }
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
        .buffer((if (duration > 0) duration else 1000).toLong(), TimeUnit.MILLISECONDS)
        .subscribe { mData.addAll(it) }

        return true
    }

    public fun stop() {
        mViewObtainSubscriptions.unsubscribe()
        mRecordingSubscription.unsubscribe()
    }

    public fun destroy() {
        mViewObtainSubscriptions.unsubscribe()
        mRecordingSubscription.unsubscribe()
        mEncodingSubscription.unsubscribe()
        isRecording = false
        isEncoding = false
    }

    private fun encode(data: List<Pair<Int, ByteArray?>>): Observable<String> {
        outputFile.createNewFile()
        isEncoding = true

        return Observable.create<String> {
            FileOutputStream(outputFile).use { fos ->
                val encoder = AnimatedGifEncoder(fos)
                encoder.start()
                encoder.repeat = loopCount
                encoder.setFrameRate(frameRate.toFloat())
                var prev = 0
                data.sortBy { it.first }.forEach {
                    if (!isEncoding) {
                        return@forEach
                    }

                    val current = it.first
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "current: $current, prev: $prev")
                    encoder.addFrame(BitmapFactory.decodeByteArray(it.second, 0, it.second!!.size()))
                    prev = current
                }
                encoder.finish()
            }
            it.onNext(outputFile.getAbsolutePath())
            it.onCompleted()
            isEncoding = false
        }.subscribeOn(Schedulers.newThread())
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

    public trait OnRecordFinishListener {
        fun onRecordFinish()
    }

    public trait OnEncodeFinishListener {
        fun onEncodeFinish(filePath: String)
    }
}