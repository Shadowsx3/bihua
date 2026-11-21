package com.bassmd.bihua.ai

import android.app.ActivityManager
import android.content.Context
import java.io.File

/**
 * A real downloadable LiteRT-LM model. Sizes and licences are the source values;
 * the device recommendation is based on total RAM, not on a benchmark claim.
 */
data class LocalModelSpec(
    val id: String,
    val name: String,
    val sizeLabel: String,
    val sizeBytes: Long,
    val minRamGb: Int,
    val url: String,
    val fileName: String,
    val license: String,
    val note: String,
    val experimental: Boolean = false,
) {
    fun file(context: Context): File =
        File(File(context.filesDir, "models").apply { mkdirs() }, fileName)

    fun isDownloaded(context: Context): Boolean {
        val file = file(context)
        return file.isFile && file.length() >= (sizeBytes * 95 / 100)
    }
}

object LocalModels {
    val QWEN_06B = LocalModelSpec(
        id = "qwen3-0.6b",
        name = "Qwen3 0.6B",
        sizeLabel = "0.6 GB",
        sizeBytes = 586_000_000L,
        minRamGb = 4,
        url = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm?download=true",
        fileName = "qwen3-0.6b.litertlm",
        license = "Apache 2.0",
        note = "Fast and small. The safest choice for an 8 GB tablet.",
    )
    val QWEN_17B = LocalModelSpec(
        id = "qwen3-1.7b",
        name = "Qwen3 1.7B",
        sizeLabel = "1.7 GB",
        sizeBytes = 1_700_000_000L,
        minRamGb = 8,
        url = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3_1.7B.litertlm?download=true",
        fileName = "qwen3-1.7b.litertlm",
        license = "Apache 2.0",
        note = "A middle ground between speed and answer quality.",
    )
    val QWEN_15B = LocalModelSpec(
        id = "qwen2.5-1.5b",
        name = "Qwen2.5 1.5B",
        sizeLabel = "1.6 GB",
        sizeBytes = 1_625_493_432L,
        minRamGb = 8,
        url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
        fileName = "qwen2.5-1.5b.litertlm",
        license = "Apache 2.0",
        note = "More capable, more memory. Recommended only when the tablet has 8 GB or more.",
    )
    val PHI_4_MINI = LocalModelSpec(
        id = "phi-4-mini",
        name = "Phi-4 mini",
        sizeLabel = "3.9 GB",
        sizeBytes = 3_906_000_000L,
        minRamGb = 8,
        url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm?download=true",
        fileName = "phi-4-mini.litertlm",
        license = "MIT",
        note = "The largest option. More capable, but slower and memory-heavy.",
    )
    val GEMMA_4_E2B = LocalModelSpec(
        id = "gemma-4-e2b",
        name = "Gemma 4 E2B",
        sizeLabel = "2.4 GB",
        sizeBytes = 2_340_870_772L,
        minRamGb = 8,
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-gpu.litertlm?download=true",
        fileName = "gemma-4-E2B-it-gpu.litertlm",
        license = "Gemma Terms of Use",
        note = "A strong Gemma model for an 8 GB tablet. GPU is preferred, CPU is the fallback.",
    )

    val all: List<LocalModelSpec> = listOf(QWEN_06B, QWEN_15B, QWEN_17B, GEMMA_4_E2B, PHI_4_MINI)

    fun byId(id: String?): LocalModelSpec = all.firstOrNull { it.id == id } ?: QWEN_06B

    fun totalRamGb(context: Context): Int {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return (info.totalMem / 1_073_741_824L).toInt().coerceAtLeast(1)
    }

    fun recommended(context: Context): LocalModelSpec =
        if (totalRamGb(context) >= 8) GEMMA_4_E2B else QWEN_06B
}
