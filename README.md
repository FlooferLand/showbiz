# Showbiz

![Created at](https://img.shields.io/github/created-at/FlooferLand/Showbiz-Mod?style=plastic&logo=github&link=https://github.com/FlooferLand/Showbiz-Mod)
![Code size in bytes](https://img.shields.io/github/languages/code-size/FlooferLand/showbiz?style=plastic&color=brightgreen&link=https://github.com/FlooferLand/showbiz)
![Issue tracker](https://img.shields.io/github/issues/FlooferLand/showbiz?style=plastic&link=https://github.com/FlooferLand/showbiz/issues)

> [!WARNING]  
> Showbiz is in VERY early development, expect things to break and be unfinished!

A fan-made Minecraft mod focused around re-creating the wonderful experience that is watching [The Rock-afire Explosion](https://www.youtube.com/watch?v=MyEbr6HvWy0) perform

I made this since Minecraft lets you combine tons of different mods and situations, making this simulator able to be enjoyed in VR via mods like ViveCraft, or with multiplayer with friends.

_Not affiliated nor associated with Aaron Fechter, Rock-afire Explosion, or Creative Engineering. This is purely a fan project._

Note that this project is split into several different components:
- [Showbiz Mod](https://github.com/FlooferLand/showbiz) - The Minecraft mod
- [Showbiz (VSCode)](https://github.com/FlooferLand/showbiz-vscode) - Visual Studio Code extension aimed at addon developers
- [BizLib](https://github.com/FlooferLand/bizlib) - Handles format reading, parsing, and other low level things.
  - [BizLibNative](https://github.com/FlooferLand/bizlib/tree/main/BizlibNative) - C#-based library I wrote to maximize engine compatibility, as it reads shows roughly the same way RR Engine does _(except using NRBF instead of BinaryFormatter since it's more secure)_

## Building

1. Clone this project _(or [fork it](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo#forking-a-repository) if you want to contribute changes)_

2. Open the project in [IntelliJ IDEA](https://www.jetbrains.com/idea/download), then open this `README.md` in it and continue reading from there on

3. Build the project by running `gradlew build` _<-- IntelliJ should be showing a play button next to this text in the preview window_

4. Decompile Minecraft sources via `gradlew genSources`

5. Build the mod assets via my own custom `gradlew runDatagen`

## Contributing

This is an open-source project, so I'm glad if you can help!

If you'd like to add anything to the project, please open an issue or send a message on the [Discord server](https://discord.gg/4MVEKKfuaY) to talk about it with us.

Make sure you're at least somewhat familliar with the project's internals before doing so if trying to submit a technical change.

## Addons
Showbiz allows you to add in your own bots, among other things.

Please see [the wiki](https://github.com/FlooferLand/showbiz/wiki) on more information about how to do this.
