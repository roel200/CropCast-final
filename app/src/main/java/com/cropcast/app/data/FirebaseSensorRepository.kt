package com.cropcast.app.data

import android.content.Context
import com.cropcast.app.data.model.AlertEvent
import com.cropcast.app.data.model.AlertSettings
import com.cropcast.app.data.model.AccountInfo
import com.cropcast.app.data.model.DeviceStatus
import com.cropcast.app.data.model.CropOutcomeFeedback
import com.cropcast.app.data.model.FieldSample
import com.cropcast.app.data.model.MonthlyCropRecommendation
import com.cropcast.app.data.model.SensorReading
import com.cropcast.app.data.model.isValidForRecommendation
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

class FirebaseSensorRepository(
    context: Context,
    private val defaultDeviceId: String = com.cropcast.app.BuildConfig.DEVICE_ID,
    forceDemo: Boolean = false
) {
    private val credentialManager = androidx.credentials.CredentialManager.create(context.applicationContext)
    private val configured = !forceDemo && FirebaseApp.getApps(context).isNotEmpty()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database by lazy { FirebaseDatabase.getInstance() }
    // The paired device from users/{uid}/deviceId, loaded in loadAccountInfo() before any listener attaches.
    @Volatile private var deviceId: String = defaultDeviceId
    private val deviceRef get() = database.reference.child("devices").child(deviceId)

    suspend fun connect(): Result<Unit> = runCatching {
        check(configured) { "Firebase is not configured. Add app/google-services.json." }
        check(auth.currentUser != null) { "Sign in before connecting to CropCast." }
    }

    fun isConfigured(): Boolean = configured

    fun hasAuthenticatedUser(): Boolean = configured && auth.currentUser != null

    suspend fun signIn(email: String, password: String): AccountInfo {
        check(configured) { "Firebase is not configured. Continue in demo mode instead." }
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        return loadAccountInfo()
    }

    suspend fun register(displayName: String, email: String, password: String): AccountInfo {
        check(configured) { "Registration needs a Firebase account. Add google-services.json first." }
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Registration failed")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()
        return updateAccountInfo(displayName)
    }

    suspend fun signInWithGoogle(idToken: String): AccountInfo {
        check(configured) { "Firebase is not configured." }
        require(idToken.isNotBlank()) { "Google did not return an ID token." }
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        return loadAccountInfo()
    }

    suspend fun continueAsGuest(): AccountInfo {
        check(configured) { "Firebase is not configured." }
        auth.signInAnonymously().await()
        return loadAccountInfo()
    }

    fun observeReading(): Flow<SensorReading> = valueFlow("readings/current", SensorReading()) {
        readingFrom(it) ?: SensorReading()
    }

    fun observeMonthlyReadings(): Flow<Map<String, List<SensorReading>>> =
        valueFlow("readings/monthly", emptyMap()) { snapshot ->
            snapshot.children.associate { month ->
                val monthKey = month.key.orEmpty()
                monthKey to month.children
                    .mapNotNull(::readingFrom)
                    .filter { reading -> monthKeyFor(reading.timestamp) == monthKey }
                    .sortedBy { it.timestamp }
            }.filterKeys { it.isNotBlank() }
        }

    fun observeMonthlyRecommendations(): Flow<Map<String, Map<String, MonthlyCropRecommendation>>> =
        valueFlow("recommendations/monthly", emptyMap()) { snapshot ->
            snapshot.children.mapNotNull { month ->
                val records = listOf("observed", "forecast").mapNotNull { recordType ->
                    month.child(recordType)
                        .getValue(MonthlyCropRecommendation::class.java)
                        ?.let { recordType to it }
                }.toMap()
                month.key.orEmpty().takeIf { it.isNotBlank() }?.let { it to records }
            }.toMap()
        }

    suspend fun saveMonthlyRecommendation(recommendation: MonthlyCropRecommendation) {
        connect().getOrThrow()
        val recordType = when (recommendation.recommendationType) {
            SeedRecommendationEngine.OBSERVED_MONTH -> "observed"
            SeedRecommendationEngine.NEXT_MONTH_FORECAST -> "forecast"
            else -> error("Unsupported recommendation type: ${recommendation.recommendationType}")
        }
        deviceRef.child("recommendations/monthly")
            .child(recommendation.monthKey)
            .child(recordType)
            .setValue(recommendation)
            .await()
    }

    fun observeOutcomeFeedback(): Flow<List<CropOutcomeFeedback>> =
        valueFlow("recommendations/feedback", emptyList()) { snapshot ->
            snapshot.children.mapNotNull { child ->
                child.getValue(CropOutcomeFeedback::class.java)?.copy(id = child.key.orEmpty())
            }.sortedByDescending { it.submittedAt }
        }

    suspend fun saveOutcomeFeedback(feedback: CropOutcomeFeedback) {
        connect().getOrThrow()
        require(feedback.plantedCrop.isNotBlank()) { "Enter the crop that was planted" }
        require(feedback.rating in 1..5) { "Outcome rating must be from 1 to 5" }
        require(feedback.harvestedKg >= 0.0) { "Harvest weight cannot be negative" }
        deviceRef.child("recommendations/feedback").push().setValue(feedback.copy(id = "")).await()
    }

    fun observeFieldSample(): Flow<FieldSample?> = valueFlow("samples/latest", null) {
        it.getValue(FieldSample::class.java)?.takeIf { sample -> sample.spots > 0 }
    }

    suspend fun saveFieldSample(sample: FieldSample) {
        connect().getOrThrow()
        deviceRef.child("samples/history").push().setValue(sample).await()
        deviceRef.child("samples/latest").setValue(sample).await()
    }

    fun observeStatus(): Flow<DeviceStatus> = valueFlow("status", DeviceStatus()) { snapshot ->
        val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
        val declaredOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
        DeviceStatus(
            online = declaredOnline,
            lastSeen = lastSeen,
            firmware = snapshot.child("firmware").getValue(String::class.java).orEmpty(),
            sensors = snapshot.child("sensors").children.mapNotNull { it.getValue(String::class.java) }
        )
    }

    fun observeSettings(): Flow<AlertSettings> = valueFlow("settings", AlertSettings()) {
        it.getValue(AlertSettings::class.java) ?: AlertSettings()
    }

    fun observeAlerts(): Flow<List<AlertEvent>> = valueFlow("alerts", emptyList()) { snapshot ->
        snapshot.children.mapNotNull { child ->
            child.getValue(AlertEvent::class.java)?.copy(id = child.key.orEmpty())
        }.sortedByDescending { it.timestamp }.take(20)
    }

    suspend fun updateAlertSettings(settings: AlertSettings) {
        deviceRef.child("settings").setValue(settings).await()
    }

    suspend fun testConnection(): Boolean {
        connect().getOrThrow()
        return deviceRef.child("status").get().await().exists()
    }

    suspend fun requestRestart() {
        connect().getOrThrow()
        deviceRef.child("commands/restart").setValue(
            mapOf(
                "status" to "pending",
                "requestedAt" to System.currentTimeMillis(),
                "requestedBy" to (auth.currentUser?.uid ?: "unknown")
            )
        ).await()
    }

    suspend fun loadAccountInfo(): AccountInfo {
        connect().getOrThrow()
        val user = requireNotNull(auth.currentUser)
        val profile = database.reference.child("users/${user.uid}/profile").get().await()
        val pairedId = database.reference.child("users/${user.uid}/deviceId").get().await()
            .getValue(String::class.java)
        deviceId = pairedId?.takeIf { it.isNotBlank() } ?: defaultDeviceId
        // Best effort: without a token the app still works, it just gets no pushes.
        runCatching { setPushToken(user.uid, true) }
        return AccountInfo(
            displayName = profile.child("displayName").getValue(String::class.java) ?: user.displayName ?: "Guest Farmer",
            role = profile.child("role").getValue(String::class.java) ?: "Farmer",
            email = user.email ?: "Anonymous account",
            isAnonymous = user.isAnonymous,
            deviceId = deviceId
        )
    }

    /** A PIN nobody has published under yet, so pairing cannot land on someone else's device. */
    suspend fun freePairingCode(): String {
        connect().getOrThrow()
        // Re-pairing keeps the account's PIN, so its history survives a Wi-Fi change.
        if (Regex("\\d{4}").matches(deviceId)) return deviceId
        repeat(10) {
            val code = PairingCode.generate()
            val taken = database.reference.child("devices").child(code).child("status").get().await().exists()
            if (!taken) return code
        }
        error("Could not find a free pairing PIN. Try again.")
    }

    /** True while the device that was given [code] in its setup portal reports itself online. */
    fun observeDeviceOnline(code: String): Flow<Boolean> = callbackFlow {
        connect().getOrThrow()
        val reference = database.reference.child("devices").child(code).child("status/online")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.getValue(Boolean::class.java) == true) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    /** Makes [code] this account's device from now on, on every phone the account signs in on. */
    suspend fun savePairedDevice(code: String) {
        connect().getOrThrow()
        val user = requireNotNull(auth.currentUser)
        database.reference.child("users/${user.uid}/deviceId").setValue(code).await()
        deviceId = code
    }

    suspend fun updateAccountInfo(displayName: String, role: String = "Farmer"): AccountInfo {
        connect().getOrThrow()
        val user = requireNotNull(auth.currentUser)
        database.reference.child("users/${user.uid}/profile").setValue(
            mapOf("displayName" to displayName, "role" to role)
        ).await()
        return AccountInfo(displayName, role, user.email ?: "Anonymous account", user.isAnonymous, deviceId)
    }

    suspend fun changePassword(password: String) {
        val user = requireNotNull(auth.currentUser)
        check(!user.isAnonymous && user.email != null) { "Sign in with an email account before changing a password." }
        user.updatePassword(password).await()
    }

    /** Lets the alert function reach this phone: users/{uid}/fcmTokens/{token}. */
    private suspend fun setPushToken(uid: String, registered: Boolean) {
        val token = FirebaseMessaging.getInstance().token.await()
        val reference = database.reference.child("users/$uid/fcmTokens/$token")
        if (registered) reference.setValue(true).await() else reference.removeValue().await()
    }

    suspend fun signOut() {
        deviceId = defaultDeviceId
        if (configured) {
            // The next account on this phone must not receive this farm's alerts.
            auth.currentUser?.let { user -> runCatching { setPushToken(user.uid, false) } }
            auth.signOut()
            try {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            } catch (_: androidx.credentials.exceptions.ClearCredentialException) {
                // Firebase is signed out even when the credential provider is unavailable.
            }
        }
    }

    suspend fun acknowledgeAlert(id: String) {
        if (id.isNotBlank()) deviceRef.child("alerts/$id/acknowledged").setValue(true).await()
    }

    suspend fun simulateMoisture(value: Double) {
        val ref = deviceRef.child("readings/current")
        ref.child("soilMoisture").setValue(value).await()
        ref.child("timestamp").setValue(System.currentTimeMillis()).await()
    }

    suspend fun recordLowMoistureAlert(value: Double) {
        recordAlert("soil_moisture_low", "Moisture dropped to ${value.toInt()}%", value)
    }

    suspend fun recordAlert(type: String, message: String, value: Double) {
        deviceRef.child("alerts").push().setValue(
            AlertEvent(type = type, message = message, value = value, timestamp = System.currentTimeMillis())
        ).await()
    }

    private fun <T> valueFlow(path: String, fallback: T, mapper: (DataSnapshot) -> T): Flow<T> = callbackFlow {
        if (!configured) {
            trySend(fallback)
            close()
            return@callbackFlow
        }
        // Authenticate before attaching listeners so authenticated database rules do not
        // cancel the flow during the app's first launch.
        connect().getOrThrow()
        val reference = deviceRef.child(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(mapper(snapshot)) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    private fun readingFrom(snapshot: DataSnapshot): SensorReading? {
        if (REQUIRED_READING_FIELDS.any { !snapshot.hasChild(it) }) return null
        return snapshot.getValue(SensorReading::class.java)?.takeIf { it.isValidForRecommendation() }
    }

    private fun monthKeyFor(timestamp: Long): String =
        YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC)).toString()

    private companion object {
        val REQUIRED_READING_FIELDS = listOf(
            "temperature",
            "humidity",
            "soilMoisture",
            "soilPh",
            "nitrogen",
            "phosphorus",
            "potassium",
            "lightIntensity",
            "timestamp"
        )
    }
}
