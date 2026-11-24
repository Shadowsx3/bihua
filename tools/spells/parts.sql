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
CREATE TABLE tag_outcome(tag TEXT PRIMARY KEY, text TEXT NOT NULL);
CREATE TABLE setting(place TEXT PRIMARY KEY);
CREATE TABLE time_of_day(name TEXT PRIMARY KEY, suffix TEXT NOT NULL, line TEXT NOT NULL);
CREATE TABLE weather(name TEXT PRIMARY KEY, suffix TEXT NOT NULL, line TEXT NOT NULL);

INSERT INTO family(id, name) VALUES
('water','Water'),('ice','Ice'),('snow','Snow'),('rain','Rain'),('fire','Fire'),
('lightning','Lightning'),('wind','Wind'),('cloud','Cloud'),('sun','Sun'),('moon','Moon'),
('light','Light'),('shadow','Shadow'),('earth','Earth'),('stone','Stone'),('mountain','Mountain'),
('wood','Wood'),('metal','Metal'),('animal','Creature'),('person','Spark'),('hand','Gesture'),
('boat','Vessel'),('mind','Mind'),('dream','Dream'),('sound','Sound'),('time','Time');

INSERT INTO family_tag(family, tag) VALUES
('water','FLOW'),('water','WET'),('water','PUSH'),
('ice','COLD'),('ice','SOLID'),
('snow','COLD'),('snow','SOFT'),
('rain','WET'),('rain','FLOW'),
('fire','HEAT'),('fire','LIGHT'),
('lightning','LIGHT'),('lightning','PUSH'),
('wind','AIR'),('wind','PUSH'),('wind','DRY'),
('cloud','AIR'),('cloud','WET'),
('sun','LIGHT'),('sun','HEAT'),('sun','DRY'),
('moon','LIGHT'),
('light','LIGHT'),
('shadow','DARK'),
('earth','SOLID'),('earth','WEIGHT'),
('stone','SOLID'),('stone','WEIGHT'),
('mountain','SOLID'),('mountain','WEIGHT'),
('wood','SOLID'),('wood','FLOAT'),('wood','GROW'),
('metal','SOLID'),('metal','RING'),
('animal','PUSH'),
('person','LIGHT'),
('hand','PUSH'),
('boat','FLOAT'),('boat','FLOW'),
('mind','AIR'),
('dream','DARK'),('dream','AIR'),
('sound','AIR'),('sound','RING'),
('time','FLOW');

-- What a tag does, once it works. {family} is the spell's name.
INSERT INTO tag_outcome(tag, text) VALUES
('FLOW','The {family} runs with the current, and the obstacle gives way around you.'),
('WET','The {family} soaks in, and the stubborn thing loosens and lets you by.'),
('PUSH','The {family} shoves hard, and the way clears with a groan.'),
('COLD','The {family} sets everything firm, and the ground holds your weight.'),
('SOLID','The {family} makes a firm path where there was none.'),
('SOFT','The {family} settles over the ground like a blanket, and the way goes soft and quiet.'),
('HEAT','The {family} warms the stuck thing, and it lets go with a sigh.'),
('LIGHT','The {family} lights the way ahead, and every step is clear.'),
('AIR','The {family} lifts the air around you, and the way opens up.'),
('DRY','The {family} draws off the damp, and the path dries out at once.'),
('DARK','The {family} softens the dark into something you can walk through.'),
('WEIGHT','The {family} presses everything down into place, and the way stays put.'),
('FLOAT','The {family} carries you clear, and the far side comes closer.'),
('GROW','The {family} sends up new growth, and the wall of green parts for you.'),
('RING','The {family} rings out true, and the lock gives with a click.');

-- Each obstacle asks for something. The need text is also the nudge when a cast
-- cannot help. It names the need, never the answer.
INSERT INTO obstacle(id, prompt, lines, need) VALUES
('river','Get across the river',
 'The river runs wide and unhurried.|The far bank looks like another country.',
 'Something that can float, or stand firm enough to walk on, is needed here.'),
('gate','Open the gate',
 'A gate with no hinge and no key guards the orchard.|The orchard smells like it knows something.',
 'Something that can push the gate open is needed here.'),
('dark-path','Find the way down',
 'The path ahead is black as ink.|Something rustles and then decides to be quiet.',
 'The path needs light, not more dark.'),
('frozen-lake','Cross the frozen lake',
 'The lake is frozen, but the ice is soft near the middle.|A fishing hut waits on the far shore.',
 'The ice needs warmth before anyone can cross.'),
('dry-field','Wake the field',
 'The field has gone dry and cracked.|The grass is brittle enough to snap.',
 'The field needs water or damp air, not more heat.'),
('stuck-cart','Free the cart',
 'A merchant''s cart is wedged deep in the mud.|The merchant is explaining the situation to the ox.',
 'Something has to push the cart free of the mud.'),
('bramble','Clear the bramble wall',
 'Thorny brambles block the path, thick as a fence.|Somewhere behind them, a bell is ringing.',
 'The brambles need heat or light to loosen their grip.'),
('becalmed','Fill the sails',
 'The boat sits on still water, sails hanging limp.|A gull is waiting, rather rudely, for you to hurry.',
 'The sails need air, or a push, to fill.'),
('locked-chest','Open the chest',
 'The chest is locked, and the lock has a bell inside it.|Nobody has the key, which is the usual problem.',
 'The lock needs something that rings true.'),
('washed-bridge','Cross the washed-out bridge',
 'The bridge is gone, and the gap is wide.|A few old planks float in the shallows.',
 'The gap needs something solid to stand on, or something to float across.'),
('bee-garden','Wake the garden',
 'Everything in the garden has gone stiff and white.|A bee is asleep on the fence, which is not a good sign.',
 'The garden needs light or warm air to wake.');

INSERT INTO obstacle_accepts(obstacle_id, tag) VALUES
('river','FLOAT'),('river','SOLID'),
('gate','PUSH'),
('dark-path','LIGHT'),
('frozen-lake','HEAT'),
('dry-field','WET'),('dry-field','FLOW'),
('stuck-cart','PUSH'),
('bramble','HEAT'),('bramble','LIGHT'),
('becalmed','AIR'),('becalmed','PUSH'),
('locked-chest','RING'),
('washed-bridge','SOLID'),('washed-bridge','FLOAT'),
('bee-garden','LIGHT'),('bee-garden','AIR');

INSERT INTO setting(place) VALUES
('The old gate'),('The mill on the stream'),('The lantern hill'),('The frozen garden'),
('The orchard wall'),('The market square'),('The dry field'),('The white road'),
('The reed marsh'),('The bell tower steps'),('The pine forest edge'),('The harbor wall');

INSERT INTO time_of_day(name, suffix, line) VALUES
('dawn','at dawn','Dawn is still deciding what colour to be.'),
('noon','at noon','The sun is directly overhead and taking it personally.'),
('dusk','at dusk','The light goes soft and orange at the edges.'),
('night','at night','Everything is quieter than it has any right to be.'),
('fog','in the fog','Fog has swallowed the distance and is working on the rest.'),
('rain','in the rain','Rain has been at this all morning and shows no sign of stopping.'),
('wind','in the wind','The wind keeps changing its mind about which way to blow.'),
('moon','under a cold moon','The moon is up, thin and sharp, and the air has teeth.'),
('plain','','');

INSERT INTO weather(name, suffix, line) VALUES
('clear','','The sky is clear, which feels like a dare.'),
('heavy','under heavy cloud','The clouds sit low and heavy, like they are listening.'),
('humid','in the heat','The air is thick enough to lean on.'),
('frost','after a frost','Everything glitters, and nothing is moving much.'),
('snow','in the snow','The snow is deep and quiet, and it muffles every step.'),
('plain','','');
