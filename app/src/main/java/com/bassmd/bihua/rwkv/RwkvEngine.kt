package com.bassmd.bihua.rwkv

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.Closeable
import java.io.File
import java.nio.FloatBuffer
import java.util.zip.ZipFile
import kotlin.math.exp
import kotlin.math.ln

data class RwkvPrediction(
    val recall: Float,
    val state: FloatArray,
)

/** The narrow step-model contract the optional ONNX package must implement. */
interface RwkvEngine : Closeable {
    val stateSize: Int

    fun predict(features: FloatArray, state: FloatArray): RwkvPrediction

    /** Largest interval whose predicted recall is still at or above [retention]. */
    fun intervalDays(
        state: FloatArray,
        event: RwkvReviewEvent,
        retention: Float,
        maxIntervalDays: Int,
        probes: Int = 12,
    ): Float {
        val floor = 1f / 1_440f
        val ceiling = maxIntervalDays.coerceAtLeast(1).toFloat()
        val logFloor = ln(floor)
        val logCeiling = ln(ceiling)
        var low = floor
        var high = -1f
        for (index in 0..probes) {
            val days = exp(logFloor + (logCeiling - logFloor) * index / probes)
            val recall = predict(
                RwkvFeatureFrame.encode(event.copy(elapsedDays = days)),
                state,
            ).recall
            if (recall >= retention) {
                low = days
            } else {
                high = days
                break
            }
        }
        if (high < 0f) return ceiling
        repeat(14) {
            val mid = kotlin.math.sqrt(low * high)
            val recall = predict(
                RwkvFeatureFrame.encode(event.copy(elapsedDays = mid)),
                state,
            ).recall
            if (recall >= retention) low = mid else high = mid
        }
        return low
    }
}

/**
 * Runs a user-supplied RWKV-compatible step model with ONNX Runtime on the
 * device. The package declares its tensor names and state size; the app checks
 * the shapes before accepting it.
 */
class RwkvOnnxEngine private constructor(
    private val environment: OrtEnvironment,
    private val session: OrtSession,
    private val manifest: RwkvManifest,
) : RwkvEngine {
    override val stateSize: Int get() = manifest.stateSize

    override fun predict(features: FloatArray, state: FloatArray): RwkvPrediction {
        require(features.size == manifest.features) { "Wrong feature frame size." }
        require(state.size == manifest.stateSize) { "Wrong state size." }
        var featureTensor: OnnxTensor? = null
        var stateTensor: OnnxTensor? = null
        try {
            featureTensor = OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(features),
                longArrayOf(1, features.size.toLong()),
            )
            stateTensor = OnnxTensor.createTensor(
                environment,
                FloatBuffer.wrap(state),
                longArrayOf(1, state.size.toLong()),
            )
            val inputs = mapOf(
                manifest.inputs.features to featureTensor,
                manifest.inputs.state to stateTensor,
            )
            session.run(inputs).use { result ->
                val recallTensor = result.get(manifest.outputs.recall)
                    .orElseThrow { IllegalStateException("The model did not return recall.") }
                    as OnnxTensor
                val stateTensorOut = result.get(manifest.outputs.stateOut)
                    .orElseThrow { IllegalStateException("The model did not return a state.") }
                    as OnnxTensor
                val recallBuffer = recallTensor.floatBuffer
                val stateBuffer = stateTensorOut.floatBuffer
                val next = FloatArray(state.size)
                stateBuffer.get(next)
                val recall = recallBuffer.get(0).coerceIn(1e-6f, 1f - 1e-6f)
                return RwkvPrediction(recall, next)
            }
        } finally {
            featureTensor?.close()
            stateTensor?.close()
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        fun load(file: File, manifest: RwkvManifest): Result<RwkvOnnxEngine> = runCatching {
            val bytes = ZipFile(file).use { zip ->
                val entry = zip.getEntry(RwkvPackage.MODEL_NAME)
                    ?: error("The package is missing ${RwkvPackage.MODEL_NAME}.")
                zip.getInputStream(entry).use { it.readBytes() }
            }
            val environment = OrtEnvironment.getEnvironment()
            val options = OrtSession.SessionOptions()
            try {
                options.setIntraOpNumThreads(1)
                options.setInterOpNumThreads(1)
            } catch (_: Exception) {
                // Older runtimes may not expose threading knobs.
            }
            val session = try {
                environment.createSession(bytes, options)
            } catch (error: Exception) {
                error("ONNX Runtime could not open the model: ${error.message}")
            } finally {
                options.close()
            }
            val engine = RwkvOnnxEngine(environment, session, manifest)
            try {
                engine.validateShapes()
            } catch (error: Exception) {
                engine.close()
                throw error
            }
            engine
        }

        fun inspect(file: File, manifest: RwkvManifest): String? =
            load(file, manifest).fold(
                onSuccess = { engine -> engine.close(); null },
                onFailure = { it.message ?: "The model could not be loaded." },
            )
    }

    private fun validateShapes() {
        val inputs = session.inputInfo
        val featureInfo = inputs[manifest.inputs.features]?.info as? TensorInfo
            ?: return invalid("The features input is missing or is not a tensor.")
        val stateInfo = inputs[manifest.inputs.state]?.info as? TensorInfo
            ?: return invalid("The state input is missing or is not a tensor.")
        if (!shapeMatches(featureInfo.shape, 1, manifest.features)) {
            invalid("The features input must be [1, ${manifest.features}].")
        }
        if (!shapeMatches(stateInfo.shape, 1, manifest.stateSize)) {
            invalid("The state input must be [1, ${manifest.stateSize}].")
        }
        val outputs = session.outputInfo
        val recallInfo = outputs[manifest.outputs.recall]?.info as? TensorInfo
            ?: return invalid("The recall output is missing or is not a tensor.")
        val nextStateInfo = outputs[manifest.outputs.stateOut]?.info as? TensorInfo
            ?: return invalid("The state output is missing or is not a tensor.")
        if (!shapeMatches(recallInfo.shape, 1, 1, scalar = true)) {
            invalid("The recall output must be one value.")
        }
        if (!shapeMatches(nextStateInfo.shape, 1, manifest.stateSize)) {
            invalid("The state output must be [1, ${manifest.stateSize}].")
        }
    }

    private fun shapeMatches(shape: LongArray, vararg expected: Int, scalar: Boolean = false): Boolean {
        if (scalar) {
            val values = shape.filter { it >= 0 }
            return values.isEmpty() || values.fold(1L) { a, b -> a * b } == 1L
        }
        if (shape.size != expected.size) return false
        return shape.indices.all { index ->
            shape[index] < 0 || shape[index] == expected[index].toLong()
        }
    }

    private fun invalid(reason: String): Nothing = throw IllegalArgumentException(reason)
}
