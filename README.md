# AutoBorder

AutoBorder is a Minecraft (Paper/Spigot) plugin that automatically grows the world border at configurable times and days. The plugin supports broadcasts, titles, sounds, logging, and advanced admin commands. All options are configurable via `config.yml`.

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
- Messages, titles, and sounds
- Logging

---

## PlaceholderAPI

Use `%autoborder_size%` to display the current border size.

---

## Questions or bugs?

Contact **JoeyNNL** or open an issue on GitHub.

---

