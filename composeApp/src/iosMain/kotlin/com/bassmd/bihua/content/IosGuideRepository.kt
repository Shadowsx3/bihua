@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bassmd.bihua.content

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/** Stroke guides from the Android content database, sharded and loaded on demand. */
class IosGuideRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val shards = mutableMapOf<Int, Map<String, GuideDto>>()

    suspend fun guide(glyphGroupId: String): CharacterGuide? = withContext(Dispatchers.Default) {
        val shard = glyphGroupId.sumOf { it.code } % 32
        val entries = shards.getOrPut(shard) { load(shard) }
        val dto = entries[glyphGroupId] ?: return@withContext null
        CharacterGuide(
            strokes = dto.strokes.map { stroke ->
                GuideStroke(
                    points = stroke.points.map { Offset(it[0], it[1]) },
                    pathData = stroke.path,
                )
            },
            source = dto.source,
            kana = dto.kana,
        )
    }

    private fun load(shard: Int): Map<String, GuideDto> {
        val name = "guide_${shard.toString().padStart(2, '0')}"
        val path = platform.Foundation.NSBundle.mainBundle.pathForResource(name, ofType = "json")
            ?: error("Missing stroke guide $name.json")
        val text = NSString.stringWithContentsOfFile(
            path = path,
            encoding = NSUTF8StringEncoding,
            error = null,
        ) ?: error("Could not read $path")
        return json.decodeFromString(
            MapSerializer(String.serializer(), GuideDto.serializer()),
            text,
        )
    }
}

@Serializable
private data class GuideDto(
    val source: String = "",
    val kana: Boolean = false,
    val strokes: List<GuideStrokeDto> = emptyList(),
)

@Serializable
private data class GuideStrokeDto(
    val points: List<List<Float>> = emptyList(),
    val path: String? = null,
)
