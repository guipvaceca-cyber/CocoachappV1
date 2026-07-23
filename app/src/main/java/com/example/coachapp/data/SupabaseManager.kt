package com.example.coachapp.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

object SupabaseManager {
    private const val SUPABASE_URL = "https://nbdwjkmvdstltvfyzgge.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5iZHdqa212ZHN0bHR2Znl6Z2dlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM1NDI0NTUsImV4cCI6MjA5OTExODQ1NX0.EOjaL7vydJUeErbrCH_V-wjZ2DUGVsHkpynPl2qcoPg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth) {}
        install(Postgrest) {}
        install(Functions) {}
        install(Realtime) {}
    }

    val auth get() = client.auth
    val db get() = client.postgrest
    val functions get() = client.functions
    val realtime get() = client.realtime
}
