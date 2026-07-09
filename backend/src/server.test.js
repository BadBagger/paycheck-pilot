import assert from "node:assert/strict";
import test from "node:test";
import { addDays, dayDiff, detectPaychecks, detectRecurringBills, emptyStore, encryptToken, normalizeMerchant, paycheckSummary, seedMockData } from "./server.js";

test("date helpers calculate recurring gaps", () => {
  assert.equal(dayDiff("2026-01-01", "2026-01-15"), 14);
  assert.equal(addDays("2026-01-01", 14), "2026-01-15");
});

test("normalizes merchant keys", () => {
  assert.equal(normalizeMerchant("  Payroll   Deposit "), "payroll deposit");
});

test("mock seed creates encrypted token and paycheck summary", () => {
  const store = emptyStore();
  seedMockData(store, "user-1");
  const encrypted = Object.values(store.connectedInstitutions)[0].encryptedAccessToken;
  const transactions = Object.values(store.bankTransactions);
  assert.ok(encrypted);
  assert.equal(encrypted.includes("mock-plaid-token-placeholder"), false);
  assert.ok(transactions.some((item) => item.merchantName === "PUBLIX PAYROLL"));
  assert.ok(transactions.some((item) => item.merchantName === "ACME PAYROLL"));
  assert.ok(transactions.some((item) => item.merchantName === "DOORDASH"));
  assert.ok(transactions.some((item) => item.merchantName === "Netflix"));
  assert.ok(transactions.some((item) => item.merchantName === "Gas Station"));
});

test("detects paycheck deposits separately from recurring bills", () => {
  const txs = [
    tx("pay-1", "Payroll Deposit", -85000, "2026-01-03", "INCOME"),
    tx("pay-2", "Payroll Deposit", -85000, "2026-01-17", "INCOME"),
    tx("rent-1", "Rent", 85000, "2026-01-01", "RENT"),
    tx("rent-2", "Rent", 85000, "2026-02-01", "RENT")
  ];
  const accounts = [{ id: "acct-1", name: "Checking" }];
  const paychecks = detectPaychecks(txs, accounts);
  const bills = detectRecurringBills(txs, accounts);
  assert.equal(paychecks[0].payerName, "Payroll Deposit");
  assert.equal(paychecks[0].cadence, "Biweekly");
  assert.equal(bills.length, 1);
  assert.equal(bills[0].name, "Rent");
});

test("encrypts tokens", () => {
  const encrypted = encryptToken("access-secret-token");
  assert.notEqual(encrypted, "access-secret-token");
  assert.equal(encrypted.includes("access-secret-token"), false);
});

function tx(id, merchantName, amountCents, date, category) {
  return {
    id,
    transactionId: id,
    userId: "user-1",
    institutionId: "inst-1",
    plaidAccountId: "acct-1",
    merchantName,
    originalDescription: merchantName,
    amountCents,
    currency: "USD",
    date,
    authorizedDate: date,
    pending: false,
    category,
    paymentChannel: "online"
  };
}
