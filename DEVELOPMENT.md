# Hubble — Developer Guide

Everything you need to build, test, run, and extend Hubble. For the product vision and
status, see [`README.md`](README.md); for design specs and plans, see
[`docs/superpowers/`](docs/superpowers/).

---

## 1. What Hubble is (one paragraph for new contributors)

A peer-to-peer **proximity dating app**: you can only connect with people you've physically
crossed paths with (Bluetooth LE). Every encryption key originates from that in-person
meeting, and the server only ever sees opaque data — a coarse, k-anonymous activity heatmap
and end-to-end-encrypted "envelopes" routed by unlinkable mailbox ids. The product loop is
**cross paths → like → match → chat** (with ephemeral stories on the side). The guiding
invariant: **the server can never read content or locate an individual.**

Clients: an **Android app** (editorial design system) and a **desktop companion**
(Compose for Desktop, deliberately skinned like classic **MSN Messenger**). New connections
are always made in person on the phone; the desktop is a network-participating companion
that shares the same identity via the recovery phrase + self-sync.

---

## 2. Repository layout

```
hubble/
├── core/        Pure-JVM Kotlin library — ALL security-critical, transport-agnostic logic.
│               No Android, no network. The root of trust. (25 tests)
├── app/         Android client (Kotlin + Jetpack Compose, editorial design system).
├── desktop/     Compose for Desktop companion (Linux/Mac/Windows), MSN-Messenger skin.
├── demo/        Runnable JVM program that exercises the whole concept without hardware.
├── server/      Elixir/OTP discovery + relay + signaling server (HTTP + WebSocket). (23 tests)
└── docs/superpowers/   specs/ (designs) and plans/ (implementation plans).
```

### Module dependency rule
`app`, `desktop`, and `demo` depend on `core`. **`core` depends on nothing** (only Bouncy
Castle + coroutines). Keep it that way — it is what makes the hard logic testable without a
phone, and it is exactly why the desktop client could reuse it unchanged.
The seam is the `Transport` interface (`core/transport`): bytes in, bytes out. BLE, WebRTC,
and the in-memory test double all implement it; the handshake/messaging logic never knows
which.

### Where the important code lives
| Concern | Path |
|---|---|
| Crypto primitives (X25519/Ed25519/HKDF/BLAKE2b/AES-GCM) | `core/.../crypto/` |
| Identity + BIP39 recovery | `core/.../identity/` |
| BLE friend handshake (state machine + SAS) | `core/.../handshake/` |
| E2E messaging — typed seal/open (post · chat · typing), mailbox routing, `SelfSync`, `MatchSnapshot` | `core/.../message/` |
| BLE radio (advertise/scan/GATT) | `app/.../proximity/` |
| WebRTC data channel + signaling client | `app/.../p2p/` |
| Local storage (Keystore vault + SQLCipher Room: friends/posts/encounters/matches/messages) | `app/.../data/` |
| HTTP client | `app/.../net/` |
| Compose UI (onboarding, discovery, matches, chat, map, profile) + ViewModel + bottom-bar nav | `app/.../ui/` |
| Desktop app logic (restore, sync, chat, heatmap, typing, sounds) | `desktop/.../HubbleClient.kt` |
| Desktop MSN skin (buddy list, chat, corner toast) | `desktop/.../Msn.kt`, `MsnUi.kt` |
| Heatmap (geohash + k-anonymity) | `server/lib/hubble/heatmap*` |
| Relay (store-and-forward) | `server/lib/hubble/relay*` |
| WebRTC signaling (WebSocket) | `server/lib/hubble/signaling*` |
| HTTP API + routes | `server/lib/hubble/api/router.ex` |

All Kotlin is under package `net.tenbo.hubble` (`…app` / `…desktop` / `…core`).

---

## 3. Toolchain prerequisites

| Tool | Version | Notes |
|---|---|---|
| **JDK** | **17, full JDK** | Must include `javac` AND `jlink` (AGP's `JdkImageTransform` needs jlink). A JRE or a JetBrains Runtime will fail the Android build. Temurin 17 works. |
| Android SDK | platform-35, build-tools 35.0.0, platform-tools | Set `ANDROID_HOME`. |
| Gradle | wrapper (8.10) | Use `./gradlew`; do not require a system Gradle. |
| Elixir / OTP | 1.17 / OTP 27 | For `server/`. |
| Node (optional) | any | Not currently required. |

### Machine-specific config (not committed)
- `local.properties` → `sdk.dir=/path/to/android-sdk`
- `~/.gradle/gradle.properties` → `org.gradle.java.installations.paths=/path/to/jdk-17`
  (keeps the project's `gradle.properties` portable)

Example environment for a build session:
```bash
export ANDROID_HOME="$HOME/android-sdk"
export JAVA_HOME="$HOME/jdks/jdk-17.0.19+10"   # a FULL JDK 17
export PATH="$JAVA_HOME/bin:$PATH"
```

---

## 4. Build, test, run — by module

### `:core` (pure JVM — no SDK needed)
```bash
./gradlew :core:test          # 25 tests: crypto, BIP39 vector, handshake, messaging, self-sync
```

### `:demo` (runnable concept demo — no SDK, no devices)
```bash
./gradlew :demo:run
```
Prints the full path: two simulated phones meet → handshake → matching SAS → shared key
→ a post is sealed into an opaque envelope → delivered → decrypted.

### `:app` (Android — needs the SDK)
```bash
./gradlew :app:assembleDebug                 # -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n net.tenbo.hubble/.app.ui.MainActivity
```
Package: `net.tenbo.hubble`. minSdk 26, target/compile 35.

### `:desktop` (Compose for Desktop — needs a display + full JDK 17)
```bash
./gradlew :desktop:run                                  # launches the window
./gradlew :desktop:packageDistributionForCurrentOS      # -> .deb / .dmg / .msi installer
```
Talks to the relay at `http://127.0.0.1:4000` (same machine — no `adb reverse`). Restore
identity with the phone's recovery phrase; the phone pushes matches via **You → Sync to my
computer** (self-sync), then chat flows over the relay. MSN skin lives in `Msn*.kt`; the
corner toast is a second always-on-top `Window`. Sounds are synthesized (`Sound.kt`,
toggleable, silent if no audio device). State persists under `~/.hubble/`.

### `server/` (Elixir)
```bash
cd server
mix deps.get
mix test                       # 23 tests: geohash, k-anon heatmap, relay, HTTP API, signaling
mix run --no-halt              # HTTP + WebSocket on :4000 (override with PORT=...)
```

---

## 5. Running the app against a local server (on-device dev)

The phone can reach a server running on your dev machine over USB with `adb reverse`:

```bash
# 1. start the server
cd server && mix run --no-halt        # listens on localhost:4000

# 2. bridge the phone's localhost:4000 to the dev machine
adb reverse tcp:4000 tcp:4000

# 3. (re)install + launch the app; it calls http://127.0.0.1:4000 by default
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n net.tenbo.hubble/.app.ui.MainActivity
```
The app's base URL is `HubbleApi(baseUrl = "http://127.0.0.1:4000")`; point it at a real
host for production. (Cleartext HTTP is allowed via `usesCleartextTraffic` for dev only.)

Useful checks:
```bash
adb logcat -s HubbleProximity HubbleSync          # BLE + sync logs
curl -s localhost:4000/heatmap                     # server state
```

---

## 6. Security & protocol model (read before touching crypto)

1. **Identity** = a device keypair derived deterministically from a **BIP39 recovery
   phrase** (the only backup). Ed25519 for signing (the public-key fingerprint is the
   "Hubble ID"); X25519 for key agreement. Private keys are Keystore-encrypted at rest.
2. **Friending** happens only over BLE GATT, in person. Both sides run the `Handshake`
   state machine: exchange keys → X25519 ECDH → HKDF → **shared root key**; both compare a
   5-emoji **SAS** (defeats relay/MITM) and sign the transcript. Output: a `FriendRecord`
   holding the root key.
3. **Messaging**: a payload (post, chat, or a typing ping — a 1-byte type tag) is signed
   (Ed25519) then encrypted (AES-256-GCM, key = HKDF(rootKey)). It's wrapped in an
   `Envelope` with opaque routing tags (`mailbox id = HKDF(recipient signing key)`,
   `senderTag = HKDF(sender signing key)`). The server stores/forwards only ciphertext + tags.
4. **Multi-device (self-sync)**: a device encrypts its own data (e.g. matches) to itself
   (`SelfSync`: key + mailbox id both derived from the identity) and drops it in a self-
   mailbox. Any device that restores the same recovery phrase can pull + decrypt; nobody
   else can. This is how matches made on the phone reach the desktop.

**Invariants to preserve:** the server must never receive identity keys or plaintext;
every content key must trace back to a root key established in person; `core` must remain
free of Android/network deps so it stays unit-testable. The recovery phrase is the crown
jewel — Keystore-encrypted on Android, owner-only (`0600`) on desktop (OS-keychain is a TODO).

---

## 7. Testing philosophy

- **`core` and `server` hold the security-critical logic and are tested in isolation**
  (JVM JUnit / ExUnit) — no hardware, no network. New crypto/protocol/aggregation logic
  belongs here with tests.
- BLE and WebRTC are thin adapters behind `Transport`; verify them on devices.
- TDD is the norm (see `docs/superpowers/plans/`): write the failing test, then the code.
- Run `./gradlew :core:test` and `cd server && mix test` before every commit that touches
  those modules.

---

## 8. Contributing a change (typical loop)

1. Find or write the design under `docs/superpowers/specs/` for non-trivial work.
2. Put security/protocol logic in `core` (or `server`) with tests; keep adapters thin.
3. `./gradlew :core:test && (cd server && mix test)` — green before proceeding.
4. For app changes, build + deploy to a device and check `logcat`.
5. Small, focused commits; conventional-commit style (`feat(core): …`, `fix(app): …`).

---

## 9. Troubleshooting (gotchas already hit)

| Symptom | Cause / fix |
|---|---|
| `jlink executable … does not exist` | `JAVA_HOME` is a JRE or JetBrains Runtime. Use a **full JDK 17** (Temurin). |
| `Cannot find a Java installation … languageVersion=21` | Toolchain auto-detection can't find a JDK; set `org.gradle.java.installations.paths` in `~/.gradle/gradle.properties`. |
| `Inconsistent JVM-target compatibility (compileJava vs compileKotlin)` | Align both to 17 (the modules already pin `JvmTarget.JVM_17` + `sourceCompatibility 17`). |
| `Plugin [com.android.application] not found` | Missing `pluginManagement { repositories { google() … } }` in `settings.gradle.kts`. |
| `android.useAndroidX property not enabled` | Set `android.useAndroidX=true` in `gradle.properties`. |
| `socket failed: EPERM` from the app | Missing `<uses-permission android:name="android.permission.INTERNET"/>`. |
| adb shows `no permissions` for the phone | Add a udev rule for vendor `18d1` (`MODE="0666"`), reload, replug. |
| adb shows `unauthorized` | Accept the "Allow USB debugging" prompt on the phone. |
| App can't reach the server | Re-run `adb reverse tcp:4000 tcp:4000`; confirm the server is up (`curl localhost:4000/health`). |
| Desktop sounds silent | No audio device (e.g. headless) — `Sound.kt` no-ops by design; also check the in-app Sounds toggle. |
| Desktop shows no matches | Restore the *same* recovery phrase as the phone, then on the phone tap **You → Sync to my computer**. |
| `:desktop:run` fails to open a window | Needs a display (`DISPLAY` set) and a full JDK 17; packaging needs `jpackage` (in the JDK). |

---

## 10. Deploying the server to production

**Stack: `mix release` on any small Linux VPS (Hetzner / Linode / OVH / Scaleway all fine) +
systemd to keep it alive + Caddy in front for TLS.** The server has no database and no
third-party services; a €5/month box is overprovisioned. For EU jurisdiction Hetzner is the
default pick; for the same shape elsewhere Linode is fine.

### Design choice: state stays in RAM
Heatmap counts and relay envelopes live in `GenServer` process state — a restart wipes
everything. That is intentional. The privacy invariant from §6 says the server can never
read content or locate an individual; persistence would create a forensic surface. The same
mailbox id hitting the same client repeatedly is enough to fingerprint a friendship without
decrypting anything, and you'd be persisting exactly that pattern. The cost of staying RAM-
only is small: envelopes deposited during the ~1 s restart window are lost (sender has
already discarded them, receiver hasn't polled yet). Push releases off-peak; if the loss
ever bites in practice the fix is client-side retry, not server-side persistence.

(Aside: if you ever do need to persist — e.g. moving from one relay node to several behind
a load balancer — start with **DETS** for the relay only, one Erlang call, no schema, no
migrations. Mnesia is *bundled* with OTP, not *simple*: schema setup, `disc_copies` vs
`ram_copies`, dump intervals, net-split recovery. Don't reach for it before you have to.)

### Server prep (once)
- VPS with inbound 22 (SSH), 80 (Let's Encrypt challenge), 443 (HTTPS) open. Nothing else.
- Erlang/OTP 27 + Elixir 1.17 (only to **build** the release; the tarball ships its own OTP,
  so you can also build elsewhere and `scp` the `_build/prod/rel/hubble` tree).
- `apt install caddy` — TLS via automatic Let's Encrypt, transparent WebSocket reverse proxy.
- `useradd --system --shell /usr/sbin/nologin --home-dir /opt/hubble hubble`.
- `git clone` the repo to `/opt/hubble`. Server lives in `/opt/hubble/server`.

### Runtime config
`config/config.exs` is compile-time and fine for dev/test. For a release add
`config/runtime.exs` so `PORT` is read at boot, not frozen at build time:
```elixir
# server/config/runtime.exs
import Config
if config_env() == :prod do
  config :hubble, :port, String.to_integer(System.get_env("PORT") || "4000")
end
```
(Already present in this repo.)

### Build the release
```bash
cd /opt/hubble/server
MIX_ENV=prod mix deps.get
MIX_ENV=prod mix release
# -> _build/prod/rel/hubble/bin/hubble
```

### systemd
`/etc/systemd/system/hubble.service`:
```ini
[Unit]
Description=Hubble discovery + relay server
After=network.target

[Service]
Type=simple
User=hubble
WorkingDirectory=/opt/hubble/server
Environment=PORT=4000
Environment=RELEASE_COOKIE=<long-random-string>
Environment=LANG=en_US.UTF-8
ExecStart=/opt/hubble/server/_build/prod/rel/hubble/bin/hubble start
Restart=on-failure
TimeoutStopSec=10
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```
`systemctl enable --now hubble`. Logs via `journalctl -u hubble -f`. SIGTERM from systemd
triggers the OTP shutdown so GenServers get a chance to drain.

### Caddy
`/etc/caddy/Caddyfile`:
```
hubble.tenbo.app {
	encode zstd gzip
	reverse_proxy 127.0.0.1:4000
}
```
Caddy auto-acquires the certificate, redirects 80 → 443, and proxies WebSockets
(`/signal/:id`) transparently. `systemctl reload caddy`.

### Don't put a CDN in front of Caddy
The router uses `Hubble.Api.SafeLogger` (not `Plug.Logger`); it collapses `/mailbox/:id` and
`/signal/:id` to `/mailbox/_` and `/signal/_` before they reach the log line — so the access
log can never link an IP to a mailbox id. **A Cloudflare or any other TLS-terminating proxy
in front of Caddy would defeat that** (its access log would see the raw paths). Caddy must
be the edge.

### Client switch
Once HTTPS is live, point the clients at the real host and drop cleartext:
- `app/.../net/HubbleApi.kt` → `baseUrl = "https://hubble.tenbo.app"`
- `app/src/main/AndroidManifest.xml` → remove `android:usesCleartextTraffic="true"`
- `desktop/.../HubbleClient.kt` and anywhere else hardcoding `http://127.0.0.1:4000` → same.
Then ship: `./gradlew :app:assembleRelease` and `./gradlew :desktop:packageDistributionForCurrentOS`.

### Verify
```bash
curl -s https://hubble.tenbo.app/health        # {"ok":true}
journalctl -u hubble -n 20 | grep mailbox            # only "/mailbox/_", never a raw id
```

---

## 11. Roadmap (open work)

### Done (recent)
- Dating pivot (hybrid): discovery → like → match → chat, editorial design system.
- Bottom-bar navigation; keyboard dismiss on send.
- Direct WebRTC data channel + signaling (`app/p2p/`, server `/signal/:id`).
- SQLCipher at-rest DB encryption (`hubble-secure.db`).
- Heatmap "where it's alive" — **OpenStreetMap (osmdroid)** in-app with k-anon circle
  overlays; **real coarse-location** beacons ("Check in here") + desktop check-in chips.
- Chat over the network (relay); self-sync (phone → desktop matches).
- Desktop companion + MSN skin; **status selector**, **sign-in/message sounds** (toggleable),
  **"is typing…"**, **nudge-shake**, classic **emoticons**.
- Safety: **unmatch / block / report** (blocked people hidden + their envelopes dropped).
- Desktop recovery phrase now lives in the **OS keychain** (macOS Keychain / libsecret /
  Windows DPAPI, via `desktop/Keychain.kt`); falls back to the owner-only file only when no
  secret store is usable, and migrates an existing plaintext file in on first read. Verify
  with `./gradlew :desktop:keychainCheck`.
- **Local push notifications** for new messages (no FCM/Google push): a WorkManager job polls
  our own opaque mailbox (~15 min) plus an immediate catch-up poll on app open, and raises a
  per-match notification (`app/notify/`). Verified on the Pixel end to end — a sealed test chat
  from a match (`./gradlew :demo:inject -PmailboxId=…`) rendered as a "Mara" notification, while
  messages from non-matches were dropped (0 notifications), exactly as the safety path intends.

### Open work
- **Two-device session** (needs a second Android handset / emulator) — do these together:
  1. verify the BLE friend handshake + full match → chat round trip between two real devices;
  2. **wire chat/typing over the WebRTC data channel** for instant delivery (relay fallback).
     Plumbing + verification belong together — deferred here by decision.
- Production server **deploy + TLS** (replace the `adb reverse` loopback + cleartext); store
  release. Optional later: instant **FCM push** (vs. the current privacy-first ~15-min poll).
