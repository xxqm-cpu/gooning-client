# VisualPlus (Fabric, Minecraft 1.21.4)

Client-side module client.

Modules: Sprint, Fullbright, ESP, Fly, NoFall, FastPlace
Visuals: aurora skies, Spotify HUD, custom enchant glint, swing speed

Menu key: Options > Controls > VisualPlus > "Open VisualPlus Menu" (default Right Shift).
Every module also has its own (unbound) toggle key in the same Controls category.

## Build
Run build.bat (Windows) or ./build.sh (Mac/Linux) after installing JDK 21 + Gradle.
Jar: build/libs/visualplus-1.0.0.jar -> .minecraft/mods (with Fabric Loader + Fabric API 1.21.4).

## Adding a module
Add another `ALL.add(new Module("Name", "description") { ... })` block in ModuleManager.java,
plus a `key.visualplus.module.name` line in lang/en_us.json.
