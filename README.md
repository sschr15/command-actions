# Command Actions

Command Actions provides an interface to automatically run `.mcfunction` files at particular events. They are loaded from the filesystem on every event call, ensuring that they are as up to date as they can be.

## How to run functions?
- Open / create the `.minecraft/config/command-actions/functions` folder
- Add function files or folders containing function files with the proper event names
    - Functions are detected either without an extension or with the `.mcfunction` extension.
    - All files in subdirectories are detected regardless of extension (or lack thereof)
    - Subdirectories are recursed to find functions

## Current capabilities
Some preprocessing occurs on the input. All preprocessing directives are preceded with a hash (`#`),
and any line preceded with a hash that is not a preprocessor is marked as a comment.

Preprocessing directives:
- `if`, `elif`, `else`, `endif` - check if a given macro evaluates to `true` or `1`, if a number is nonzero, if an equation results in a nonzero answer, or if two values are equal or unequal
- `define` - define a replacement to be replaced later. Not recursive
- `undef` - remove the definition of an already defined macro. This also works for system-provided macros.

## Current events

Built-in macros are listed, and all builtins (including those provided by other mods) are preceded with `C_`

- `server-ready` - called after the server reports it's ready for players
    - `serverName`
    - `isSinglePlayer`
    - `online`
    - `motd`
- `player-join` - called when a player is placed into the server
    - All macros provided by `server-ready`
    - `playerName` and `player` as shorthand
    - `playerUUID`
    - `auth`
    - `firstJoin`
