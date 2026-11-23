package com.bassmd.bihua.rwkv

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.exp
import kotlin.math.ln
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RwkvOnnxEngineTest {

    private fun modelBytes(): ByteArray =
        javaClass.getResourceAsStream("/rwkv/synthetic.onnx")?.use { it.readBytes() }
            ?: error("synthetic model missing")

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun packageFile(bytes: ByteArray, manifest: RwkvManifest): File {
        val file = File.createTempFile("bihua-rwkv-", ".zip")
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(RwkvPackage.MANIFEST_NAME))
            zip.write(
                """{"format":"${manifest.format}","name":"${manifest.name}",
                |"architecture":"${manifest.architecture}","features":${manifest.features},
                |"stateSize":${manifest.stateSize},"sha256":"${manifest.sha256}",
                |"license":"${manifest.license}","source":"${manifest.source}"}""".trimMargin().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(RwkvPackage.MODEL_NAME))
            zip.write(bytes)
            zip.closeEntry()
        }
        return file
    }

    private fun manifest(bytes: ByteArray) = RwkvManifest(
        format = RwkvPackage.FORMAT,
        name = "Synthetic step model",
        architecture = RwkvPackage.ARCHITECTURE,
        features = RwkvPackage.FEATURE_COUNT,
        stateSize = 8,
        sha256 = sha256(bytes),
        license = "Test only",
        source = "https://example.com/test",
    )

    @Test
    fun thePackageIsValidatedAndLoaded() {
        val bytes = modelBytes()
        val manifest = manifest(bytes)
        val file = packageFile(bytes, manifest)
        try {
            val state = RwkvPackage.inspect(file)
            assertTrue(state is RwkvPackageState.Installed)
            val engine = RwkvOnnxEngine.load(file, manifest).getOrThrow()
            engine.use {
                assertEquals(8, it.stateSize)
                val prediction = it.predict(FloatArray(7), FloatArray(8))
                assertTrue(prediction.recall in 0f..1f)
                assertEquals(8, prediction.state.size)
                assertEquals(0.01f, prediction.state[0], 1e-5f)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun aBadChecksumIsRejected() {
        val bytes = modelBytes()
        val manifest = manifest(bytes).copy(sha256 = "0".repeat(64))
        val file = packageFile(bytes, manifest)
        try {
            val state = RwkvPackage.inspect(file)
            assertTrue(state is RwkvPackageState.Invalid)
        } finally {
            file.delete()
        }
    }

    @Test
    fun theIntervalSearchUsesTheModelsRecall() {
        val bytes = modelBytes()
        val manifest = manifest(bytes)
        val file = packageFile(bytes, manifest)
        try {
            val engine = RwkvOnnxEngine.load(file, manifest).getOrThrow()
            engine.use {
                val interval = it.intervalDays(
                    state = FloatArray(8),
                    event = RwkvReviewEvent(0f, 3, 1, 0, 0, 12, 2),
                    retention = 0.90f,
                    maxIntervalDays = 365,
                )
                // sigmoid(1.5 - ln(t)) = 0.9 => t ~= 0.5 day.
                assertTrue("interval was $interval", interval in 0.30f..0.60f)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun theManifestRejectsUnsafeOrIncompletePackages() {
        val base = manifest(modelBytes())
        assertTrue(RwkvPackage.validateManifest(base) == null)
        assertTrue(RwkvPackage.validateManifest(base.copy(license = "")) != null)
        assertTrue(RwkvPackage.validateManifest(base.copy(source = "http://example.com")) != null)
        assertTrue(RwkvPackage.validateManifest(base.copy(sha256 = "xyz")) != null)
        assertTrue(RwkvPackage.validateManifest(base.copy(architecture = "other")) != null)
        assertTrue(RwkvPackage.validateManifest(base.copy(features = 92)) != null)
        assertTrue(RwkvPackage.validateManifest(base.copy(stateSize = 0)) != null)
    }

    @Test
    fun featureFrameIsStableAndFinite() {
        val frame = RwkvFeatureFrame.encode(
            RwkvReviewEvent(7f, 3, 12, 2, 1, 18, 4),
        )
        assertEquals(RwkvPackage.FEATURE_COUNT, frame.size)
        assertTrue(frame.all { it.isFinite() })
        assertEquals(ln(7f + 1f / 1_440f), frame[0], 1e-5f)
        assertEquals(0.75f, frame[1], 1e-6f)
        assertNotNull(exp(frame[0]))
    }
}
