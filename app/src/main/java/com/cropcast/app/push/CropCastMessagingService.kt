package com.cropcast.app.push

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives the data-only pushes sent by functions/index.js; showing them ourselves lets us translate them. */
class CropCastMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        val text = message.data["message"] ?: return
        Notifier.show(this, type, text)
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return  // signed in later: loadAccountInfo registers it
        FirebaseDatabase.getInstance().reference.child("users/$uid/fcmTokens/$token").setValue(true)
    }
}
