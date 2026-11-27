# Custom build notes

This fork of the URnetwork Android app is a customized community build.
The changes below were applied on top of upstream `main` (2026-11-24).

## What changed

1. **Sign-in** — the login screen offers only **Create Instant Account**
   (instant, seedphrase-backed account). SSO (Google/Apple), wallet sign-ins
   (Solana, Bittensor), the auth-code and seedphrase tiles, the email field
   and the network API selector were removed from the login screen
   (the underlying flows remain in the code but are unreachable).
2. **No subscriptions** — every purchase and plan surface is gone: the
   upgrade route and screens, plan cards, Get Pro/Get Plus banners, gift
   and redeem-code UI, purchase dialogs and the onboarding sales funnel.
   Data still refreshes with the daily free grant.
3. **Three tabs + navigation styles** — the bottom bar is Connect, Account
   and **IP check** (v2026.11.26-4), with three selectable styles in
   **Settings → Navigation style** (سبک ناوبری): the classic full-width
   suite (default; bar on phones, rail on wide windows), a floating
   **Morphing** pill bar whose highlight glides between tabs with a spring
   while the chosen icon pops, and an icon-only floating **Capsule** whose
   circular fill springs in and whose name pill swaps with a soft slide
   (v2026.11.27-1). The vertical **Side Dock** of v2026.11.26-4 was
   **removed** at the owner's request — a saved Side Dock choice falls back
   to Classic. Tab switches crossfade with a whisper of scale (all styles).
   Leaderboard, Earnings/Top 200, Referrals, Support/Feedback and Balance
   codes screens and their entries were removed.
4. **Locations** — country rows show the **full flag** (the standard
   flagcdn art at its true aspect ratio, kept un-cropped) in a **rounded
   rectangle** (28dp tall, 6dp corners, hairline border — v2026.11.26-4;
   the v3 center-square-crop circles were replaced at the owner's request):
   `res/drawable/flag_*.webp`, 252 of 265 codes; the residual
   non-ISO/retired codes keep the colored circle. Best available shows a
   globe, and the connect screen's location card shows the **selected
   country's flag** instead of the globe. The per-country ping test was
   **removed**: the sdk names a regional resolver endpoint for only a
   handful of countries (cn/ru/ir/kz), so a pre-connection "real latency"
   test cannot cover the list's countries.
5. **Persian (فارسی) + language picker** — full fa translation
   (res/values-fa), per-app language selection (Settings → Language) with
   **System default** and every shipped language, applied through
   AppCompatDelegate with `autoStoreLocales`, plus `locales_config` for
   Android 13+ per-app language settings.
6. **Dark-only appearance** — the app is always dark (the light/system
   modes and the Settings → Appearance picker shipped in v2026.11.26-2
   were reverted at the owner's request; the AppTheme semantic tokens stay
   and now always resolve to the dark palette).

7. **Social channels** — Account lists the network's **Telegram**
   (t.me/Aliem_061) and **Instagram** (instagram.com/Aliem061) channels,
   each row with the platform's own glyph in its brand color.
8. **Smaller APK** — the release build shrinks unused code and resources
   (R8 tree-shaving with names kept for the Go JNI bridge, and resource
   shrinking), and the GitHub release attaches the per-ABI APKs
   (arm64-v8a, armeabi-v7a) beside the universal one.
9. **No client-side paywalls** — every location in the network's provider
   list is selectable (the app has no plan gate), and the only account
   limits left are the ones the URnetwork **servers** enforce (bandwidth
   balance, device count), which the app cannot and does not try to
   bypass.
10. **Right-to-left layout (v2026.11.26-4)** — when the running locale is
    an RTL language (fa, ar, …), the whole app lays out right-to-left: rows
    mirror (labels at the right, chevrons and values at the left), lists
    scroll from the right, and Persian text aligns right. Technical strings
    — the social handles, the post-quantum identity key hash and client
    id, and the public IP — pin themselves as clean left-to-right runs
    (`TextDirection.Ltr`), so their characters never reorder.
11. **IP check tab (v2026.11.26-4)** — a third tab showing the app's own
    connection state (connected / connecting / disconnected, with the
    selected location and its flag) and the **public IP** with the location
    and ISP the internet reads it from (ipwho.is, api.ipify.org as the
    IP-only fallback; the read travels the tunnel while connected, so it
    names the exit node), with a re-read button.
12. **Settings trimmed (v2026.11.26-4)** — the **stay-in-touch** section
    (product-updates email preference, support email, Discord, DePIN Hub)
    was removed at the owner's request; the social channels stay in the
    Account tab. The **Use WireGuard / SOCKS / HTTPS proxy** row was also
    removed (v2026.11.27-1).
13. **VLESS relay servers (v2026.11.27-1)** — Settings → VLESS gained a
    **relay library**: the current server can be saved under its name, and
    any number of servers can be imported at once from pasted `vless://`
    links **or a subscription** — a URL whose body is the list, plain or
    base64 (fetched in-app, capped at 200, duplicates skipped, each named
    by its `#fragment` remark or its address). Tapping a saved server
    activates it at once (its link fills the form and is saved to the
    space, which applies it live), the active one carries a badge, and a
    row's trash icon removes it. The library persists in the app's
    preferences and survives restarts.
14. **App-wide motion (v2026.11.27-1)** — one soft motion kit
    (`ui/components/Motion.kt`) is shared by every screen: a press bounce
    on every button and tappable row (URButton, settings rows, the location
    selector and list rows, social rows, the nav bars, the IP check's
    re-read pill, VLESS rows), a staggered fade-and-rise cascade on
    sections and pickers (settings sections, language and navigation-style
    lists, the VLESS card, the login screen, the IP check cards), breathing
    status dots, crossfading values (connection state, public IP), sliding
    name pills, animated list placement (location rows), size-follows-
    content cards, and haptic ticks on tab taps — the "soft, clean, smooth"
    pass the owner asked for, everywhere.
15. **Shared relay network + personal relays hub (v2026.11.27-2)** —
    Settings shows one **Relays** row (the old VLESS row) that opens the
    relays hub: two sides, **Public** and **Personal**. The public side is
    a shared relay network the app carries itself: two public relay pools
    (community aggregators that publish thousands of live `vless://`
    servers, refreshed about every 15 minutes) are fetched, deduped,
    parsed with the SDK's own VLESS link parser and grouped by country —
    every location open, flags, counts, a search box, and a per-server
    **Test** pill that measures the real TCP RTT to the server's own
    address (green under 120 ms, amber under 250, red above; a dash when
    it does not answer). Tapping a server activates it live through the
    same path as the personal library. The fetched pool is cached in the
    app's preferences (instant reopen, quiet re-fetch when older than 12
    hours, a manual Refresh). The personal side is the user's own relays:
    pasted links, subscription imports and the full manual form (the
    previous VLESS editor), unchanged.
16. **Page transitions (v2026.11.27-2)** — every page change now animates
    softly: forward navigation slides the page in from the trailing edge
    (the edge the reading direction travels toward — mirrored for RTL
    Persian) with a spring settle and a quiet fade while the covered page
    slips toward the leading edge; back navigation mirrors it. The three
    top tabs keep their quiet crossfade. The screen-switch animation the
    owner asked for, without a hard cut anywhere.
17. **Settings copy (v2026.11.27-2)** — the navigation-style row lost its
    label ("سبک ناوبری"): the row now shows the style's name (e.g.
    "کلاسیک") with its own note ("the look of the bottom bar"), and the
    picker sheet lost its title too, so the two words appear nowhere.

## Build

The target flavor is **github** (the direct APK, no Play services):

```
./gradlew clean buildSdk assembleGithubRelease
```

See the `Build APKs (github flavor)` GitHub Actions workflow in
`.github/workflows/` (added by this fork) — it clones the Go SDK sibling
repos at pinned refs, builds the AAR and the APKs, signs them with the
repo's release keystore (from Actions secrets) and publishes a GitHub
Release.

Note: the play/solana_dapp/ethos_dapp flavors still reference removed
symbols and do not compile until adapted; only the github flavor is
built and supported by this fork.

## Licenses

- Upstream app: MPL-2.0 (see LICENSE).
- Flags: the standard flag art served by flagcdn.com (Wikimedia
  Commons-derived; national flags are public domain) — the
  res/drawable/flag_*.webp drawables are rendered from those designs; the
  residual flag_*.xml files for non-ISO/retired codes derive from the
  circle-flags collection (MIT, © 2021-2025 HatScripts).
- The Telegram and Instagram glyphs (res/drawable/ic_telegram.xml,
  ic_instagram.xml) are the platforms' own logo outlines in monochrome
  form, tinted with their brand colors.
