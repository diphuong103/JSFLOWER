package com.example.jsflower.ImgBB

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ImgBBApi {
    @Multipart
    @POST("1/upload")
    fun uploadImage(
        @Query("key") apiKey: String,
        @Part image: MultipartBody.Part
    ): Call<ImgBBResponse>
}