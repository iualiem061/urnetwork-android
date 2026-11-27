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
15. **Relays hub removed (v2026.11.28-1)** — the shared relay network and the
    relays hub shipped in v2026.11.27-2 were **removed** at the owner's
    request; the fetch/caching/RTT machinery went with it. In its place,
    Settings carries a **Proxy mode** row (حالت پروکسی) that opens the VLESS
    proxy settings — the enable switch, the pasted `vless://` link, the
    manual form, and the saved-servers library (import links or a
    subscription, tap to activate, badge on the active one). The screen's
    title reads **Proxy mode**; the library is "saved servers".
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
18. **Two more navigation styles (v2026.11.28-1)** — beside Classic,
    Morphing and Capsule, Settings → the navigation-style row now offers
    **Neon** (نئون): a floating icon bar whose selected tab carries a soft
    neon aura of light that breathes and glides from tab to tab with the
    same springy glide, and **Bubble** (حبابی): a floating bar of round
    bubbles where the chosen one inflates with a springy overshoot into a
    pill carrying the tab's name inside, lifting a touch above its
    siblings. Five styles in all, each with its press bounce and haptic
    tick.
19. **Every location lists without a search (v2026.11.28-1)** — the
    network's provider-locations answer carries regions and cities too,
    but the SDK's grouping showed them only while searching (the reason
    most of the 108 locations were invisible until searched). The build's
    workflow patches the SDK at the pinned ref so the idle list shows
    **every country, region and city** the network carries, each row with
    the country's **full flag** (regions included, all one treatment),
    country rows named in the app's language («آلمان» on a Persian
    interface), a line under the search box counting the locations
    (۱۰۸ لوکیشن), and a **refresh-servers** button that re-asks the
    network and spins while the answer is away. The connect card's
    location row and the ip check's selected-location line read the same
    localized name as the list.
20. **Persian plurals (v2026.11.28-1)** — the fa translation shipped no
    plurals, so every plural string fell back to English inside the
    Persian UI ("1 provider" and friends). values-fa now carries the
    Persian forms of every plural the base file declares (39 + the new
    locations count), and the two English style names in the picker read
    Persian: مورفینگ and کپسول.

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
