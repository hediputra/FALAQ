package com.example

import kotlin.math.*

// Data Class representing the complete breakdown of celestial and astronomical coordinates
data class HisabResult(
    val yearHijri: Int,
    val monthHijri: Int,
    val monthNameHijri: String,
    
    // Inputs
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timeZone: Double,
    
    // Step 1: Conjunction (Ijtima')
    val HY: Double,
    val K: Double,
    val T_conjunction: Double,
    val JD_conjunction_base: Double,
    val MT_corrections: List<Double>, // T1 to T13
    val MT_sum: Double,
    val ijtimaJD: Double,
    val conjunctionLocalDateStr: String, // Gregorian Date (e.g. 30 May 2026)
    val conjunctionLocalDayName: String, // (e.g. Sabtu)
    val conjunctionLocalPasaran: String, // (e.g. Pahing)
    val conjunctionLocalTimeStr: String, // (e.g. 15:45:12)
    val conjunctionLocalDayZ: Long,
    
    // Step 2: Solar Data at Sunset
    val sunsetJD: Double,
    val T_sunset: Double,
    val sunS: Double,
    val sun_m: Double,
    val sunN: Double,
    val sunKp: Double,
    val sunKpp: Double,
    val sunRp: Double,
    val sunRpp: Double,
    val sunQp: Double,
    val sunE: Double,
    val sunSp: Double,
    val sunDelta: Double,
    val sunPT: Double,
    val sunPT_adj: Double,
    val equationOfTime: Double,
    val sunSD: Double,
    val dipCorrection: Double,
    val virtualSunHeight: Double,
    val sunHourAngleT: Double,
    val rawGhurubLMT: Double,
    val ghurubLocalHours: Double,
    val ghurubTimeStr: String, // e.g. 17:45:23 WIB
    
    // Step 3: Lunar Data at Sunset
    val moonM: Double,
    val moonA: Double,
    val moonF: Double,
    val moonD: Double,
    val moonT1toT14: List<Double>,
    val moonC: Double,
    val moonMo: Double,
    val moonA_prime: Double,
    val hashVal: Double,
    val ampersandVal: Double,
    val L_prime: Double,
    val moonX: Double,
    val moonY: Double,
    val moonDeltaC: Double,
    val moonPTc: Double,
    val moonPTc_prime: Double,
    val moonHourAngleTc: Double,
    val geocentricMoonHeight_hc: Double,
    val moonDistance_P: Double,
    val p_prime: Double,
    val horizontalParallax_HP: Double,
    val moonSD: Double,
    val parallaxCorrection_Pr: Double,
    val refraction_Ref: Double,
    val topocentricMoonHeight_hc_prime: Double,
    
    // Azimuth & Differences
    val sunAzimuth: Double,
    val moonAzimuth: Double,
    val diffAzimuth_z: Double,
    val diffHeight_Dh: Double,
    val diffRightAscension_Dc: Double,
    val angularDistance_AL: Double,
    val crescentWidth_Cw: Double,
    val elongationGeocentric_EloG: Double,
    val phaseAngle_FIa: Double,
    val illuminatedFraction_Fl: Double,
    val moonsetLocalHours: Double,
    val moonsetLocalTimeStr: String,
    
    // Outputs
    val muktsulHilalMinutes: Double, // duration hilal above horizon
    val elongationTopocentric_EloTopo: Double,
    val moonAgeHours: Double, // age from conjunction to sunset
    
    // Visibility Verification
    val mavirAltitudeMeet: Boolean,   // hc' >= 3.0° (MABIMS)
    val elongationMeet: Boolean,      // EloTopo >= 6° 24' (6.4°)
    val isImkanuRukyat: Boolean       // Both parameters met
)

object HisabCalculator {

    // Helper degree trig functions
    private fun sinDeg(deg: Double) = sin(Math.toRadians(deg))
    private fun cosDeg(deg: Double) = cos(Math.toRadians(deg))
    private fun tanDeg(deg: Double) = tan(Math.toRadians(deg))
    private fun asinDeg(x: Double) = Math.toDegrees(asin(x))
    private fun acosDeg(x: Double) = Math.toDegrees(acos(x))
    private fun atanDeg(x: Double) = Math.toDegrees(atan(x))

    private fun mod360(valIn: Double): Double {
        var r = valIn % 360.0
        if (r < 0) r += 360.0
        return r
    }

    // Traditional month names (Hijriah)
    val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi'ul-Awwal", "Rabi'ul-Akhir",
        "Jumadil-Awwal", "Jumadil-Akhir", "Rajab", "Sya'ban",
        "Ramadan", "Syawwal", "Zulkaidah", "Zulhijah"
    )

    // Gregorian Month abbreviations
    private val gregorianMonths = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    // Day of week Indonesian names starting from Sunday (index 0)
    private val indonesianDays = listOf(
        "Ahad", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu"
    )

    // Traditional Javanese Pasaran names
    private val javanesePasarans = listOf(
        "Legi", "Pahing", "Pon", "Wage", "Kliwon"
    )

    // Decimal degrees to Degrees, Minutes, Seconds formatting (e.g., 6° 54' 12")
    fun formatDMS(deg: Double): String {
        val absVal = abs(deg)
        val d = absVal.toInt()
        val mFract = (absVal - d) * 60.0
        val m = mFract.toInt()
        val s = round((mFract - m) * 60.0).toInt()
        
        val sign = if (deg < 0) "-" else ""
        return "$sign$d° $m' $s\""
    }

    // Decimal hours to Time (HH:MM:SS) formatting
    fun formatTime(hours: Double): String {
        val absVal = abs(hours)
        val h = (absVal.toInt()) % 24
        val mFract = (absVal - absVal.toInt()) * 60.0
        val m = mFract.toInt()
        val s = round((mFract - m) * 60.0).toInt()
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // Julian Date to Gregorian Date
    // Returns Triple(Day, Month, Year)
    fun jdtoGregorian(jd: Double): Triple<Int, Int, Int> {
        val Z = (jd + 0.5).toLong()
        val F = (jd + 0.5) - Z
        
        val A = if (Z < 2299161L) {
            Z
        } else {
            val alpha = ((Z - 1867216.25) / 36524.25).toLong()
            Z + 1 + alpha - (alpha / 4)
        }
        
        val B = A + 1524
        val C = ((B - 122.1) / 365.25).toLong()
        val D = (365.25 * C).toLong()
        val E = ((B - D) / 30.6001).toLong()
        
        val day = (B - D - (30.6001 * E).toLong()).toInt()
        val month = (if (E < 14) E - 1 else E - 13).toInt()
        val year = (if (month > 2) C - 4716 else C - 4715).toInt()
        
        return Triple(day, month, year)
    }

    // Get Day of the Week name
    fun getDayOfWeek(z: Long): String {
        // Z=2440588 corresponds to 1970-01-01 (Thursday, index 4)
        val idx = (((z - 2440588) % 7 + 4) % 7 + 7) % 7
        return indonesianDays[idx.toInt()]
    }

    // Get Javanese Pasaran Name
    fun getPasaran(z: Long): String {
        // Z=2440588 corresponds to Wage (index 3)
        val idx = (((z - 2440588) % 5 + 3) % 5 + 5) % 5
        return javanesePasarans[idx.toInt()]
    }

    // Main calculation method
    fun calculate(
        yearHijri: Int,
        monthHijri: Int, // 1 to 12
        latitude: Double, // negative if LS (Lintang Selatan)
        longitude: Double, // positive if BT (Bujur Timur)
        altitude: Double, // meters
        timeZone: Double // e.g. 7.0 for WIB
    ): HisabResult {
        
        // 1. Calculate Conjunction (Ijtima')
        val M = monthHijri.toDouble()
        val Y = yearHijri.toDouble()
        
        val HY = Y + (M * 29.53) / 354.3671
        val K = (HY - 1410.0) * 12.0
        val T_conjunction = K / 1200.0
        
        val JD_conjunction_base = 2447740.652 + 29.53058868 * K + 0.0001178 * T_conjunction * T_conjunction
        
        val M_deg = mod360(207.9587074 + 29.10535608 * K - 0.0000333 * T_conjunction * T_conjunction)
        val M_prime_deg = mod360(111.1791307 + 385.81691806 * K + 0.0107306 * T_conjunction * T_conjunction)
        val F_deg = mod360(164.2162296 + 390.67050646 * K - 0.0016528 * T_conjunction * T_conjunction)
        
        val T1 = (0.1734 - 0.000393 * T_conjunction) * sinDeg(M_deg)
        val T2 = 0.0021 * sinDeg(2.0 * M_deg)
        val T3 = -0.4068 * sinDeg(M_prime_deg)
        val T4 = 0.0161 * sinDeg(2.0 * M_prime_deg)
        val T5 = -0.0004 * sinDeg(3.0 * M_prime_deg)
        val T6 = 0.0104 * sinDeg(2.0 * F_deg)
        val T7 = -0.0051 * sinDeg(M_deg + M_prime_deg)
        val T8 = -0.0074 * sinDeg(M_deg - M_prime_deg)
        val T9 = 0.0004 * sinDeg(2.0 * F_deg + M_deg)
        val T10 = -0.0004 * sinDeg(2.0 * F_deg - M_deg)
        val T11 = -0.0004 * sinDeg(2.0 * F_deg - M_deg) // duplicated correction
        val T12 = 0.0010 * sinDeg(2.0 * F_deg - M_prime_deg)
        val T13 = 0.0005 * sinDeg(M_deg + 2.0 * M_prime_deg)
        
        val MT_corrections = listOf(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)
        val MT_sum = MT_corrections.sum()
        
        val ijtimaJD = JD_conjunction_base + 0.5 + MT_sum
        
        // Local Conjunction time & date
        val localIjtimaJD = ijtimaJD + (timeZone / 24.0)
        val Z_conjunction_local = floor(localIjtimaJD + 0.5).toLong()
        val fract_local = (localIjtimaJD + 0.5) - Z_conjunction_local
        val conjunctionLocalHours = fract_local * 24.0
        
        val gregConjunctionTriple = jdtoGregorian(localIjtimaJD)
        val cDayName = getDayOfWeek(Z_conjunction_local)
        val cPasaran = getPasaran(Z_conjunction_local)
        val cDateStr = "${gregConjunctionTriple.first} ${gregorianMonths[gregConjunctionTriple.second - 1]} ${gregConjunctionTriple.third}"
        val tzSuffix = when (timeZone) {
            7.0 -> "WIB"
            8.0 -> "WITA"
            9.0 -> "WIT"
            else -> "GMT" + (if (timeZone >= 0) "+$timeZone" else "$timeZone")
        }
        val cTimeStr = "${formatTime(conjunctionLocalHours)} $tzSuffix"
        
        // 2. Solar Data at Sunset (Iterative Tadqiqi)
        // Conjunction Day
        val SD_Day = gregConjunctionTriple.first
        val SD_Month = gregConjunctionTriple.second
        val SD_Year = gregConjunctionTriple.third
        
        // Estimate UT Sunset as 11:00 UT
        var curUTSunset = 11.0
        var repSunsetJD = 0.0
        var repT_sunset = 0.0
        var repSunS = 0.0
        var repSun_m = 0.0
        var repSunN = 0.0
        var repSunKp = 0.0
        var repSunKpp = 0.0
        var repSunRp = 0.0
        var repSunRpp = 0.0
        var repSunQp = 0.0
        var repSunE = 0.0
        var repSunSp = 0.0
        var repSunDelta = 0.0
        var repSunPT = 0.0
        var repSunPT_adj = 0.0
        var repEquationOfTime = 0.0
        var repSunSD = 0.0
        var repDipCorrection = 0.0
        var repVirtualSunHeight = 0.0
        var repSunHourAngleT = 0.0
        var repRawGhurubLMT = 0.0
        var repGhurubLocalHours = 0.0
        
        // Run iteration 2 times to improve sunset calculations precision
        for (iter in 1..2) {
            // Find JD for sunset
            var sY = SD_Year
            var sM = SD_Month
            if (sM <= 2) {
                sY -= 1
                sM += 12
            }
            val sB = 2 - (sY / 100) + ((sY / 100) / 4)
            repSunsetJD = floor(365.25 * (sY + 4716)) + floor(30.6001 * (sM + 1)) + SD_Day + (curUTSunset / 24.0) + sB - 1524.5
            repT_sunset = (repSunsetJD - 2451545.0) / 36525.0
            
            // Solar positions
            repSunS = mod360(280.46645 + 36000.76983 * repT_sunset)
            repSun_m = mod360(357.52910 + 35999.05030 * repT_sunset)
            repSunN = mod360(125.04 - 1934.136 * repT_sunset)
            
            repSunKp = (17.264 / 3600.0) * sinDeg(repSunN) + (0.206 / 3600.0) * sinDeg(2.0 * repSunN)
            repSunKpp = (-1.264 / 3600.0) * sinDeg(2.0 * repSunS)
            repSunRp = (9.23 / 3600.0) * cosDeg(repSunN) - (0.090 / 3600.0) * cosDeg(2.0 * repSunN)
            repSunRpp = (0.548 / 3600.0) * cosDeg(2.0 * repSunS)
            
            repSunQp = 23.43929111 + repSunRp + repSunRpp - (46.8150 / 3600.0) * repT_sunset
            repSunE = (6898.06 / 3600.0) * sinDeg(repSun_m) + (72.095 / 3600.0) * sinDeg(2.0 * repSun_m) + (0.966 / 3600.0) * sinDeg(3.0 * repSun_m)
            
            repSunSp = repSunS + repSunE + repSunKp + repSunKpp - (20.47 / 3600.0)
            repSunDelta = asinDeg(sinDeg(repSunSp) * sinDeg(repSunQp))
            
            val pt = mod360(atanDeg(tanDeg(repSunSp) * cosDeg(repSunQp)))
            val spMod = mod360(repSunSp)
            repSunPT_adj = when {
                spMod in 90.0..270.0 -> pt + 180.0
                spMod in 270.0..360.0 -> pt + 360.0
                else -> pt
            }
            repSunPT_adj = mod360(repSunPT_adj)
            repSunPT = pt
            
            // Equation of Time (E) in hours
            repEquationOfTime = (-1.915 * sinDeg(repSun_m) - 0.02 * sinDeg(2.0 * repSun_m) + 2.466 * sinDeg(2.0 * repSunSp) - 0.053 * sinDeg(4.0 * repSunSp)) / 15.0
            
            repSunSD = 0.267 / (1.0 - 0.017 * cosDeg(repSun_m))
            repDipCorrection = (1.76 / 60.0) * sqrt(altitude)
            repVirtualSunHeight = - (repSunSD + (34.5 / 60.0) + repDipCorrection)
            
            // Hour Angle T
            val cos_T = (-tanDeg(latitude) * tanDeg(repSunDelta)) + (sinDeg(repVirtualSunHeight) / (cosDeg(latitude) * cosDeg(repSunDelta)))
            repSunHourAngleT = acosDeg(cos_T.coerceIn(-1.0, 1.0))
            
            // Sunset LMT and local Time (WIB)
            repRawGhurubLMT = (repSunHourAngleT / 15.0) + (12.0 - repEquationOfTime)
            repGhurubLocalHours = repRawGhurubLMT + ((timeZone * 15.0 - longitude) / 15.0)
            
            // Adjust current UT Sunset parameter for maximum precision recalculation
            curUTSunset = repGhurubLocalHours - timeZone
        }
        
        val ghurubTimeStr = "${formatTime(repGhurubLocalHours)} $tzSuffix"
        
        // 3. Lunar Position at Sunset
        val moonM_base = mod360(218.31617 + 481267.88088 * repT_sunset)
        val moonA_base = mod360(134.96292 + 477198.86753 * repT_sunset)
        val moonF_base = mod360(93.27283 + 483202.01873 * repT_sunset)
        val moonD_base = mod360(297.85027 + 445267.11135 * repT_sunset)
        
        // 14 lunar corrections:
        val L_T1 = (22640.0 / 3600.0) * sinDeg(moonA_base)
        val L_T2 = (-4586.0 / 3600.0) * sinDeg(moonA_base - 2.0 * moonD_base)
        val L_T3 = (2370.0 / 3600.0) * sinDeg(2.0 * moonD_base)
        val L_T4 = (769.0 / 3600.0) * sinDeg(2.0 * moonA_base)
        val L_T5 = (-668.0 / 3600.0) * sinDeg(repSun_m) // with sun m
        val L_T6 = (-412.0 / 3600.0) * sinDeg(2.0 * moonF_base)
        val L_T7 = (-212.0 / 3600.0) * sinDeg(2.0 * moonA_base - 2.0 * moonD_base)
        val L_T8 = (-206.0 / 3600.0) * sinDeg(moonA_base + repSun_m - 2.0 * moonD_base)
        val L_T9 = (192.0 / 3600.0) * sinDeg(moonA_base + 2.0 * moonD_base)
        val L_T10 = (-165.0 / 3600.0) * sinDeg(repSun_m - 2.0 * moonD_base)
        val L_T11 = (148.0 / 3600.0) * sinDeg(moonA_base - repSun_m)
        val L_T12 = (-125.0 / 3600.0) * sinDeg(moonD_base)
        val L_T13 = (-110.0 / 3600.0) * sinDeg(moonA_base + repSun_m)
        val L_T14 = (-55.0 / 3600.0) * sinDeg(2.0 * moonF_base - 2.0 * moonD_base)
        
        val moonT1toT14 = listOf(L_T1, L_T2, L_T3, L_T4, L_T5, L_T6, L_T7, L_T8, L_T9, L_T10, L_T11, L_T12, L_T13, L_T14)
        val moonC_sum = moonT1toT14.sum()
        
        val Mo_unmod = moonM_base + moonC_sum + repSunKp + repSunKpp - (20.47 / 3600.0)
        val moonMo = mod360(Mo_unmod)
        val moonA_prime = moonA_base + L_T2 + L_T3 + L_T5
        
        // Calculations for #, & and L'
        val hashVal = (18461.0 / 3600.0) * sinDeg(moonF_base) + (1010.0 / 3600.0) * sinDeg(moonA_base + moonF_base) + (1000.0 / 3600.0) * sinDeg(moonA_base - moonF_base)
        val ampersandVal = hashVal * sinDeg(moonA_base - moonF_base) - (624.0 / 3600.0) * sinDeg(moonF_base - 2.0 * moonD_base) - (199.0 / 3600.0) * sinDeg(moonF_base + 2.0 * moonD_base)
        val L_prime = ampersandVal * sinDeg(moonA_base - moonF_base - 2.0 * moonD_base) - (167.0 / 3600.0) * sinDeg(moonA_base + moonF_base - 2.0 * moonD_base)
        
        val moonX = atanDeg(sinDeg(moonMo) * tanDeg(repSunQp))
        val moonY = L_prime + moonX
        
        val sinDeltaC = sinDeg(moonMo) * sinDeg(repSunQp) * sinDeg(moonY) / sinDeg(moonX)
        val moonDeltaC = asinDeg(sinDeltaC.coerceIn(-1.0, 1.0))
        
        val cosPTc = (cosDeg(moonMo) * cosDeg(L_prime)) / cosDeg(moonDeltaC)
        val moonPTc = acosDeg(cosPTc.coerceIn(-1.0, 1.0))
        
        val moMod = mod360(moonMo)
        var moonPTc_prime = if (moMod in 180.0..360.0) {
            360.0 - moonPTc
        } else {
            moonPTc
        }
        moonPTc_prime = mod360(moonPTc_prime)
        
        // Lunar Hour Angle Tc
        val moonHourAngleTc = (repSunPT_adj - moonPTc_prime) + repSunHourAngleT
        val geocentricMoonHeight_hc = asinDeg(sinDeg(latitude) * sinDeg(moonDeltaC) + cosDeg(latitude) * cosDeg(moonDeltaC) * cosDeg(moonHourAngleTc))
        
        // Lunar Distance, Parallax & corrections
        val P_dist = (384401.0 * (1.0 - 0.0549 * 0.0549)) / (1.0 + 0.0549 * cosDeg(moonA_prime + L_T1))
        val p_prime = P_dist / 384401.0
        val horizontalParallax_HP = 0.9507 / p_prime
        val moonSD = (0.5181 / p_prime) / 2.0
        val parallaxCorrection_Pr = horizontalParallax_HP * cosDeg(geocentricMoonHeight_hc)
        
        val refraction_Ref = 0.0167 / tanDeg(geocentricMoonHeight_hc + 7.31 / (geocentricMoonHeight_hc + 4.4))
        
        // Topocentric altitude of moon
        val topocentricMoonHeight_hc_prime = geocentricMoonHeight_hc - parallaxCorrection_Pr + moonSD + refraction_Ref + repDipCorrection
        
        // Azimuths
        val sunAzimuth = mod360(atanDeg(-sinDeg(latitude) / tanDeg(repSunHourAngleT) + cosDeg(latitude) * tanDeg(repSunDelta) / sinDeg(repSunHourAngleT)) + 270.0)
        val moonAzimuth = mod360(atanDeg(-sinDeg(latitude) / tanDeg(moonHourAngleTc) + cosDeg(latitude) * tanDeg(moonDeltaC) / sinDeg(moonHourAngleTc)) + 270.0)
        
        // Azimuth, Height, Right Ascension differences
        val diffAzimuth_z = moonAzimuth - sunAzimuth
        val diffHeight_Dh = topocentricMoonHeight_hc_prime - repVirtualSunHeight
        val diffRightAscension_Dc = (moonPTc_prime - repSunPT_adj) / 15.0
        
        val angularDistance_AL = acosDeg((cosDeg(diffHeight_Dh) * cosDeg(diffAzimuth_z)).coerceIn(-1.0, 1.0))
        val crescentWidth_Cw = (1.0 - cosDeg(angularDistance_AL)) * moonSD * 60.0
        
        val elongationGeocentric_EloG = acosDeg((sinDeg(geocentricMoonHeight_hc) * sinDeg(repVirtualSunHeight) + cosDeg(geocentricMoonHeight_hc) * cosDeg(repVirtualSunHeight) * cosDeg(diffAzimuth_z)).coerceIn(-1.0, 1.0))
        val phaseAngle_FIa = acosDeg((-cosDeg(elongationGeocentric_EloG)).coerceIn(-1.0, 1.0))
        val illuminatedFraction_Fl = (1.0 + cosDeg(phaseAngle_FIa)) / 2.0
        
        // Moonset WIB
        val moonsetLocalHours = repGhurubLocalHours + diffRightAscension_Dc
        val moonsetLocalTimeStr = "${formatTime(moonsetLocalHours)} $tzSuffix"
        
        // Statistics: Moonset to Sunset duration (Muktsul Hilal)
        val muktsulHilalMinutes = diffRightAscension_Dc * 60.0
        
        // Topocentric Elongation
        val elongationTopocentric_EloTopo = acosDeg((sinDeg(topocentricMoonHeight_hc_prime) * sinDeg(repVirtualSunHeight) + cosDeg(topocentricMoonHeight_hc_prime) * cosDeg(repVirtualSunHeight) * cosDeg(diffAzimuth_z)).coerceIn(-1.0, 1.0))
        
        // Age of Moon in Hours
        val sunsetJDLocal = repSunsetJD + (timeZone / 24.0)
        val moonAgeHours = (sunsetJDLocal - localIjtimaJD) * 24.0
        
        // MABIMS visibility validation (Altitude >= 3.0° AND Elongation >= 6.4°)
        val mavirAltitudeMeet = topocentricMoonHeight_hc_prime >= 3.0
        val elongationMeet = elongationTopocentric_EloTopo >= 6.4
        val isImkanuRukyat = mavirAltitudeMeet && elongationMeet
        
        return HisabResult(
            yearHijri = yearHijri,
            monthHijri = monthHijri,
            monthNameHijri = hijriMonths[monthHijri - 1],
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            timeZone = timeZone,
            
            HY = HY,
            K = K,
            T_conjunction = T_conjunction,
            JD_conjunction_base = JD_conjunction_base,
            MT_corrections = MT_corrections,
            MT_sum = MT_sum,
            ijtimaJD = ijtimaJD,
            conjunctionLocalDateStr = cDateStr,
            conjunctionLocalDayName = cDayName,
            conjunctionLocalPasaran = cPasaran,
            conjunctionLocalTimeStr = cTimeStr,
            conjunctionLocalDayZ = Z_conjunction_local,
            
            sunsetJD = repSunsetJD,
            T_sunset = repT_sunset,
            sunS = repSunS,
            sun_m = repSun_m,
            sunN = repSunN,
            sunKp = repSunKp,
            sunKpp = repSunKpp,
            sunRp = repSunRp,
            sunRpp = repSunRpp,
            sunQp = repSunQp,
            sunE = repSunE,
            sunSp = repSunSp,
            sunDelta = repSunDelta,
            sunPT = repSunPT,
            sunPT_adj = repSunPT_adj,
            equationOfTime = repEquationOfTime,
            sunSD = repSunSD,
            dipCorrection = repDipCorrection,
            virtualSunHeight = repVirtualSunHeight,
            sunHourAngleT = repSunHourAngleT,
            rawGhurubLMT = repRawGhurubLMT,
            ghurubLocalHours = repGhurubLocalHours,
            ghurubTimeStr = ghurubTimeStr,
            
            moonM = moonM_base,
            moonA = moonA_base,
            moonF = moonF_base,
            moonD = moonD_base,
            moonT1toT14 = moonT1toT14,
            moonC = moonC_sum,
            moonMo = moonMo,
            moonA_prime = moonA_prime,
            hashVal = hashVal,
            ampersandVal = ampersandVal,
            L_prime = L_prime,
            moonX = moonX,
            moonY = moonY,
            moonDeltaC = moonDeltaC,
            moonPTc = moonPTc,
            moonPTc_prime = moonPTc_prime,
            moonHourAngleTc = moonHourAngleTc,
            geocentricMoonHeight_hc = geocentricMoonHeight_hc,
            moonDistance_P = P_dist,
            p_prime = p_prime,
            horizontalParallax_HP = horizontalParallax_HP,
            moonSD = moonSD,
            parallaxCorrection_Pr = parallaxCorrection_Pr,
            refraction_Ref = refraction_Ref,
            topocentricMoonHeight_hc_prime = topocentricMoonHeight_hc_prime,
            
            sunAzimuth = sunAzimuth,
            moonAzimuth = moonAzimuth,
            diffAzimuth_z = diffAzimuth_z,
            diffHeight_Dh = diffHeight_Dh,
            diffRightAscension_Dc = diffRightAscension_Dc,
            angularDistance_AL = angularDistance_AL,
            crescentWidth_Cw = crescentWidth_Cw,
            elongationGeocentric_EloG = elongationGeocentric_EloG,
            phaseAngle_FIa = phaseAngle_FIa,
            illuminatedFraction_Fl = illuminatedFraction_Fl,
            moonsetLocalHours = moonsetLocalHours,
            moonsetLocalTimeStr = moonsetLocalTimeStr,
            
            muktsulHilalMinutes = muktsulHilalMinutes,
            elongationTopocentric_EloTopo = elongationTopocentric_EloTopo,
            moonAgeHours = moonAgeHours,
            
            mavirAltitudeMeet = mavirAltitudeMeet,
            elongationMeet = elongationMeet,
            isImkanuRukyat = isImkanuRukyat
        )
    }
}
