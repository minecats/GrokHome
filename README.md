# GrokHome Plugin

GrokHome is a lightweight Spigot/Paper plugin for Minecraft servers (1.20+), allowing players to set, teleport to, and manage homes with support for SQLite or MySQL storage. It includes admin tools for managing player homes and a first-join spawn location feature. The plugin enforces home limits (default: 1 for non-admins, 10 for admins) and provides a user-friendly command interface.

## Features
- **Player Home Management**:
  - Set homes with `/home sethome <name>`.
  - Teleport to homes with `/home <name>` or `/home` (for single home).
  - List homes with `/home listhomes`.
  - Delete homes with `/home deletehome <name>`.
- **Home Limits**:
  - Non-admins: 1 home by default.
  - Admins (`grokhome.admin.player`): 10 homes by default.
  - Custom limits via `grokhome.limit.N` (e.g., `grokhome.limit.5` for 5 homes).
  - Unlimited homes with `grokhome.unlimited`.
- **Admin Tools**:
  - View, teleport to, or delete other players' homes with `/home <player> [list|tp <home>|del <home>]`.
- **First-Join Spawn**:
  - Set a first-join location with `/firstjoin set` (admin-only).
  - View with `/firstjoin`.
  - New players are teleported to this location on first join.
- **Database Support**:
  - SQLite (default, stored in `plugins/GrokHome/homes.db`).
  - MySQL (configurable in `config.yml`).
- **Safety Checks**:
  - Prevents teleporting to unsafe locations (e.g., lava, suffocation risks).
- **User-Friendly Commands**:
  - Alias `/h` for `/home`.
  - Help command `/home ?` shows usage.
  - Spaced usage text for readability.
  - Restricted home names to avoid subcommand confusion.

## Installation
1. **Download**:
   - Clone the repository or download the JAR from the build output (`target/grokhome-1.0-SNAPSHOT.jar`).
2. **Place JAR**:
   - Copy `grokhome-1.0-SNAPSHOT.jar` to your server's `plugins/` folder.
3. **Configure**:
   - Start the server to generate `plugins/GrokHome/config.yml`.
   - Edit `config.yml` to set database type (`sqlite` or `mysql`) and MySQL credentials if needed.
4. **Restart Server**:
   - Restart to initialize the database and load the plugin.

## Commands
All commands are subcommands of `/home` or its alias `/h`. Usage: `/home <subcommand>` or `/h <subcommand>`.

| Command                        | Description                                      | Permission             |
|--------------------------------|--------------------------------------------------|------------------------|
| `/home sethome <name>`         | Set a home with the given name.                  | `grokhome.sethome`     |
| `/home <name>`                 | Teleport to the named home.                      | `grokhome.home`        |
| `/home`                        | Teleport to the only home (if one exists) or list homes (if multiple). | `grokhome.home`        |
| `/home listhomes`              | List all homes with current/max count.           | `grokhome.listhomes`   |
| `/home deletehome <name>`      | Delete the named home (alias: `/home delhome`).  | `grokhome.delete`      |
| `/home ?`                      | Show help text with available commands.          | None                   |
| `/home <player> list`          | List another player's homes (admin).             | `grokhome.admin.player`|
| `/home <player> tp <home>`     | Teleport to another player's home (admin).       | `grokhome.admin.player`|
| `/home <player> del <home>`    | Delete another player's home (admin).            | `grokhome.admin.player`|
| `/firstjoin`                   | View the first-join spawn location (admin).      | `grokhome.admin.firstjoin` |
| `/firstjoin set`               | Set the first-join spawn to current location (admin). | `grokhome.admin.firstjoin` |

### Notes
- Home names cannot be `sethome`, `home`, `listhomes`, `deletehome`, `delhome`, or `?` to avoid confusion.
- `/home` lists homes if multiple exist, with instructions to use `/home <name>`.
- Invalid home names show `Home '<name>' not found.` followed by usage text.

## Permissions
| Permission                | Description                                      | Default    |
|---------------------------|--------------------------------------------------|------------|
| `grokhome.sethome`        | Set homes.                                       | true       |
| `grokhome.home`           | Teleport to homes.                               | true       |
| `grokhome.listhomes`      | List homes.                                      | true       |
| `grokhome.delete`         | Delete homes.                                    | true       |
| `grokhome.unlimited`      | Unlimited homes.                                 | op         |
| `grokhome.limit.N`        | Set max homes to N (e.g., `grokhome.limit.5`).   | op         |
| `grokhome.admin.player`   | Manage other players' homes (default 10 homes).  | op         |
| `grokhome.admin.firstjoin`| Manage first-join spawn location.                | op         |
| `grokhome.*`              | All permissions.                                 | op         |

### Home Limit Logic
- **Non-Admins**: Default to 1 home.
- **Admins** (`grokhome.admin.player`): Default to 10 homes.
- **Overrides**:
  - `grokhome.unlimited`: Unlimited homes.
  - `grokhome.limit.N`: Exactly N homes (checked from 50 down to 1).
- Example: A player with `grokhome.admin.player` and `grokhome.limit.5` has a 5-home limit.

## Configuration
Edit `plugins/GrokHome/config.yml` to configure the plugin:
```yaml
database:
  type: sqlite  # 'sqlite' or 'mysql'
  mysql:
    host: localhost
    port: 3306
    database: grokhome
    username: root
    password: ''
first-join:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
```
## Building the Plugin
1. **Prerequisites**:
   - Java 17
   - Maven 3.9+
2. **Clone Repository**:
   ```
   bash
   git clone <repository-url>
   cd GrokHome
  
3. **Build**:
   ```
    mvn clean package -U
   ```
   - Outputs target/grokhome-1.0-SNAPSHOT.jar.
  
4. **Copy to Server**:
   - Move grokhome-1.0-SNAPSHOT.jar to plugins/.

5. **Test on Server**:

   - Deploy to Spigot 1.20+ (preferably 1.21).
   Test scenarios:

     - Admin (grokhome.admin.player): /home sethome home1 to home10 (succeeds), /home sethome home11 (fails with Max homes reached (10/10).).
     - Non-Admin: /home sethome home1 (succeeds), /home sethome home2 (fails with Max homes reached (1/1).).
     - Commands: /home home1, /home, /home listhomes, /home deletehome home1, /home ?, /home <player> list (admin).
     - Permissions: Test with grokhome.limit.5, grokhome.unlimited.
   - Check console logs for SQLException or Home count for UUID ...: X.

6. **Verify Database**:

   - Check homes.db (SQLite) or MySQL table.
   - Use /home listhomes to confirm counts (10 for admins, 1 for non-admins).
   - Ensure config.yml has valid database.type.

## Troubleshooting

**Markdown Rendering**:

If the Markdown displays incorrectly, view in a Markdown-compatible editor (e.g., VS Code, GitHub) or confirm the file is saved as README.md.
Verify the content includes all sections (e.g., "Building the Plugin" and "Troubleshooting") in proper Markdown format.


**Plugin Issues**:

- Check server logs for SQLException or Home count for UUID ...: X messages.
- Verify homes.db (SQLite) or MySQL table contents.
- Test with a fresh database:
- Delete homes.db and restart the server to recreate it.

Check permissions using:/lp user <name> permission

(Requires LuckPerms plugin).
If issues persist, provide:
- Command used (e.g., /home sethome home1).
- Server response (exact message).
- Console logs (look for SQLException or Home count entries).
- Output of /home listhomes.




 
     
