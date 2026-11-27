package com.bassmd.bihua.spellbook

/**
 * A small signature picture that rides on top of a cast. The element decides
 * the burst; the motif makes one character feel different from another, so 花
 * gives petals, 雪 gives flakes, 鱼 gives bubbles, and 石 gives shards.
 *
 * Mapping a character to a motif is a design choice, not etymology.
 */
enum class Motif {
    PETAL, LEAF, SNOW, EMBER, DROP, SPARK, STAR, WING, BUBBLE, PAW, SHARD, WISP, RUNE;

    companion object {
        private val petals = setOf("花", "草", "果", "米", "红", "黄")
        private val leaves = setOf("木", "林", "森", "竹", "叶", "根", "树", "青")
        private val snows = setOf("雪", "冰", "冷", "冻")
        private val embers = setOf("火", "焰", "烧", "热", "暖")
        private val drops = setOf("水", "川", "海", "湖", "泉", "井", "露", "流", "浪", "云")
        private val sparks = setOf("电", "雷", "光", "亮", "星")
        private val stars = setOf("日", "月", "天", "明", "影")
        private val wings = setOf("鸟", "蝶", "蜂", "飞", "龙")
        private val bubbles = setOf("鱼", "船", "浮", "游", "沉")
        private val paws = setOf("马", "犬", "猫", "虎", "兔", "牛", "羊", "狐", "熊", "龟", "蛙", "蛇", "虫")
        private val shards = setOf("石", "岩", "山", "土", "洞", "塔", "城", "门", "桥", "路", "压", "切", "关")
        private val wisps = setOf("风", "雾", "烟", "沙", "尘", "空")

        fun of(character: String): Motif = when (character) {
            in petals -> PETAL
            in leaves -> LEAF
            in snows -> SNOW
            in embers -> EMBER
            in drops -> DROP
            in sparks -> SPARK
            in stars -> STAR
            in wings -> WING
            in bubbles -> BUBBLE
            in paws -> PAW
            in shards -> SHARD
            in wisps -> WISP
            else -> RUNE
        }
    }
}
