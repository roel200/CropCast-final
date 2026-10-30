// Pure alert rules, kept free of Firebase so they can be unit-tested (node --test).
// Defaults and message texts mirror AlertSettings and CropCastViewModel in the app.

const DEFAULTS = {
  enabled: true,
  lowMoistureAlertEnabled: true,
  soilMoistureThreshold: 40,
  highTemperatureThreshold: 35,
  minSoilPh: 5.5,
  maxSoilPh: 7.5,
  deviceOfflineMinutes: 2,
  repeatIntervalMinutes: 30,
};

const withDefaults = (settings) => ({ ...DEFAULTS, ...(settings || {}) });

// Data contract: 0 means "no data" (the firmware zeroes untrusted fields), so it never raises an alert.
function evaluate(reading, rawSettings) {
  const s = withDefaults(rawSettings);
  const r = reading || {};
  const alerts = [];
  if (!s.enabled) return alerts;
  if (s.lowMoistureAlertEnabled && r.soilMoisture > 0 && r.soilMoisture < s.soilMoistureThreshold) {
    alerts.push({ type: "soil_moisture_low", value: r.soilMoisture, message: `Moisture dropped to ${Math.trunc(r.soilMoisture)}%` });
  }
  if (r.temperature > s.highTemperatureThreshold) {
    alerts.push({ type: "temperature_high", value: r.temperature, message: `Temperature reached ${r.temperature.toFixed(1)}°C` });
  }
  if (r.soilPh > 0 && (r.soilPh < s.minSoilPh || r.soilPh > s.maxSoilPh)) {
    alerts.push({ type: "soil_ph_out_of_range", value: r.soilPh, message: `Soil pH is ${r.soilPh.toFixed(1)}` });
  }
  return alerts;
}

const isDue = (lastSentAt, now, repeatMinutes) => !lastSentAt || now - lastSentAt >= repeatMinutes * 60000;

// One push per outage. The outage is identified by the last time the device was seen, so a long-dead
// device (seeded demo data, a unit in a drawer) is never reported again and again.
const OFFLINE_REPORT_WINDOW_MS = 24 * 3600 * 1000;

function offlineAlert(status, rawSettings, now, reportedOutage) {
  const s = withDefaults(rawSettings);
  const lastSeen = status && status.lastSeen;
  if (!s.enabled || !lastSeen || (status && status.simulated)) return null;
  const silentMs = now - lastSeen;
  if (silentMs < s.deviceOfflineMinutes * 60000 || silentMs > OFFLINE_REPORT_WINDOW_MS) return null;
  if (reportedOutage === lastSeen) return null;
  return { type: "device_offline", value: lastSeen, message: "Device offline" };
}

module.exports = { evaluate, isDue, offlineAlert, withDefaults };
