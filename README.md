# Miraculous Fabric Mod (Minecraft 1.21.11)

This is a Fabric mod port of the uploaded Spigot plugin source. A Spigot plugin JAR cannot run as a Fabric mod, so the Bukkit event and command code has been replaced with Fabric command/tick APIs.

## Current features
- `/miraculous list` — lists the heroes
- `/miraculous ladybug` (or another hero ID) — transforms and grants hero-themed passive effects
- `/miraculous power` — activates that hero's power effect
- `/miraculous off` — detransforms
- Transformations expire after five minutes

This is a first playable port of the command and effects core. It does **not** yet include the original clickable inventory menu, custom colored suit armor, right-click power star, or every original target/block interaction. Those need additional Fabric-specific implementation.

## Requirements
- Minecraft Java Edition 1.21.11
- Java 21
- Fabric Loader 0.18.1 or newer
- Fabric API for Minecraft 1.21.11
- Gradle 8.14+ (or open this project in a Gradle-capable IDE)

## Build
From this folder run:

```bash
gradle build
```

The remapped mod JAR will be in `build/libs/`. Put that JAR in the Minecraft instance's `mods` folder alongside Fabric API, then launch the Fabric 1.21.11 profile.

The build needs internet access the first time so Gradle can download Minecraft/Fabric dependencies. This environment has no network access, so I could prepare the source project but could not download dependencies to verify/compile the final JAR here.

## Build online without installing Gradle locally

This project includes a GitHub Actions workflow at `.github/workflows/build.yml`.
Upload the project to a GitHub repository, open the **Actions** tab, select **Build Miraculous Fabric Mod**, and choose **Run workflow** (or push a commit). When it finishes, open the run and download the `miraculous-fabric-1.21.11` artifact. It contains the built JAR(s).
