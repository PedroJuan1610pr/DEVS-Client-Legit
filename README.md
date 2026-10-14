# Devs Client Legit — Minecraft **26.1.2**

Cliente Fabric "legit" (QoL, HUD, información de combate y utilidades visuales) para Minecraft **26.1.2**.
Versión actual del mod: **2.1.7-26.1.2** · 55 módulos · ClickGUI, editor de HUD, perfiles y sistema Multi.

---

## Índice

- [Requisitos](#requisitos)
- [Build](#build)
- [Controles y comandos](#controles-y-comandos)
- [Cómo funcionan los módulos](#cómo-funcionan-los-módulos)
- [Módulos](#módulos)
  - [Combat](#combat-5)
  - [Render](#render-15)
  - [World](#world-4)
  - [Misc](#misc-31)
- [Sistema Multi (bots cracked)](#sistema-multi-bots-cracked)
- [Red, cosméticos y peer-api](#red-cosméticos-y-peer-api)
- [Archivos de configuración](#archivos-de-configuración)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Notas del puerto](#notas-del-puerto)
- [Aviso sobre reglas de servidores](#aviso-sobre-reglas-de-servidores)

---

## Requisitos

- **Java 25** (obligatorio en 26.1+)
- Fabric Loader **0.19.5+**
- Fabric API para **26.1.2**

En Windows, `gradle.properties` fija la ruta de Java 25:

```
org.gradle.java.home=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
```

Si Gradle dice que la ruta no es válida, lista las carpetas y corrige el nombre:

```
dir "C:\Program Files\Eclipse Adoptium"
```

## Build

```bash
./gradlew build
```

Jar → `build/libs/devs-client-legit-2.1.7-26.1.2.jar`

---

## Controles y comandos

| Acción | Tecla / comando |
|--------|-----------------|
| Abrir / cerrar el **ClickGUI** | `Right Shift` |
| Abrir el **editor de HUD** | `H` |
| Añadir / quitar amigo | Click central (rueda) sobre un jugador |
| Gestionar amigos | `.friend add <nick>` · `.friend remove <nick>` · `.friend list` · `.friend toggle` |
| Gestionar bots Multi | `.multi list` · `.multi start <perfil>` · `.multi stop` · `.multi chat <msg>` · `.multi add <nick>` · `.multi profiles` |

Los comandos que empiezan por `.` se procesan en el cliente y **no se envían al servidor**.

Dentro del ClickGUI hay buscador de módulos y de ajustes, asignación de tecla por módulo y selector de ítems para los módulos que usan listas. En el editor de HUD, `R` y `Tab` tienen funciones propias (reset y cambio de vista).

## Cómo funcionan los módulos

Todos los módulos heredan de `Module` y tienen **nombre, descripción, categoría, tecla opcional y ajustes**.

- **Activar / desactivar:** desde el ClickGUI o con la tecla asignada al módulo (se pulsa una vez para alternar).
- **Ajustes:** cada módulo declara los suyos. Los tipos son:

  | Tipo | Qué es |
  |------|--------|
  | `BooleanSetting` | Interruptor |
  | `NumberSetting` | Número con mínimo, máximo y paso |
  | `ModeSetting` | Lista de opciones |
  | `ColorSetting` | Color ARGB |
  | `StringSetting` | Texto |
  | `ItemListSetting` | Lista de ítems elegida con un selector |

- **Módulos HUD:** se dibujan en pantalla y se pueden mover desde el editor de HUD (`H`). Las posiciones se guardan como fracciones de pantalla.
- **Botones "(click)":** algunos ajustes booleanos funcionan como botón de un solo uso (por ejemplo *Clear Counters*, *Save (click)*, *Reload File*).
- **Notificaciones:** al activar o desactivar un módulo sale un aviso, si *Notifications → Module Toggle* está activo.

---

## Módulos

### Combat (5)

Todos son de **solo información**: muestran datos, no modifican el combate.

| Módulo | Qué hace | Ajustes principales |
|--------|----------|---------------------|
| **Hit Log** | Muestra el daño que haces en cada golpe: texto flotante, chat y/o lista en el HUD. | Floating Text, Chat, HUD List, Duration, Color |
| **Totem Pops** | Cuenta y anuncia los tótems que revientan los jugadores. | Chat, Floating Text, Include Self, Death Message, Duration, Forget After, Color, Clear Counters |
| **Consumable Counter** | Cuenta ítems usados, lanzados o colocados (comida, pociones por tipo, perlas, botellas de XP, bloques). HUD propio y de enemigos, con selector de ítems. | Range, Items, Display (Icons/Text), HUD Self, HUD Enemy, Track Eating/Throws/Hand Drop/Block Place, Per Player HUD, Clear On Death, Bind Reset Key |
| **Damage Numbers** | Números flotantes con el daño recibido por las entidades. | Duration, Color, Crit Yellow |
| **Combo Counter** | Muestra la racha de golpes seguidos sobre el mismo objetivo. | Timeout, Min Combo, Color |

### Render (15)

| Módulo | Qué hace | Ajustes principales |
|--------|----------|---------------------|
| **Block Outline** | Contorno personalizado del bloque al que miras. | Mode (Outline/Filled/Outline + Filled/Corners), Opacity, Line Width, Color |
| **Cosmetics** | Efectos sobre tu personaje: sombrero chino, estela animada, halo, anillos en los pies, orbes y *Mahoraga Wheel*. Pueden verse entre jugadores que usen el mod (ver [Red](#red-cosméticos-y-peer-api)). | China Hat, Trail, Halo, Foot Rings, Orbit Orbs, Mahoraga Wheel (tamaño, grosor, offset, giro al golpear), Trail Mode (Fade/Rainbow/Pulse/Spark), Spin Speed, Color, Rainbow Accent |
| **FreeLook** | Mira alrededor sin girar a tu personaje. | Perspective (Back/Front), No Pitch Limit |
| **Friend Marker** | Marca a tus amigos cuando están a la vista (no a través de paredes). | Box, Label, Range, Color |
| **Fullbright** | Elimina la oscuridad: brillo máximo en todas partes (sube el gamma). | Gamma |
| **Hitboxes** | Dibuja la hitbox real de las entidades cercanas (no a través de paredes). | Mode, Opacity, Line Width, Range, filtros (Players/Hostile/Other/Projectiles/Self), Eye Level, Look Ray, colores por tipo |
| **Idle Aura** | Anillos cosméticos cuando estás quieto. | Radius, Speed, Color, Idle After (ticks) |
| **Nametags** | Nombre, vida, distancia, armadura, encantamientos, objeto en mano y tótems reventados sobre las entidades. Puede reemplazar las etiquetas vanilla y agrupar ítems del suelo. | Style (Screen/World), Range, Scale, Distance Scaling, filtros, Health, Distance, Armor, Enchant Style, Durability, Held Item, Placement, Through Walls |
| **NoRender** | Oculta overlays, elementos del HUD y ruido del mundo. | Fire/Underwater/In-Wall Overlay, Nausea, Totem Animation, Hurt Camera, Boss Bar, Scoreboard, Crosshair, Title, Potion Icons, Held Item Name, Weather, Fog, Blindness Fog |
| **Saturation Overlay** | Saturación sobre la barra de hambre, estilo AppleSkin. | Show Overlay, Opacity, Color, Pulse |
| **Target HUD** | HUD estilo nametag de la entidad a la que miras, con fundido de entrada y salida. | Range, Hold Time, Fade In/Out, Players Only, Health Bar, Armor, Held Item, Background, Soft Edges, Accent |
| **Viewmodel** | Posición, escala y rotación de mano y objeto en primera persona. | Main/Off Hand: Scale, X, Y, Z, Rot X, Rot Y, Rot Z |
| **Waypoint HUD** | Flechas y distancia en pantalla hacia tus waypoints. | Max Shown, Background, Color |
| **Waypoints** | Guarda y dibuja destinos con nombre, con tracers y cajas. Puede crear un waypoint al morir. | Waypoint On Death, Track Deaths When Off, Death Memory, Show Distance, Tracers, Boxes, Add Here, Clear All, colores |
| **Weather Control** | Solo en tu cliente: oculta lluvia, nieve y truenos. | No Rain / Snow, No Thunder |

### World (4)

| Módulo | Qué hace | Ajustes principales |
|--------|----------|---------------------|
| **Chunk Borders** | Dibuja los bordes de chunk y, opcionalmente, las líneas de sección. | Chunk Radius, Section Lines, Color, Neighbor Color |
| **Light Level** | Nivel de luz bajo tus pies, útil para saber dónde pueden aparecer mobs. | HUD, Block Outline, Unsafe Below, Safe/Unsafe Color |
| **Placement Preview** | Muestra dónde se colocaría el bloque que llevas en la mano. | Opacity, Color, Blocked Color |
| **Slime Chunks** | Indica si el chunk actual es un *slime chunk* (necesita la semilla del mundo). | HUD, Chunk Outline, World Seed, Use Seed, Color |

### Misc (31)

#### Información en HUD

| Módulo | Qué hace |
|--------|----------|
| **HUD Info** | Coordenadas (con equivalente Nether/Overworld), chunk, altura sobre el suelo, dirección, bioma y FPS. Cada dato se activa por separado. |
| **Armor HUD** | Armadura y objetos en mano con durabilidad. Layout vertical u horizontal, aviso por durabilidad baja. |
| **Potion HUD** | Efectos activos con nivel y tiempo restante. Orden por tiempo o nombre, aviso al quedar poco tiempo. |
| **Block Counter** | Cuenta ítems clave del inventario (obsidiana, cristales…). Elegibles con el selector de ítems; solo hotbar opcional. |
| **Totem Indicator** | Número de tótems y estado de la mano secundaria, con aviso por debajo de un umbral. |
| **Hotbar Warning** | Avisa si algún objeto de la hotbar tiene poca durabilidad o pocas unidades. |
| **Elytra HUD** | Pitch, velocidad, cohetes y ayuda de aterrizaje mientras planeas. |
| **Speedometer** | Velocidad en bloques/s (opcionalmente km/h y componente vertical). |
| **Reach Display** | Distancia al bloque o entidad que miras. |
| **World Clock** | Hora del mundo y fase del día, en formato 24h opcional. |
| **Session Stats** | Tiempo de juego, muertes y golpes de la sesión. |
| **Network HUD** | Ping, TPS estimado y FPS opcional. |
| **Ping Graph** | Ping con una pequeña gráfica del historial. |
| **Keybind List** | Lista en el HUD de los módulos que tienen tecla asignada. |
| **Module Display** | Lista de módulos activos en pantalla (ArrayList), con orden, alineación, color estático, degradado o arcoíris. |
| **Custom Display** | Texto propio de hasta 4 líneas en el HUD, con color, fondo y barra de acento. |
| **Image Display** | Muestra un PNG o GIF desde `config/devs/images/` en el HUD. Ajusta tamaño, opacidad, proporción y velocidad del GIF; botones para recargar y abrir la carpeta. |

#### Chat y avisos

| Módulo | Qué hace |
|--------|----------|
| **Chat Utils** | Marcas de hora, resaltado de tu nombre con sonido y antispam. |
| **Chat Mentions** | Resalta y avisa cuando aparece tu nombre en el chat (toast, sonido, prefijo). |
| **Notifications** | Avisos en pantalla para eventos: módulos activados, tótems reventados, muertes y tótems bajos. |
| **Friends** | Lista de amigos (ver [Controles](#controles-y-comandos)). Color propio y opciones de limpiar y listar en el chat. |

#### Utilidades

| Módulo | Qué hace |
|--------|----------|
| **AutoReconnect** | Se reconecta automáticamente tras una desconexión (retraso configurable). |
| **Death Point** | Marca dónde moriste con caja y tracer. Desactívalo si ya usas *Waypoint On Death*. |
| **Copy Coords** | Copia tus coordenadas al portapapeles con una tecla; opcionalmente al morir y con la dimensión. |
| **Panic** | Botón de pánico: desactiva todos los módulos al instante, cierra el GUI y avisa por chat. Asígnale una tecla. |
| **Screenshot Clean** | Oculta el HUD de DEVS al hacer capturas (con F1 o forzado). |
| **Keybind Conflicts** | Detecta módulos que comparten la misma tecla. |
| **Profiles** | Guarda y carga todos los ajustes en 5 ranuras, más un perfil automático al cerrar el juego. |
| **HUD Presets** | Presets rápidos de HUD (PvP, Mining, Stream): guardar y cargar. |
| **GUI Theme** | Colores de acento y borde del ClickGUI. |
| **Multi** | Abre el panel del sistema Multi (ver siguiente sección). |

---

## Sistema Multi (bots cracked)

Añadido en la **2.1.0**. Permite conectar varias cuentas *cracked* (modo offline, solo con nick) a un servidor desde el propio cliente. No usa cuentas Microsoft.

- **Cuentas:** se añaden con `.multi add <nick>` o desde el panel. Se guardan en `config/devs/multi-accounts.json`.
- **Perfiles:** agrupan un servidor y una lista de cuentas (`config/devs/multi-profiles.json`).
- **Iniciar / parar:** `.multi start <perfil>` y `.multi stop`. Solo puede haber un lote activo a la vez.
- **Chat en grupo:** `.multi chat <mensaje>` envía el mensaje con todos los bots conectados.
- **Estado:** `.multi list` muestra cada bot con su estado y detalle; el panel (MultiScreen) añade posición cuando hay datos.

Las conexiones se hacen con un login propio (handshake + login hello) en hilos separados del juego.

## Red, cosméticos y peer-api

El mod puede mostrar un icono **✦** en la lista de jugadores y los **cosméticos** de otros usuarios del mod. Hay dos vías:

1. **Servidor Fabric con el mod:** el servidor reenvía el estado (flags y color) de cada jugador a los demás mediante un canal propio.
2. **Peer API HTTP:** funciona en cualquier servidor de Minecraft. El cliente consulta un servicio HTTP externo.

**Cómo localiza el cliente la peer API** (en este orden):

1. `config/devs/peer-api.txt`, si contiene una URL válida.
2. Un Gist de GitHub que publica la URL actual del túnel.
3. Descubrimiento UDP en la red local (puerto 8788).
4. Direcciones de respaldo: Tailscale, LAN y localhost.

**Qué envía el cliente a la peer API** (`POST /api/v1/presence`): UUID, nombre, flags, color y una referencia del servidor al que estás conectado. Y consulta `GET /api/v1/players?uuids=…` con los UUID del tab para saber quién más usa el mod. Si no quieres este intercambio, no uses los cosméticos entre jugadores ni dejes configurada una peer API.

### Servidor `peer-api/` (opcional)

Servicio FastAPI para Ubuntu con túnel Cloudflare:

| Archivo | Función |
|---------|---------|
| `main.py` | API: `/health`, `/api/v1/presence`, `/api/v1/players`, `/api/v1/player/{uuid}`. Guarda presencia en memoria con timeout de 20 s. Escucha HTTP en el 8787 y responde descubrimiento UDP en el 8788. |
| `requirements.txt` | fastapi, uvicorn, pydantic |
| `devs-peer-api.service` | Servicio systemd de la API |
| `devs-peer-tunnel.service` | Túnel `cloudflared` hacia `127.0.0.1:8787` |
| `devs-peer-tunnel-watch.service` | Vigila el log del túnel |
| `watch-tunnel-peer.sh` | Detecta la nueva URL `trycloudflare.com` y llama al script de actualización |
| `update-gist-peer.sh` | Publica la URL del túnel en el Gist (necesita un token de GitHub en `~/.config/licencias/github-gist-token`; el token **no** va en el repo) |

Notas para quien lo despliegue: la API no tiene autenticación y escucha en `0.0.0.0`, así que cualquiera que conozca la URL puede registrar presencias; si lo expones, conviene limitarlo o añadir autenticación.

---

## Archivos de configuración

Todo vive en `.minecraft/config/devs/`:

| Archivo / carpeta | Contenido |
|-------------------|-----------|
| `profiles/*.json` | Perfiles de ajustes (ranuras 1–5 y `auto`) |
| `hud.json` | Posiciones del HUD |
| `friends.json` | Lista de amigos |
| `images/` | Imágenes para *Image Display* |
| `peer-api.txt` | URL de la peer API (se rellena sola al encontrarla) |
| `multi-accounts.json` | Cuentas del sistema Multi |
| `multi-profiles.json` | Perfiles del sistema Multi |

---

## Estructura del proyecto

```
src/main/java/com/devs/client/
├── DevsClient.java        Entrada del cliente: registra módulos, teclas y HUD
├── DevsMain.java          Entrada común: canal de red
├── McCompat.java          Compatibilidad entre versiones de Minecraft
├── module/                Los 55 módulos + Module / ModuleManager
├── setting/               Tipos de ajustes
├── gui/                   ClickGUI, editor de HUD, selector de ítems, panel Multi
├── hud/                   Layout y guardado de posiciones
├── mixin/                 18 mixins (cámara, chat, HUD, render, red…)
├── friends/               Amigos y comandos .friend
├── multi/                 Sistema Multi (cuentas, perfiles, sesiones, comandos)
├── net/                   Canal con servidor y cliente de la peer API
├── profile/               Guardado y carga de perfiles
└── render/                Utilidades de resaltado
peer-api/                  Servicio opcional (FastAPI + systemd)
```

---

## Notas del puerto

Minecraft **26.1** ya no está ofuscado y Fabric dejó de mantener Yarn oficial. El proyecto usa **Modern Yarn** (RelativityMC) para conservar nombres parecidos a Yarn y facilitar el puerto desde 1.21.11.

Al compilar pueden salir errores por cambios de API (métodos renombrados, mixins, render, HUD). El log de `./gradlew build` indica qué ajustar. Los cambios pendientes de portar entre 26.1.2 y 26.2 están en `CROSS_VERSION_UPDATES.md`; el historial completo, en `CHANGELOG.md`.

## Aviso sobre reglas de servidores

Aunque el cliente se presenta como "legit", varias funciones dan ventaja informativa o visual y **muchos servidores las prohíben o las consideran trampa**: *Fullbright*, *Hitboxes*, *Nametags* a través de paredes, *FreeLook*, *Chunk Borders*, *Slime Chunks*, *Light Level*, *Placement Preview*, *Viewmodel* y el sistema *Multi*. Revisa las normas de cada servidor antes de usarlas. *Panic* sirve para desactivarlo todo de golpe, pero no sustituye a respetar las reglas.

## Licencia

Ver `LICENSE`.
