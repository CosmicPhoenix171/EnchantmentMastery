```text
You are my senior Minecraft modding engineer. Help me build a Forge mod for the latest Minecraft Java version (1.21.x). The mod adds a per-player Enchantment Mastery system plus a Standard Galactic Alphabet decoding UI effect. Design for multiplayer correctness (server authority), persistence, and reasonable compatibility with other mods.

Core features

1) Per-player mastery data
- Store per-player learned enchantments and their mastery levels and XP.
- Persist across sessions and death.
- Data model
  - Map enchant_id -> mastery_level (unbounded, can get very high)
  - Map enchant_id -> mastery_xp
  - total_levels_spent (optional stat)
- Mastery level is not clamped by vanilla enchant max, but must still respect conflicts and item compatibility when applying.

2) Absorb enchanted books
- Player sneaks and right-clicks while holding an enchanted book to absorb it.
- Only absorbable if the book contains exactly one stored enchantment. If multiple enchants, deny with a message.
- Absorb costs XP levels (levels) and consumes the book.
- Numeric progression requirement: to absorb level N of a specific enchant, the player must already have that enchant unlocked at level N-1. Absorb must be in order.
- Absorb sets or increases the player mastery level for that enchant to N (if allowed).
- Absorb should fail if player does not have enough levels.

3) Apply learned enchants while enchanting
- Provide a way to apply learned enchantments to items at an Enchanting Table without relying on vanilla random offers.
- Preferred approach: sneak-right-click an Enchanting Table (with non-book item in hand) opens a custom “Mastery Enchanter” menu and screen.
- Menu has an input slot for the item, optionally a lapis slot (decide: levels-only by default), and a scrollable list of learned enchantments.
- Player selects an enchantment and a target level (1..player mastery level for that enchant).
- Applying costs XP levels (levels), charges on server, updates the item, and increases mastery XP for that enchant based on the cost spent.

4) Mastery leveling by use with scaling costs
- Each time the player applies a learned enchant, add mastery XP to that enchant.
- When XP crosses a threshold, increase mastery level by 1, and subtract required XP. Repeat if multiple levels gained.
- The XP required per next level must ramp up like enchanting and continue indefinitely.
- Provide a tunable progression math module with functions:
  - absorbCostLevels(bookLevel)
  - applyCostLevels(targetLevel)
  - masteryXpToNext(currentMasteryLevel)
  - masteryXpGainFromApplyCost(applyCostLevels)

5) Keep vanilla enchants normal for compatibility but show true level in tooltip
- Do NOT write extremely high levels into vanilla enchantments.
- Keep the vanilla enchant level at its normal cap (or minimal needed), but store the true effective level separately (either per-item NBT tag or an item data component) as Map enchant_id -> effective_level.
- Override the item tooltip so enchantment lines display the effective level using Roman numerals, not Arabic numbers.
- Tooltip line should look like “Sharpness LXXIII”.
- Provide a Roman numeral converter that supports very large integers by repeating M as needed.

6) Decoding system for enchant names
- Enchantment names initially display in Standard Galactic Alphabet look.
- As players spend levels via absorbing and applying, the enchantment names slowly decode into English letter by letter.
- Letters revealed are random but persistent (so it doesn’t reshuffle).
- Decoding becomes more expensive as more letters are revealed, similar to enchanting scaling.
- Implement decoding using mixed-font rendering: locked letters use minecraft:alt font, unlocked letters use normal font.
- Store decoding progress per player (preferred) or per enchant mastery, but keep it consistent with “spend levels unlock letters”.
- Use a stable set of unlocked letters or unlocked character indices so it persists and is deterministic per player.
- Use only A–Z letters for unlock tracking; spaces and punctuation remain normal.
- Apply decoding to the enchantment name portion only, while the Roman numeral level is always readable.

Implementation requirements

- Use Forge events where possible. For Enchanting Table apply, implement a custom menu/screen instead of trying to hook vanilla offer selection.
- Handle Enchanted Book stored enchantments via 1.21+ item data components (STORED_ENCHANTMENTS).
- Handle item enchantments via 1.21+ data components (ENCHANTMENTS).
- Validate enchant compatibility:
  - Enchant can apply to the target item type
  - No conflicts with existing enchants on the item
- Server is authoritative for applying enchants and spending levels.
- Sync capability data to client for UI and tooltip display using packets.
- Provide clear file/class structure and step-by-step build order.
- Write the code in Java 21, Forge MDK style, with minimal dependencies.

Deliverables

- Suggested package layout and class list.
- Capability definition, provider, attach and clone events, serialization.
- Absorb handler code.
- Menu provider + menu + screen skeleton.
- Network packets for syncing mastery data and requesting apply.
- Item tag/component storage for effective levels.
- Tooltip override implementation using Roman numerals and decoding font mixing.
- Utility modules:
  - RomanNumerals
  - ProgressionMath
  - EnchantComponentUtil (read STORED_ENCHANTMENTS, set ENCHANTMENTS)
  - EnchantRegistryUtil (lookup enchant by id, compatibility checks)
- Notes about any Forge 1.21.x mapping differences I should watch for and how to find the right methods in my IDE.

Start by generating the project structure and the minimal working skeleton that compiles, then expand step-by-step into each feature.