---
summary: Living forests: trees drop seeds, saplings sprout, and the woods slowly spread across your world. Works with every vanilla tree and most modded ones, looks natural, and runs entirely on the server.
---

> ⚠️ **Disclaimer: This project is AI-generated.** The code, this description and the icon of this mod were created with generative AI (Claude by Anthropic), directed by me. While I am a professional software developer, I am not a Java developer, so I use generative AI to build mods that my friends and I want to play with, and I like to share the ones that work well with everyone. I understand if you do not want to support the use of generative AI, which is why I put this disclaimer here. If you run into any problems, please report them on GitHub.

# Forest Growth

Trees spread on their own: from time to time natural leaves drop a seed, and a sapling of the same tree sprouts nearby. The saplings grow like any other, so forests slowly expand and fill gaps over time.

## Features

- **Every tree with a sapling** – oak, spruce, birch, jungle, acacia, dark oak, cherry, pale oak, mangrove (propagule) and azalea, plus modded trees that follow the usual `<name>_leaves` / `<name>_sapling` naming. Other pairs can be added in the config.
- **Only natural trees** – leaves placed by players never drop seeds, crops are not affected, and saplings are never planted on farmland.
- **Natural looking** – seeds land within a few blocks of the leaves and only sprout on suitable ground with enough light and some distance to other trunks and saplings. Seeds caught in a canopy or falling into water are lost.
- **Dark oak and pale oak** are planted as 2×2 squares, since they cannot grow from a single sapling.
- **Follows `randomTickSpeed`** – spreading only happens in loaded chunks and speeds up or slows down with the gamerule.

## Installation

All logic runs on the server, so the mod works in **singleplayer** and on **dedicated servers** without being installed on the clients: players with an unmodded game can join.

## Configuration

`config/forestgrowth.properties` is created on first start and reloaded automatically when it changes. It can also be edited in game: on NeoForge in *Mods → Forest Growth → Config*, on Fabric through [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu). Invalid values are marked red and cannot be saved, and every value can be reset to its default.

The spreading speed and radius, the spacing to other trees, the minimum light level, excluded trees and additional leaves/sapling pairs can be configured.

## Versions

Available for **Fabric** and **NeoForge** on Minecraft **1.21 – 26.3**. Download the file that matches your loader and Minecraft version.

## Links

- [Source code](https://github.com/welles/minecraft-forest-growth)
- [Report a problem](https://github.com/welles/minecraft-forest-growth/issues)
- [Changelog](https://github.com/welles/minecraft-forest-growth/blob/main/CHANGELOG.md)
