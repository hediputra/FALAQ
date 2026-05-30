package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FalakDatabase.getDatabase(application, viewModelScope)
    private val repository = FalakRepository(db.falakLocationDao())

    // All available stored tracking locations
    val savedLocations: StateFlow<List<FalakLocation>> = repository.allLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Input parameters
    val inputYear = MutableStateFlow(1447)
    val inputMonth = MutableStateFlow(9) // Ramadan default
    
    val inputLatitudeStr = MutableStateFlow("-6.9463")
    val inputLongitudeStr = MutableStateFlow("106.0150")
    val inputAltitudeStr = MutableStateFlow("20")
    val inputTimeZoneStr = MutableStateFlow("7.0")
    val selectedLocationName = MutableStateFlow("Padepokan Pasantren An-Nirwana (Banten)")

    // Dynamic Hisab result calculation state
    private val _calculationResult = MutableStateFlow<HisabResult?>(null)
    val calculationResult: StateFlow<HisabResult?> get() = _calculationResult

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    init {
        // Run initial calculation with default constants
        performCalculation()
    }

    fun setLocationValues(loc: FalakLocation) {
        inputLatitudeStr.value = loc.latitude.toString()
        inputLongitudeStr.value = loc.longitude.toString()
        inputAltitudeStr.value = loc.altitude.toString()
        inputTimeZoneStr.value = loc.timeZone.toString()
        selectedLocationName.value = loc.name
        performCalculation()
    }

    fun performCalculation() {
        try {
            val yr = inputYear.value
            val mn = inputMonth.value
            
            val lat = inputLatitudeStr.value.toDoubleOrNull() ?: -6.9463
            val lon = inputLongitudeStr.value.toDoubleOrNull() ?: 106.0150
            val alt = inputAltitudeStr.value.toDoubleOrNull() ?: 20.0
            val tz = inputTimeZoneStr.value.toDoubleOrNull() ?: 7.0
            
            val result = HisabCalculator.calculate(
                yearHijri = yr,
                monthHijri = mn,
                latitude = lat,
                longitude = lon,
                altitude = alt,
                timeZone = tz
            )
            _calculationResult.value = result
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = "Format input tidak valid: ${e.localizedMessage}"
        }
    }

    fun saveCurrentLocation(name: String) {
        viewModelScope.launch {
            val lat = inputLatitudeStr.value.toDoubleOrNull() ?: return@launch
            val lon = inputLongitudeStr.value.toDoubleOrNull() ?: return@launch
            val alt = inputAltitudeStr.value.toDoubleOrNull() ?: 0.0
            val tz = inputTimeZoneStr.value.toDoubleOrNull() ?: 7.0
            
            val newLoc = FalakLocation(
                name = name,
                latitude = lat,
                longitude = lon,
                altitude = alt,
                timeZone = tz,
                isDefault = false
            )
            repository.insert(newLoc)
            selectedLocationName.value = name
        }
    }

    fun deleteLocation(loc: FalakLocation) {
        viewModelScope.launch {
            repository.delete(loc)
            if (selectedLocationName.value == loc.name) {
                selectedLocationName.value = "Padepokan Pasantren An-Nirwana (Banten)"
                inputLatitudeStr.value = "-6.9463"
                inputLongitudeStr.value = "106.0150"
                inputAltitudeStr.value = "20"
                inputTimeZoneStr.value = "7.0"
                performCalculation()
            }
        }
    }
}
