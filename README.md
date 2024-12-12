<h1 align="center" style="font-weight: normal;"><b>MC-Runtime-Test-Mods</b></h1>
<p align="center">Mods for <a href="https://github.com/headlesshq/mc-runtime-test">Mc-Runtime-Test</a>.</p>
<p align="center">MC-Runtime-Test | <a href="https://github.com/3arthqu4ke/headlessmc">HMC</a> | <a href="https://github.com/3arthqu4ke/hmc-specifics">HMC-Specifics</a> | <a href="https://github.com/3arthqu4ke/hmc-optimizations">HMC-Optimizations</a></p>

<div align="center">

[![GitHub All Releases](https://img.shields.io/github/downloads/headlesshq/mc-runtime-test-mod/total.svg)](https://github.com/headlesshq/mc-runtime-test-mod/releases)
![](https://github.com/headlesshq/mc-runtime-test-mod/actions/workflows/run-matrix.yml/badge.svg)
![GitHub](https://img.shields.io/github/license/headlesshq/mc-runtime-test-mod)
![Github last-commit](https://img.shields.io/github/last-commit/headlesshq/mc-runtime-test-mod)

</div>

> [!WARNING]
> NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

This is a set of mods developed for the [mc-runtime-test](https://github.com/headlesshq/mc-runtime-test) Github action.
The action allows you to run the Minecraft Java client inside your Github CI/CD pipeline for testing purposes.

The mods in this repository all do the same thing:
When the game is ready, they join a SinglePlayer world, possibly execute all registered
[gametests](https://learn.microsoft.com/en-us/minecraft/creator/documents/gametestgettingstarted?view=minecraft-bedrock-stable)
and then exit the game signaling success or failure via the exit code.

Mc-Runtime-Test-Mods can be configured via a few System properties.
These are listed and documented [here](api/src/main/java/io/github/headlesshq/mcrtapi/McRuntimeTest.java).
You can set these properties in the mc-runtime-test action with the headlessmc-command input, like this:
```yaml
uses: headlesshq/mc-runtime-test@3.0.0
with:
  mc: 1.21.4
  modloader: fabric
  regex: .*fabric.*
  mc-runtime-test: fabric
  java: ${{ env.java_version }}
  headlessmc-command: -lwjgl --retries 2 --jvm "-D<systemProperty>=<value> -Djava.awt.headless=true"
```
If you run the game with xvfb the -lwjgl flag is not needed.
For more information how the command works checkout the [headlessmc](https://github.com/3arthqu4ke/headlessmc) launcher that mc-runtime-test uses.

The Mc-Runtime-Test-Mod has been written with [unimined](https://github.com/unimined/unimined)
which allows us to support forge, neoforge and fabric.
Additionally, we use [manifold](https://github.com/manifold-systems/manifold) for its Java Pre-Processor, 
which allows us to support all the following Minecraft versions within one code base:

<div align="center">
  
|     Version     | Forge | Fabric | NeoForge | 
|:---------------:| :-: | :-: | :-: |
|  1.21 - 1.21.3  | :white_check_mark:  | :white_check_mark:  | :white_check_mark: |
| 1.20.2 - 1.20.6 | :white_check_mark:  | :white_check_mark:  | :white_check_mark: |
|     1.20.1      | :white_check_mark:  | :white_check_mark:  | :warning:  |
|  1.19 - 1.19.4  | :white_check_mark:  | :white_check_mark:  | - |
|     1.18.2      | :white_check_mark:  | :white_check_mark:  | - |
|     1.17.1      | :white_check_mark:  | :white_check_mark:  | - |
|     1.16.5      | :white_check_mark:  | :white_check_mark:  | - |
|     1.12.2      | :white_check_mark:  | :warning:  | - |
|      1.8.9      | :white_check_mark:  | :warning:  | - |
|     1.7.10      | :white_check_mark:  | :warning:  | - |

</div>

Versions marked with :warning: have not been tested yet, HeadlessMc should support them, though.

# Running your own tests
MC-Runtime-Test does not provide a framework for full integration tests.
You can, however, use Minecraft's own [Game-Test Framework](https://www.minecraft.net/en-us/creator/article/get-started-gametest-framework).
MC-Runtime-Test will basically execute the `/test runall` command after joining the world.
On Neoforge/Lexforge gametest discovery does really not work in production, you might need to register
them themselves and use other [hacks](https://github.com/headlesshq/mc-runtime-test/blob/main/gametest/src/main/java/me/earth/clientgametest/mixin/MixinGameTestRegistry.java)
to get the structure templates correctly, but we are working on it.
You can also use the `headlessmc-command` input to specify additional SystemProperties with the `--jvm` flag.
E.g. `-DMcRuntimeGameTestMinExpectedGameTests=<int>` to specify how many gametests you expect to be executed
at minimum and otherwise fail if not enough gametests have been found.
