package net.tenbo.hubble.desktop

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin

/**
 * Synthesized notification tones (no copyrighted assets). Each sound plays on a daemon
 * thread; if the machine has no audio device, playback fails silently. Whether sounds
 * play at all is the caller's decision (the user's setting).
 */
object Sound {
    private data class Tone(val hz: Double, val ms: Int)

    /** The classic "you're online" chime — a bright ascending two-note. */
    fun signIn() = play(listOf(Tone(659.25, 110), Tone(987.77, 180)))

    /** A short incoming-message blip. */
    fun message() = play(listOf(Tone(880.0, 70), Tone(1174.66, 110)))

    /** A low buzz for a nudge. */
    fun nudge() = play(listOf(Tone(196.0, 220)))

    private fun play(tones: List<Tone>) {
        Thread {
            runCatching {
                val rate = 44100f
                val format = AudioFormat(rate, 16, 1, true, false)
                val line = AudioSystem.getSourceDataLine(format)
                line.open(format); line.start()
                for (t in tones) line.write(render(t, rate), 0, render(t, rate).size)
                line.drain(); line.close()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun render(t: Tone, rate: Float): ByteArray {
        val n = (rate * t.ms / 1000).toInt()
        val out = ByteArray(n * 2)
        for (i in 0 until n) {
            // Sine with short attack/release fades so there are no clicks.
            val fade = minOf(1.0, i / 400.0, (n - i) / 400.0)
            val s = (sin(2 * PI * t.hz * i / rate) * 0.35 * fade * Short.MAX_VALUE).toInt()
            out[i * 2] = (s and 0xFF).toByte()
            out[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }
}
