package com.klintsoft.basketballcourts

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface BasketCourtsApiService {
    /*@GET("basketcourts.json")
    suspend fun getBasketCourts(): List<BasketCourt>*/

    @GET("/allbasketcourts")
    suspend fun getAllBasketCourts(): List<BasketCourt>

    @DELETE("/file/{id}")
    suspend fun deleteBasketCourt(@Path("id") id:String): Response<ResponseBody>

    /*@GET("/randombasketcourt")
    suspend fun getRandomBasketCourt(): BasketCourt*/

    @GET("/file/{id}")
    suspend fun getBasketCourtById(@Path("id") id:String): BasketCourt

    @POST("/newcourt")
    suspend fun uploadCourt(@Body requestBody: RequestBody)

    //@GET("restaurants.json?orderBy=\"r_id\"")
    //suspend fun getRestaurant(@Query("equalTo") id: Int): Map<String, Restaurant>

    @Multipart
    @POST("/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part)

    @Multipart
    @POST("/newcourtwithimage")
    suspend fun uploadCourtAndImage(
        @Part courtData: MultipartBody.Part,
        @Part image: MultipartBody.Part
    )
}