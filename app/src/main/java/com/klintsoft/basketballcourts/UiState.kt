package com.klintsoft.basketballcourts

import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class UiState(
    val setOfCourts: Set<BasketCourt> = setOf(),

    val currentLocation: Location? = null,
    val geoInfo: GeoLocationInfo? = null,

    //val courtImage: Bitmap? = null,
    val imageUri: Uri = Uri.EMPTY,

    val activeCourt: BasketCourt = BasketCourt(),
    val isSaveEnabled: Boolean = false,

    val oneBasketCourt: BasketCourt? = null,

    val showDeleteAlert: Boolean = false,
)

data class GeoLocationInfo(
    val countryName: String = "",
    val postalCode: String = "",
    val locality: String = "",
    val streetName: String = "",
    val streetNumber: String = "",
    val adminArea: String = "",
)

val valpurinpuisto = BasketCourt(
    name = "Valpurinpuisto school",
    district = "Meilahti",
    latitude = "60.193734",
    longitude = "24.898878",
    numberOfBaskets = 2
)

val basketCourtsDemoSet = List(6){ BasketCourt(name = "basketCourt$it")}.toSet()

@Serializable
data class BasketCourt(
    val id: Int? = null,
    val name: String = "",
    val latitude: String = "",
    val longitude: String =  "",
    val description: String = "",
    val district: String = "",
    val numberOfBaskets: Int = 0,
    val isClosedCourt: Boolean = false,
    val terrain: String = "",
    val isPaid: Boolean = false,
    val imageUrl: String = "",
)