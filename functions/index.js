const admin = require("firebase-admin");
const { onValueWritten } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { evaluate, isDue, offlineAlert, withDefaults } = require("./alerts");

admin.initializeApp();
// Resolved on first use: deploy-time analysis loads this file without a database URL.
const db = { ref: (path) => admin.database().ref(path) };

// The Realtime Database is in asia-southeast1; a database trigger must run in the same region.
const REGION = "asia-southeast1";
const INSTANCE = "cropcast-84fb5-default-rtdb";

/** Phones of the accounts paired to this device: users/<uid>/deviceId == deviceId, tokens in users/<uid>/fcmTokens. */
async function tokensFor(deviceId) {
  const users = await db.ref("users").orderByChild("deviceId").equalTo(deviceId).get();
  const out = [];
  users.forEach((user) => {
    Object.keys(user.child("fcmTokens").val() || {}).forEach((token) => out.push({ uid: user.key, token }));
  });
  return out;
}

/** Records the alert for the app's list, then pushes it. The app builds the localized text from type, value and message. */
async function raise(deviceId, alert) {
  const now = Date.now();
  await db.ref(`devices/${deviceId}/alerts`).push({ ...alert, timestamp: now, acknowledged: false });
  const targets = await tokensFor(deviceId);
  if (!targets.length) return;
  const result = await admin.messaging().sendEachForMulticast({
    tokens: targets.map((t) => t.token),
    data: { type: alert.type, message: alert.message, value: String(alert.value), deviceId },
    android: { priority: "high" },
  });
  // Drop tokens of uninstalled apps so they are not retried forever.
  await Promise.all(result.responses.map((res, i) =>
    !res.success && res.error && res.error.code === "messaging/registration-token-not-registered"
      ? db.ref(`users/${targets[i].uid}/fcmTokens/${targets[i].token}`).remove()
      : null));
}

/** True once per repeat interval and type; the transaction stops two overlapping runs both sending. */
async function claim(deviceId, type, repeatMinutes) {
  const now = Date.now();
  const { committed } = await db.ref(`devices/${deviceId}/alertState/${type}`)
    .transaction((last) => (isDue(last, now, repeatMinutes) ? now : undefined));
  return committed;
}

// Every live reading (about every 15 s per device); most runs find nothing to report.
exports.onReading = onValueWritten(
  { ref: "/devices/{deviceId}/readings/current", region: REGION, instance: INSTANCE },
  async (event) => {
    const reading = event.data.after.val();
    if (!reading) return;
    const { deviceId } = event.params;
    const settings = withDefaults((await db.ref(`devices/${deviceId}/settings`).get()).val());
    for (const alert of evaluate(reading, settings)) {
      if (await claim(deviceId, alert.type, settings.repeatIntervalMinutes)) await raise(deviceId, alert);
    }
  }
);

// A device that is off writes nothing, so nothing triggers: look for silent devices once a minute.
exports.checkOffline = onSchedule({ schedule: "every 1 minutes", region: REGION }, async () => {
  const users = await db.ref("users").get();
  const deviceIds = new Set();
  users.forEach((user) => { const id = user.child("deviceId").val(); if (id) deviceIds.add(id); });
  const now = Date.now();
  for (const deviceId of deviceIds) {
    const base = db.ref(`devices/${deviceId}`);
    const [status, settings, reported] = await Promise.all([
      base.child("status").get(), base.child("settings").get(), base.child("alertState/device_offline").get(),
    ]);
    const alert = offlineAlert(status.val(), settings.val(), now, reported.val());
    if (!alert) continue;
    await base.child("alertState/device_offline").set(alert.value);
    await raise(deviceId, alert);
  }
});
