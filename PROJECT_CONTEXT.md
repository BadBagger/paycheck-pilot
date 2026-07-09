# Paycheck Pilot Project Context

## App

- Name: Paycheck Pilot
- Package: `com.paycheckpilot`
- Repo: `https://github.com/BadBagger/paycheck-pilot`
- Current release target: `v1.0.11-setup-input-fix`
- Previous release: `v1.0.10-setup-entry-fix`
- Release APK assets target: `PaycheckPilot.apk`, `PaycheckPilot-release-v1.0.11-setup-input-fix.apk`
- Release signing: local-only `keystore.properties`; release builds use the Smithware outside-Play release key for `com.paycheckpilot`.
- Release SHA-256: `950e5e9346644cd910112f53e244cb4582059155bf65aaaaacd623bebd94873f`

## Product Boundaries

- Local-first paycheck and bills planning.
- No login, cloud sync, paid APIs, or Play publishing automation in this release.
- Keep calendar behavior stable: selectable days should show daily details and date conversion must preserve the intended local date.

## DevHub

DevHub should list Paycheck Pilot with package `com.paycheckpilot`, repo `BadBagger/paycheck-pilot`, pinned release `v1.0.11-setup-input-fix`, and asset `PaycheckPilot.apk`.

## Current Update

- Setup input hotfix.
- Replaces onboarding money/hour text boxes with direct editable Compose text fields to avoid first-screen input lockups.
- Keeps the previous setup save-state navigation fix and currency-tolerant money parsing.
