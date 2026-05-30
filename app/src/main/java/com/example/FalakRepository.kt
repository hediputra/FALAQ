package com.example

import kotlinx.coroutines.flow.Flow

class FalakRepository(private val dao: FalakLocationDao) {
    val allLocations: Flow<List<FalakLocation>> = dao.getAllLocations()

    suspend fun insert(location: FalakLocation): Long {
        return dao.insertLocation(location)
    }

    suspend fun delete(location: FalakLocation) {
        dao.deleteLocation(location)
    }
}
