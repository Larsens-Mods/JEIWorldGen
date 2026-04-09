# JEI WorldGen
JEI WorldGen is an addon mod for Just Enough Items (JEI) that adds information on ore generation in the world in a very complete and compatible way.

## Download
You can download JEI WorldGen from the following links:
 - [Modrinth](https://modrinth.com/mod/jei-worldgen)
 - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jei-worldgen)

On your client you need to additionally install Just Enough Items (JEI):
 - [Modrinth](https://modrinth.com/mod/jei)
 - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jei)

If you use the Fabric version of JEI WorldGen, you also need to install FabricAPI:
 - [Modrinth](https://modrinth.com/mod/fabric-api)
 - [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

## How does it work?
JEI WorldGen allows you to view the ore generation information for various ores in the world, including those of other mods.
It reads the information directly from the biome data, so it is based on the actual configuration used in the save.
This makes the shown information very complete and JEI WorldGen able to show information for all mods that use the standard Minecraft world generation system, without needing specific support for each mod.

![Iron Ore Generation Page](https://cdn.modrinth.com/data/YYrsrQ6q/images/25a595812e221338cad6027c9fac3fc07ecd757c.png)

## Mod Compatibility Requirements
To be compatible with JEI WorldGen, a mod must:

 - Generate its ores during either the **UNDERGROUND_ORES** or **UNDERGROUND_DECORATION** generation stages.
 - Use the **minecraft:ore** configured feature type

If both of these requirements are met, JEI WorldGen will be able to show the ore generation information for that mod without needing any specific support.

## Multiplayer and Servers
As JEI WorldGen reads the ore generation information directly from the biome data and those data is only available on the server side,
it is recommended to install JEI WorldGen on the server as well. You do not need to install JEI itself on the server.

If you cannot install JEI WorldGen on the server, the information can not be received from the server, so the client will not show any WorldGen information.
You can however join a singleplayer save before joining the server; in this case JEI WorldGen will keep the cached information from the singleplayer save and show it while you are on the server.

As JEI WorldGen is entirely optional, you can join servers with JEI WorldGen even if you don't have the mod yourself.