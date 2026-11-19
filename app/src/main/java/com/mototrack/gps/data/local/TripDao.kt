package com.mototrack.gps.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on motorcycle trip sessions.
 */
@Dao
interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startTimeMillis DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId LIMIT 1")
    suspend fun getTripById(tripId: Long): TripEntity?

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: Long)

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()
}
