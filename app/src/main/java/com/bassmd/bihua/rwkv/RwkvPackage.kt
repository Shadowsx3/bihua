package com.bassmd.bihua.rwkv

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * An optional, user-supplied RWKV-Instant model package.
 *
 * Bihua does not ship the trained weights. The published weights are marked
 * research/personal-use only, so the app can only validate and keep a package
 * that the learner obtained themselves. Nothing here downloads a model or sends
 * one off the device.
 */
@Serializable
data class RwkvNames(
    val features: String = "features",
    val state: String = "state",
    val recall: String = "recall",
    val stateOut: String = "state_out",
)

@Serializable
data class RwkvManifest(
    val format: String,
    val name: String,
    val architecture: String,
    val features: Int,
    val stateSize: Int,
    val sha256: String,
    val license: String,
    val source: String,
    val inputs: RwkvNames = RwkvNames(),
    val outputs: RwkvNames = RwkvNames(),
    val notes: String = "",
)

sealed interface RwkvPackageState {
    data object Missing : RwkvPackageState
    data class Installed(
        val manifest: RwkvManifest,
        val sizeBytes: Long,
    ) : RwkvPackageState
    data class Invalid(val reason: String) : RwkvPackageState
}

object RwkvPackage {
    const val FORMAT = "bihua-rwkv-package/2"
    const val ARCHITECTURE = "rwkv-instant-step"
    const val FEATURE_VERSION = 1
    const val FEATURE_COUNT = 7
    const val MAX_BYTES = 250L * 1024L * 1024L
    const val MANIFEST_NAME = "manifest.json"
    const val MODEL_NAME = "model.onnx"

    private val json = Json { ignoreUnknownKeys = true }

    /** Validates a zip with a manifest and an ONNX model; never executes it. */
    fun inspect(file: File): RwkvPackageState {
        if (!file.isFile || file.length() <= 0L) {
            return RwkvPackageState.Invalid("The package is empty.")
        }
        if (file.length() > MAX_BYTES) {
            return RwkvPackageState.Invalid("The package is larger than 250 MB.")
        }
        return try {
            ZipFile(file).use { zip ->
                val entries = zip.entries().toList()
                if (entries.any { it.isDirectory || it.name.startsWith("/") || ".." in it.name }) {
                    return RwkvPackageState.Invalid("The package has an unsafe path.")
                }
                val manifestEntry = entries.firstOrNull { it.name == MANIFEST_NAME }
                    ?: return RwkvPackageState.Invalid("The package is missing $MANIFEST_NAME.")
                val modelEntry = entries.firstOrNull { it.name == MODEL_NAME }
                    ?: return RwkvPackageState.Invalid("The package is missing $MODEL_NAME.")
                if (modelEntry.size <= 0L || modelEntry.size > MAX_BYTES) {
                    return RwkvPackageState.Invalid("The model size is not valid.")
                }
                val manifestText = zip.getInputStream(manifestEntry).bufferedReader().use { reader ->
                    reader.readText().take(64 * 1024)
                }
                val manifest = runCatching {
                    json.decodeFromString<RwkvManifest>(manifestText)
                }.getOrElse {
                    return RwkvPackageState.Invalid("The manifest is not valid JSON.")
                }
                val problem = validateManifest(manifest)
                if (problem != null) return RwkvPackageState.Invalid(problem)
                val digest = zip.getInputStream(modelEntry).use { input ->
                    val md = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        md.update(buffer, 0, read)
                    }
                    md.digest().joinToString("") { "%02x".format(it) }
                }
                if (!digest.equals(manifest.sha256.trim(), ignoreCase = true)) {
                    return RwkvPackageState.Invalid("The model checksum does not match the manifest.")
                }
                RwkvPackageState.Installed(manifest, modelEntry.size)
            }
        } catch (error: Exception) {
            RwkvPackageState.Invalid("The package is damaged or is not a zip.")
        }
    }

    fun validateManifest(manifest: RwkvManifest): String? {
        if (manifest.format != FORMAT) return "Unsupported package format."
        if (manifest.name.isBlank()) return "The manifest has no name."
        if (manifest.architecture != ARCHITECTURE) return "This is not an RWKV-Instant model."
        if (manifest.features != FEATURE_COUNT) return "The model expects a different feature frame."
        if (manifest.stateSize !in 1..4_096) return "The model state size is not supported."
        if (manifest.inputs.features.isBlank() || manifest.inputs.state.isBlank()) {
            return "The manifest must name its inputs."
        }
        if (manifest.outputs.recall.isBlank() || manifest.outputs.stateOut.isBlank()) {
            return "The manifest must name its outputs."
        }
        if (manifest.sha256.length != 64 || manifest.sha256.any { it !in "0123456789abcdefABCDEF" }) {
            return "The manifest checksum is not a SHA-256 value."
        }
        if (manifest.license.isBlank()) return "The manifest must state the model licence."
        if (!manifest.source.startsWith("https://")) return "The manifest must link to the model source."
        return null
    }
}
