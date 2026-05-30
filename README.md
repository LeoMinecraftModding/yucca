# Yucca

A Minecraft modding development assistance tool for NeoForge 1.21.1.  
Exports entity models, textures, and animations from the running game into Blockbench-compatible `.bbmodel` files.

## Build

```bash
./gradlew build
```

Requires JDK 21.

## Usage

Run the mod in a development environment:

```bash
./gradlew runClient
```

In-game, use the command:

```
/yucca dump_model <include_textures> <include_animations> <entity_type> <target>
```

| Argument | Type | Description |
|---|---|---|
| `include_textures` | `bool` | Whether to embed texture data as Base64 in the output |
| `include_animations` | `bool` | Whether to scan and export entity animations |
| `entity_type` | `EntityType` | Entity type used to locate its renderer for entity animations (1.19+) and textures, e.g. `minecraft:warden` (auto-suggested) |
| `target` | `String` | `namespace:path#layer` for a single model layer, or `"all"` to export everything (auto-suggested) |

### Examples

```
/yucca dump_model false false minecraft:warden all
/yucca dump_model true true minecraft:warden minecraft:warden#main
```

### Output

`.bbmodel` files are written to `<gameDir>/yucca/model_dump/<namespace>/<path>/<layer>.bbmodel`.  
Open them directly in [Blockbench](https://www.blockbench.net/).

## License

MIT
