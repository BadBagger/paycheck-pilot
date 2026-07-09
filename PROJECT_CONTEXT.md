# Paycheck Pilot Project Context

## App

- Name: Paycheck Pilot
- Package: `com.paycheckpilot`
- Repo: `https://github.com/BadBagger/paycheck-pilot`
- Current release target: `v1.0.8-premium-bank-sync-plan`
- Previous release: `v1.0.7-demo-financial-data`
- Release APK assets target: `PaycheckPilot.apk`, `PaycheckPilot-release-v1.0.8-premium-bank-sync-plan.apk`
- Release signing: local-only `keystore.properties`; release builds use the Smithware outside-Play release key for `com.paycheckpilot`.
- Release SHA-256: `950e5e9346644cd910112f53e244cb4582059155bf65aaaaacd623bebd94873f`

## Product Boundaries

- Local-first paycheck and bills planning.
- No login, cloud sync, paid APIs, or Play publishing automation in this release.
- Keep calendar behavior stable: selectable days should show daily details and date conversion must preserve the intended local date.

## DevHub

DevHub should list Paycheck Pilot with package `com.paycheckpilot`, repo `BadBagger/paycheck-pilot`, pinned release `v1.0.8-premium-bank-sync-plan`, and asset `PaycheckPilot.apk`.

## Current Update

- Adds a fair mock Premium gate plan for bank/card sync while keeping manual paycheck planning free.
- Free users keep manual paycheck setup, manual bills, manual safe-to-spend, payday countdown, basic reminders, and demo financial previews.
- Mock Premium unlocks real bank/card connect and sync testing for automatic paycheck detection, recurring bill detection, auto safe-to-spend, alerts/watch-outs, Renewal Radar sharing, reports, backup/export, style packs, and widgets.
