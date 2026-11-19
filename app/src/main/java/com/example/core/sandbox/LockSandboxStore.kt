package com.example.core.sandbox

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.core.model.GameId
import com.example.core.model.GameStats
import com.example.core.model.TelaJogoSettings
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Isolated LockSurface sandbox store.
 *
 * Security boundary:
 * - Stores strictly non-sensitive game states, local high scores, and display preferences.
 * - Never stores PII, notifications, contacts, or network tokens.
 * - Encrypts serialized payloads with AndroidKeyStore AES-256-GCM when available.
 */
class LockSandboxStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): TelaJogoSettings {
        val raw = readDecrypted(KEY_SETTINGS) ?: return TelaJogoSettings()
        return runCatching {
            val obj = JSONObject(raw)
            val gameName = obj.optString("activeGame", GameId.MICRO_2048.name)
            TelaJogoSettings(
                activeGame = GameId.entries.firstOrNull { it.name == gameName } ?: GameId.MICRO_2048,
                oledPowerSaver = obj.optBoolean("oledPowerSaver", true),
                silentMode = obj.optBoolean("silentMode", false),
                pocketGuardEnabled = obj.optBoolean("pocketGuardEnabled", true),
                inactivityTimeoutSec = obj.optInt("inactivityTimeoutSec", 12).coerceIn(6, 30),
                allowQuickSwitchOnLock = obj.optBoolean("allowQuickSwitchOnLock", true),
            )
        }.getOrDefault(TelaJogoSettings())
    }

    fun saveSettings(settings: TelaJogoSettings) {
        val obj = JSONObject().apply {
            put("activeGame", settings.activeGame.name)
            put("oledPowerSaver", settings.oledPowerSaver)
            put("silentMode", settings.silentMode)
            put("pocketGuardEnabled", settings.pocketGuardEnabled)
            put("inactivityTimeoutSec", settings.inactivityTimeoutSec)
            put("allowQuickSwitchOnLock", settings.allowQuickSwitchOnLock)
        }
        writeEncrypted(KEY_SETTINGS, obj.toString(), immediate = true)
    }

    fun loadAllStats(): Map<GameId, GameStats> {
        val raw = readDecrypted(KEY_STATS) ?: return GameId.entries.associateWith { GameStats() }
        return runCatching {
            val obj = JSONObject(raw)
            GameId.entries.associateWith { id ->
                val item = obj.optJSONObject(id.name)
                if (item != null) {
                    GameStats(
                        highScore = item.optInt("highScore", 0),
                        roundsPlayed = item.optInt("roundsPlayed", 0),
                        totalSecondsPlayed = item.optInt("totalSecondsPlayed", 0),
                    )
                } else {
                    GameStats()
                }
            }
        }.getOrDefault(GameId.entries.associateWith { GameStats() })
    }

    fun recordRound(gameId: GameId, score: Int, durationSec: Int): Map<GameId, GameStats> {
        val current = loadAllStats().toMutableMap()
        val prev = current[gameId] ?: GameStats()
        current[gameId] = GameStats(
            highScore = maxOf(prev.highScore, score),
            roundsPlayed = prev.roundsPlayed + 1,
            totalSecondsPlayed = prev.totalSecondsPlayed + durationSec.coerceAtLeast(1),
        )
        val obj = JSONObject()
        current.forEach { (id, stats) ->
            obj.put(
                id.name,
                JSONObject().apply {
                    put("highScore", stats.highScore)
                    put("roundsPlayed", stats.roundsPlayed)
                    put("totalSecondsPlayed", stats.totalSecondsPlayed)
                },
            )
        }
        writeEncrypted(KEY_STATS, obj.toString(), immediate = true)
        return current
    }

    fun saveGamePayload(gameId: GameId, payloadJson: String, immediate: Boolean = false) {
        writeEncrypted("game_state_${gameId.name}", payloadJson, immediate = immediate)
    }

    fun loadGamePayload(gameId: GameId): String? =
        readDecrypted("game_state_${gameId.name}")

    fun saveLastSavedTimestamp(epochMs: Long) {
        prefs.edit().putLong(KEY_LAST_SAVED_MS, epochMs).apply()
    }

    fun getLastSavedTimestamp(): Long = prefs.getLong(KEY_LAST_SAVED_MS, 0L)

    fun saveArrayAsJson(values: IntArray): String {
        val arr = JSONArray()
        values.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun writeEncrypted(key: String, plainText: String, immediate: Boolean) {
        val encoded = encryptWithKeystore(plainText) ?: "PLAIN:$plainText"
        val editor = prefs.edit().putString(key, encoded)
        if (immediate) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    private fun readDecrypted(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return if (stored.startsWith("PLAIN:")) {
            stored.removePrefix("PLAIN:")
        } else {
            decryptWithKeystore(stored)
        }
    }

    private fun encryptWithKeystore(plainText: String): String? = runCatching {
        val secretKey = getOrCreateSecretKey() ?: return null
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        "GCM:$ivB64:$dataB64"
    }.getOrNull()

    private fun decryptWithKeystore(stored: String): String? = runCatching {
        if (!stored.startsWith("GCM:")) return null
        val parts = stored.split(":")
        if (parts.size != 3) return null
        val iv = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[2], Base64.NO_WRAP)
        val secretKey = getOrCreateSecretKey() ?: return null
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateSecretKey(): SecretKey? = runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }.getOrNull()

    private companion object {
        const val PREFS_NAME = "telajogo_lock_sandbox"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "telajogo_sandbox_aes_key"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SETTINGS = "sandbox_settings"
        const val KEY_STATS = "sandbox_stats"
        const val KEY_LAST_SAVED_MS = "last_saved_ms"
    }
}
