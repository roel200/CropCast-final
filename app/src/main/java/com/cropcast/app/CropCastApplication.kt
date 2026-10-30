package com.cropcast.app

import android.app.Application
import com.cropcast.app.push.Notifier
import com.google.firebase.database.FirebaseDatabase

class CropCastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
    }
}
