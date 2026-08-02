# Autoclicker

Minecraft **1.21.11 Fabric** client mod. Hold a key — the game clicks 10 times per second.

## Binds

Two separate binds, both **unbound by default** (so nothing collides with your existing controls):

| Bind | What it does |
|---|---|
| Autoclick: left (attack) | repeats a left click — hitting mobs, mining |
| Autoclick: right (use) | repeats a right click — placing, eating, using items |

Set them in **Options → Controls → Key Binds → Autoclicker**. Same screen as every other keybind.

## Notes

- 10 cps = one click every 2 game ticks. If the game runs below 20 tps, the rate drops with it.
- Only fires in-world; while any GUI is open, the mod stays quiet.
- Client-side only. Works in single player; on servers, anti-cheat may not like it — that's on you.

## Install

1. Fabric loader for 1.21.11 + [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop the jar into `mods/`

## Build

No local Java needed — GitHub Actions builds every push and puts the jar in the `latest` release.

## License

MIT
