package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FalakLocationDao {
    @Query("SELECT * FROM falak_locations ORDER BY isDefault DESC, name ASC")
    fun getAllLocations(): Flow<List<FalakLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: FalakLocation): Long

    @Delete
    suspend fun deleteLocation(location: FalakLocation)

    @Query("SELECT COUNT(*) FROM falak_locations")
    suspend fun getCount(): Int
}
