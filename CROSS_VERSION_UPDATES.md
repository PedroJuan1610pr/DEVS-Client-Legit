# Cambios entre versiones (26.1.2 ↔ 26.2)

Este archivo lista **features y fixes** que se añadieron en una rama de Minecraft
y todavía **no** están (o no se han verificado) en la otra.

Úsalo para portar cambios de un zip a otro.

Convención:
- `[26.1.2 only]` → está en 26.1.2, falta en 26.2
- `[26.2 only]` → está en 26.2, falta en 26.1.2
- `[ported]` → ya se pasó a ambas

---

## Pendiente de portar

### [26.1.2 only]

| 2026-10-11 | 2.1.0 | **Multi** (bots cracked): cuentas, perfiles, GUI, `.multi` | `multi/*`, `gui/MultiScreen`, `module/MultiModule` |

### [26.1.2 only prev]


| 2026-10-11 | 2.0.19 | **Peer API HTTP** (Ubuntu casa) para cosméticos/tab en cualquier MC server | `peer-api/`, `PeerApiClient.java`, `config/devs/peer-api.txt` |

### [26.1.2 only prev]


| Fecha | Versión mod | Cambio | Archivos / notas |
|-------|-------------|--------|------------------|
| 2026-10-11 | 2.0.18 | **Peer sync**: icono ✦ en tab + cosméticos de otros (requiere Fabric server con el mod) | `net/DevsNetworking`, `PeerTracker`, `PlayerListHudMixin`, `Cosmetics`, `DevsMain` |

### [26.1.2 only — anteriores]


| Fecha | Versión mod | Cambio | Archivos / notas |
|-------|-------------|--------|------------------|
| 2026-10-11 | 2.0.14 | **Saturation Overlay** estilo AppleSkin sobre la barra de hambre | `module/SaturationOverlay.java`, mixin `InGameHudMixin.renderFood` |
| 2026-10-11 | 2.0.15 | **Consumable Counter**: HUD por jugador, clear on death, keybind de reset | `module/ConsumableCounter.java` |

### [26.2 only]

| Fecha | Versión mod | Cambio | Archivos / notas |
|-------|-------------|--------|------------------|
| 2026-10-11 | 3.0.x | **McCompat 26.2**: `GuiManager` (`setScreen`, `getCurrentScreen`), `hudHidden` por reflexión, `RenderPipelines` en `client.render`, overlays con `OrderedRenderCommandQueue` | `McCompat.java`, `DevsClient.java`, `FriendCommands.java`, `ClickGuiScreen.java`, `ImageDisplay.java`, `NoRenderOverlayMixin.java`, etc. |
| 2026-10-11 | 3.0.x | Gradle/Loom 1.17.21 + Fabric API `0.161.0+26.2` + modern-yarn `26.2-rc-2` | `build.gradle`, `gradle.properties`, `settings.gradle` |

---

## Ya en ambas (referencia)

| Feature | 26.1.2 | 26.2 |
|---------|--------|------|
| Friends middle-click + `.friend add/remove/list/toggle` | sí (2.0.8+) | sí (base 3.0 desde 2.0.12) |
| Mahoraga Wheel (cabeza, spin on hit, line width) | sí (2.0.10–2.0.12) | sí (heredado) |
| Cosmetics (china hat, trail, halo, rings, orbs) | sí | sí |
| Target HUD, Image Display, Hitboxes, etc. | sí | sí |

---

## Cómo portar

1. Marca la fila en **Pendiente**.
2. Copia/adapta los archivos de la tabla a la otra versión.
3. Ajusta APIs con `McCompat` si hace falta (nombres distintos en 26.2).
4. Sube `mod_version` y mueve la fila a **Ya en ambas** o márcala `[ported]`.
