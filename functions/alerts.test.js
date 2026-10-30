const test = require("node:test");
const assert = require("node:assert");
const { evaluate, isDue, offlineAlert } = require("./alerts");

const ok = { temperature: 28, soilMoisture: 55, soilPh: 6.5 };

test("healthy reading raises nothing", () => assert.deepStrictEqual(evaluate(ok, null), []));

test("each threshold raises its own alert, with the app's message text", () => {
  const found = evaluate({ temperature: 36.04, soilMoisture: 38.9, soilPh: 4.96 }, null);
  assert.deepStrictEqual(found.map((a) => a.message), ["Moisture dropped to 38%", "Temperature reached 36.0°C", "Soil pH is 5.0"]);
});

test("0 means no data and never alerts; master switch and per-alert switch are honoured", () => {
  assert.deepStrictEqual(evaluate({ temperature: 28, soilMoisture: 0, soilPh: 0 }, null), []);
  assert.deepStrictEqual(evaluate({ ...ok, soilMoisture: 10 }, { enabled: false }), []);
  assert.deepStrictEqual(evaluate({ ...ok, soilMoisture: 10 }, { lowMoistureAlertEnabled: false }), []);
});

test("custom thresholds apply", () => {
  assert.strictEqual(evaluate({ ...ok, soilMoisture: 45 }, { soilMoistureThreshold: 50 }).length, 1);
});

test("repeat interval", () => {
  assert.ok(isDue(undefined, 1000, 30));
  assert.ok(!isDue(0 + 1, 1 + 29 * 60000, 30));
  assert.ok(isDue(1, 1 + 30 * 60000, 30));
});

test("offline: once per outage, not for demo or long-dead devices", () => {
  const now = 10 * 3600 * 1000;
  const seen = now - 5 * 60000;
  assert.strictEqual(offlineAlert({ lastSeen: seen }, null, now, null).type, "device_offline");
  assert.strictEqual(offlineAlert({ lastSeen: seen }, null, now, seen), null);          // already reported
  assert.strictEqual(offlineAlert({ lastSeen: now - 30000 }, null, now, null), null);    // still inside the window
  assert.strictEqual(offlineAlert({ lastSeen: seen, simulated: true }, null, now, null), null);
  assert.strictEqual(offlineAlert({ lastSeen: now - 25 * 3600 * 1000 }, null, now, null), null);
  assert.strictEqual(offlineAlert({}, null, now, null), null);
});
