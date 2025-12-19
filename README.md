# Enchantment Mastery

A Minecraft Forge mod for 1.21.x that adds a per-player Enchantment Mastery system with Standard Galactic Alphabet decoding UI effects.

## Features

### 1. Per-Player Mastery Data
- Each player has their own learned enchantments with mastery levels
- Data persists across sessions and death
- Mastery levels can exceed vanilla enchantment caps
- Stored data includes:
  - Enchantment ID → Mastery Level (unbounded)
  - Enchantment ID → Mastery XP
  - Total levels spent statistic
  - Unlocked letter indices for decoding

### 2. Absorb Enchanted Books
- **Sneak + Right-click** while holding an enchanted book to absorb it
- Only books with exactly one enchantment can be absorbed
- Sequential progression: Must have level N-1 to absorb level N
- Costs XP levels (scales with book level)
- Consumes the book on success

### 3. Mastery Enchanter
- **Sneak + Right-click** an Enchanting Table (with non-book item) to open
- Custom menu for applying learned enchantments
- Select enchantment and target level (up to your mastery level)
- Costs XP levels (scales with target level)
- Respects vanilla enchantment compatibility and conflicts

### 4. Mastery Leveling
- Applying enchantments grants mastery XP
- XP thresholds increase like enchanting costs
- Level up to apply even higher level enchantments

### 5. True Effective Levels
- Items store effective level separately from vanilla cap
- Tooltip shows true level using Roman numerals
- Example: "Sharpness LXXIII" for level 73

### 6. Standard Galactic Decoding
- Enchantment names initially appear in Galactic alphabet
- Spending levels gradually reveals letters
- Letters unlock randomly but persistently per player
- Uses mixed-font rendering (minecraft:alt for locked letters)

## Project Structure

```
src/main/java/com/enchantmentmastery/
├── EnchantmentMastery.java          # Main mod class
├── capability/
│   ├── MasteryCapability.java       # Player data capability
│   └── MasteryDataHelper.java       # Data access utilities
├── client/
│   ├── ClientModEvents.java         # Screen registration
│   ├── EnchantmentDisplayHelper.java
│   ├── TooltipHandler.java          # Custom tooltips
│   └── screen/
│       └── MasteryEnchanterScreen.java
├── command/
│   └── MasteryCommands.java         # Debug commands
├── data/
│   ├── EffectiveLevelsComponent.java
│   └── ModDataComponents.java
├── handler/
│   ├── AbsorbHandler.java           # Book absorption
│   ├── DecodingHandler.java         # Letter unlocking
│   ├── MasteryEnchanterHandler.java # Menu opening
│   └── PlayerSyncHandler.java       # Data sync events
├── menu/
│   └── MasteryEnchanterMenu.java    # Custom container
├── mixin/
│   └── ItemStackMixin.java          # Tooltip hook
├── network/
│   ├── ApplyEnchantmentPacket.java
│   ├── ModNetworking.java
│   └── SyncMasteryDataPacket.java
├── registry/
│   └── ModMenuTypes.java
└── util/
    ├── DecodingUtil.java            # Galactic text rendering
    ├── EnchantComponentUtil.java    # Data component helpers
    ├── EnchantRegistryUtil.java     # Registry lookups
    ├── ProgressionMath.java         # Cost calculations
    └── RomanNumerals.java           # Numeral conversion
```

## Building

1. Ensure you have Java 21 installed
2. Clone the repository
3. Run `./gradlew build`
4. Find the mod JAR in `build/libs/`

## Development

### Running the Client
```bash
./gradlew runClient
```

### Running the Server
```bash
./gradlew runServer
```

### Debug Commands

- `/mastery list` - Show all learned enchantments
- `/mastery set <enchant_id> <level>` - Set mastery level
- `/mastery reset` - Reset all mastery data
- `/mastery stats` - Show statistics

## Progression Math

All costs use quadratic scaling similar to vanilla enchanting:

| Action | Formula |
|--------|---------|
| Absorb Cost | `3 * level + 1.5 * level²` |
| Apply Cost | `2 * level + 1.2 * level²` |
| Mastery XP Needed | `10 + level * 3 + level² * 1.5` |
| XP Gain from Apply | `applyCost * 5` |
| Decode Letter Cost | `1 + 0.5 * lettersUnlocked` |

## Forge 1.21.x Notes

### API Differences from Older Versions

1. **Capabilities**: Uses Forge Capability system for player data
2. **Data Components**: Items use data components instead of NBT tags
3. **Codecs**: Serialization uses Mojang codecs
4. **Packets**: Uses Forge SimpleChannel networking

### Finding Methods in Your IDE

- **Enchantment data**: `DataComponents.ENCHANTMENTS`, `DataComponents.STORED_ENCHANTMENTS`
- **Registry access**: `level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)`
- **Component styling**: `Style.EMPTY.withFont(ResourceLocation)`
- **Events**: Check `net.minecraftforge.event` package

### Mapping Notes

- Method names may differ between mappings
- Use Parchment mappings for readable names
- Check Forge documentation for renamed methods

## Compatibility

- Designed for multiplayer (server authoritative)
- Data synced via packets
- Uses data components for item storage
- Should work with most other mods

## License

MIT License - See LICENSE file
