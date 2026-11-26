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
3. **Two tabs** — the bottom bar is Connect and Account, the standard
   full-width navigation suite (bar on phones, rail on wide windows; the
   floating glass bar shipped in v2026.11.26-2 was reverted at the owner's
   request). Leaderboard, Earnings/Top 200, Referrals, Support/Feedback
   and Balance codes screens and their entries were removed.
4. **Locations** — country rows show circular flags rendered from the
   **standard flag art** (the Wikimedia-based designs flagcdn.com serves,
   as center-square-cropped WebP drawables so the true aspect, emblems and
   colors are exact — `res/drawable/flag_*.webp`, 252 of 265 codes; the
   residual non-ISO/retired codes keep the old circle-flags vector). Best
   available shows a globe, and the connect screen's location card shows
   the **selected country's flag** instead of the globe. The per-country
   ping test was **removed**: the sdk names a regional resolver endpoint
   for only a handful of countries (cn/ru/ir/kz), so a pre-connection
   "real latency" test cannot cover the list's countries.
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
