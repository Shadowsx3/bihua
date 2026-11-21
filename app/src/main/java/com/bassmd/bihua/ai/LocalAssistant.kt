package com.bassmd.bihua.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(val bytes: Long, val total: Long) {
    val fraction: Float get() = if (total <= 0) 0f else (bytes.toFloat() / total).coerceIn(0f, 1f)
}

class ModelDownloader(
    private val context: Context,
    private val spec: LocalModelSpec,
) {

    fun download(): Flow<DownloadProgress> = flow {
        val target = spec.file(context)
        val temp = File(target.parentFile, "${target.name}.part")
        val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "Bihua/1.0")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) error("Model download failed: HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            var copied = 0L
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        emit(DownloadProgress(copied, total))
                    }
                }
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            emit(DownloadProgress(target.length(), target.length()))
        } finally {
            connection.disconnect()
            if (temp.exists() && !target.exists()) temp.delete()
        }
    }.flowOn(Dispatchers.IO)

    fun delete() {
        spec.file(context).delete()
    }
}

/**
 * A tiny wrapper around LiteRT-LM. The engine is created only when the learner
 * opens the assistant, and it is released when the screen leaves.
 */
private val SAMPLER = SamplerConfig(
    temperature = 0.65,
    topK = 40,
    topP = 0.92,
)

class LocalAssistant(
    private val context: Context,
    private val spec: LocalModelSpec,
) : AutoCloseable {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var warmedUp = false

    val isLoaded: Boolean get() = engine != null

    /**
     * Loads the engine and runs one tiny generation. The first generation is
     * much slower on GPU because shaders are compiled then; doing it while the
     * header says "Loading" keeps the first real question quick.
     */
    suspend fun prepare() {
        val created = prepareEngine()
        if (warmedUp) return
        withContext(Dispatchers.IO) {
        runCatching {
            created.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of("Reply with one word."),
                    samplerConfig = SAMPLER,
                )
            ).use { convo ->
                convo.sendMessageAsync("Hi").collect { }
            }
            warmedUp = true
        }
        }
    }

    suspend fun ask(prompt: String, system: String): Flow<String> {
        val created = prepareEngine()
        return flow {
            created.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(system),
                    samplerConfig = SAMPLER,
                )
            ).use { convo ->
                convo.sendMessageAsync(prompt).collect { message ->
                    val text = message.toString()
                    if (text.isNotBlank()) emit(text)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun prepareEngine(): Engine {
        engine?.let { return it }
        return withContext(Dispatchers.IO) {
            val path = spec.file(context).absolutePath
            val cache = context.cacheDir.absolutePath
            val gpu = runCatching {
                Engine(
                    EngineConfig(
                        modelPath = path,
                        backend = Backend.GPU(),
                        maxNumTokens = 2_048,
                        cacheDir = cache,
                    )
                ).also { it.initialize() }
            }
            val created = gpu.getOrElse {
                Engine(
                    EngineConfig(
                        modelPath = path,
                        backend = Backend.CPU(),
                        maxNumTokens = 2_048,
                        cacheDir = cache,
                    )
                ).also { it.initialize() }
            }
            engine = created
            created
        }
    }

    override fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}
