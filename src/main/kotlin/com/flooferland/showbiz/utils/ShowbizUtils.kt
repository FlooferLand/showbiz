package com.flooferland.showbiz.utils

import javax.sound.sampled.*

object ShowbizUtils {
    fun convertAudio(data: ByteArray, sourceFormat: AudioFormat, targetFormat: AudioFormat): ByteArray {
        val source = AudioInputStream(
            data.inputStream(),
            sourceFormat,
            (data.size / sourceFormat.frameSize).toLong()
        )
        val target = AudioSystem.getAudioInputStream(targetFormat, source)
        return target.readAllBytes()
    }

    /** Gets the source data line; Will return an error if Java's audio system is acting up, as it does most times */
    fun startAudioDevice(bestFormat: AudioFormat, bufferSize: Int, device: Mixer.Info? = null): Result<SourceDataLine> {
        // Finding a line
        val result = runCatching {
            val info = DataLine.Info(SourceDataLine::class.java, bestFormat)
            if (device == null) {
                (AudioSystem.getLine(info) as SourceDataLine)
            } else {
                AudioSystem.getMixer(device).getLine(info) as SourceDataLine
            }
        }
        result.onFailure { err ->
            return Result.failure(err)
        }

        // Starting the line
        val line = result.getOrNull()!!
        val lineResult = runCatching {
            line.open(bestFormat, bufferSize * 2)
            line.start()
        }
        lineResult.onSuccess {
            return Result.success(line)
        }
        lineResult.onFailure { err ->
            return Result.failure(Error("Error opening line: $err"))
        }
        return Result.failure(Error("Unknown"))
    }
}