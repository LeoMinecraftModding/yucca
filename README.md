# Yucca

A Minecraft modding development assistance tool for NeoForge 1.21.1.

Two core features:

- **`/yucca dump_model`** — Export the entity model layers (`LayerDefinition`) the client has baked to
  Blockbench-compatible `.bbmodel` files, together with the texture each layer's renderer actually uses.
- **`/yucca item_transforms`** — Real-time in-game `ItemTransforms` editor with instant visual feedback.
  Adjust rotation / translation / scale / right_rotation for any item and export the `display` section as
  an item model JSON.

Keyboard shortcuts:

- **G** — Toggle the ItemTransforms editor for the currently held item
- **F6** — Toggle tick freeze / unfreeze (equivalent to `/tick freeze` / `/tick unfreeze`; singleplayer only)

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.230 |
| JDK | 21 |

This mod is meant to be run from a development environment only; it is not intended to be shipped.

## Build

```bash
./gradlew build
```

## Usage

Run in a development environment:

```bash
./gradlew runClient
```

### dump_model

```
/yucca dump_model <target> [textures] [animations] [entity <entity_type>]
```

| Argument | Type | Required | Description |
|---|---|---|---|
| `target` | model layer id | yes | `namespace:path#layer` for a single layer, or `all` (auto-suggested) |
| `textures` | literal | no | Embed the matched texture into the bbmodel as Base64 |
| `animations` | literal | no | Scan and export the model's animations |
| `entity <entity_type>` | `EntityType` | no | Force this entity's renderer for the layers it references (auto-suggested) |

The options may be written in any order and all of them may be omitted.

**Textures are resolved automatically.** Every registered entity renderer is indexed against the model
layers it was baked from, so a dumped layer borrows the texture from whichever renderer actually uses it
— no entity type required. Without `textures` the texture path is still recorded in the bbmodel, it is
simply not embedded. `entity` is only needed to disambiguate when several entities share one layer
(`zombie`, `husk` and `drowned` all use `minecraft:zombie#main`).

Examples:

```
/yucca dump_model all
/yucca dump_model minecraft:warden#main textures animations
/yucca dump_model minecraft:zombie#main entity minecraft:husk textures
/yucca dump_model animations textures all
```

Output: `<gameDir>/yucca/model_dump/<namespace>/<path>/<layer>.bbmodel`

The files target Blockbench's `modded_entity` format (format version 5.0, `modded_entity_flip_y`, box UV),
so the geometry round-trips back to the same `PartPose` offsets. The command reports what it wrote plus a
texture summary, for example:

```
Exported 12 model(s) to yucca/model_dump/ (textures embedded for 12 of 12 model(s))
```

Because the texture lookup builds a throwaway entity from the client level, the command has to run while
in a world.

### item_transforms

```
/yucca item_transforms <item>
```

| Argument | Type | Description |
|---|---|---|
| `item` | `Item` | Item ID, e.g. `minecraft:stick` (auto-suggested) |

Example:

```
/yucca item_transforms minecraft:stick
```

Opens a transparent overlay GUI (does not pause the game) positioned in the top-left corner. Features:

- Two cycle buttons to select the **display context** (3rd Person Left/Right, 1st Person Left/Right, Head,
  GUI, Ground, Fixed) and the **parameter** (rotation, translation, scale, right_rotation)
- Three input fields for X, Y, Z — scroll over a field to nudge it, values start from the item's own model
- **Reset** restores the original values for the current context and parameter
- **Export JSON** writes `yucca/item_transforms/<namespace>/<path>.json`, containing only the `display`
  section, ready to drop into `assets/<namespace>/models/item/<path>.json`

Edits show up immediately while the GUI is open. The editor opens from the G shortcut only while holding a
non-empty item.

## Project layout

Functionality is grouped by feature, so each command owns one package:

```
src/main/java/team/leomcm/yucca/
├── Yucca.java, YuccaClient.java   # mod entry points, command and keybind registration
├── dump/                          # /yucca dump_model: command, bbmodel writers, renderer/texture lookup
├── transforms/                    # /yucca item_transforms: command, editor screen, item model export
└── mixin/                         # injection points (model layer bookkeeping, live item transforms)
```

Both features are client side; only the mixins and the access transformers reach into Minecraft internals.

## License

MIT
