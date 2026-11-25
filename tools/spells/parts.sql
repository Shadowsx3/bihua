-- Procedural puzzle parts. The app never shows these rows directly: the forge
-- combines them with a seeded random source, so the same seed always gives the same
-- puzzle, and new seeds give new ones. Authoring is SQL only.
--
-- A family solves an obstacle when it carries one of the obstacle's accepted tags.
-- That keeps every solution honest: a family that fits, works.

CREATE TABLE family(id TEXT PRIMARY KEY, name TEXT NOT NULL);
CREATE TABLE family_tag(family TEXT NOT NULL, tag TEXT NOT NULL, PRIMARY KEY(family, tag));
CREATE TABLE obstacle(id TEXT PRIMARY KEY, prompt TEXT NOT NULL, lines TEXT NOT NULL, need TEXT NOT NULL);
CREATE TABLE obstacle_accepts(obstacle_id TEXT NOT NULL, tag TEXT NOT NULL, PRIMARY KEY(obstacle_id, tag));
CREATE TABLE tag_outcome(tag TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(tag, text));
CREATE TABLE setting(place TEXT PRIMARY KEY);
CREATE TABLE time_of_day(name TEXT PRIMARY KEY, suffix TEXT NOT NULL, line TEXT NOT NULL);
CREATE TABLE weather(name TEXT PRIMARY KEY, suffix TEXT NOT NULL, line TEXT NOT NULL);

INSERT INTO family(id, name) VALUES
('water','Water'),
('ice','Ice'),
('snow','Snow'),
('rain','Rain'),
('fire','Fire'),
('lightning','Lightning'),
('wind','Wind'),
('cloud','Cloud'),
('sun','Sun'),
('moon','Moon'),
('light','Light'),
('shadow','Shadow'),
('earth','Earth'),
('stone','Stone'),
('mountain','Mountain'),
('wood','Wood'),
('metal','Metal'),
('animal','Creature'),
('person','Spark'),
('hand','Gesture'),
('boat','Vessel'),
('tool','Tool'),
('mind','Mind'),
('dream','Dream'),
('sound','Sound'),
('time','Time'),
('rune','Rune'),
('mirror','Mirror'),
('key','Key'),
('potion','Brew'),
('illusion','Illusion'),
('oracle','Omen'),
('spirit','Spirit'),
('soul','Soul'),
('ward','Ward');

INSERT INTO family_tag(family, tag) VALUES
('water','FLOW'),
('water','WET'),
('water','PUSH'),
('water','HEAL'),
('ice','COLD'),
('ice','SOLID'),
('snow','COLD'),
('snow','SOFT'),
('rain','WET'),
('rain','FLOW'),
('rain','GROW'),
('fire','HEAT'),
('fire','LIGHT'),
('fire','DRY'),
('lightning','LIGHT'),
('lightning','PUSH'),
('lightning','TURN'),
('wind','AIR'),
('wind','PUSH'),
('wind','DRY'),
('cloud','AIR'),
('cloud','WET'),
('cloud','SOFT'),
('sun','LIGHT'),
('sun','HEAT'),
('sun','DRY'),
('sun','TURN'),
('moon','LIGHT'),
('moon','DARK'),
('moon','TURN'),
('light','LIGHT'),
('light','TURN'),
('shadow','DARK'),
('shadow','SOFT'),
('earth','SOLID'),
('earth','WEIGHT'),
('earth','DRY'),
('earth','GROW'),
('stone','SOLID'),
('stone','WEIGHT'),
('stone','BIND'),
('mountain','SOLID'),
('mountain','WEIGHT'),
('mountain','BIND'),
('wood','SOLID'),
('wood','FLOAT'),
('wood','GROW'),
('wood','HEAL'),
('metal','SOLID'),
('metal','RING'),
('metal','BIND'),
('metal','WEIGHT'),
('animal','PUSH'),
('animal','HEAT'),
('animal','GROW'),
('person','LIGHT'),
('person','PUSH'),
('person','HEAL'),
('person','GROW'),
('hand','PUSH'),
('hand','SOLID'),
('hand','BIND'),
('boat','FLOAT'),
('boat','FLOW'),
('tool','SOLID'),
('tool','WEIGHT'),
('tool','PUSH'),
('tool','BIND'),
('mind','AIR'),
('mind','LIGHT'),
('mind','TURN'),
('dream','DARK'),
('dream','AIR'),
('dream','SOFT'),
('dream','TURN'),
('sound','AIR'),
('sound','RING'),
('sound','TURN'),
('time','TURN'),
('time','FLOW'),
('time','BIND'),
('rune','LIGHT'),
('rune','RING'),
('rune','BIND'),
('rune','TURN'),
('mirror','LIGHT'),
('mirror','DARK'),
('mirror','TURN'),
('key','RING'),
('key','PUSH'),
('key','BIND'),
('potion','WET'),
('potion','HEAL'),
('potion','GROW'),
('potion','HEAT'),
('illusion','DARK'),
('illusion','AIR'),
('illusion','SOFT'),
('illusion','TURN'),
('oracle','LIGHT'),
('oracle','AIR'),
('oracle','RING'),
('oracle','TURN'),
('spirit','DARK'),
('spirit','LIGHT'),
('spirit','AIR'),
('spirit','HEAL'),
('soul','DARK'),
('soul','LIGHT'),
('soul','HEAL'),
('soul','TURN'),
('ward','SOLID'),
('ward','LIGHT'),
('ward','RING'),
('ward','BIND'),
('ward','HEAL');

-- What a tag does, once it works. {family} is replaced with the spell name.

INSERT INTO tag_outcome(tag, text) VALUES ('FLOW','The {family} runs with the current, and the obstacle gives way around you.');

INSERT INTO tag_outcome(tag, text) VALUES ('FLOW','The {family} finds the channel, and the stuck thing slides free.');

INSERT INTO tag_outcome(tag, text) VALUES ('WET','The {family} soaks in, and the stubborn thing loosens and lets you by.');

INSERT INTO tag_outcome(tag, text) VALUES ('WET','The {family} softens the dry grip of the obstacle, and it opens.');

INSERT INTO tag_outcome(tag, text) VALUES ('PUSH','The {family} shoves hard, and the way clears with a groan.');

INSERT INTO tag_outcome(tag, text) VALUES ('PUSH','The {family} leans in, and the obstacle gives up the path.');

INSERT INTO tag_outcome(tag, text) VALUES ('COLD','The {family} sets everything firm, and the ground holds your weight.');

INSERT INTO tag_outcome(tag, text) VALUES ('COLD','The {family} makes the soft thing steady, and you cross.');

INSERT INTO tag_outcome(tag, text) VALUES ('SOLID','The {family} makes a firm path where there was none.');

INSERT INTO tag_outcome(tag, text) VALUES ('SOLID','The {family} settles into place, and the way holds.');

INSERT INTO tag_outcome(tag, text) VALUES ('SOFT','The {family} settles over the ground like a blanket, and the way goes soft and quiet.');

INSERT INTO tag_outcome(tag, text) VALUES ('SOFT','The {family} takes the sharpness out of the world, and you pass.');

INSERT INTO tag_outcome(tag, text) VALUES ('HEAT','The {family} warms the stuck thing, and it lets go with a sigh.');

INSERT INTO tag_outcome(tag, text) VALUES ('HEAT','The {family} drives the cold back, and the way opens.');

INSERT INTO tag_outcome(tag, text) VALUES ('LIGHT','The {family} lights the way ahead, and every step is clear.');

INSERT INTO tag_outcome(tag, text) VALUES ('LIGHT','The {family} makes a small honest light, and the dark step back.');

INSERT INTO tag_outcome(tag, text) VALUES ('AIR','The {family} lifts the air around you, and the way opens up.');

INSERT INTO tag_outcome(tag, text) VALUES ('AIR','The {family} moves the heavy air aside, and you breathe again.');

INSERT INTO tag_outcome(tag, text) VALUES ('DRY','The {family} draws off the damp, and the path dries out at once.');

INSERT INTO tag_outcome(tag, text) VALUES ('DRY','The {family} takes the wetness with it, and the ground is firm.');

INSERT INTO tag_outcome(tag, text) VALUES ('DARK','The {family} softens the dark into something you can walk through.');

INSERT INTO tag_outcome(tag, text) VALUES ('DARK','The {family} wraps the bright thing in quiet, and you slip past.');

INSERT INTO tag_outcome(tag, text) VALUES ('WEIGHT','The {family} presses everything down into place, and the way stays put.');

INSERT INTO tag_outcome(tag, text) VALUES ('WEIGHT','The {family} settles the loose thing with its own weight.');

INSERT INTO tag_outcome(tag, text) VALUES ('FLOAT','The {family} carries you clear, and the far side comes closer.');

INSERT INTO tag_outcome(tag, text) VALUES ('FLOAT','The {family} rides the surface, and the gap is no longer a wall.');

INSERT INTO tag_outcome(tag, text) VALUES ('GROW','The {family} sends up new growth, and the wall of green parts for you.');

INSERT INTO tag_outcome(tag, text) VALUES ('GROW','The {family} finds the living thread and pulls the way open.');

INSERT INTO tag_outcome(tag, text) VALUES ('RING','The {family} rings out true, and the lock gives with a click.');

INSERT INTO tag_outcome(tag, text) VALUES ('RING','The {family} answers the bell inside the thing, and it opens.');

INSERT INTO tag_outcome(tag, text) VALUES ('HEAL','The {family} settles into the hurt thing, and it mends.');

INSERT INTO tag_outcome(tag, text) VALUES ('HEAL','The {family} closes the break, and the way is whole again.');

INSERT INTO tag_outcome(tag, text) VALUES ('BIND','The {family} holds the way together, and it bears your weight.');

INSERT INTO tag_outcome(tag, text) VALUES ('BIND','The {family} ties the loose ends, and the path stays.');

INSERT INTO tag_outcome(tag, text) VALUES ('TURN','The {family} turns the moment, and the way opens.');

INSERT INTO tag_outcome(tag, text) VALUES ('TURN','The {family} gives the world a nudge, and the stuck moment moves.');

-- Each obstacle asks for something. The need text is also the nudge when a
-- cast cannot help. It names the need, never the answer.

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('river','Get across the river','The river runs wide and unhurried.|The far bank looks like another country.','Something that can float, or stand firm enough to walk on, will do.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river','SOLID');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('gate','Open the gate','A gate with no hinge and no key guards the orchard.|The orchard smells like it knows something.','Something that can push, turn, or hold the gate open will do.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('gate','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('gate','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('gate','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('dark-path','Find the way down','The path ahead is black as ink.|Something rustles and then decides to be quiet.','The path needs light, or moving air, not more dark.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('dark-path','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('dark-path','AIR');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('frozen-lake','Cross the frozen lake','The lake is frozen, but the ice is soft near the middle.|A fishing hut waits on the far shore.','The ice needs warmth, or a firm path across, before anyone can cross.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('frozen-lake','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('frozen-lake','SOLID');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('dry-field','Wake the field','The field has gone dry and cracked.|The grass is brittle enough to snap.','The field needs water, damp air, or new growth, not more heat.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('dry-field','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('dry-field','FLOW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('dry-field','GROW');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('stuck-cart','Free the cart','A merchant''s cart is wedged deep in the mud.|The merchant is explaining the situation to the ox.','Something has to push, lift, or steady the cart free.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stuck-cart','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stuck-cart','WEIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stuck-cart','SOLID');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('bramble','Clear the bramble wall','Thorny brambles block the path, thick as a fence.|Somewhere behind them, a bell is ringing.','The brambles need heat or light, or a strong push, to loosen their grip.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bramble','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bramble','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bramble','PUSH');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('becalmed','Fill the sails','The boat sits on still water, sails hanging limp.|A gull is waiting, rather rudely, for you to hurry.','The sails need air, a push, or a current to fill.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('becalmed','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('becalmed','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('becalmed','FLOW');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('locked-chest','Open the chest','The chest is locked, and the lock has a bell inside it.|Nobody has the key, which is the usual problem.','The lock needs something that rings true, pushes, or holds it open.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('locked-chest','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('locked-chest','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('locked-chest','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('washed-bridge','Cross the washed-out bridge','The bridge is gone, and the gap is wide.|A few old planks float in the shallows.','The gap needs something solid to stand on, float across, or hold together.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('washed-bridge','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('washed-bridge','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('washed-bridge','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('bee-garden','Wake the garden','Everything in the garden has gone stiff and white.|A bee is asleep on the fence, which is not a good sign.','The garden needs light, warm air, or new growth to wake.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bee-garden','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bee-garden','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bee-garden','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('bee-garden','HEAT');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('sleeping-guard','Pass the sleeping guard','The guard is asleep across the only door.|The keys are on the table, and the floorboards have opinions.','The way needs quiet, softness, or a little dark.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-guard','DARK');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-guard','SOFT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-guard','AIR');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('broken-wheel','Mend the broken wheel','A cartwheel has split at the rim.|The road ahead is all stones and no patience.','The wheel needs something solid, a true ring, or a binding to hold.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-wheel','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-wheel','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-wheel','WEIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-wheel','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('cold-child','Warm the cold child','A child sits by a dead fire, blue at the fingers.|The wood is damp and the wind is unkind.','The child needs warmth, light, or something healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cold-child','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cold-child','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cold-child','HEAL');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cold-child','SOFT');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('fog-lighthouse','Guide the boat home','Fog has eaten the harbor.|A boat is out there, ringing its bell and getting no answer.','The boat needs a light, a bell, or clear air to find the harbor.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fog-lighthouse','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fog-lighthouse','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fog-lighthouse','AIR');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('shadow-door','Open the shadow door','A door appears only where the lamplight and shadow meet.|It has no handle on this side.','The door needs both light and dark, or something that turns the moment.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('shadow-door','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('shadow-door','DARK');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('shadow-door','TURN');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('shadow-door','RING');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('mirror-maze','Find the true path','The maze is all mirrors, and every turn looks like the last one.|Your reflection is starting to look smug.','The maze needs a true light, a ringing note, or a turn of the moment.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('mirror-maze','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('mirror-maze','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('mirror-maze','TURN');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('mirror-maze','AIR');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('silent-bell','Ring the silent bell','The bell has no clapper, but the tower still expects a sound.|The rope is frayed and the wind is listening.','The bell needs a true ring, moving air, or a turn of time.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('silent-bell','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('silent-bell','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('silent-bell','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('sealed-jar','Open the sealed jar','A clay jar is sealed with wax and a stubborn knot.|Something inside is humming.','The jar needs heat, a push, or a binding undone.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sealed-jar','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sealed-jar','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sealed-jar','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sealed-jar','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('overgrown-shrine','Clear the shrine','The shrine is buried under vines and old flowers.|A small stone face watches from the leaves.','The shrine needs growth parted, heat, light, or healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('overgrown-shrine','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('overgrown-shrine','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('overgrown-shrine','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('overgrown-shrine','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('ghost-bridge','Cross the ghost bridge','A bridge of mist spans the gorge, and it is not quite there.|The far side is real enough.','The bridge needs dark, air, or something that floats and binds.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ghost-bridge','DARK');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ghost-bridge','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ghost-bridge','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ghost-bridge','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('hungry-fire','Feed the hungry fire','The campfire has burned down to one red eye.|The night is cold and the pot is still full of water.','The fire needs heat, fuel, or dry air.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hungry-fire','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hungry-fire','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hungry-fire','DRY');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('paper-boat','Make a paper boat','The stream is too wide to wade and too quick to swim.|A sheet of paper waits on the bank.','The boat needs something that floats, holds, or grows into a hull.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-boat','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-boat','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-boat','GROW');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('deep-well','Draw from the deep well','The well is dry at the bucket and full somewhere below.|The rope is short and the stone is patient.','The well needs water, a current, or something that floats down.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-well','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-well','FLOW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-well','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-well','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('thorn-crown','Lift the thorn crown','A crown of thorns hangs just out of reach.|It is caught on a branch and on an old promise.','The crown needs heat, a push, growth, or healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('thorn-crown','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('thorn-crown','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('thorn-crown','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('thorn-crown','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('lost-voice','Find the lost voice','A voice is caught in the reeds, repeating one word.|The word is not a name, which is worse.','The voice needs a ring, moving air, light, or healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('lost-voice','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('lost-voice','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('lost-voice','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('lost-voice','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('stone-heart','Wake the stone heart','A stone heart sits in the hill, cold and slow.|The hill is waiting for a reason to beat.','The heart needs warmth, light, growth, or healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stone-heart','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stone-heart','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stone-heart','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('stone-heart','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('moon-path','Walk the moon path','A path of moonlight lies on the water.|It folds whenever you look straight at it.','The path needs light, air, floating, or a turn of the moment.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('moon-path','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('moon-path','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('moon-path','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('moon-path','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('time-loop','Break the time loop','The same minute has happened four times.|The clock is polite about it, which is annoying.','The loop needs a turn, a true ring, or a binding to break.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('time-loop','TURN');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('time-loop','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('time-loop','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('broken-clock','Mend the broken clock','The clock has stopped at a minute that never happened.|The bell above it is holding its breath.','The clock needs a ring, something solid, weight, or a turn.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-clock','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-clock','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-clock','WEIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('broken-clock','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('ink-flood','Stop the ink flood','Ink is spreading across the floor like a tide.|Every page it touches forgets what it said.','The ink needs water, dryness, or something solid to hold it.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ink-flood','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ink-flood','DRY');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('ink-flood','SOLID');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('paper-wall','Pass the paper wall','A wall of paper blocks the corridor.|It is thin, but it has been told it is a wall.','The paper needs heat, a push, or water to give way.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-wall','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-wall','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('paper-wall','WET');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('sleeping-forest','Wake the sleeping forest','Every tree is asleep, and the path has gone with them.|A woodcutter is snoring in a hollow.','The forest needs light, growth, warmth, or softness.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-forest','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-forest','GROW');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-forest','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-forest','SOFT');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('iron-gate','Open the iron gate','The iron gate is rusted shut.|The hinges remember a different century.','The gate needs heat, a push, solid strength, or a binding.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('iron-gate','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('iron-gate','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('iron-gate','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('iron-gate','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('wishing-well','Make the wish','The wishing well is full of coins and not one wish.|The water is very still.','The well needs light, water, a ring, or a turn of fortune.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('wishing-well','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('wishing-well','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('wishing-well','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('wishing-well','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('star-chart','Read the star chart','The chart is missing the one star that matters.|The sky outside is no help at all.','The chart needs light, air, a ring, or a turn of the moment.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('star-chart','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('star-chart','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('star-chart','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('star-chart','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('buried-key','Find the buried key','The key is under the old apple tree, or under the story of it.|The ground is hard and the light is going.','The key needs solid ground moved, light, weight, or a ring.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('buried-key','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('buried-key','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('buried-key','WEIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('buried-key','RING');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('cracked-mirror','Mend the cracked mirror','The mirror is split from corner to corner.|Every reflection arrives a little late.','The mirror needs light, solid care, a ring, or healing.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cracked-mirror','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cracked-mirror','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cracked-mirror','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('cracked-mirror','HEAL');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('runaway-broom','Stop the runaway broom','The broom is sweeping everything out of the house.|It has already taken the doormat.','The broom needs a push, moving air, weight, or a binding.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('runaway-broom','PUSH');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('runaway-broom','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('runaway-broom','WEIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('runaway-broom','BIND');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('empty-lantern','Light the empty lantern','The lantern is clean and cold and waiting.|The wick is there, but the flame is not.','The lantern needs light, heat, or moving air.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('empty-lantern','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('empty-lantern','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('empty-lantern','AIR');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('deep-snow','Cross the deep snow','The snow is up to the windows and still falling.|The road is somewhere under all that quiet.','The snow needs warmth, floating, solid footing, or softness.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-snow','HEAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-snow','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-snow','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('deep-snow','SOFT');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('river-of-ink','Cross the river of ink','The river runs black and smooth and very deep.|The far bank is written in a language you almost know.','The ink river needs floating, solid footing, dryness, or water.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river-of-ink','FLOAT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river-of-ink','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river-of-ink','DRY');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('river-of-ink','WET');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('sleeping-dragon','Pass the sleeping dragon','The dragon is asleep across the pass.|Its breath is warm and its tail is everywhere.','The dragon needs dark, softness, air, or a gentle light.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-dragon','DARK');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-dragon','SOFT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-dragon','AIR');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('sleeping-dragon','LIGHT');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('hollow-moon','Fill the hollow moon','The moon has a hole in it, and the night is leaking out.|The stars are trying not to stare.','The moon needs light, water, a ring, or a turn.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hollow-moon','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hollow-moon','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hollow-moon','RING');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('hollow-moon','TURN');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('salt-flats','Cross the salt flats','The salt flats shine like water and are not water.|The horizon is doing something unkind with distance.','The flats need water, dry air, or a firm line to follow.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('salt-flats','WET');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('salt-flats','DRY');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('salt-flats','SOLID');

INSERT INTO obstacle(id, prompt, lines, need) VALUES ('glass-storm','Weather the glass storm','The storm is raining small pieces of glass.|The shelter has a window and no roof.','A solid roof, light, or moving air will do.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('glass-storm','LIGHT');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('glass-storm','SOLID');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('glass-storm','AIR');

INSERT INTO setting(place) VALUES
('The old gate'),
('The mill on the stream'),
('The lantern hill'),
('The frozen garden'),
('The orchard wall'),
('The market square'),
('The dry field'),
('The white road'),
('The reed marsh'),
('The bell tower steps'),
('The pine forest edge'),
('The harbor wall'),
('The salt flat'),
('The sunken chapel'),
('The moss bridge'),
('The star observatory'),
('The abandoned library'),
('The herb garden'),
('The cliff stair'),
('The lake island'),
('The fire tower'),
('The moonlit quarry'),
('The city of tents'),
('The old railway'),
('The windmill ridge'),
('The ice cave'),
('The bamboo grove'),
('The marble hall'),
('The tide pools'),
('The copper market'),
('The ash field'),
('The glasshouse'),
('The clock tower'),
('The shipwreck beach'),
('The mountain pass'),
('The underground river'),
('The orchard in blossom'),
('The ruined amphitheatre'),
('The lighthouse'),
('The fog bank');

INSERT INTO time_of_day(name, suffix, line) VALUES
('dawn','at dawn','Dawn is still deciding what colour to be.'),
('early','in the early morning','The morning has not made up its mind yet.'),
('noon','at noon','The sun is directly overhead and taking it personally.'),
('afternoon','in the afternoon','The afternoon stretches out like a cat.'),
('dusk','at dusk','The light goes soft and orange at the edges.'),
('evening','in the evening','The evening arrives with a lantern and a list.'),
('night','at night','Everything is quieter than it has any right to be.'),
('midnight','at midnight','Midnight makes every sound feel like a secret.'),
('fog','in the fog','Fog has swallowed the distance and is working on the rest.'),
('rain','in the rain','Rain has been at this all morning and shows no sign of stopping.'),
('wind','in the wind','The wind keeps changing its mind about which way to blow.'),
('moon','under a cold moon','The moon is up, thin and sharp, and the air has teeth.'),
('sunset','at sunset','The sky is doing its best impression of a furnace.'),
('sunrise','at sunrise','The first light arrives like an apology.'),
('blue','in the blue hour','The blue hour makes everything look half-remembered.'),
('first-light','in the first light','The first light finds every cobweb.'),
('last-light','in the last light','The last light is holding on with both hands.'),
('storm','in the storm','The storm is loud enough to interrupt a thought.'),
('snowfall','in the snowfall','The snow is deep and quiet, and it muffles every step.'),
('spring','in spring','Spring is making promises it may or may not keep.'),
('summer','in summer','Summer sits on the roof and refuses to move.'),
('autumn','in autumn','Autumn has left a dozen colours on the path.'),
('winter','in winter','Winter has locked the gate and swallowed the key.'),
('festival','on a festival night','Somewhere a drum is keeping time for everyone.'),
('quiet','in the quiet hour','The quiet hour belongs to the things that wait.'),
('bell','before the bell','The bell is not due yet, and the air knows it.');

INSERT INTO weather(name, suffix, line) VALUES
('clear','','The sky is clear, which feels like a dare.'),
('heavy','under heavy cloud','The clouds sit low and heavy, like they are listening.'),
('humid','in the heat','The air is thick enough to lean on.'),
('frost','after a frost','Everything glitters, and nothing is moving much.'),
('snow','in the snow','The snow is deep and quiet, and it muffles every step.'),
('mist','in the mist','The mist has opinions about how far you can see.'),
('drizzle','in a fine drizzle','The drizzle is too polite to call itself rain.'),
('thunder','under thunder','Thunder is rearranging the furniture upstairs.'),
('gale','in a gale','The gale is trying to take the roofs with it.'),
('still','in still air','The air is so still it feels like a held breath.'),
('ash','under falling ash','Ash drifts down like grey snow with a memory.'),
('salt','in the salt wind','The salt wind gets into everything and stays.'),
('warm-rain','in warm rain','The rain is warm and slightly unexpected.'),
('cold','in a cold snap','The cold has found the one gap in your coat.'),
('golden','in golden light','The light is gold and generous and brief.'),
('green-haze','in a green haze','The green haze makes the distance look underwater.'),
('smoke','in the smoke','The smoke has decided which way the wind is going.'),
('starry','under a starry sky','The stars are out in numbers that feel personal.'),
('moonless','under a moonless sky','The moon has stepped out, and the dark is doing its work.'),
('rainbow','under a rainbow','The rainbow is far too pleased with itself.');

-- A cool hand for the one thing warmth cannot fix.
INSERT INTO obstacle(id, prompt, lines, need) VALUES ('fever','Cool the fever',
 'A traveller burns under three blankets and a bad joke.|The room is too warm and the night is too long.',
 'The fever needs cold, water, or something healing.');
INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fever','COLD');
INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fever','HEAL');
INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES ('fever','WET');
