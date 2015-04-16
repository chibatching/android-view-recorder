package com.chibatching.viewrecorder

import android.content.Context
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


public class ViewRecorder {

    private val mContext: Context
    private val mView: View
    private val mDuration: Int
    private val mFrameRate: Int
    private val mScale: Double
    private val mLoopCount: Int
    private val mOnRecordFinishListener: OnRecordFinishListener?
    private val mOnEncodeFinishListener: OnEncodeFinishListener?

    private var mIsEncording: Boolean = false
    private var mIsRecording: Boolean = false

    private var mRecordingSubscription: Subscription = Subscriptions.empty()
    private var mEncodingSubscription: Subscription = Subscriptions.empty()
    private var mViewObtainSubscriptions: CompositeSubscription = CompositeSubscription()

    private val mMaxCount: Int
    private val mInterval: Int

    private var mData: MutableList<Pair<Int, ByteArray?>>

    internal constructor(context: Context, view: View, duration: Int, frameRate: Int, scale: Double,
                         loopCount: Int, onRecordFinishListener: OnRecordFinishListener?,
                         onEncodeFinishListener: OnEncodeFinishListener?) {
        this.mContext = context
        this.mView = view
        this.mDuration = duration
        this.mFrameRate = frameRate
        this.mScale = scale
        this.mLoopCount = loopCount
        this.mOnRecordFinishListener = onRecordFinishListener
        this.mOnEncodeFinishListener = onEncodeFinishListener

        mMaxCount = if (duration > 0) (frameRate * duration / 1000).toInt() else -1
        mInterval = 1000 / frameRate
        if (mMaxCount > 0) {
            mData = ArrayList(mMaxCount)
        } else {
            mData = ArrayList()
        }
    }

    public fun start(): Boolean {

        if (mIsRecording) {
            return false
        }

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "start")
        mIsRecording = true

        Log.d(javaClass<ViewRecorder>().getSimpleName(), "maxCount: $mMaxCount, interval: $mInterval")

        var savedFrame = 0

        mRecordingSubscription = Observable.create<Pair<Int, ByteArray?>> {subscriber ->
            // When stop recording, start encoding
            subscriber.add(Subscriptions.create{
                mIsRecording = false
                mOnRecordFinishListener?.onRecordFinish()
                mEncodingSubscription = encode(mData).subscribe {
                    mOnEncodeFinishListener?.onEncordFinish(it)
                    mEncodingSubscription.unsubscribe()
                }
            })

            fun loopCheck(count: Int) = mIsRecording and ((count < mMaxCount) or (mMaxCount < 0))

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
        .buffer((if (mDuration > 0) mDuration else 1000).toLong(), TimeUnit.MILLISECONDS)
        .subscribe { mData.addAll(it) }

        return true
    }

    public fun stop() {
        mViewObtainSubscriptions.unsubscribe()
        mRecordingSubscription.unsubscribe()
    }

    private fun encode(data: List<Pair<Int, ByteArray?>>): Observable<String> {
        val outputFile = File(mContext.getExternalFilesDir(null).getAbsolutePath().plus("/output.gif"))
        outputFile.createNewFile()
        mIsEncording = true

        return Observable.create<String> {
            FileOutputStream(outputFile).use { fos ->
                val encoder = AnimatedGifEncoder(fos)
                encoder.start()
                encoder.repeat = mLoopCount
                encoder.setFrameRate(mFrameRate.toFloat())
                var prev = 0
                data.sortBy { it.first }.forEach {
                    val current = it.first
                    Log.d(javaClass<ViewRecorder>().getSimpleName(), "current: $current, prev: $prev")
                    encoder.addFrame(BitmapFactory.decodeByteArray(it.second, 0, it.second!!.size()))
                    prev = current
                }
                encoder.finish()
            }
            it.onNext(outputFile.getAbsolutePath())
            it.onCompleted()
            mIsEncording = false
        }.subscribeOn(Schedulers.newThread())
    }

    private fun getViewDrawingCache(): Observable<Bitmap> {
        return Observable.create<Bitmap> {
            mView.setDrawingCacheEnabled(true)
            var orgBitmap = mView.getDrawingCache()
            val bitmap =
                    Bitmap.createScaledBitmap(
                            orgBitmap,
                            (orgBitmap.getWidth() * mScale).toInt(),
                            (orgBitmap.getHeight() * mScale).toInt(),
                            false)
            it.onNext(bitmap)
            it.onCompleted()
            mView.destroyDrawingCache()
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
        fun onEncordFinish(filePath: String)
    }
}