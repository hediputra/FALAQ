package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "falak_locations")
data class FalakLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double, // Negative for LS
    val longitude: Double, // Positive for BT
    val altitude: Double, // TT in meters above sea level
    val timeZone: Double, // TZ offset
    val isDefault: Boolean = false
)
