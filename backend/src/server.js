import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";

const currentFile = fileURLToPath(import.meta.url);
const __dirname = path.dirname(currentFile);
const dataDir = path.resolve(__dirname, "../data");
const storePath = path.join(dataDir, "dev-store.json");

const config = {
  port: Number(process.env.PORT || 8791),
  publicBaseUrl: process.env.BACKEND_PUBLIC_BASE_URL || "http://localhost:8791",
  plaidClientId: process.env.PLAID_CLIENT_ID || "",
  plaidSecret: process.env.PLAID_SECRET || "",
  plaidEnv: process.env.PLAID_ENV || "sandbox",
  plaidProducts: splitEnv(process.env.PLAID_PRODUCTS || "transactions"),
  plaidCountryCodes: splitEnv(process.env.PLAID_COUNTRY_CODES || "US"),
  plaidAndroidPackageName: process.env.PLAID_ANDROID_PACKAGE_NAME || "com.paycheckpilot",
  mockMode: (process.env.PLAID_MOCK_MODE || "true").toLowerCase() === "true",
  seedMockData: (process.env.SEED_MOCK_DATA || "true").toLowerCase() === "true"
};

const plaidHosts = {
  sandbox: "https://sandbox.plaid.com",
  development: "https://development.plaid.com",
  production: "https://production.plaid.com"
};

const tokenKey = loadEncryptionKey();
ensureStorage();
const store = loadStore();
if (config.mockMode && config.seedMockData) {
  seedMockData(store, "local-user");
  saveStore(store);
}

const server = http.createServer(async (req, res) => {
  try {
    if (!config.mockMode && !isHttpsRequest(req)) {
      return sendJson(res, 403, { error: "https_required" });
    }
    const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
    const userId = getUserId(req);

    if (req.method === "GET" && url.pathname === "/health") {
      return sendJson(res, 200, {
        status: "ok",
        app: "paycheck-pilot",
        environment: config.plaidEnv,
        plaidConfigured: Boolean(config.plaidClientId && config.plaidSecret),
        mockMode: config.mockMode
      });
    }
    if (req.method === "POST" && url.pathname === "/api/plaid/create-link-token") {
      return handleCreateLinkToken(res, userId);
    }
    if (req.method === "POST" && url.pathname === "/api/plaid/exchange-public-token") {
      return handleExchangePublicToken(req, res, userId);
    }
    if (req.method === "POST" && url.pathname === "/api/plaid/sync-transactions") {
      const result = await syncUserTransactions(userId);
      return sendJson(res, 200, result);
    }
    if (req.method === "POST" && url.pathname === "/api/paycheck/sync") {
      await syncUserTransactions(userId);
      return sendJson(res, 200, paycheckSummary(userId));
    }
    if (req.method === "POST" && url.pathname === "/api/plaid/disconnect") {
      return handleDisconnect(req, res, userId);
    }
    if (req.method === "GET" && url.pathname === "/api/account/export") {
      return handleExport(res, userId);
    }
    if (req.method === "POST" && url.pathname === "/api/account/delete") {
      return handleDeleteAccount(res, userId);
    }
    return sendJson(res, 404, { error: "not_found" });
  } catch (error) {
    return sendJson(res, 500, { error: "server_error", message: redact(error?.message || "unknown_error") });
  }
});

if (process.argv[1] === currentFile) {
  server.listen(config.port, () => console.log(`Paycheck Pilot bank backend listening on ${config.port}`));
}

async function handleCreateLinkToken(res, userId) {
  requireUser(store, userId);
  if (config.mockMode) {
    audit(userId, "plaid.create_link_token.mock", {});
    return sendJson(res, 200, {
      link_token: `link-sandbox-${crypto.randomUUID()}`,
      mock_mode: true,
      expiration: new Date(Date.now() + 30 * 60 * 1000).toISOString()
    });
  }
  const response = await plaidPost("/link/token/create", {
    client_name: "Paycheck Pilot",
    language: "en",
    country_codes: config.plaidCountryCodes,
    user: { client_user_id: userId },
    products: config.plaidProducts,
    webhook: `${config.publicBaseUrl.replace(/\/$/, "")}/api/plaid/webhook`,
    android_package_name: config.plaidAndroidPackageName
  });
  audit(userId, "plaid.create_link_token", {});
  return sendJson(res, 200, { link_token: response.link_token, mock_mode: false, expiration: response.expiration });
}

async function handleExchangePublicToken(req, res, userId) {
  const body = await readJson(req);
  if (!body.public_token || typeof body.public_token !== "string") {
    return sendJson(res, 400, { error: "public_token_required" });
  }
  requireUser(store, userId);
  if (config.mockMode) {
    const existingInstitution = Object.values(store.connectedInstitutions)
      .find((item) => item.userId === userId && item.plaidItemId === "mock-item");
    const institutionId = existingInstitution?.id || `inst-${crypto.randomUUID()}`;
    store.connectedInstitutions[institutionId] = {
      id: institutionId,
      userId,
      plaidItemId: "mock-item",
      institutionName: body.institution?.name || "Plaid Sandbox Bank",
      encryptedAccessToken: encryptToken("mock-plaid-token-placeholder"),
      transactionCursor: null,
      status: "connected",
      createdAt: nowIso(),
      disconnectedAt: null
    };
    const existingAccount = Object.values(store.connectedAccounts)
      .find((item) => item.userId === userId && item.institutionId === institutionId && item.plaidAccountId === "mock-account");
    const accountId = existingAccount?.id || `acct-${crypto.randomUUID()}`;
    store.connectedAccounts[accountId] = {
      id: accountId,
      userId,
      institutionId,
      plaidAccountId: "mock-account",
      name: "Everyday Checking",
      mask: "0000",
      type: "depository",
      subtype: "checking",
      status: "connected",
      lastSyncedAt: null
    };
    seedMockData(store, userId, institutionId, accountId);
    audit(userId, "plaid.exchange_public_token.mock", { institutionId });
    saveStore(store);
    return sendJson(res, 200, { institutionId, accounts: safeAccounts(userId) });
  }
  const institutionId = `inst-${crypto.randomUUID()}`;
  const exchange = await plaidPost("/item/public_token/exchange", { public_token: body.public_token });
  store.connectedInstitutions[institutionId] = {
    id: institutionId,
    userId,
    plaidItemId: exchange.item_id,
    institutionName: body.institution?.name || "Connected institution",
    encryptedAccessToken: encryptToken(exchange.access_token),
    transactionCursor: null,
    status: "connected",
    createdAt: nowIso(),
    disconnectedAt: null
  };
  for (const account of normalizeLinkAccounts(body.accounts || [], institutionId, userId)) {
    store.connectedAccounts[account.id] = account;
  }
  audit(userId, "plaid.exchange_public_token", { institutionId });
  saveStore(store);
  return sendJson(res, 200, { institutionId, accounts: safeAccounts(userId) });
}

async function syncUserTransactions(userId) {
  const institutions = Object.values(store.connectedInstitutions).filter((item) => item.userId === userId && item.status === "connected");
  for (const institution of institutions) {
    if (config.mockMode) {
      const account = Object.values(store.connectedAccounts).find((item) => item.institutionId === institution.id);
      seedMockData(store, userId, institution.id, account?.id);
      markAccountsSynced(userId, institution.id);
      continue;
    }
    const accessToken = decryptToken(institution.encryptedAccessToken);
    let cursor = institution.transactionCursor;
    let hasMore = true;
    while (hasMore) {
      const response = await plaidPost("/transactions/sync", { access_token: accessToken, cursor, count: 250 });
      for (const transaction of response.added || []) {
        store.bankTransactions[transaction.transaction_id] = normalizeTransaction(transaction, userId, institution.id);
      }
      for (const transaction of response.modified || []) {
        store.bankTransactions[transaction.transaction_id] = normalizeTransaction(transaction, userId, institution.id);
      }
      for (const transaction of response.removed || []) delete store.bankTransactions[transaction.transaction_id];
      cursor = response.next_cursor;
      hasMore = Boolean(response.has_more);
    }
    institution.transactionCursor = cursor;
    markAccountsSynced(userId, institution.id);
  }
  saveStore(store);
  return { status: "ok", syncedAt: nowIso(), accounts: safeAccounts(userId) };
}

function paycheckSummary(userId) {
  const transactions = Object.values(store.bankTransactions)
    .filter((item) => item.userId === userId && !item.pending)
    .sort((a, b) => a.date.localeCompare(b.date));
  const accounts = safeAccounts(userId);
  const paychecks = detectPaychecks(transactions, accounts);
  const bills = detectRecurringBills(transactions, accounts);
  const today = new Date().toISOString().slice(0, 10);
  const nextPayday = paychecks.find((item) => item.date >= today)?.date || addDays(today, 7);
  const beforePayday = bills.filter((bill) => bill.nextDueDate <= nextPayday);
  const currentBalance = estimateBalance(transactions);
  const billsBeforePayday = beforePayday.reduce((sum, bill) => sum + bill.amountInCents, 0);
  const expectedPaycheck = paychecks[0]?.amountInCents || 0;
  const safetyBuffer = 20000;
  const safeToSpend = Math.max(0, currentBalance - billsBeforePayday - safetyBuffer);
  return {
    status: "ok",
    syncedAt: nowIso(),
    accounts,
    paychecks,
    bills,
    summary: {
      accountBalanceInCents: currentBalance,
      expectedPaycheckInCents: expectedPaycheck,
      nextPayday,
      billsBeforePaydayInCents: billsBeforePayday,
      safeToSpendInCents: safeToSpend,
      warning: currentBalance - billsBeforePayday < 0 ? "Upcoming charges may hit before the next paycheck." : null
    }
  };
}

function detectPaychecks(transactions, accounts) {
  const income = transactions.filter(isIncomeTransaction);
  const groups = groupBy(income, (item) => normalizeMerchant(item.merchantName || item.originalDescription));
  return [...groups.values()].flatMap((items) => {
    if (items.length < 1) return [];
    const sorted = [...items].sort((a, b) => a.date.localeCompare(b.date));
    const cadence = classifyCadence(sorted);
    const avg = Math.round(sorted.reduce((sum, item) => sum + Math.abs(item.amountCents), 0) / sorted.length);
    const latest = sorted.at(-1);
    const nextDate = addDays(latest.date, predictedGapDays(cadence || "Biweekly"));
    return [{
      id: `pay-${hash(`${latest.plaidAccountId}:${normalizeMerchant(latest.merchantName)}:${avg}`)}`,
      payerName: latest.merchantName || "Paycheck",
      amountInCents: avg,
      date: nextDate,
      cadence: cadence || "Recurring",
      confidence: sorted.length >= 2 ? 0.88 : 0.58,
      accountNickname: accountName(accounts, latest.plaidAccountId)
    }, {
      id: `pay-last-${hash(latest.transactionId || latest.id)}`,
      payerName: latest.merchantName || "Paycheck",
      amountInCents: Math.abs(latest.amountCents),
      date: latest.date,
      cadence: cadence || "Recurring",
      confidence: sorted.length >= 2 ? 0.82 : 0.50,
      accountNickname: accountName(accounts, latest.plaidAccountId)
    }];
  }).sort((a, b) => a.date.localeCompare(b.date));
}

function detectRecurringBills(transactions, accounts) {
  const outflows = transactions.filter((item) => !isIncomeTransaction(item) && item.amountCents > 0);
  const groups = groupBy(outflows, (item) => `${item.plaidAccountId}:${normalizeMerchant(item.merchantName)}`);
  const bills = [];
  for (const items of groups.values()) {
    if (items.length < 2) continue;
    const sorted = [...items].sort((a, b) => a.date.localeCompare(b.date));
    const cadence = classifyCadence(sorted);
    if (!cadence && sorted.length < 3) continue;
    const avg = Math.round(sorted.reduce((sum, item) => sum + item.amountCents, 0) / sorted.length);
    const latest = sorted.at(-1);
    bills.push({
      id: `bill-${hash(`${latest.plaidAccountId}:${normalizeMerchant(latest.merchantName)}:${avg}`)}`,
      name: latest.merchantName,
      amountInCents: avg,
      nextDueDate: addDays(latest.date, predictedGapDays(cadence || "Monthly")),
      cadence: cadence || "Irregular but repeated",
      confidence: sorted.length >= 3 ? 0.82 : 0.62,
      accountNickname: accountName(accounts, latest.plaidAccountId),
      category: latest.category || "Detected bill"
    });
  }
  return bills.sort((a, b) => a.nextDueDate.localeCompare(b.nextDueDate));
}

async function handleDisconnect(req, res, userId) {
  const body = await readJson(req);
  for (const institution of Object.values(store.connectedInstitutions)) {
    if (institution.userId !== userId) continue;
    if (!config.mockMode && institution.encryptedAccessToken) {
      await plaidPost("/item/remove", { access_token: decryptToken(institution.encryptedAccessToken) }).catch(() => null);
    }
    institution.encryptedAccessToken = null;
    institution.status = "disconnected";
    institution.disconnectedAt = nowIso();
  }
  for (const account of Object.values(store.connectedAccounts)) {
    if (account.userId === userId && (!body.accountId || account.id === body.accountId)) account.status = "disconnected";
  }
  saveStore(store);
  return sendJson(res, 200, { status: "disconnected" });
}

function handleExport(res, userId) {
  return sendJson(res, 200, {
    connectedInstitutions: Object.values(store.connectedInstitutions).filter((item) => item.userId === userId).map(({ encryptedAccessToken, ...safe }) => safe),
    connectedAccounts: safeAccounts(userId),
    bankTransactions: Object.values(store.bankTransactions).filter((item) => item.userId === userId),
    summary: paycheckSummary(userId)
  });
}

function handleDeleteAccount(res, userId) {
  for (const table of ["users", "connectedInstitutions", "connectedAccounts", "bankTransactions", "auditLogs"]) {
    for (const [id, record] of Object.entries(store[table])) {
      if (id === userId || record.userId === userId) delete store[table][id];
    }
  }
  saveStore(store);
  return sendJson(res, 200, { status: "deleted" });
}

async function plaidPost(endpoint, payload) {
  if (!config.plaidClientId || !config.plaidSecret) throw new Error("Plaid credentials are required when PLAID_MOCK_MODE=false.");
  const response = await fetch(`${plaidHosts[config.plaidEnv]}${endpoint}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ client_id: config.plaidClientId, secret: config.plaidSecret, ...payload })
  });
  const json = await response.json();
  if (!response.ok) throw new Error(`Plaid request failed: ${json.error_code || response.status}`);
  return json;
}

function seedMockData(currentStore, userId, institutionId = "mock-institution", accountId = "mock-account") {
  requireUser(currentStore, userId);
  currentStore.connectedInstitutions[institutionId] ||= {
    id: institutionId,
    userId,
    plaidItemId: "mock-item",
    institutionName: "Plaid Sandbox Bank",
    encryptedAccessToken: encryptToken("mock-plaid-token-placeholder"),
    transactionCursor: null,
    status: "connected",
    createdAt: nowIso(),
    disconnectedAt: null
  };
  currentStore.connectedAccounts[accountId] ||= {
    id: accountId,
    userId,
    institutionId,
    plaidAccountId: accountId,
    name: "Everyday Checking",
    mask: "0000",
    type: "depository",
    subtype: "checking",
    status: "connected",
    lastSyncedAt: nowIso()
  };
  const today = new Date().toISOString().slice(0, 10);
  const rows = [
    ...recurringRows("PUBLIX PAYROLL", [-70000, -70000, -61800], 7, "INCOME", today),
    ...recurringRows("ACME PAYROLL", [-142000, -142000, -142000], 14, "INCOME", today),
    ...recurringRows("DOORDASH", [-16300, -9200, -18750, -12400], 4, "INCOME", today),
    ...recurringRows("Rent", [85000, 85000, 85000, 85000], 30, "RENT", today),
    ...recurringRows("Phone Bill", [8200, 8200, 8200, 8200], 30, "UTILITIES", today),
    ...recurringRows("Electric Bill", [13600, 15750, 14820, 16990], 30, "UTILITIES", today),
    ...recurringRows("Netflix", [1549, 1549, 1549, 1549], 30, "ENTERTAINMENT", today),
    ...recurringRows("Gym", [2999, 2999, 2999, 2999], 30, "HEALTH_AND_FITNESS", today),
    ...recurringRows("Gas Station", [4200, 4600, 3850, 5120], 7, "TRANSPORTATION", today),
    ...recurringRows("Grocery Store", [9250, 8840, 10110, 7420], 7, "FOOD_AND_DRINK", today),
    ["Spotify", 1199, addDays(today, -3), "ENTERTAINMENT"],
    ["Spotify", 1199, addDays(today, -2), "ENTERTAINMENT"]
  ];
  rows.forEach(([merchantName, amountCents, date, category], index) => {
    const id = `mock-${normalizeMerchant(merchantName).replaceAll(" ", "-")}-${index}-${accountId}`;
    currentStore.bankTransactions[id] = {
      id,
      transactionId: id,
      userId,
      institutionId,
      plaidAccountId: accountId,
      merchantName,
      originalDescription: merchantName,
      amountCents,
      currency: "USD",
      date,
      authorizedDate: date,
      pending: false,
      category,
      paymentChannel: "online",
      createdAt: nowIso()
    };
  });
}

function recurringRows(merchantName, amounts, cadenceDays, category, today) {
  return amounts.map((amountCents, index) => [
    merchantName,
    amountCents,
    addDays(today, -cadenceDays * (amounts.length - index)),
    category
  ]);
}

function isIncomeTransaction(item) {
  const text = `${item.merchantName} ${item.originalDescription || ""} ${item.category || ""}`.toLowerCase();
  return item.amountCents < 0 || ["payroll", "paycheck", "salary", "income", "direct deposit"].some((term) => text.includes(term));
}

function classifyCadence(sorted) {
  if (sorted.length < 2) return null;
  const gaps = sorted.slice(1).map((item, index) => dayDiff(sorted[index].date, item.date));
  const average = gaps.reduce((sum, gap) => sum + gap, 0) / gaps.length;
  const spread = Math.max(...gaps) - Math.min(...gaps);
  if (average >= 6 && average <= 8 && spread <= 2) return "Weekly";
  if (average >= 13 && average <= 16 && spread <= 3) return "Biweekly";
  if (average >= 25 && average <= 35 && spread <= 7) return "Monthly";
  if (average >= 330 && average <= 395 && spread <= 35) return "Yearly";
  return sorted.length >= 3 ? "Irregular but repeated" : null;
}

function predictedGapDays(cadence) {
  if (cadence === "Weekly") return 7;
  if (cadence === "Biweekly") return 14;
  if (cadence === "Yearly") return 365;
  return 30;
}

function normalizeLinkAccounts(accounts, institutionId, userId) {
  return accounts.map((account) => ({
    id: `acct-${crypto.randomUUID()}`,
    userId,
    institutionId,
    plaidAccountId: account.id || account.account_id,
    name: account.name || "Account",
    mask: account.mask || "",
    type: account.type || "unknown",
    subtype: account.subtype || "",
    status: "connected",
    lastSyncedAt: null
  }));
}

function normalizeTransaction(transaction, userId, institutionId) {
  return {
    id: transaction.transaction_id,
    transactionId: transaction.transaction_id,
    userId,
    institutionId,
    plaidAccountId: transaction.account_id,
    merchantName: transaction.merchant_name || transaction.name || "Unknown merchant",
    originalDescription: transaction.name || transaction.original_description || transaction.merchant_name || "Unknown merchant",
    amountCents: Math.round(Number(transaction.amount || 0) * 100),
    currency: transaction.iso_currency_code || transaction.unofficial_currency_code || "USD",
    date: transaction.date,
    authorizedDate: transaction.authorized_date || null,
    pending: Boolean(transaction.pending),
    category: transaction.personal_finance_category?.primary || (transaction.category || []).join(" / ") || "",
    paymentChannel: transaction.payment_channel || "",
    createdAt: nowIso()
  };
}

function estimateBalance(transactions) {
  const recent = transactions.slice(-90);
  return 125000 - recent.reduce((sum, item) => sum + item.amountCents, 0);
}

function accountName(accounts, plaidAccountId) {
  return accounts.find((item) => item.plaidAccountId === plaidAccountId || item.id === plaidAccountId)?.name || "Connected account";
}

function safeAccounts(userId) {
  return Object.values(store.connectedAccounts)
    .filter((account) => account.userId === userId)
    .map(({ plaidAccountId, ...safe }) => ({
      ...safe,
      institutionName: store.connectedInstitutions[safe.institutionId]?.institutionName || "Connected institution"
    }));
}

function requireUser(currentStore, userId) {
  currentStore.users[userId] ||= { id: userId, createdAt: nowIso(), mode: config.mockMode ? "mock" : "plaid" };
}

function markAccountsSynced(userId, institutionId) {
  for (const account of Object.values(store.connectedAccounts)) {
    if (account.userId === userId && account.institutionId === institutionId) account.lastSyncedAt = nowIso();
  }
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  if (!chunks.length) return {};
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

function sendJson(res, status, value) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" });
  res.end(JSON.stringify(value));
}

function ensureStorage() {
  fs.mkdirSync(dataDir, { recursive: true });
  if (!fs.existsSync(storePath)) saveStore(emptyStore());
}

function emptyStore() {
  return { users: {}, connectedInstitutions: {}, connectedAccounts: {}, bankTransactions: {}, auditLogs: {} };
}

function loadStore() {
  return JSON.parse(fs.readFileSync(storePath, "utf8"));
}

function saveStore(value) {
  fs.writeFileSync(storePath, JSON.stringify(value, null, 2));
}

function splitEnv(value) {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function nowIso() {
  return new Date().toISOString();
}

function dayDiff(first, second) {
  return Math.round((Date.parse(`${second}T00:00:00Z`) - Date.parse(`${first}T00:00:00Z`)) / 86_400_000);
}

function addDays(dateText, days) {
  const date = new Date(`${dateText}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function normalizeMerchant(value) {
  return String(value || "").trim().toLowerCase().replace(/\s+/g, " ");
}

function groupBy(items, keyFor) {
  const groups = new Map();
  for (const item of items) {
    const key = keyFor(item);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(item);
  }
  return groups;
}

function hash(value) {
  return crypto.createHash("sha256").update(String(value)).digest("hex").slice(0, 16);
}

function audit(userId, action, metadata) {
  const id = `audit-${crypto.randomUUID()}`;
  store.auditLogs[id] = { id, userId, action, metadata, createdAt: nowIso() };
}

function encryptToken(token) {
  if (!token) return null;
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", tokenKey, iv);
  const ciphertext = Buffer.concat([cipher.update(token, "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();
  return `${iv.toString("base64")}.${tag.toString("base64")}.${ciphertext.toString("base64")}`;
}

function decryptToken(value) {
  const [ivText, tagText, cipherText] = value.split(".");
  const decipher = crypto.createDecipheriv("aes-256-gcm", tokenKey, Buffer.from(ivText, "base64"));
  decipher.setAuthTag(Buffer.from(tagText, "base64"));
  return Buffer.concat([decipher.update(Buffer.from(cipherText, "base64")), decipher.final()]).toString("utf8");
}

function loadEncryptionKey() {
  const configured = process.env.TOKEN_ENCRYPTION_KEY || process.env.PLAID_TOKEN_ENCRYPTION_KEY || "";
  if (configured) {
    const decoded = Buffer.from(configured, "base64");
    if (decoded.length === 32) return decoded;
    const raw = Buffer.from(configured, "utf8");
    if (raw.length === 32) return raw;
    throw new Error("TOKEN_ENCRYPTION_KEY must be 32 bytes or base64-encoded 32 bytes.");
  }
  if (!config.mockMode) throw new Error("TOKEN_ENCRYPTION_KEY is required outside mock mode.");
  return crypto.createHash("sha256").update("paycheck-pilot-local-mock-key").digest();
}

function isHttpsRequest(req) {
  return req.headers["x-forwarded-proto"] === "https" || req.socket.encrypted;
}

function getUserId(req) {
  const value = req.headers["x-paycheckpilot-user-id"];
  return typeof value === "string" && value.trim() ? value.trim() : "local-user";
}

function redact(value) {
  return String(value).replace(/access-[A-Za-z0-9_-]+/g, "[redacted-access-token]").replace(/public-[A-Za-z0-9_-]+/g, "[redacted-public-token]");
}

export {
  addDays,
  dayDiff,
  detectPaychecks,
  detectRecurringBills,
  emptyStore,
  encryptToken,
  normalizeMerchant,
  paycheckSummary,
  seedMockData
};
