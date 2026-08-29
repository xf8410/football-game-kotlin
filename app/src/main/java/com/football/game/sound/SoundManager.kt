package com.football.game.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * 音效管理器
 * 全部音效由代码实时合成 PCM（AudioTrack 播放），无需任何音频资源文件：
 * - WHISTLE       裁判长哨（2100+2400Hz 双频颤音）
 * - WHISTLE_SHORT 裁判短哨（开球/重新开球）
 * - KICK          踢球（低频冲击 + 噪声）
 * - TACKLE        铲球/身体碰撞（闷响）
 * - CHEER         进球欢呼（人群噪声涌起）
 */
object SoundManager {

    enum class Sfx { WHISTLE, WHISTLE_SHORT, KICK, TACKLE, CHEER }

    private const val SAMPLE_RATE = 22050

    fun play(sfx: Sfx, volume: Float = 0.9f) {
        Thread {
            var track: AudioTrack? = null
            try {
                val samples = when (sfx) {
                    Sfx.WHISTLE -> whistleSamples(1.15f)
                    Sfx.WHISTLE_SHORT -> whistleSamples(0.38f)
                    Sfx.KICK -> kickSamples()
                    Sfx.TACKLE -> tackleSamples()
                    Sfx.CHEER -> cheerSamples()
                }
                val pcm = ShortArray(samples.size) { i ->
                    (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE * volume).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / SAMPLE_RATE + 150)
                track.release()
            } catch (_: Exception) {
                try { track?.release() } catch (_: Exception) {}
            }
        }.start()
    }

    /** 裁判哨：双频 + 颤音 + 起止包络 */
    private fun whistleSamples(duration: Float): FloatArray {
        val n = (duration * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toFloat()
            val warble = 1f + 0.22f * sin(2f * PI.toFloat() * 30f * t)
            val v = 0.5f * sin(2f * PI.toFloat() * 2100f * warble * t) +
                    0.32f * sin(2f * PI.toFloat() * 2400f * t) +
                    0.08f * (Random.nextFloat() * 2f - 1f)
            val env = when {
                t < 0.02f -> t / 0.02f
                t > duration - 0.09f -> max(0f, (duration - t) / 0.09f)
                else -> 1f
            }
            out[i] = v * env * 0.85f
        }
        return out
    }

    /** 踢球：低频冲击 + 快速衰减噪声 */
    private fun kickSamples(): FloatArray {
        val duration = 0.12f
        val n = (duration * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        var noise = 0f
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toFloat()
            noise = noise * 0.72f + (Random.nextFloat() * 2f - 1f) * 0.28f
            val thump = 0.8f * sin(2f * PI.toFloat() * (110f - 60f * t / duration) * t)
            val env = exp(-t * 34f)
            out[i] = (thump * 0.85f + noise * 0.5f) * env
        }
        return out
    }

    /** 铲球/倒地：闷响 + 拖地摩擦 */
    private fun tackleSamples(): FloatArray {
        val duration = 0.22f
        val n = (duration * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        var noise = 0f
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toFloat()
            noise = noise * 0.8f + (Random.nextFloat() * 2f - 1f) * 0.2f
            val thud = 0.7f * sin(2f * PI.toFloat() * 140f * t)
            val env = exp(-t * 20f)
            out[i] = (thud * 0.8f + noise * 0.55f) * env
        }
        return out
    }

    /** 人群欢呼：噪声涌起回落 + 随机起伏 */
    private fun cheerSamples(): FloatArray {
        val duration = 1.6f
        val n = (duration * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        var noise = 0f
        var swell = 0f
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toFloat()
            noise = noise * 0.94f + (Random.nextFloat() * 2f - 1f) * 0.06f
            swell += (Random.nextFloat() - 0.5f) * 0.04f
            swell = swell.coerceIn(-0.25f, 0.25f)
            val env = sin(PI.toFloat() * t / duration) * (0.6f + swell)
            val bright = 0.3f * sin(2f * PI.toFloat() * (600f + 200f * swell) * t)
            out[i] = (noise * 1.6f + bright * 0.4f) * env
        }
        return out
    }
}
