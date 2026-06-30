# Client Integrity

Requires a matching client verifier mod and blocks configured banned client-side mods.

Fabric verifier mod for Minecraft `26.1.2`. Install this jar on both the server and each player's Fabric client when enforcement is enabled.

## Features

- Requires players to install the matching verifier mod before joining when enforcement is enabled.
- Blocks configured banned client-side mod IDs. The default blocked ID is seedcrackerx.
- The client sends only challenge results, not the full mod list.
- This is a verifier and policy gate, not an unbreakable anti-cheat.

## Build

```powershell
java -version
java -classpath '.\gradle\wrapper\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain build
```

Make sure `java -version` reports Java `25` before building.

The compiled jar will be in:

- `build/libs/client-integrity-1.0.2.jar`

## Install

Copy the jar into the Fabric server's `mods` folder and into each player's Fabric client `mods` folder.
Server config is created at `config/client-integrity.properties`.

## Mod Metadata

- Mod ID: `client_integrity`
- Minecraft: `26.1.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.153.0+26.1.2`
