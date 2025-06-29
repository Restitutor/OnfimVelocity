# Velocity Relay System

## Overview

This is a cross-server chat relay system for linking Velocity proxies with multiple servers, Discord, and Matrix. It syncs chat messages, player movements, and previews images.

### Key Features

- **Player Movement Tracking**: Announces joins, quits, and server switches
- **Image Relay**: Share images across servers with Unicode block rendering
- **Timezone Support**: Automatic Discord timestamp conversion based on player location
- **Configurable Relay**: Players can toggle relay and image features
- **Command Relay**: Specific commands like `/me` are relayed across servers

### User Commands

- `/togglerelay [on/off]` - Enable/disable seeing relayed chat messages
- `/toggleimage [on/off]` - Enable/disable seeing relayed images
- `/globalrelay on/off` - Admin command to toggle global relay system
- `/mytimezone set <timezone> <username>` - Set your timezone for inbound Discord timestamps

## Technical Architecture

### Project Structure

```
onfim-relays/
├── onfim-lib/          # Core networking and data structures
│   ├── format/         # Event data models
│   ├── out/            # Outbound messaging components
│   └── utils/          # Utilities and configuration
└── onfim-velocity/     # Velocity plugin implementation
    ├── OnfimVelocity.kt    # Main plugin class
    ├── ChatSender.kt       # Chat distribution logic
    └── timezone/           # Timezone handling
```

### Network Architecture

Onfim uses a distributed peer-to-peer architecture where each Velocity proxy acts as both client and server:

1. **Port Configuration**: Each node binds to port configurable via `SELF_PORT`
2. **Dual Protocol**: Messages are sent via both UDP (fast, unreliable) and SCTP (reliable, ordered)
3. **Heartbeat System**: Nodes broadcast heartbeats every 30 seconds to discover other nodes

### Message Flow

```
Player Chat → Velocity Event → Serialization → UDP/SCTP Broadcast
                                                    ↓
                                    Other Nodes → Deserialization → Chat Display
```

### Event Types

#### SerializedEvent (Base Class)

All events extend this base class containing:

- `type`: Event type identifier
- `id`: Unique event ID for deduplication
- `node`: Origin node information

#### Chat Event

```kotlin
Chat(
    plaintext: String,        // Processed message text
    user: ChatUser,          // Sender information
    server: EventLocation,   // Origin server
    platform: String,        // "In-Game", "Discord", "Matrix", "Onfim"
    context: DiscordContext? // Reply context if applicable
)
```

#### Player Movement Events

- `Join/Quit`: Player connection events
- `Switch`: Server-to-server transfers
- `SJoin/SQuit`: Silent variants (not relayed externally)

#### System Events

- `Heartbeat`: Network topology maintenance
- `ServerMessage`: System announcements
- `ImageEvt`: Compressed RGB image data

### Configuration

Environment variables required:

- `ARC_ID`: Discord channel ID for main chat
- `MATRIX_MAIN_CHANNEL`: Matrix room ID

### Persistence

Player preferences stored in new line delimited lists:

- `data/no-relay.txt`: UUIDs of players with relay disabled
- `data/no-image.txt`: UUIDs of players with images disabled

### Image Relay Protocol

Images are:

1. Compressed as gzipped RGB hex strings by `https://github.com/Restitutor/ImgToMC`
2. Transmitted as `ImageEvt` with dimensions
3. Rendered using Unicode block characters (▏) with color codes
4. Displayed line-by-line to players who haven't disabled images

### Timezone System

1. On player join, timezone is determined via:
   - Username-based lookup from external service
   - IP geolocation fallback
2. Discord timestamps (`<t:timestamp:format>`) are automatically converted
3. Players without timezone data see system time with setup instructions

### Development Setup

1. **Dependencies**:

   - See `gradle/libs.versions.toml`

2. **Building**:

   ```bash
   ./gradlew :onfim-velocity:build
   ```

3. **Configuration**:
   - Set required environment variables
   - Ensure nodes can reach each other on port 2403
   - Add jar to plugins/
