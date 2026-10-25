# Forest Growth

A Minecraft mod for **Fabric** and **NeoForge** (Minecraft 1.21.1 – 26.3) that lets trees spread on their own:
from time to time, natural leaves drop a seed, and a sapling of the same kind sprouts near the tree. The saplings
grow like any other sapling, so forests slowly expand and fill gaps over time.

- **Every tree with a sapling** – oak, spruce, birch, jungle, acacia, dark oak, cherry, pale oak, mangrove
  (propagule) and azalea, plus modded trees whose blocks follow the usual `<name>_leaves` / `<name>_sapling`
  naming. Other pairs can be added in the config.
- **Only natural trees** – leaves placed by players (`persistent=true`) never drop seeds, and crops are not
  affected at all. Saplings are never planted on farmland.
- **Natural looking** – a seed falls from the leaves to the ground within `spreadRadius` blocks. It only sprouts on
  ground the sapling can grow on, with enough light, and with some distance to existing trunks and saplings. Seeds
  caught in a canopy or falling into water are lost.
- **Dark oak and pale oak** are planted as 2×2 squares, since they cannot grow from a single sapling.
- **Follows `randomTickSpeed`** – seeds are dropped by leaves random ticks, so spreading only happens in loaded
  chunks and speeds up or slows down with the gamerule.

## Server-side only

All logic runs on the server, so the mod works

- **in singleplayer** when installed on the client (the integrated server runs it), and
- **on dedicated servers** without being installed on the clients – vanilla clients can join.

## Configuration

`config/forestgrowth.properties` is created on first start. Changes are applied automatically while the game is
running.
It can also be edited in game: on NeoForge in *Mods → Forest Growth → Config*, on Fabric through
[Mod Menu](https://modrinth.com/mod/modmenu). It changes the file of the game it is opened in (e.g. for singleplayer
worlds), not the config of a server you are connected to.

| Key               | Default | Description                                                                                                    |
|-------------------|---------|----------------------------------------------------------------------------------------------------------------|
| `enabled`         | `true`  | Master switch. `false` = no new saplings are planted.                                                          |
| `spreadSpeed`     | `1.0`   | Speed multiplier. `1.0` ≈ one seed per tree every four in-game days (most seeds find no suitable spot). `0` = off. |
| `spreadRadius`    | `8`     | How far (in blocks, horizontally) from the leaves a seed can land.                                             |
| `minSpacing`      | `4`     | New saplings keep at least this many blocks distance to logs and other saplings.                               |
| `minLight`        | `9`     | Minimum light level (ignoring the time of day) at the spot. Saplings need 9 to grow.                           |
| `excludedPlants`  | (empty) | Comma separated ids of saplings or leaves that should not spread, e.g. `minecraft:dark_oak_sapling`.          |
| `additionalPairs` | (empty) | Comma separated `leaves=sapling` pairs for modded trees with unusual names.                                    |
| `logPlanting`     | `false` | Log every planted sapling, useful for testing.                                                                 |

To watch it happen quickly, set `spreadSpeed=100` and `logPlanting=true`, or raise `randomTickSpeed`.

## Building

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build every Minecraft version and loader from one
source tree. Each node in `settings.gradle.kts` targets one Minecraft version and declares the compatible range in
`stonecutter.properties.toml`.

```sh
./gradlew build                          # build all versions and loaders
./gradlew buildAndCollect                # copy all jars to build/libs/<mod version>/
./gradlew :1.21.1-fabric:runClient       # run a specific version (Fabric)
./gradlew :26.3-neoforge:runClient       # run a specific version (NeoForge)
```

Every push to `main` builds all versions on GitHub Actions; the jars can be downloaded from the run's
artifacts.

On **NixOS**, run the game inside the provided Nix shell so Minecraft finds its native libraries
(otherwise it crashes with `GLFW error during init: Failed to detect any supported platform`):

```sh
nix-shell --run './gradlew :1.21.1-fabric:runClient'
```

Version-specific code uses Stonecutter comments (`//? if >=1.21.9 {`). The source tree is checked in for the
`1.21.1-fabric` node; switch the active version with the `Set active project to …` Gradle tasks before editing code
for another version, and switch back to `1.21.1-fabric` (`Reset active project`) before committing.

## License

[MIT](LICENSE)
