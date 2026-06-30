# Client Integrity

Client Integrity requires players to install the matching `client_integrity` verifier mod before joining the server.
The server sends a short challenge with the current banned mod IDs, and the client replies with only the banned mods it found.
It does not send the player's full mod list.

Version `1.0.3` also sends a client `ready` payload after joining. This helps clients that load Fabric networking channels slowly, such as Feather Client profiles. The ready fallback only reports which built-in blocked IDs were checked and any matching findings.

Default blocked mod IDs:

- `seedcrackerx`

Server config is created at `config/client-integrity.properties`:

```properties
enforce=true
timeoutTicks=300
responseTimeoutTicks=200
bannedMods=seedcrackerx
logPassedPlayers=false
```

Commands:

- `/integrity status`
- `/integrity status <player>`
- `/integrity reload`

This is a verifier and policy gate, not an unbreakable anti-cheat.
A modified client can fake responses, so high-value servers should combine this with server-side seed protection, logging, and sensible view-distance/exploration limits.
