package com.hora.jnana.models

import android.net.Uri

data class SavedKundali(
    val name: String,
    val date: String,
    val time: String,
    val locationName: String?,
    val lat: Double?,
    val lon: Double?,
    val dashaResponse: DashaResponse? = null,
    val kundaliResponse: KundaliResponse? = null,
    val chartUrl: String? = null,
    val svgContent: String? = null,
    val chartStyle: String = "south"
)

data class SavedKundaliMeta(
    val name: String,
    val date: String,
    val time: String,
    val locationName: String?,
    val uri: Uri
)
