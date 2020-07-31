# Minecraft Obsidian Miner for 1.16.1 Forge
This mod was made to automate obsidian mining.
If you need a lot of obsidian but don't have a wither powered obsidian farm, don't waste your time by manually mining it, try out this mod.

## Warning
This mod is only recommended for singleplayer or for SMP. Using this mod on a server that has good anti-cheat may get you banned!

## How to use it
1. Get a diamond or netherite pickaxe in your hand
2. Get on top of an obsidian pillar in the end
3. Press F4 to turn on the mod
4. Feel free to leave the game and go AFK while it's mining for you
5. When the mod is done mining the pillar, you will hear XP sounds, press F4 again to turn off the mod

## Features
* Automatic obsidian mining and collecting
* Stops when you're below Y=60 for safety
* Very small or no chance of falling off
* XP sounds and action bar text to remind you when
   * your inventory is full
   * no near obsidian found
   * your tool is about to break
   * your tool is not able to harvest obsidian
   * you are hungry
* Automatic eating from offhand when the player is hungry

## Known bugs
* Automatic eating only works when some GUI is open
* Manual player movement can mess with the mod a bit
* Not mining and trying to pick up every obsidian in a tower

-------------------------------------------
Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "unrenamed" MCP source code (aka
srgnames) - this means that you will not be able to read them directly against
normal code.

Source pack installation information:

Standalone source installation
==============================

See the Forge Documentation online for more detailed instructions:
[http://mcforge.readthedocs.io/en/latest/gettingstarted/](http://mcforge.readthedocs.io/en/latest/gettingstarted/)

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: "gradlew genEclipseRuns" (./gradlew genEclipseRuns if you are on Mac/Linux)
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run "gradlew eclipse" to generate the project.
(Current Issue)
4. Open Project > Run/Debug Settings > Edit runClient and runServer > Environment
5. Edit MOD_CLASSES to show [modid]%%[Path]; 2 times rather then the generated 4.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: "gradlew genIntellijRuns" (./gradlew genIntellijRuns if you are on Mac/Linux)
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can run "gradlew --refresh-dependencies" to refresh the local cache. "gradlew clean" to reset everything {this does not affect your code} and then start the processs again.

Should it still not work, 
Refer to #ForgeGradle on EsperNet for more information about the gradle environment.
or the Forge Project Discord discord.gg/UvedJ9m

Forge source installation
=========================
MinecraftForge ships with this code and installs it as part of the forge
installation process, no further action is required on your part.

LexManos' Install Video
=======================
[https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be](https://www.youtube.com/watch?v=8VEdtQLuLO0&feature=youtu.be)

For more details update more often refer to the Forge Forums:
[http://www.minecraftforge.net/forum/index.php/topic,14048.0.html](http://www.minecraftforge.net/forum/index.php/topic,14048.0.html)
