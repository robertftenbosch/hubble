# Hubble

A peer-to-peer social network where **friendships can only be formed in true physical proximity**. Two phones must be within Bluetooth LE range to connect; every encryption key originates from a real-world, in-person meeting. A backend (later) provides only a *coarse activity heatmap* — never precise locations. Friends share **ephemeral posts/stories** over direct P2P with an encrypted store-and-forward fallback.

See the design spec and plan:
- `docs/superpowers/specs/2026-06-15-hubble-ble-proximity-friending-design.md`
- `docs/superpowers/plans/2026-06-15-hubble-ble-proximity-friending.md`

> **Direction:** evolving toward a **proximity dating app** (hybrid model: cross paths → like →
> match → chat). See `DEVELOPMENT.md` for the design system and full backlog.

### TODO — known UX issues (from on-device testing)
- [x] **Navigation reworked** — persistent bottom bar (Paths · Nearby · Matches · You); sub-screens go full-screen.
- [x] **Keyboard dismisses after sending a chat message** (hide IME + clear focus on send).

## Architecture

Hubble decomposes into four sub-projects (build order A → C → D, with B in parallel):

| # | Sub-project | Status |
|---|---|---|
| **A** | Identity + BLE proximity friending | `:core` engine tested ✅; `:app` runs on a Pixel 7 — advertises + scans + GATT server live ✅ (friend handshake needs a 2nd phone to exercise) |
| **B** | Discovery server: coarse heatmap (Elixir) | privacy core + HTTP API tested ✅ (`POST /beacon`, `GET /heatmap`) |
| **C** | P2P transport: WebRTC + E2E + store-and-forward | E2E messaging + relay tested ✅; HTTP relay + app sync verified on device ✅; **direct WebRTC data channel + signaling verified on device** ✅ |
| **D** | Ephemeral content + app UX | posts + feed UI live on device ✅; publish deposits to friends' mailboxes, feed syncs incoming |

## Modules

### `:core` — pure-JVM, fully tested ✅
All security-critical, transport-agnostic logic. No Android dependencies, so it
builds and tests on any JVM. This is the root of trust for the whole system.

- `crypto/` — Bouncy Castle: X25519 ECDH, Ed25519 sign/verify, HKDF, BLAKE2b; emoji SAS
- `identity/` — in-house BIP39 mnemonic + deterministic identity derivation (recovery phrase = backup)
- `handshake/` — the friend-handshake state machine (mutual key exchange → shared root key → in-person SAS → signed transcript) and its binary codec
- `transport/` — the `Transport` byte-moving seam + an in-memory two-peer test double
- `friend/` — the persisted `FriendRecord`

Run the tests:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # any JDK 17+
./gradlew :core:test
```

18 tests pass, including the official BIP39 spec vector, deterministic identity
recovery, a full two-peer handshake (both sides derive the same root key and a
matching SAS), and the MITM/forgery abort path.

### `:app` — Android client (compiles to an installable APK ✅)
Wires `:core` to real BLE. `data/` (Keystore vault + Room store), `proximity/`
(advertiser, scanner, GATT-backed `Transport`, central `GattClient`),
`friendship/` (orchestrator), `ui/` (Compose onboarding, nearby, SAS-confirm, friends).

Build the debug APK (package `net.tenbo.hubble`):

```bash
export ANDROID_HOME=$HOME/android-sdk            # SDK with platform-35 + build-tools;35.0.0
export JAVA_HOME=$HOME/jdks/jdk-17.0.19+10       # a FULL JDK 17 (needs javac AND jlink)
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Toolchain notes: AGP 8.5 needs a complete JDK 17 — a JRE (no `javac`) or a JetBrains
Runtime (no `jlink`) will fail the `JdkImageTransform`. `local.properties` holds
`sdk.dir`; the JDK path goes in `~/.gradle/gradle.properties` so the project stays portable.

Remaining on-device work (see plan Task 14–16): the peripheral-role `GattServer`,
runtime BLE permission flow, wiring `BleScanner.scan()` + `GattClient.connect()`
into the ViewModel, SQLCipher key wrapping, and the manual two-device verification.

### `server/` — Elixir discovery server (privacy core tested ✅)
Plain OTP app holding the privacy-preserving heatmap logic (no DB/network deps),
so the security-critical aggregation is fully unit-testable.

- `lib/hubble/geo.ex` — spec-correct base32 geohash encode + prefix coarsening
- `lib/hubble/heatmap.ex` — pure aggregation: TTL expiry → coarse bucketing → k-anonymity suppression
- `lib/hubble/heatmap/server.ex` — GenServer holding anonymous beacons + periodic sweep

Run the tests:

```bash
export PATH="$HOME/.elixir-install/installs/elixir/1.17.3-otp-27/bin:$HOME/.elixir-install/installs/otp/27.1.2/bin:$PATH"
cd server && mix test
```

12 tests pass, including the canonical geohash reference vector and the
k-anonymity privacy floor. Next layer: a Phoenix endpoint (`POST /beacon`,
`GET /heatmap`) and, for Sub-project C, WebRTC signaling via Channels +
`Phoenix.Presence` and the encrypted store-and-forward relay.
