# Showbiz

![Created at](https://img.shields.io/github/created-at/FlooferLand/Showbiz-Mod?style=plastic&logo=github&link=https://github.com/FlooferLand/Showbiz-Mod)
![Code size in bytes](https://img.shields.io/github/languages/code-size/FlooferLand/showbiz?style=plastic&color=brightgreen&link=https://github.com/FlooferLand/showbiz)
![Issue tracker](https://img.shields.io/github/issues/FlooferLand/showbiz?style=plastic&link=https://github.com/FlooferLand/showbiz/issues)

> [!WARNING]  
> Showbiz is in VERY early development, expect things to break and be unfinished!

A fan-made Minecraft mod focused around re-creating the wonderful experience that is watching [The Rock-afire Explosion](https://www.youtube.com/watch?v=MyEbr6HvWy0) perform

I made this since Minecraft lets you combine tons of different mods and situations, making this simulator able to be enjoyed in VR via mods like ViveCraft, or with multiplayer with friends.

_Not affiliated nor associated with Aaron Fechter, Rock-afire Explosion, or Creative Engineering. This is purely a fan project._

## Building

1. Clone this project _(or [fork it](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo#forking-a-repository) if you want to contribute changes)_

2. Open the project in [IntelliJ IDEA](https://www.jetbrains.com/idea/download), then open this `README.md` in it and continue reading from there on

3. Build the project by running `gradlew build` _<-- IntelliJ should be showing a play button next to this text in the preview window_

4. Decompile Minecraft sources via `gradlew genSources`

5. Build the mod assets via my own custom `gradlew runDatagen`

## Addons
Showbiz allows you to add in your own bots, among other things.

Currently, very undocumented due to the fact the mod is in alpha.

Make sure to install the [Showbiz VSCode extension](https://marketplace.visualstudio.com/items?itemName=FlooferLand.showbiz)
as it helps with addon creation quite a bit.
