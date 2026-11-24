"""Build app/src/main/assets/spellbook.csv for the 100-character starter set.

Family comes from the radical in char_meta; signature characters override it.
Run from the repo root: python3 tools/spells/build_spellbook.py
"""
import csv
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DB = ROOT / "app/src/main/assets/bihua_content.db"
OUT = ROOT / "app/src/main/assets/spellbook.csv"
CHARS_FILE = Path(__file__).with_name("starter_chars.txt")

# Radical → family. Only the families we will draw; others fall through to an override.
RADICAL_FAMILY = {
    "氵": "water", "冫": "ice", "火": "fire", "灬": "fire", "石": "stone", "岩": "stone",
    "土": "earth", "木": "wood", "艹": "wood", "禾": "wood", "雨": "rain", "雪": "snow",
    "风": "wind", "日": "sun", "月": "moon", "山": "mountain", "人": "person", "亻": "person",
    "扌": "hand", "犭": "animal", "马": "animal", "鸟": "animal", "鱼": "animal", "虫": "animal",
    "舟": "boat", "金": "metal", "钅": "metal", "电": "lightning", "云": "cloud",
}

# Signature characters: an explicit family and effect id, with the reason kept in the note.
OVERRIDES = {
    "马": ("animal", "fx_horse", "A horse. The animal family with a horse silhouette."),
    "桥": ("stone", "fx_bridge", "A bridge: stone span that crosses water."),
    "船": ("boat", "fx_boat", "A boat that floats on water."),
    "门": ("stone", "fx_door", "A door that opens or blocks a path."),
    "刀": ("metal", "fx_sword", "A blade that cuts."),
    "光": ("sun", "fx_light", "Light that shines through the dark."),
    "影": ("moon", "fx_shadow", "A shadow that hides what stands in the light."),
    "星": ("sun", "fx_star", "A star that lights the night."),
    "树": ("wood", "fx_tree", "A tree whose roots hold the ground."),
    "花": ("wood", "fx_flower", "A flower that opens in warm light."),
    "鸟": ("animal", "fx_bird", "A bird that flies across gaps."),
    "鱼": ("animal", "fx_fish", "A fish that swims in water."),
    "龙": ("animal", "fx_dragon", "A dragon that moves the weather."),
    "塔": ("stone", "fx_tower", "A tower that stands tall."),
    "城": ("stone", "fx_castle", "A walled city that stays put."),
    "洞": ("stone", "fx_cave", "A cave that goes deep."),
    "雾": ("cloud", "fx_fog", "Fog that hides and then lifts."),
    "雷": ("lightning", "fx_thunder", "Thunder that cracks through the air."),
    "电": ("lightning", "fx_bolt", "A bolt of lightning."),
    "烟": ("cloud", "fx_smoke", "Smoke that rises and drifts."),
    "沙": ("earth", "fx_sand", "Sand that spills and piles up."),
    "尘": ("earth", "fx_dust", "Dust that drifts on the wind."),
    "井": ("water", "fx_well", "A well that holds water below."),
    "湖": ("water", "fx_lake", "A lake that fills a basin."),
    "海": ("water", "fx_sea", "A sea with tall waves."),
    "川": ("water", "fx_river", "A river that flows down the valley."),
    "泉": ("water", "fx_spring", "A spring that bubbles up from the ground."),
    "雪": ("snow", "fx_snow", "Snow that falls soft and deep."),
    "水": ("water", "fx_water", "Water itself, the source of the tides."),
    "犬": ("animal", "fx_dog", "A dog that runs ahead."),
    "牛": ("animal", "fx_ox", "An ox that pulls the plough."),
    "羊": ("animal", "fx_sheep", "A sheep that grazes the hills."),
    "虎": ("animal", "fx_tiger", "A tiger that prowls the ridge."),
    "兔": ("animal", "fx_rabbit", "A rabbit that hops away."),
    "龟": ("animal", "fx_turtle", "A turtle that carries its home."),
    "云": ("cloud", "fx_cloud", "A cloud that gathers and drifts."),
    "天": ("wind", "fx_sky", "The open sky above."),
    "竹": ("wood", "fx_bamboo", "Bamboo that bends and springs back."),
    "叶": ("wood", "fx_leaf", "A leaf that turns in the breeze."),
    "米": ("wood", "fx_grain", "Grain that grows in the field."),
    "路": ("stone", "fx_path", "A path laid across the land."),
    "飞": ("wind", "fx_fly", "Flight lifting up over the ground."),
    "走": ("earth", "fx_walk", "Steady steps on the ground."),
    "跑": ("earth", "fx_run", "A quick run that kicks up dust."),
    "跳": ("earth", "fx_jump", "A jump that clears a gap."),
    "爬": ("stone", "fx_climb", "A climb up the rock face."),
    "举": ("hand", "fx_lift", "Lifting something up."),
    "压": ("stone", "fx_press", "A weight that presses down."),
    "切": ("metal", "fx_cut", "A clean cut through."),
    "开": ("hand", "fx_open", "A door or path that opens."),
    "关": ("stone", "fx_gate", "A gate that closes the way."),
    "亮": ("sun", "fx_bright", "Something that starts to glow."),
    "黑": ("moon", "fx_dark", "Darkness that settles in."),
    "白": ("sun", "fx_white", "Pale light."),
    "红": ("fire", "fx_red", "A warm red glow."),
    "青": ("wood", "fx_green", "Fresh green growth."),
    "黄": ("earth", "fx_yellow", "Golden dust on the wind."),
}

# Grimoire chapters. Each family belongs to exactly one spell type, so the book
# groups spells the way learners will think about them.
CATEGORY = {
    "water": "Tides", "ice": "Tides", "snow": "Tides", "rain": "Tides",
    "fire": "Flame", "lightning": "Flame",
    "wind": "Sky", "cloud": "Sky", "sun": "Sky", "moon": "Sky",
    "earth": "Ground", "stone": "Ground", "mountain": "Ground", "wood": "Ground",
    "animal": "Creatures", "person": "Creatures", "hand": "Creatures",
    "boat": "Constructs", "metal": "Constructs",
}
CATEGORY_NAMES = ["Tides", "Flame", "Sky", "Ground", "Creatures", "Constructs"]

# Learners see the family name and the effect, never the character before casting.
FAMILY_PLAIN = {
    "water": "Water", "fire": "Fire", "stone": "Stone", "earth": "Earth", "wood": "Wood",
    "wind": "Wind", "rain": "Rain", "snow": "Snow", "ice": "Ice", "sun": "Sun", "moon": "Moon",
    "animal": "Creature", "mountain": "Mountain", "person": "Spark", "hand": "Gesture",
    "boat": "Vessel", "metal": "Metal", "lightning": "Lightning", "cloud": "Cloud",
}


def main() -> None:
    chars = list(dict.fromkeys(CHARS_FILE.read_text(encoding="utf-8").split()))
    db = sqlite3.connect(DB)
    radicals = dict(db.execute("SELECT character, radical FROM char_meta"))
    rows = []
    for char in chars:
        if char in OVERRIDES:
            family, effect, note = OVERRIDES[char]
            source = "override"
        else:
            radical = radicals.get(char, "") or ""
            family = RADICAL_FAMILY.get(radical)
            if family is None:
                continue
            if family not in CATEGORY:
                raise SystemExit(f"family {family} has no grimoire category")
            effect = f"fx_{family}"
            note = f"Radical {radical}."
            source = "radical"
        rows.append({
            "character": char,
            "family": family,
            "category": CATEGORY[family],
            "effect": effect,
            "source": source,
            "note": note,
        })
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with OUT.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["character", "family", "category", "effect", "source", "note"])
        writer.writeheader()
        writer.writerows(rows)
    print(f"wrote {len(rows)} spells to {OUT.relative_to(ROOT)}")
    for name in CATEGORY_NAMES:
        print(f"  {name}: {sum(r['category'] == name for r in rows)}")
    unmapped = [c for c in chars if c not in {r['character'] for r in rows}]
    print(f"unmapped ({len(unmapped)}): {' '.join(unmapped)}")


if __name__ == "__main__":
    main()
