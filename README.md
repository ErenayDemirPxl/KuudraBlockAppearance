# Kuudra Block Appearance v0.5.1

Client-side Fabric mod for Minecraft 26.1.2.

## Changes in 0.5.1
- Added **Mossy Preset**:
  - Andesite -> Cobbled Deepslate
  - Bedrock -> Green Wool
  - Coal Block -> Brown Terracotta
  - Cobblestone -> Green Concrete Powder
  - Gray Wool -> Lime Terracotta
  - Nether Bricks -> Cobbled Deepslate
  - Stone -> Deepslate
  - Cobblestone Stairs -> Cobbled Deepslate Stairs
  - Cobblestone Slab -> Cobbled Deepslate Slab
- Removed the old Moss Test preset.
- Stair replacements now preserve facing, half, and stair shape when replacing stairs with stairs.
- Slab replacements now preserve slab type when replacing slabs with slabs.
- Added save export/import.
  - **Export** writes a shareable JSON into `config/kuudraappearance/exports/`.
  - To import a shared save, place its JSON in that same `exports` folder and click **Import JSONs** in the Saves tab.
- Source/Appearance inputs remain at the expanded 128-character limit.

## Build
Requires JDK 25.

```bat
gradlew.bat build
```

The mod jar is created in `build/libs/`.


## v0.5.1
- Added `/kba` as a client-side command to open the KBA GUI.
- The existing keybind is unchanged.


## v0.5.1
- UI overhaul to the Kuudra Mod Editor spec.
- Added built-in Bloody Kuudra preset.
- Fabric only, Java 25, Minecraft 26.1.2.
