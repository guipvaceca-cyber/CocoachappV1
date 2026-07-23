package com.example.coachapp

import android.app.Application
import android.util.Log

class CoachApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configuration d'un gestionnaire d'exceptions global pour logger les crashs fatals
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FATAL_CRASH", "CRASH DÉTECTÉ dans le thread ${thread.name}")
            Log.e("FATAL_CRASH", "Message: ${throwable.message}")
            Log.e("FATAL_CRASH", "Stacktrace:\n${Log.getStackTraceString(throwable)}")
            
            // On laisse le système gérer le crash après avoir loggé
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        Log.i("CoachApp", "Application démarrée et Crash Logger initialisé")
    }
}
