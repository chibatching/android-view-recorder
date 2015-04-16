package com.chibatching.viewrecorder

import android.view.View
import java.io.File


public class ViewRecorderBuilder(val output: File, val view: View) {

    private var mDuration: Int = DEFAULT_DURATION
    private var mFrameRate: Int = DEFAULT_FRAME_RATE
    private var mScale: Double = DEFAULT_SCALE
    private var mLoopCount: Int = DEFAULT_LOOP_COUNT
    private var mOnRecordFinishListener: ViewRecorder.OnRecordFinishListener? = null
    private var mOnEncodeFinishListener: ViewRecorder.OnEncodeFinishListener? = null

    public fun create(): ViewRecorder {
        return ViewRecorder(
                outputFile = output,
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
