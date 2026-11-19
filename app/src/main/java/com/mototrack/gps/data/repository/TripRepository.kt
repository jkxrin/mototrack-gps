package com.mototrack.gps.data.repository

import com.mototrack.gps.data.local.TripDao
import com.mototrack.gps.data.local.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for motorcycle trip persistence and queries.
 */
class TripRepository(private val tripDao: TripDao) {

    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    suspend fun saveTrip(trip: TripEntity): Long = withContext(Dispatchers.IO) {
        tripDao.insertTrip(trip)
    }

    suspend fun getTripById(id: Long): TripEntity? = withContext(Dispatchers.IO) {
        tripDao.getTripById(id)
    }

    suspend fun deleteTrip(id: Long) = withContext(Dispatchers.IO) {
        tripDao.deleteTripById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        tripDao.clearAllTrips()
    }
}
