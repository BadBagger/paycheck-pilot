# Paycheck Pilot Project Context

## App

- Name: Paycheck Pilot
- Package: `com.paycheckpilot`
- Repo: `https://github.com/BadBagger/paycheck-pilot`
- Current release target: `v1.0.9-bank-sync-qa`
- Previous release: `v1.0.8-premium-bank-sync-plan`
- Release APK assets target: `PaycheckPilot.apk`, `PaycheckPilot-release-v1.0.9-bank-sync-qa.apk`
- Release signing: local-only `keystore.properties`; release builds use the Smithware outside-Play release key for `com.paycheckpilot`.
- Release SHA-256: `950e5e9346644cd910112f53e244cb4582059155bf65aaaaacd623bebd94873f`

## Product Boundaries

- Local-first paycheck and bills planning.
- No login, cloud sync, paid APIs, or Play publishing automation in this release.
- Keep calendar behavior stable: selectable days should show daily details and date conversion must preserve the intended local date.

## DevHub

DevHub should list Paycheck Pilot with package `com.paycheckpilot`, repo `BadBagger/paycheck-pilot`, pinned release `v1.0.9-bank-sync-qa`, and asset `PaycheckPilot.apk`.

## Current Update

- Full QA pass on bank sync and paycheck planning.
- Fixes duplicate manual entries when confirming detected paychecks/bills, adds exclude controls for detected paycheck/bill candidates, adds keep/delete imported-data choice on disconnect, and makes mock backend account exchange idempotent.
- Confirms release backend URL is not localhost.
