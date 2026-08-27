package com.hora.jnana.models

import com.squareup.moshi.Json

data class MatchMakingRequest(
    val groom: MatchMakingProfile,
    val bride: MatchMakingProfile,
    val lang: String,
    val ayanamsa: String? = null
)

data class MatchMakingProfile(
    val name: String,
    val dob: String, // YYYY-MM-DD
    val tob: String, // HH:MM:SS
    val pob: String
)

data class MatchMakingResponse(
    val ayanamsa: String,
    @Json(name = "groom_info") val groomInfo: MatchPersonInfo,
    @Json(name = "bride_info") val brideInfo: MatchPersonInfo,
    @Json(name = "guna_milan") val gunaMilan: GunaMilan,
    val kootas: Map<String, KootaInfo>,
    @Json(name = "doshas_summary") val doshasSummary: DoshasSummary,
    @Json(name = "manglik_analysis") val manglikAnalysis: ManglikAnalysis
)

data class MatchPersonInfo(
    val name: String,
    val datetime: String,
    val location: String,
    @Json(name = "moon_rasi") val moonRasi: String,
    @Json(name = "moon_rasi_lord") val moonRasiLord: String,
    val nakshatra: String,
    @Json(name = "nakshatra_number") val nakshatraNumber: Int,
    @Json(name = "nakshatra_lord") val nakshatraLord: String,
    val pada: Int,
    val varna: String,
    val vashya: String,
    val yoni: String,
    val gana: String,
    val nadi: String
)

data class GunaMilan(
    @Json(name = "total_points") val totalPoints: Double,
    @Json(name = "max_points") val maxPoints: Double,
    val percentage: Double,
    val result: String,
    @Json(name = "is_recommended") val isRecommended: Boolean,
    @Json(name = "summary_message") val summaryMessage: String
)

data class KootaInfo(
    val name: String,
    @Json(name = "obtained_points") val obtainedPoints: Double,
    @Json(name = "max_points") val maxPoints: Double,
    @Json(name = "groom_attribute") val groomAttribute: String,
    @Json(name = "bride_attribute") val brideAttribute: String,
    @Json(name = "is_favorable") val isFavorable: Boolean,
    val dosha: String?,
    @Json(name = "parihara_applied") val pariharaApplied: Boolean,
    val description: String
)

data class DoshasSummary(
    @Json(name = "has_nadi_dosha") val hasNadiDosha: Boolean,
    @Json(name = "has_bhakoot_dosha") val hasBhakootDosha: Boolean,
    @Json(name = "has_gana_dosha") val hasGanaDosha: Boolean,
    @Json(name = "dosha_details") val doshaDetails: List<DoshaDetail>
)

data class DoshaDetail(
    val koota: String,
    val dosha: String,
    val description: String
)

data class ManglikAnalysis(
    @Json(name = "groom_manglik") val groomManglik: ManglikInfo,
    @Json(name = "bride_manglik") val brideManglik: ManglikInfo,
    @Json(name = "manglik_compatibility") val manglikCompatibility: String,
    val description: String
)

data class ManglikInfo(
    @Json(name = "is_manglik") val isManglik: Boolean,
    val status: String,
    @Json(name = "mars_house_lagna") val marsHouseLagna: Int,
    @Json(name = "mars_house_moon") val marsHouseMoon: Int
)
