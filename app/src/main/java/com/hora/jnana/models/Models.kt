package com.hora.jnana.models

import com.squareup.moshi.Json

data class AllResponse(
    val data: Map<String, Any> = emptyMap()
)

data class KundaliResponse(
    val date: String,
    val datetime: String,
    val timezone: String,
    val ayanamsa: String,
    val lagna: LagnaInfo,
    val houses: List<HouseInfo>,
    val planets: List<PlanetInfo>,
    val panchanga: Map<String, String>,
    @Json(name = "panchanga_details") val panchangaDetails: PanchangaDetails,
    @Json(name = "yogi_avayogi") val yogiAvayogi: YogiAvayogiInfo,
    val atmakaraka: KarakaInfo,
    val darakaraka: KarakaInfo,
    @Json(name = "chara_karakas") val charaKarakas: List<KarakaInfo>
)

data class KarakaInfo(
    val rank: Int,
    val karaka: String,
    @Json(name = "karaka_code") val karakaCode: String,
    val planet: String,
    val symbol: String,
    @Json(name = "degree_in_rasi") val degreeInRasi: Double,
    val longitude: Double,
    val rasi: String,
    @Json(name = "rasi_number") val rasiNumber: Int,
    @Json(name = "rasi_lord") val rasiLord: String,
    val house: Int,
    val nakshatra: String,
    @Json(name = "nakshatra_number") val nakshatraNumber: Int,
    @Json(name = "nakshatra_lord") val nakshatraLord: String,
    val pada: Int,
    @Json(name = "navamsha_rasi") val navamshaRasi: String,
    @Json(name = "navamsha_rasi_number") val navamshaRasiNumber: Int,
    val retrograde: Boolean,
    val signification: String
)

data class LagnaInfo(
    val rasi: String,
    val number: Int,
    val longitude: Double,
    @Json(name = "degree_in_rasi") val degreeInRasi: Double
)

data class HouseInfo(
    val house: Int,
    val rasi: String,
    val planets: List<String>
)

data class PlanetInfo(
    val planet: String,
    val symbol: String,
    val longitude: Double,
    @Json(name = "degree_in_rasi") val degreeInRasi: Double,
    val rasi: String,
    val house: Int,
    val retrograde: Boolean
)

data class PanchangaDetails(
    val tithi: TithiDetail,
    val nakshatra: NakshatraDetail,
    val yoga: YogaDetail,
    val karana: KaranaDetail
)

data class TithiDetail(
    val name: String,
    val number: Int,
    val progress: Double,
    @Json(name = "longitude_degrees") val longitudeDegrees: Double,
    @Json(name = "ends_at") val endsAt: String,
    val paksha: String,
    @Json(name = "lunar_day_number") val lunarDayNumber: Int,
    @Json(name = "paksha_day_number") val pakshaDayNumber: Int
)

data class NakshatraDetail(
    val name: String,
    val number: Int,
    val progress: Double,
    @Json(name = "longitude_degrees") val longitudeDegrees: Double,
    @Json(name = "ends_at") val endsAt: String,
    val pada: Int
)

data class YogaDetail(
    val name: String,
    val number: Int,
    val progress: Double,
    @Json(name = "longitude_degrees") val longitudeDegrees: Double,
    @Json(name = "ends_at") val endsAt: String
)

data class KaranaDetail(
    val name: String,
    val number: Int,
    val progress: Double,
    @Json(name = "longitude_degrees") val longitudeDegrees: Double,
    @Json(name = "ends_at") val endsAt: String
)

data class YogiAvayogiInfo(
    @Json(name = "yogi_planet") val yogiPlanet: String,
    @Json(name = "duplicate_yogi") val duplicateYogi: String,
    @Json(name = "avayogi_planet") val avayogiPlanet: String,
    @Json(name = "duplicate_avayogi") val duplicateAvayogi: String,
    @Json(name = "yogi_point") val yogiPoint: PointInfo,
    @Json(name = "avayogi_point") val avayogiPoint: PointInfo
)

data class PointInfo(
    val longitude: Double,
    @Json(name = "degree_in_rasi") val degreeInRasi: Double,
    val rasi: String,
    @Json(name = "rasi_number") val rasiNumber: Int,
    @Json(name = "rasi_lord") val rasiLord: String,
    val nakshatra: String,
    @Json(name = "nakshatra_number") val nakshatraNumber: Int,
    @Json(name = "nakshatra_lord") val nakshatraLord: String,
    val pada: Int,
    val house: Int
)

data class LocationData(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val description: String? = null
)

// Base fields shared by most responses
data class BaseApiResponse(
    val date: String,
    @Json(name = "local_date") val localDate: String,
    @Json(name = "vedic_day_date") val vedicDayDate: String,
    val datetime: String,
    val timezone: String,
    val location: String,
    val coordinates: Map<String, Double>,
    val ayanamsa: String
)

data class PanchangaResponse(
    val date: String,
    @Json(name = "vedic_day_date") val vedicDayDate: String,
    val panchanga: Map<String, String>,
    @Json(name = "panchanga_details") val panchangaDetails: Map<String, Any>,
    val moon: Map<String, Any>,
    val sun: Map<String, Any>
)

data class MuhurtaResponse(
    val date: String,
    @Json(name = "vedic_day_date") val vedicDayDate: String,
    val sunrise: String,
    val sunset: String,
    @Json(name = "sunrise_at") val sunriseAt: String,
    @Json(name = "sunset_at") val sunsetAt: String,
    val muhurta: Map<String, MuhurtaInterval>
)

data class HoraResponse(
    val date: String,
    @Json(name = "vedic_day_date") val vedicDayDate: String,
    val hora: HoraData,
    @Json(name = "day_hora") val dayHora: List<HoraListItem> = emptyList(),
    @Json(name = "night_hora") val nightHora: List<HoraListItem> = emptyList()
)

data class HoraListItem(
    val planet: String,
    val symbol: String,
    val number: Int,
    val starts: String? = null,
    @Json(name = "starts_at") val startsAt: String? = null,
    val ends: String,
    @Json(name = "ends_at") val endsAt: String
)

data class HoraData(
    val planet: String,
    val symbol: String,
    val number: Int,
    val period: String,
    @Json(name = "period_number") val periodNumber: Int,
    val started: String,
    val ends: String,
    @Json(name = "started_at") val startedAt: String,
    @Json(name = "ends_at") val endsAt: String,
    val remaining: String,
    @Json(name = "remaining_seconds") val remainingSeconds: Int,
    val next: String
)

data class DayResponse(
    val date: String,
    @Json(name = "vedic_day_date") val vedicDayDate: String,
    val sunrise: String,
    val sunset: String,
    @Json(name = "sunrise_at") val sunriseAt: String,
    @Json(name = "sunset_at") val sunsetAt: String,
    @Json(name = "next_sunrise_at") val nextSunriseAt: String,
    @Json(name = "solar_noon_at") val solarNoonAt: String,
    @Json(name = "daylight_midpoint_at") val daylightMidpointAt: String,
    @Json(name = "day_duration_seconds") val dayDurationSeconds: Double,
    @Json(name = "night_duration_seconds") val nightDurationSeconds: Double,
    val vara: String,
    @Json(name = "vara_sanskrit") val varaSanskrit: String
)

data class MuhurtaInterval(
    val name: String,
    val start: String,
    val end: String,
    val display: String,
    @Json(name = "duration_seconds") val durationSeconds: Double
)

data class DashaResponse(
    val date: String,
    val datetime: String,
    val timezone: String,
    val ayanamsa: String,
    @Json(name = "year_type") val yearType: String,
    val moon: MoonInfo,
    @Json(name = "dasha_balance") val dashaBalance: DashaBalance,
    @Json(name = "active_dasha") val activeDasha: ActiveDasha,
    val timeline: List<DashaPeriod>
)

data class MoonInfo(
    val longitude: Double,
    @Json(name = "degree_in_rasi") val degreeInRasi: Double,
    val rasi: String,
    @Json(name = "rasi_number") val rasiNumber: Int,
    val nakshatra: String,
    @Json(name = "nakshatra_number") val nakshatraNumber: Int,
    @Json(name = "nakshatra_lord") val nakshatraLord: String,
    @Json(name = "nakshatra_pada") val nakshatraPada: Int? = null
)

data class DashaBalance(
    val lord: String,
    @Json(name = "total_years") val totalYears: Double,
    @Json(name = "elapsed_years") val elapsedYears: Double,
    @Json(name = "remaining_years") val remainingYears: Double,
    @Json(name = "elapsed_fraction") val elapsedFraction: Double,
    @Json(name = "remaining_fraction") val remainingFraction: Double
)

data class ActiveDasha(
    val mahadasha: String,
    val antardasha: String? = null,
    val pratyantardasha: String? = null
)

data class DashaPeriod(
    val level: Int,
    val lord: String,
    val start: String,
    val end: String,
    @Json(name = "duration_years") val durationYears: Double,
    @Json(name = "sub_periods") val subPeriods: List<DashaPeriod> = emptyList()
)
