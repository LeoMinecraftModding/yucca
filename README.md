# Yucca

A Minecraft modding development assistance tool for NeoForge 1.21.1.

Two core features:

- **`/yucca dump_model`** — Export entity model layers (LayerDefinition), textures, and animations to
  Blockbench-compatible `.bbmodel` files.
- **`/yucca item_transforms`** — Real-time in-game ItemTransforms editor with instant visual feedback. Adjust rotation /
  translation / scale / right_rotation for any item and export the `display` section as a JSON item model.

Keyboard shortcuts:

- **G** — Toggle ItemTransforms editor for the currently held item
- **F6** — Toggle tick freeze / unfreeze (equivalent to `/tick freeze` / `/tick unfreeze`)

## Build

```bash
./gradlew build
```

Requires JDK 21.

## Usage

Run in a development environment:

```bash
./gradlew runClient
```

### dump_model

```
/yucca dump_model <include_textures> <include_animations> <entity_type> <target>
```

| Argument             | Type         | Description                                                                           |
|----------------------|--------------|---------------------------------------------------------------------------------------|
| `include_textures`   | `bool`       | Embed texture data as Base64 in the output                                            |
| `include_animations` | `bool`       | Scan and export entity animations                                                     |
| `entity_type`        | `EntityType` | Entity type for renderer lookup, e.g. `minecraft:warden` (auto-suggested)             |
| `target`             | `String`     | `namespace:path#layer` for a single layer, or `"all"` for everything (auto-suggested) |

Examples:
```
/yucca dump_model false false minecraft:warden all
/yucca dump_model true true minecraft:warden minecraft:warden#main
```

Output: `<gameDir>/yucca/model_dump/<namespace>/<path>/<layer>.bbmodel`

### item_transforms

```
/yucca item_transforms <item>
```

| Argument | Type   | Description                                      |
|----------|--------|--------------------------------------------------|
| `item`   | `Item` | Item ID, e.g. `minecraft:stick` (auto-suggested) |

Example:

```
/yucca item_transforms minecraft:stick
```

Opens a transparent overlay GUI (does not pause the game) positioned in the top-left corner. Features:

- Two cycle buttons to select the **display context** (3rd Left/Right, 1st Left/Right, Head, GUI, Ground, Fixed) and *
  *parameter type** (rotation, translation, scale, right_rotation)
- Three input fields for X, Y, Z — with tooltip (scroll to adjust), initial values from the original model
- **Reset** button to restore original values for the current context + parameter
- **Export JSON** button saves all transforms as `yucca/item_transforms/<namespace>/<path>.json` (display section only)

All edits are reflected in real-time while the GUI is open. The screen and G shortcut only work when holding a non-empty
item.

## License

MIT
