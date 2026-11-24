"""The authored spell catalog, shared by every supported script.

Each family lists characters that carry that kind of magic. The builder maps
them to Traditional, Japanese kanji, and Korean Hanja, then fills any shortfall
from the same course. Add characters here, rerun the builder, and the book,
quest, and audits pick them up.
"""

CATEGORY_FAMILIES = {
    "Tides": ("water", "ice", "snow", "rain"),
    "Flame": ("fire", "lightning"),
    "Sky": ("wind", "cloud", "sun", "moon", "light", "shadow"),
    "Ground": ("earth", "stone", "mountain", "wood", "metal"),
    "Creatures": ("animal", "person", "hand"),
    "Craft": ("boat", "tool"),
    "Mind": ("mind", "dream", "sound"),
    "Time": ("time",),
    "Arcana": ("rune", "mirror", "key", "potion", "illusion", "oracle"),
    "Spirit": ("spirit", "soul", "ward"),
}

FAMILY_EFFECT = {
    "water": "RIPPLE", "ice": "FROST", "snow": "FROST", "rain": "RIPPLE",
    "fire": "BURST", "lightning": "BURST",
    "wind": "GUST", "cloud": "GUST", "sun": "GLOW", "moon": "DRIFT",
    "light": "GLOW", "shadow": "VEIL",
    "earth": "RISE", "stone": "RISE", "mountain": "RISE", "wood": "GROW", "metal": "RISE",
    "animal": "GROW", "person": "GROW", "hand": "PULSE",
    "boat": "RIPPLE", "tool": "RISE",
    "mind": "PULSE", "dream": "DRIFT", "sound": "PULSE",
    "time": "CLOCK",
    "rune": "GLOW", "mirror": "VEIL", "key": "PULSE", "potion": "RIPPLE",
    "illusion": "VEIL", "oracle": "DRIFT",
    "spirit": "DRIFT", "soul": "DRIFT", "ward": "PULSE",
}

# The authored core. Order matters: the first family that claims a character
# keeps it, so the most specific idea is listed before the general one.
FAMILIES = {
    "water": "水 河 江 海 湖 泉 池 洋 浪 流 潮 滴 汗 泪 洗 喝 饮 汤 汁 游 泳 沉 湿 深 波 溪 泡 沫 灌 满 酒 茶 油 渡 渔 沿 沸 溶 洒 溢",
    "ice": "冰 冻 寒 冷 霜 凉 凝 凛 澈",
    "snow": "雪 雹 霜",
    "rain": "雨 露 淋 伞 阵 雷 雾",
    "fire": "火 炎 焰 烧 燃 热 暖 烤 炒 煮 灯 烛 炉 灰 燥 烫 烈 爆 炮 炸 焦 煤 炭 烘 焙 炊 炖 焚 赤 红 丹 暑 温",
    "lightning": "电 雷 闪 震 霹 雳 霆 弧 烁 辉 耀",
    "wind": "风 吹 刮 飘 飞 气 空 轻 扇 汽 蒸 喷 呼 吸 叹",
    "cloud": "云 雾 烟 阴 霾 霞 霭",
    "sun": "日 阳 晴 晒 照 晨 旦 曙 曦 暑 暖",
    "moon": "月 夜 夕 暮 晚 银 盘",
    "light": "光 明 亮 星 灯 烛 辉 耀 闪 晶 珠 镜",
    "shadow": "影 黑 暗 幽 阴 墨 玄",
    "earth": "土 地 田 场 园 沙 尘 泥 岛 岸 滩 谷 原 野 坪 圃 壤 坎",
    "stone": "石 岩 峰 坡 洞 矿 砖 墙 崖 砾 壁 垒 磐 碑",
    "mountain": "山 岭 岳 峦 巅 丘 陵",
    "wood": "木 林 森 树 草 花 叶 根 竹 米 果 种 苗 芽 枝 藤 松 柳 桃 梅 兰 菊 莲 荷 麦 稻 菜 茶",
    "metal": "金 铁 钢 银 铜 玉 宝 钱 珠 铅 锡 锁 针 剑",
    "animal": "马 牛 羊 狗 猫 鸟 鱼 龙 虎 兔 龟 蛙 蜂 蝶 蛇 虫 鸡 鸭 猪 熊 鹿 狐 鼠 象 狮 狼 猴 鹅 鹤 鹰 燕 蚁 蛛 鲸 鲨 猿 豹 驴 骆 驼",
    "person": "人 男 女 儿 孩 子 母 父 友 朋 师 生 医 王 后 身 体 面 目 眼 耳 口 鼻 足 姓 名 兵 民 官 客 家 亲",
    "hand": "手 指 掌 拿 持 打 推 拉 提 抬 举 抱 抓 握 投 扔 接 摸 拍 敲 切 开 关 挥 招 摆 放",
    "boat": "船 舟 帆 桨 港 岸 渡 航 泊 浮 漂 桥 筏",
    "tool": "车 门 窗 房 屋 室 城 市 街 路 塔 墙 书 纸 笔 刀 剑 针 线 布 衣 帽 鞋 杯 碗 盘 瓶 箱 盒 网 绳 篮 板 桌 椅 伞 钟 琴 鼓 笛 尺 斧 锤 锯 铲 犁 轮",
    "mind": "心 想 思 念 意 知 识 学 问 懂 记 忘 怕 爱 恨 喜 怒 哀 乐 信 疑 真 假 智 慧 理 由 因 果 计 算 选 择 决 定 希 望 愿 感 觉 悟 忆 猜",
    "dream": "梦 睡 醒 眠 幻 醉",
    "sound": "声 音 听 闻 语 言 话 说 讲 读 写 唱 歌 问 答 叫 喊 笑 哭 琴 鼓 笛 钟 铃 响 静 默 吵 闹",
    "time": "时 间 年 月 日 分 秒 刻 钟 早 晚 今 明 昨 春 夏 秋 冬 季 节 岁 老 新 旧 先 后 初 终 始 末 久 暂 永 恒 期 候 等 待 快 慢 长 短 古 过 去 现 在 历 史 纪 代 世 元 朝 夕 暮 夜 晨",
    "rune": "魔 法 术 符 咒 祝 祈 愿 圆 环 星 秘 密 卡 牌 签 印 章 标 记 号 封 阵 坛 卦",
    "mirror": "镜 影 映 照 反 复 双 对 像 形 貌 颜 玻 璃 晶 珠",
    "key": "钥 匙 锁 开 关 门 封 密 码 秘 禁 解 通",
    "potion": "药 瓶 病 康 毒 疗 丹 炉 炼 医 汤 汁 酒 茶 油 沸 溶 泡 沫 香 苦 甜 酸 辣 咸",
    "illusion": "幻 假 变 现 隐 藏 骗 迷 惑 雾 烟 像 貌 鬼 怪",
    "oracle": "卜 占 卦 签 命 运 福 祸 吉 凶 幸 厄 星 象 兆 预 言 诗 珠 算",
    "spirit": "灵 魂 鬼 神 仙 佛 圣 庙 寺 塔 祖 先 拜 祭 供 福 祸 吉 凶 死 亡 运 血 精 气 天 地 堂 墓 坟 骨 息 道 德",
    "soul": "魂 生 命 心 肉 身 体 影 梦 忆 念",
    "ward": "护 卫 守 保 防 盾 障 安 全 平 稳 固 坚 咒 符 印 章 阵 网",
}
