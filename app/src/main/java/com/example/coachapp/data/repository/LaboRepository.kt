package com.example.coachapp.data.repository

import com.example.coachapp.data.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.*
import android.util.Log

class LaboRepository(private val supabase: SupabaseClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchResources(): List<LaboResource> {
        return try {
            val response = supabase.from("labo_situationpropose")
                .select {
                    order("created_at", Order.DESCENDING)
                }
            val data = response.data
            json.decodeFromString<List<LaboResource>>(data)
        } catch (e: Exception) {
            Log.e("LABO_REPO", "Error fetching resources", e)
            emptyList()
        }
    }

    suspend fun proposeResource(resource: LaboResource): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            // We map the LaboResource to a JsonObject to ensure the structure matches Supabase
            // and we inject the author_id for RLS.
            val resourceJson = json.encodeToJsonElement(resource).jsonObject.toMutableMap()
            resourceJson["author_id"] = JsonPrimitive(userId)
            
            // Note: If 'category' is an enum, serialization might need care. 
            // Kotlinx.serialization handles it by default using name.
            
            supabase.from("labo_situationpropose").insert(JsonObject(resourceJson))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LABO_REPO", "Error proposing resource", e)
            Result.failure(e)
        }
    }
}
