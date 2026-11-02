# Changelog

All notable changes to this mod are documented in this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the mod follows [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-11-03

### Added

- A pixel art icon, shown in Mod Menu, in NeoForge's mod list and at the top of the README.

## [1.1.0] - 2026-11-02

### Added

- The config screen checks every value while typing: numbers outside their range are marked red, and Done stays
  disabled until every value is valid, listing the invalid ones in its tooltip.
- Every value in the config screen can be reset to its default with the ↺ button next to it, or all at once with
  Reset all. Nothing is saved until Done.

## [1.0.0] - 2026-10-26

First release, for Fabric and NeoForge on Minecraft 1.21.1 – 26.3.

### Added

- Natural leaves drop a seed from time to time, and a sapling of the same tree sprouts nearby, so forests slowly
  spread and fill gaps on their own.
- Works for every tree with a sapling, including mangrove propagules, azalea and modded trees that follow the usual
  `<name>_leaves` / `<name>_sapling` naming; other pairs can be added in the config.
- Only natural trees spread: leaves placed by players never drop seeds, crops are not affected and saplings are never
  planted on farmland.
- Seeds land within `spreadRadius` blocks and only sprout on suitable ground with enough light and some distance to
  other trunks and saplings.
- `config/forestgrowth.properties`, reloaded automatically and editable in game through NeoForge's mod list or
  Mod Menu.

[1.2.0]: https://github.com/welles/minecraft-forest-growth/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/welles/minecraft-forest-growth/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/welles/minecraft-forest-growth/releases/tag/v1.0.0
