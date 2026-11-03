# Notes for Claude

## Changelog

`CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and must be updated for every
release:

- Add a `## [x.y.z] - YYYY-MM-DD` section at the top with `### Added` / `### Changed` / `### Fixed` /
  `### Removed`, describing the changes from a player's point of view, not commit by commit.
- Add the compare link at the bottom: `[x.y.z]: https://github.com/welles/minecraft-forest-growth/compare/vPREVIOUS...vx.y.z`.
- Commit the changelog before creating the release.

## CurseForge description

`CURSEFORGE_DESCRIPTION.md` is the text of the mod's CurseForge page, pasted there by hand. Keep it up to date: whenever
a change affects what players see (features, defaults, installation, requirements, supported versions), update it in
the same commit as the README and tell the user to paste it into CurseForge.

- The front matter holds the `summary`, the text CurseForge shows in its lists: catchy, for players, at most 256
  characters, with the hook in the first sentence. Everything below the front matter is the description.
- The description must always start with the prominent notice that the project is AI-generated (code, description
  and icon).
- Write it for players: what the mod does, how to install it, what can be configured; no build instructions.
- Keep every paragraph and list item on a single line, since the CurseForge editor may turn line breaks into breaks.

## Releases

1. Check that the `Build` workflow run of the latest commit on `main` is green. If a node fails while resolving
   dependencies (e.g. a POM from libraries.minecraft.net) although it builds locally, the GitHub Actions cache is
   probably broken: `gh cache delete --all -R welles/minecraft-forest-growth`, then rerun the failed jobs.
2. Create the release with the changelog section as notes, followed by
   `**Full Changelog**: https://github.com/welles/minecraft-forest-growth/compare/vPREVIOUS...vx.y.z`:
   `gh release create vx.y.z -R welles/minecraft-forest-growth --target main --title x.y.z --notes-file <notes>`
3. Publishing the release runs `release.yml`, which builds every node with the version from the tag and attaches
   the jars. Check that all of them are attached. `mod.version` in the repo stays `0.0.0-dev`.

New features are a minor release, fixes only a patch release.
