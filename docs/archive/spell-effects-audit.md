# Spell quest effects audit

Status: 2026-11-25, after the 1.1.1 reader and result-timing pass.

The quest should feel like a small piece of interactive fiction, not a form. The
writing is the action; the effects are the world answering. Motion is layered so
the screen never competes with the learner’s ink.

## Screens and motion

| Screen | Motion | Notes |
|---|---|---|
| Gate | Mò idle, drifting chapter haze | `SceneBackdrop` with `EffectStyle.DRIFT`; the gate is calm before the road. |
| Scene / visual novel | Mottled sky, three hill layers, drifting motes, typewriter problem | The title, goal, label, and Draw/Grimoire buttons stay fixed; only the situation text scrolls. Long problems finish typing at once. |
| Drawing circle | Rune ring, star band, paper centre, guide playback, learner ink | The ring rotates slowly. The paper stays plain and still. |
| Cast | Circle replay, chapter-colour burst, motif weather | The result and actions appear when the stroke replay ends; the last effect starts at that same moment. |
| Gift | Guide draws stroke by stroke, flourish, motif weather | Actions unlock on the last stroke, not after the flourish. |
| Result | Chapter glow, motif burst/residue, note card, Walk on | No extra wait after the last stroke. A success haptic and the cast sound land together. |
| Grimoire | Page spread, tabs, met stars, hinge | Deliberately quiet; the book is the collection, not another effect screen. |
| Chapter banner | Short banner beat | Skipped with reduce motion. |

## Chapter signatures

| Chapter | Colour | Families | Effect style(s) |
|---|---|---|---|
| Tides | Sky | Water, Rain, Ice, Snow | Ripple, Frost |
| Flame | Peach | Fire, Lightning | Burst |
| Sky | Lilac | Wind, Cloud, Sun, Moon, Light, Shadow | Gust, Glow, Drift, Veil |
| Ground | Mint | Earth, Stone, Mountain, Wood, Metal | Rise, Grow |
| Creatures | Butter | Creature, Spark, Gesture | Grow, Pulse |
| Craft | Rose | Vessel, Tool | Ripple, Rise |
| Mind | Gem | Mind, Dream, Sound | Pulse, Drift |
| Time | Coral | Time | Clock |
| Arcana | Fuchsia | Rune, Mirror, Key, Brew, Illusion, Omen | Glow, Veil, Pulse, Ripple, Drift |
| Spirit | Slate | Spirit, Soul, Ward | Drift, Pulse |

The backdrop now uses the chapter colour for its haze and motes, and changes the
ambient speed by effect family: fire and lightning move fastest, time and spirit
slowest. This keeps `Flame` and `Tides` distinct even before a spell is cast.

## Per-spell variation

The effect style is only the skeleton. Every family gets a stable seed from its
family id, which changes:

- ripple ring count, spacing, and stroke width
- burst count, angle offset, and distance
- frost ray count and rotation
- gust line count and direction
- glow ring count and bloom
- grow spiral direction and turns
- rise particle count and sway
- drift direction and mote count
- pulse ring count and phase
- clock hand direction

The character also selects a `Motif` (petal, leaf, snow, ember, drop, spark,
star, wing, bubble, paw, shard, wisp, rune), so two families that share a style
still end in different weather. The same spell always gives the same weather;
nothing is random per frame.

## Stroke timing

`guideStrokeMs` gives the page a total writing beat, then clamps each stroke:

| Strokes | Per stroke | Gift total | Cast replay |
|---|---|---|---|
| 1 | 500 ms | 650 ms | 1.2 s |
| 3 | 500 ms | 1.65 s | 2.3 s |
| 5 | 480 ms | 2.55 s | 3.1 s |
| 10 | 240 ms | 2.55 s | 3.0 s |
| 15 | 160 ms | 2.55 s | 3.2 s |
| 20 | 120 ms | 2.55 s | 3.6 s |

The result is resolved when the replay ends. `resultDelayMs` only controls how
long the glow remains after the result is already available; it never blocks the
Walk on button.

## Reduced motion

- `reduceMotion` or system animator scale `0` removes ambient drift, rune
  rotation, bursts, and the flourish.
- The page is already drawn; guide playback is one frame per stroke.
- The result appears immediately at the last stroke rather than after a replay
  wait.
- Static hills, haze, stars, and the chapter glow remain so the screen still has
  atmosphere.
- Settings says the switch also follows the system remove-animations setting.

## Copy clarity

- The scene goal (`Wake the field`, `Clear the shrine`) is fixed above the
  scrolling situation.
- The line `Write one character that fits the scene.` appears only while the
  learner is choosing a spell.
- A category door says which chapter answers it, not just “a whole chapter”.
- An exact-shape door says only one character will fit.
- Obstacle needs name the kind of help that works without naming the answer.
  Contradictory needs were removed (`shadow-door` no longer asks for both light
  and dark when either is accepted).

## Coverage

- `spell-quest`: gate, scene, gift, real draw/cast, result, note, wrong answer.
- `spell-long`: a ten-line injected problem; only the body scrolls, Draw and
  Grimoire stay fixed.
- `spell-quest-calm`: reduce motion.
- `spell-quest-dark`, `spell-quest-ja`, `spell-quest-ko`,
  `spell-quest-traditional`.
- `screen-pass`: gate, scene, cast controls, grimoire.
- `SpellQuestLogicTest`: stroke timing and copy helpers.
- `PuzzleForgeTest`: category and exact-shape copy.
