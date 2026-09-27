// Copy this file to secrets.h (gitignored) and fill in your own values.
#pragma once

#define WIFI_SSID "YOUR_WIFI_SSID"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"

#define FIREBASE_API_KEY "YOUR_FIREBASE_WEB_API_KEY"
#define FIREBASE_DATABASE_URL "https://YOUR_PROJECT-default-rtdb.firebaseio.com"
// Email/password user created in Firebase Authentication for this device.
#define FIREBASE_USER_EMAIL "esp32-device@example.com"
#define FIREBASE_USER_PASSWORD "CHANGE_ME"

#define DEVICE_ID "esp32-field-01"

// Optional overrides; see section B of CropCastESP32.ino.
// #define CROPCAST_SENSOR_DHT_ENABLED false
// #define CROPCAST_SENSOR_SOIL_ENABLED false
// #define CROPCAST_SENSOR_LIGHT_ENABLED false
// #define CROPCAST_SD_ENABLED false
// #define CROPCAST_LOG_LEVEL 3
