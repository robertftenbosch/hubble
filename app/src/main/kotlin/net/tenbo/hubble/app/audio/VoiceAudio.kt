package net.tenbo.hubble.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File

/** MIME used for our recordings (AMR narrowband in a 3GP container — tiny, voice-optimized). */
const val VOICE_MIME = "audio/3gpp"

/**
 * Records a short voice clip to a 3GP/AMR file. AMR-NB keeps clips small (~1KB/sec), so a
 * note fits comfortably in one E2E envelope. One recorder instance = one recording.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAt = 0L

    fun start(): Boolean = runCatching {
        val file = File(context.cacheDir, "rec-${System.nanoTime()}.3gp")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r; output = file; startedAt = SystemClock.elapsedRealtime()
        true
    }.getOrElse { Log.w("VoiceRecorder", "start failed: ${it.message}"); cleanup(); false }

    /** Stop and return the finished clip + its duration, or null if it was too short/failed. */
    fun stop(): Pair<File, Long>? {
        val r = recorder ?: return null
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        val result = runCatching { r.stop(); output }.getOrNull()
        runCatching { r.release() }
        recorder = null
        val file = result?.takeIf { it.exists() && it.length() > 0 && durationMs >= 500 }
        if (file == null) { output?.delete(); return null }
        return file to durationMs
    }

    private fun cleanup() {
        runCatching { recorder?.release() }; recorder = null; output?.delete(); output = null
    }
}

/** Plays one voice clip at a time. Call [toggle] with a file path; calling it again stops. */
object VoicePlayer {
    private var player: MediaPlayer? = null
    private var playingPath: String? = null

    /** Start playing [path] (stopping anything else). [onState] reports the path now playing, or null when it stops. */
    fun toggle(path: String, onState: (String?) -> Unit) {
        if (playingPath == path) { stop(); onState(null); return }
        stopInternal()
        runCatching {
            val mp = MediaPlayer()
            mp.setDataSource(path)
            mp.setOnCompletionListener { stop(); onState(null) }
            mp.prepare(); mp.start()
            player = mp; playingPath = path
            onState(path)
        }.onFailure { Log.w("VoicePlayer", "play failed: ${it.message}"); onState(null) }
    }

    fun stop() { stopInternal() }

    private fun stopInternal() {
        runCatching { player?.stop(); player?.release() }
        player = null; playingPath = null
    }
}
