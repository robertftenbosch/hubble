# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`DEVELOPMENT.md` is the long-form developer guide (toolchain quirks, on-device dev loop, troubleshooting table, roadmap). Read it before non-trivial work; this file is the cheat sheet.

## What this is

Hubble is a peer-to-peer **proximity dating app**: connections can only form between devices that have been physically near each other (Bluetooth LE). Every content key traces back to a key established in person. The server holds only opaque data — a coarse k-anonymous heatmap and end-to-end-encrypted "envelopes" routed by unlinkable mailbox ids.

## Modules

Four Gradle modules (`net.tenbo.hubble.*`) plus an Elixir/OTP server:

- `:core` — pure JVM Kotlin. **Root of trust.** All security-critical, transport-agnostic logic (crypto, BIP39 identity, BLE friend handshake, typed E2E messaging, self-sync, the `Transport` seam). No Android, no network deps. Must stay that way so it remains unit-testable without hardware.
- `:app` — Android client. Thin adapters around `:core`: BLE radio (`proximity/`), WebRTC + signaling (`p2p/`), SQLCipher Room + Keystore vault (`data/`), Compose UI (`ui/`), HTTP (`net/`), local-poll notifications (`notify/`).
- `:desktop` — Compose for Desktop companion (MSN-Messenger skin). Reuses `:core` unchanged; receives matches from the phone via self-sync; talks to the relay over HTTP.
- `:demo` — runnable JVM program exercising the whole concept (handshake → seal → deliver → decrypt) with no hardware.
- `server/` — Elixir/OTP (Bandit + Plug + WebSocket): privacy core (`hubble/heatmap*`, `hubble/relay*`, `hubble/signaling*`) + HTTP/WebSocket API at `:4000`.

### Architectural seam

`core/transport/Transport` is the byte-moving interface. BLE GATT, the WebRTC data channel, and the in-memory test double all implement it; handshake/messaging code never knows which. **New transports plug in here; never thread protocol knowledge down into transports.**

## Build & test

### Toolchain requirements
- **Full JDK 17** (not a JRE, not JetBrains Runtime) — AGP's `JdkImageTransform` needs `jlink`. Temurin works.
- Android SDK with platform-35 + build-tools 35.0.0 (`ANDROID_HOME`).
- Elixir 1.17 / OTP 27 (server only).

Machine-specific config (not committed): `local.properties` (`sdk.dir=...`) and `~/.gradle/gradle.properties` (`org.gradle.java.installations.paths=...`).

### Common commands

```bash
# Pure-JVM core — the security-critical tests. Run before commits touching core/.
./gradlew :core:test

# Run a single core test (Gradle test filter)
./gradlew :core:test --tests "net.tenbo.hubble.core.handshake.HandshakeTest"

# Concept demo — no hardware needed
./gradlew :demo:run

# Android APK (needs ANDROID_HOME + full JDK 17)
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk  (package: net.tenbo.hubble)

# Desktop app
./gradlew :desktop:run                                # launches the window
./gradlew :desktop:packageDistributionForCurrentOS    # .deb / .dmg / .msi
./gradlew :desktop:keychainCheck                      # round-trips the OS keychain integration

# Inject a sealed test chat into a phone's mailbox to verify notifications
./gradlew :demo:inject -PmailboxId=<id> [-PbaseUrl=...]

# Elixir server
cd server && mix test            # heatmap, relay, HTTP API, signaling
cd server && mix run --no-halt   # serves :4000 (override with PORT=...)
```

### On-device dev loop

```bash
cd server && mix run --no-halt          # 1. start relay/signaling on :4000
adb reverse tcp:4000 tcp:4000           # 2. bridge phone -> host
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n net.tenbo.hubble/.app.ui.MainActivity
adb logcat -s HubbleProximity HubbleSync   # BLE + sync logs
```

The app defaults to `HubbleApi(baseUrl = "http://127.0.0.1:4000")` with cleartext allowed (dev only).

## Invariants — do not break

1. **`core` has no Android or network deps.** Only Bouncy Castle + coroutines. Anything else belongs in `:app`, `:desktop`, or `server/`.
2. **The server never sees identity keys or plaintext.** Heatmap is k-anonymous + TTL'd; relay forwards opaque envelopes keyed by `mailboxId = HKDF(recipient signing key)`. Don't add server endpoints that would change this.
3. **Every content key traces to a root key established in person** (the BLE friend handshake's X25519 ECDH → HKDF → root key, gated by a 5-emoji SAS the humans compare). No back doors, no key escrow.
4. **The recovery phrase is the crown jewel.** Keystore-encrypted on Android; OS keychain (macOS Keychain / libsecret / DPAPI) on desktop with an owner-only (`0600`) file fallback.
5. **Security-critical logic gets tests in `core` or `server` before the adapter wires it up.** TDD is the norm — see the existing `core/.../*Test.kt` and `server/test/`.

## Code organization quick map

| Concern | Path |
|---|---|
| Crypto (X25519/Ed25519/HKDF/BLAKE2b/AES-GCM) | `core/.../crypto/` |
| Identity + BIP39 recovery | `core/.../identity/` |
| Friend handshake (state machine + SAS) | `core/.../handshake/` |
| Typed E2E messaging (post / chat / typing), mailbox routing, `SelfSync` | `core/.../message/` |
| `Transport` seam | `core/.../transport/` |
| BLE radio (advertise / scan / GATT) | `app/.../proximity/` |
| WebRTC data channel + signaling client | `app/.../p2p/` |
| Encrypted Room (SQLCipher) + Keystore vault | `app/.../data/` |
| Compose UI + bottom-bar nav | `app/.../ui/` |
| Desktop logic / MSN skin | `desktop/.../HubbleClient.kt`, `Msn*.kt` |
| Heatmap (geohash + k-anonymity) | `server/lib/hubble/heatmap*` |
| Relay (store-and-forward) | `server/lib/hubble/relay*` |
| WebRTC signaling (WebSocket) | `server/lib/hubble/signaling*` |
| HTTP routes | `server/lib/hubble/api/router.ex` |

## Conventions

- All Kotlin lives under `net.tenbo.hubble` (`.core` / `.app` / `.desktop` / `.demo`).
- Plugin versions are declared once in the root `build.gradle.kts` (`apply false`); modules apply without a version. Don't duplicate versions.
- JVM target is 17 everywhere; align `sourceCompatibility` + `JvmTarget.JVM_17` together if you touch a module's build script.
- Conventional-commit style for messages: `feat(core): …`, `fix(app): …`.
