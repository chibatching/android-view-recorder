package com.chibatching.viewrecorder

import android.content.Context
import android.view.View


public class ViewRecorderBuilder(val context: Context, val view: View) {

    private var mDuration: Int = -1
    private var mFrameRate: Int = 12
    private var mScale: Double = 0.3
    private var mLoopCount: Int = 0
    private var mOnRecordFinishListener: ViewRecorder.OnRecordFinishListener? = null
    private var mOnEncodeFinishListener: ViewRecorder.OnEncodeFinishListener? = null

    public fun create(): ViewRecorder {
        return ViewRecorder(
                context = context,
                view = view,
                duration = mDuration,
                frameRate = mFrameRate,
                scale = mScale,
                loopCount = mLoopCount,
                onRecordFinishListener = mOnRecordFinishListener,
                onEncodeFinishListener = mOnEncodeFinishListener)
    }

    public fun setDuration(duration: Int): ViewRecorderBuilder {
        this.mDuration = duration
        return this
    }

    public fun setFrameRate(frameRate: Int): ViewRecorderBuilder {
        this.mFrameRate = frameRate
        return this
    }

    public fun setScale(scale: Double): ViewRecorderBuilder {
        this.mScale = scale
        return this
    }

    public fun setLoopCount(loopCount: Int): ViewRecorderBuilder {
        this.mLoopCount = loopCount
        return this
    }

    public fun setOnRecordFinishListener(listener: ViewRecorder.OnRecordFinishListener): ViewRecorderBuilder {
        this.mOnRecordFinishListener = listener
        return this
    }

    public fun setOnEncodeFinishListener(listener: ViewRecorder.OnEncodeFinishListener): ViewRecorderBuilder {
        this.mOnEncodeFinishListener = listener
        return this
    }
}
