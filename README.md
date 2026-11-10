# Autofill Orders

A lightweight client-side Fabric mod for Minecraft 26.x designed to automate repetitive order deliveries and container inventory fulfillment.

---

### Direct Download

* **Latest Release**: [Download `autofillorders-2.4.9+26.x.jar`](https://github.com/bruhslow/autofill-orders/releases/latest)
* **Repository Build**: [`build/libs/autofillorders-2.4.9+26.x.jar`](build/libs/autofillorders-2.4.9+26.x.jar)

---

### Overview

| Detail | Specification |
| :--- | :--- |
| **Platform** | Fabric (Client-Side) |
| **Minecraft Version** | 26.2 (26.x) |
| **Java Version** | 25 |
| **Dependencies** | Fabric Loader (>= 0.15.0), Fabric API |
| **License** | MIT |

---

### Key Features

* **Automated Order Fulfillment**: Automatically dispatches the configured server order command, scans container menus for requested items, and deposits inventory stacks.
* **Multi-Page Menu Navigation**: Automatically browses across paginated shop menus until target items are found.
* **In-Game Configuration GUI**: Dedicated settings interface with a visual, searchable item browser.
* **Anti-Cheat Safe Delay**: Configurable deposit speed (0 to 20 ticks) to maintain compatibility with server rate limits.
* **Custom Keybinds**: Toggle automation with single keys or modifier key combinations (default: `V`).
* **Audio & Chat Feedback**: Configurable sound chimes and chat alerts for cycle completion.

---

### Commands

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `/autofillorders` | None | Opens the graphical configuration menu |
| `/autofillorders toggle` | None | Starts or stops the autofill routine |
| `/autofillorders status` | None | Prints active settings and statistics to chat |
| `/autofillorders command` | `<cmd>` | Updates the server order command |
| `/autofillorders item` | `<itemId>` | Sets target item by identifier |
| `/autofillorders delay` | `<ticks>` | Adjusts click delay between 0 and 20 ticks |
| `/autofillorders key` | `<combo>` | Rebinds the shortcut hotkey |

---

### Quick Start

1. Download **`autofillorders-2.4.9+26.x.jar`** and place it in `.minecraft/mods` along with Fabric Loader and Fabric API.
2. Launch Minecraft and press **`V`** (or type `/autofillorders`) to open settings.
3. Configure your server's delivery command and select your target item.
4. Press **`V`** in-game to start autofilling. Press again at any time to pause.

---

### Building from Source

Prerequisites: JDK 25.

```bash
# Clone the repository
git clone https://github.com/bruhslow/autofill-orders.git

# Build the mod archive
./gradlew build
```

Compiled JARs will be generated in `build/libs/`.

---

### License

This project is licensed under the MIT License.
