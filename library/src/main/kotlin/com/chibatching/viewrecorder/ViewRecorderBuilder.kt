package com.chibatching.viewrecorder

import android.view.View
import java.io.File

/**
 * Helper for constructing ViewRecorder instance
 *
 * @param output Destination file for output gif animation file.
 * @param view Target view.
 */
public class ViewRecorderBuilder(val output: File, val view: View) {

    private var mDuration: Int = DEFAULT_DURATION
    private var mFrameRate: Int = DEFAULT_FRAME_RATE
    private var mScale: Double = DEFAULT_SCALE
    private var mLoopCount: Int = DEFAULT_LOOP_COUNT
    private var mOnRecordFinishListener: ViewRecorder.OnRecordFinishListener? = null
    private var mOnEncodeFinishListener: ViewRecorder.OnEncodeFinishListener? = null

    /**
     * Construct ViewRecorder instance with given parameters
     */
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

    /**
     * Set recording duration (ms)
     *
     * If duration is less than 0, recording will not stop until calling [ViewRecorder.stop] or [ViewRecorder.destroy]
     *
     * @param duration duration (ms)
     */
    public fun setDuration(duration: Int): ViewRecorderBuilder {
        this.mDuration = duration
        return this
    }

    /**
     * Set frame rate (fps)
     *
     * @param frameRate frame rate (fps)
     */
    public fun setFrameRate(frameRate: Int): ViewRecorderBuilder {
        this.mFrameRate = frameRate
        return this
    }

    /**
     * Set gif animation size scale against original view size
     *
     * @param scale gif size scale
     */
    public fun setScale(scale: Double): ViewRecorderBuilder {
        this.mScale = scale
        return this
    }

    /**
     * Set loop count
     *
     * If loop count is 0, gif animation loop infinitely.
     *
     * @param loopCount loop count
     */
    public fun setLoopCount(loopCount: Int): ViewRecorderBuilder {
        this.mLoopCount = loopCount
        return this
    }

    /**
     * Set listener for callback recording finish
     *
     * @param listener Listener
     */
    public fun setOnRecordFinishListener(listener: ViewRecorder.OnRecordFinishListener): ViewRecorderBuilder {
        this.mOnRecordFinishListener = listener
        return this
    }

    /**
     * Set listener for callback encoding finish
     *
     * @param listener Listener
     */
    public fun setOnEncodeFinishListener(listener: ViewRecorder.OnEncodeFinishListener): ViewRecorderBuilder {
        this.mOnEncodeFinishListener = listener
        return this
    }
}
