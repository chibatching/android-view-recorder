package com.chibatching.screenrecorder

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import java.io.ByteArrayOutputStream
import java.io.File


object ScreenRecorder {
    var activity: Activity? = null
    var isRecording: Boolean = false

    fun setup(activity: Activity) {
        this.activity = activity
    }

    fun start() {
        if (activity == null) {
            return
        }

        if (activity!!.isDestroyed() or activity!!.isFinishing()) {
            return
        }

        Log.d(javaClass<ScreenRecorder>().getSimpleName(), "start")
        isRecording = true

        val view = activity!!.getWindow().getDecorView()
        view.setDrawingCacheEnabled(true)

        val fileDirPath = activity!!.getFilesDir().getAbsolutePath().plus("/screen_record_temp")
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

            var frame = 0
            while (frame < 30) {
                Log.d(javaClass<ScreenRecorder>().getSimpleName(), "frame: $frame")
                view.setDrawingCacheEnabled(true)
                val bitmap = view.getDrawingCache();
                val frameFile = File(fileDirPath.plus("/$frame.png"))
                ByteArrayOutputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    frameFile.createNewFile()
                    frameFile.writeBytes(it.toByteArray())
                    view.setDrawingCacheEnabled(false)
                    frame++
                }
                Thread.sleep(100)
            }

            encodeBitmapToMp4(fileDirPath, activity!!.getFilesDir().getAbsolutePath().plus("/output.mp4"))
            isRecording = false
        }).start()
    }
}