# Credits

**Maxim (Mbolt1337)** — idea, design and direction, the four architectural concepts the casinos follow, and the
review and approval of every model, screen, sound and building in the game.

**[Claude Code](https://claude.com/claude-code)** (Claude Opus 5.5), working with Maxim:

- the Minecraft side of the mod: vault, cashier, slot machine, roulette and croupier, menus, screens and network
  packets;
- the models of the vault, cashier and slot machine, their animations, icons and screens, made with a
  [Blockbench MCP](https://github.com/jasonjgardner/blockbench-mcp-plugin) server;
- the decor, the neon lettering and signs, the guide book, the achievements, the CASINO marquees, giant chips,
  banner and the other facade blocks;
- both casino buildings as they are now, and their world generation;
- every sound, synthesised from scratch by [`art/sounds.py`](art/sounds.py). No recordings or samples, and no
  Minecraft audio, were copied.

**Codex**, working with Maxim:

- the first prototype's bank maths, which the mod still uses and which its original tests still cover;
- the dining chair, table lamp, bottle shelf and serving tray, the food and drinks, sitting on furniture, and the
  leg pose when seated;
- the gilded trim block, and fixes to the journal, tooltips, client settings and building layouts.

All textures are pixel grids of palette letters in [`art/textures/`](art/textures) and [`art/`](art), drawn pixel by
pixel and built into PNGs by [`art/build.py`](art/build.py) and its siblings. No image generation model produced any
game asset. No assets were taken from other games.

**Libraries and tools**

- [NeoForge](https://neoforged.net) and the NeoForge MDK; the MDK's own files keep its licence, see
  [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).
- [GeckoLib](https://github.com/bernie-g/geckolib) for the animated models; none of its assets are copied.
- [Blockbench](https://www.blockbench.net) and the Blockbench MCP plugin by Jason J. Gardner (GPL-3.0), used only
  while making the models and not shipped in the jar.
- [Parchment](https://parchmentmc.org) mappings for readable parameter names.
