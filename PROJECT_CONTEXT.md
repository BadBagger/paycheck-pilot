# Paycheck Pilot Project Context

## App

- Name: Paycheck Pilot
- Package: `com.paycheckpilot`
- Repo: `https://github.com/BadBagger/paycheck-pilot`
- Current release target: `v1.0.7-demo-financial-data`
- Previous release: `v1.0.6-release-signed`
- Release APK assets target: `PaycheckPilot.apk`, `PaycheckPilot-release-v1.0.7-demo-financial-data.apk`
- Release signing: local-only `keystore.properties`; release builds use the Smithware outside-Play release key for `com.paycheckpilot`.
- Release SHA-256: `950e5e9346644cd910112f53e244cb4582059155bf65aaaaacd623bebd94873f`

## Product Boundaries

- Local-first paycheck and bills planning.
- No login, cloud sync, paid APIs, or Play publishing automation in this release.
- Keep calendar behavior stable: selectable days should show daily details and date conversion must preserve the intended local date.

## DevHub

DevHub should list Paycheck Pilot with package `com.paycheckpilot`, repo `BadBagger/paycheck-pilot`, pinned release `v1.0.7-demo-financial-data`, and asset `PaycheckPilot.apk`.

## Current Update

- Adds demo financial data for paycheck and spending calculations without Plaid credentials, a hosted backend, or real bank accounts.
- Demo scenarios cover weekly PUBLIX PAYROLL, biweekly ACME PAYROLL, variable DOORDASH gig income, rent, phone, electric, Netflix, gym, gas, groceries, duplicate charges, lower paycheck, missing paycheck, and bill-before-payday warnings.
- Developer controls: Use demo financial data, Reset demo data, Simulate next payday, Simulate lower paycheck, Simulate missing paycheck, and Simulate bill before payday.
