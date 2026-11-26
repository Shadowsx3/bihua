package com.bassmd.bihua.backup

import com.bassmd.bihua.settings.AppSettings
import com.bassmd.bihua.settings.SettingsJsonMigration
import com.bassmd.bihua.settings.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The backup file. `version` only changes when the shape changes in a way old
 * readers can't handle; unknown fields are ignored so new fields are safe to add.
 */
@Serializable
data class BackupBundle(
    val format: String = FORMAT,
    val version: Int = CURRENT_VERSION,
    val exportedAtEpochMs: Long,
    val profile: UserProfile,
    val settings: AppSettings,
    /** The current avatar, embedded so a backup can move to another device. */
    val photoBase64: String? = null,
) {
    companion object {
        const val FORMAT = "bihua-backup"
        const val CURRENT_VERSION = 1
    }
}

class BackupException(message: String) : Exception(message)

object BackupCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(bundle: BackupBundle): String = json.encodeToString(BackupBundle.serializer(), bundle)

    /** Throws [BackupException] with a message fit to show the learner. */
    fun decode(text: String): BackupBundle {
        val bundle = try {
            json.decodeFromString(
                BackupBundle.serializer(),
                SettingsJsonMigration.migrateBackupText(text),
            )
        } catch (e: Exception) {
            throw BackupException(NOT_A_BACKUP)
        }
        if (bundle.format != BackupBundle.FORMAT) throw BackupException(NOT_A_BACKUP)
        if (bundle.version > BackupBundle.CURRENT_VERSION) throw BackupException(NEWER_VERSION)
        return bundle
    }

    const val NOT_A_BACKUP = "This file is not a Bihua backup."
    const val NEWER_VERSION = "This backup is from a newer version of Bihua. Update the app to import it."
}
