package com.todoplus.audio

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.todoplus.settings.TodoSettingsService
import java.awt.Toolkit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.concurrent.thread

/**
 * Service for playing subtle audio feedback when completing/un-completing tasks
 */
@Service(Service.Level.APP)
class SoundEffectsService {

    fun playCompleteSound() {
        if (!isAudioEnabled()) return
        thread(start = true, isDaemon = true, name = "TODO-Plus-Audio-Complete") {
            try {
                playChimeTone()
            } catch (e: Throwable) {
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }

    fun playIncompleteSound() {
        if (!isAudioEnabled()) return
        thread(start = true, isDaemon = true, name = "TODO-Plus-Audio-Incomplete") {
            try {
                playPopTone()
            } catch (e: Throwable) {
                Toolkit.getDefaultToolkit().beep()
            }
        }
    }

    private fun isAudioEnabled(): Boolean {
        return try {
            TodoSettingsService.getInstance().state.enableAudioFeedback
        } catch (e: Throwable) {
            true
        }
    }

    private fun playChimeTone() {
        val sampleRate = 44100f
        val format = AudioFormat(sampleRate, 8, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, 4410)
        line.start()

        val buf = ByteArray(2205) // ~50ms tone
        val freqs = doubleArrayOf(1318.5, 1975.5) // Crisp E6 - B6 chime
        for (f in freqs) {
            for (i in buf.indices) {
                val angle = 2.0 * Math.PI * i / (sampleRate / f)
                val decay = (buf.size - i).toDouble() / buf.size
                buf[i] = (Math.sin(angle) * 80 * decay).toInt().toByte()
            }
            line.write(buf, 0, buf.size)
        }
        line.drain()
        line.close()
    }

    private fun playPopTone() {
        val sampleRate = 44100f
        val format = AudioFormat(sampleRate, 8, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, 2205)
        line.start()

        val buf = ByteArray(1500)
        val f = 400.0
        for (i in buf.indices) {
            val angle = 2.0 * Math.PI * i / (sampleRate / (f - i * 0.15))
            val decay = (buf.size - i).toDouble() / buf.size
            buf[i] = (Math.sin(angle) * 70 * decay).toInt().toByte()
        }
        line.write(buf, 0, buf.size)
        line.drain()
        line.close()
    }

    companion object {
        fun getInstance(): SoundEffectsService = service()
    }
}
