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
3. **Two tabs** — the bottom bar is Connect and Account. Leaderboard,
   Earnings/Top 200, Referrals, Support/Feedback and Balance codes screens
   and their entries were removed.
4. **Locations** — country rows show circular flags
   (VectorDrawables converted from the [circle-flags](https://github.com/HatScripts/circle-flags)
   collection, MIT © HatScripts), Best available shows a globe, and a
   **Test ping** action measures the TCP handshake round trip to
   URnetwork's ingress resolver of each listed country
   (countries without an ingress endpoint show `–`).
5. **Persian (فارسی) + language picker** — full fa translation
   (res/values-fa), per-app language selection (Settings → Language) with
   **System default** and every shipped language, applied through
   AppCompatDelegate with `autoStoreLocales`, plus `locales_config` for
   Android 13+ per-app language settings.

## Build

The target flavor is **github** (the direct APK, no Play services):

```
./gradlew clean buildSdk assembleGithubRelease
```

See the `Build universal APK` GitHub Actions workflow in
`.github/workflows/` (added by this fork) — it clones the Go SDK sibling
repos at pinned refs, builds the AAR and the universal APK, signs it with
the repo's release keystore (from Actions secrets) and publishes a GitHub
Release.

Note: the play/solana_dapp/ethos_dapp flavors still reference removed
symbols and do not compile until adapted; only the github flavor is
built and supported by this fork.

## Licenses

- Upstream app: MPL-2.0 (see LICENSE).
- Circle flags: MIT, © 2021-2025 HatScripts
  (https://github.com/HatScripts/circle-flags) — res/drawable/flag_*.xml
  are derived from that collection.
