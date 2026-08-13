package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class EarnMitraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ensureFirebaseInitialized(this)
    }

    companion object {
        @Volatile
        private var isInitialized = false

        fun ensureFirebaseInitialized(context: Context) {
            if (isInitialized && FirebaseApp.getApps(context).isNotEmpty()) {
                return
            }
            synchronized(this) {
                if (isInitialized && FirebaseApp.getApps(context).isNotEmpty()) {
                    return
                }
                try {
                    val appContext = context.applicationContext ?: context
                    if (FirebaseApp.getApps(appContext).isEmpty()) {
                        var app = try {
                            FirebaseApp.initializeApp(appContext)
                        } catch (e: Exception) {
                            Log.w("EarnMitraApp", "Standard FirebaseApp.initializeApp failed", e)
                            null
                        }

                        if (app == null) {
                            val options = FirebaseOptions.fromResource(appContext)
                                ?: FirebaseOptions.Builder()
                                    .setApiKey("AIzaSyAXrDY2uHZeWPFEy00R_4LmKlCCNZp9xHs")
                                    .setApplicationId("1:759487877750:android:488fad354a40179dfa0557")
                                    .setProjectId("earnmitra-669c5")
                                    .setStorageBucket("earnmitra-669c5.firebasestorage.app")
                                    .setGcmSenderId("759487877750")
                                    .build()
                            FirebaseApp.initializeApp(appContext, options)
                        }
                    }
                    isInitialized = FirebaseApp.getApps(context).isNotEmpty()
                    if (isInitialized) {
                        Log.d("EarnMitraApp", "FirebaseApp successfully initialized")
                    }
                } catch (e: Exception) {
                    Log.e("EarnMitraApp", "Failed to initialize FirebaseApp", e)
                }
            }
        }
    }
}

