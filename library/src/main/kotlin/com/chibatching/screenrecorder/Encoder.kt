package com.chibatching.screenrecorder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer


fun encodeBitmapToMp4(srcFileDirPath: String, dstFileName: String) {
    val fileDir: File = File(srcFileDirPath)

    val codec = MediaCodec.createEncoderByType("video/avc")
    val videoFormat = MediaFormat.createVideoFormat("video/avc", 1080, 1776)
    videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 700000);
    videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
    videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
    videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

    codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    codec.start()
    val codecInputBuffers = codec.getInputBuffers()
    val codecOutputBuffers = codec.getOutputBuffers()

    val outputFile = File(dstFileName)
    outputFile.createNewFile()

    FileOutputStream(outputFile).use { fos ->
        fileDir.listFiles().forEach {
            val inputBufferIndex = codec.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val input = it.readBytes()
                codecInputBuffers[inputBufferIndex].put(input)
//                codec.getInputBuffer(inputBufferIndex).put(input)
                codec.queueInputBuffer(inputBufferIndex, 0, input.size(), 0, 0)

                val info = MediaCodec.BufferInfo()
                val outputBufferIndex = codec.dequeueOutputBuffer(info, 100)
                if (outputBufferIndex >= 0) {
                    val byteArray = ByteArray(info.size)
                    codecOutputBuffers[outputBufferIndex].get(byteArray)
//                    codec.getOutputBuffer(outputBufferIndex).get(byteArray)

                    fos.write(byteArray)

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    codecInputBuffers[inputBufferIndex].clear()
                    codecOutputBuffers[outputBufferIndex].clear()
                }
            }
        }

        fos.flush()
    }
    codec.flush()
    codec.stop()
    codec.release()
}

