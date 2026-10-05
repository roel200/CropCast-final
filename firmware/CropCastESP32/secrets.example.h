// Copy this file to secrets.h (gitignored) and fill in your own values.
// Wi-Fi and the device id are NOT set here any more: the ESP32 gets them from the setup page
// (pairing). See firmware/CropCastESP32/README.md.
#pragma once

#define FIREBASE_API_KEY "YOUR_FIREBASE_WEB_API_KEY"
#define FIREBASE_DATABASE_URL "https://YOUR_PROJECT-default-rtdb.firebaseio.com"
// Email/password user created in Firebase Authentication for this device.
#define FIREBASE_USER_EMAIL "esp32-device@example.com"
#define FIREBASE_USER_PASSWORD "CHANGE_ME"

// Optional: WPA2 password (8+ characters) for the setup access point. Empty = open AP.
// #define CROPCAST_AP_PASSWORD "change-me-please"

// Optional overrides; see section B of CropCastESP32.ino.
// #define CROPCAST_SENSOR_DHT_ENABLED false
// #define CROPCAST_SENSOR_SOIL_ENABLED false
// #define CROPCAST_SENSOR_LIGHT_ENABLED false
// #define CROPCAST_SD_ENABLED false
// #define CROPCAST_LOG_LEVEL 3
