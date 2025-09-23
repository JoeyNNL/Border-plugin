# AutoBorder

AutoBorder is a Minecraft (Paper/Spigot) plugin that automatically grows the world border at configurable times and days. The plugin supports broadcasts, titles, sounds, logging, and advanced admin commands. All options are configurable via `config.yml` and messages via `messages.yml`.

---

## Features

- Automatically grows the world border at set times and days
- Animated border growth
- Optional: chat message, title, and sound for all players when the border grows
- Optional: logs border changes to a file
- Admins can directly change the border, get info, or allow players to move outside the border

---

## Installation

1. Place the plugin JAR in your server's `plugins` folder.
2. Start the server. The `config.yml` will be generated automatically.

---

## Commands

All commands start with `/border`:

| Command                        | Description                                         |
|--------------------------------|-----------------------------------------------------|
| `/border reload`               | Reload the config and immediately update the border |
| `/border set <size>`           | Set the border to a specific size (diameter in blocks) |
| `/border add <amount>`         | Increase the border by a number of blocks           |
| `/border remove <amount>`      | Decrease the border by a number of blocks           |
| `/border center <x> <z>`       | Set the border center to coordinates (x, z)         |
| `/border center here`          | Set the border center to your current location      |
| `/border info`                 | Show current border information                     |
| `/border log`                  | Show the last 10 lines of border.log                |
| `/border bypass [player]`      | Allow yourself or another player to move outside the border |
| `/border help`                 | Overview of all commands                           |

> Only players with the `border.admin` permission can use these commands.

---

## Configuration

All options are found in `config.yml`. Main options include:

- Times and days for border growth
- Animation settings
- Titles and sounds (messages are in `messages.yml`)
- Logging

---

## PlaceholderAPI

Use `%autoborder_size%` to display the current border size.

---

## Questions or bugs?

Contact **JoeyNNL** or open an issue on GitHub.

---

## Changelog

### 2.0
- Messages moved to `messages.yml` with support for both MiniMessage and `&` color codes (auto-detected).
- Added daily probabilistic shrink (chance %, amount) with titles and sounds, and a configurable minimum border size.
- Local PlaceholderAPI expansion included (no external cloud dependency).
- Cleaned up `config.yml` (message strings removed; titles/sounds remain here).

### 1.1
- Scheduling improvements and debug logging.
- PlaceholderAPI local registration fixes.
- Initial MiniMessage migration and English defaults.

### 1.0
- Initial release.

---

## Migration notes (to 2.0)

- Messages have moved to `messages.yml`.
	- If you customized texts in `config.yml` before, copy them into `messages.yml`.
	- You can use either MiniMessage tags (e.g., `<yellow>`) or `&` color codes (e.g., `&e`, `&l`). The plugin auto-detects which to use.
- `config.yml` keeps behavior and title timings; message content is no longer read from `config.yml`.
- New options:
	- `border.shrink_chance_percent`, `border.shrink_amount`, `border.min_size`
	- `border.broadcast_title_enabled` and title fade timings apply to both grow and shrink titles.

