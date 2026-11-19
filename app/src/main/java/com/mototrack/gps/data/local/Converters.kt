package com.mototrack.gps.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mototrack.gps.domain.model.TrackPoint

/**
 * Room TypeConverter to seamlessly serialize and deserialize lists of TrackPoint
 * into JSON text for local database persistence.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTrackPointList(points: List<TrackPoint>?): String {
        if (points.isNullOrEmpty()) return "[]"
        val type = object : TypeToken<List<TrackPoint>>() {}.type
        return gson.toJson(points, type)
    }

    @TypeConverter
    fun toTrackPointList(json: String?): List<TrackPoint> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<TrackPoint>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
