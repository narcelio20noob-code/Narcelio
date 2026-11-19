package com.example.core.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackEngine(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    @Volatile
    var isSilentMode: Boolean = false

    fun tapLight() = vibrate(durationMs = 12L, amplitude = 55)

    fun mergeTick() = vibrate(durationMs = 18L, amplitude = 95)

    fun scorePulse() = vibrate(durationMs = 24L, amplitude = 125)

    fun collisionHeavy() = vibrate(durationMs = 45L, amplitude = 180)

    fun unlockReject() = vibrate(durationMs = 30L, amplitude = 110)

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (isSilentMode) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }
}
