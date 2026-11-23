package com.bassmd.bihua.content

/**
 * A small, deliberately human list that gives the daily word a warm start.
 * The catalogue is much larger, so after this list the repository falls back
 * to common, short, non-mature words in a deterministic daily order.
 *
 * Keep these words concrete, kind, and useful. Do not add slogans, rare
 * literary forms, names, places, or anything explicit.
 */
internal object FeaturedWords {
    private val curated: Map<StudyMode, List<String>> = mapOf(
        StudyMode.SIMPLIFIED to listOf(
            "春天", "微笑", "旅行", "朋友", "希望", "温暖", "月光", "梦想",
            "勇敢", "幸福", "风景", "阳光", "故事", "礼物", "拥抱", "彩虹",
            "森林", "风筝", "晚安", "加油", "温柔", "好奇", "惊喜", "未来",
            "自由", "勇气", "童年", "清晨", "喜欢", "思念", "陪伴", "远方",
        ),
        StudyMode.TRADITIONAL to listOf(
            "溫暖", "夢想", "風景", "陽光", "禮物", "擁抱", "風箏", "溫柔",
            "驚喜", "未來", "勇氣", "喜歡", "遠方",
        ),
        StudyMode.JAPANESE to listOf(
            "笑顔", "旅行", "友達", "希望", "夢中", "勇気", "幸せ", "風景",
            "太陽", "物語", "贈り物", "森林", "風鈴", "発見", "驚き", "未来",
            "自由", "花火", "手紙", "歌詞",
        ),
        StudyMode.KOREAN to listOf(
            "微笑", "旅行", "友情", "幸福", "希望", "風景", "自由", "靑春",
            "追憶", "親舊", "勇氣", "未來", "音樂", "家族", "自然", "感謝",
            "平和", "時間", "故鄕", "散策", "讀書", "健康", "夢想",
        ),
    )

    fun forMode(mode: StudyMode): List<String> = curated[mode].orEmpty()
}
