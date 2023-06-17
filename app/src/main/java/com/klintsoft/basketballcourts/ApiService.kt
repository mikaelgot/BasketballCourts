package com.klintsoft.basketballcourts

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface BasketCourtsApiService {
    @GET("basketcourts.json")
    suspend fun getBasketCourts(): List<BasketCourt>

    //@GET("restaurants.json?orderBy=\"r_id\"")
    //suspend fun getRestaurant(@Query("equalTo") id: Int): Map<String, Restaurant>
}