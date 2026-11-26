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
3. **Two tabs** — the bottom bar is Connect and Account, a floating,
   rounded, translucent **glass** bar on phones (a pill inset from the
   screen's edges with a hairline border, a soft shadow and a top-lit
   sheen; the navigation suite's item semantics, indicator and labels
   inside). Leaderboard, Earnings/Top 200, Referrals, Support/Feedback
   and Balance codes screens and their entries were removed.
4. **Locations** — country rows show circular flags
   (VectorDrawables converted from the [circle-flags](https://github.com/HatScripts/circle-flags)
   collection, MIT © HatScripts), Best available shows a globe, and the
   connect screen's location card shows the **selected country's flag**
   instead of the globe. The per-country ping test was **removed**: the
   sdk names a regional resolver endpoint for only a handful of countries
   (cn/ru/ir/kz), so a pre-connection "real latency" test cannot cover
   the list's countries.
5. **Persian (فارسی) + language picker** — full fa translation
   (res/values-fa), per-app language selection (Settings → Language) with
   **System default** and every shipped language, applied through
   AppCompatDelegate with `autoStoreLocales`, plus `locales_config` for
   Android 13+ per-app language settings.
6. **Light and dark appearance** — Settings → **Appearance** pins light,
   dark or System default (kept in the app's preferences, applied at
   once). The UI draws through a theme-aware palette (AppTheme tokens),
   the M3 scheme and typography follow the mode, and the status bar's
   icon contrast flips with it.
7. **Social channels** — Account lists the network's **Telegram**
   (t.me/Aliem_061) and **Instagram** (instagram.com/Aliem061) channels,
   each row with the platform's own glyph in its brand color.
8. **Smaller APK** — the release build shrinks unused code and resources
   (R8 tree-shaving with names kept for the Go JNI bridge, and resource
   shrinking), and the GitHub release attaches the per-ABI APKs
   (arm64-v8a, armeabi-v7a) beside the universal one.

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
- Circle flags: MIT, © 2021-2025 HatScripts
  (https://github.com/HatScripts/circle-flags) — res/drawable/flag_*.xml
  are derived from that collection.
- The Telegram and Instagram glyphs (res/drawable/ic_telegram.xml,
  ic_instagram.xml) are the platforms' own logo outlines in monochrome
  form, tinted with their brand colors.
