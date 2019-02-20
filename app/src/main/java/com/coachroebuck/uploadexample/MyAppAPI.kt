package com.coachroebuck.uploadexample

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MyAppAPI {
    @GET("port.php")
    fun requestAvailablePort(): Call<Long>

    @GET("socket.php")
    fun openWebSocket(@Query("portId") portId: Long): Call<JsonElement>

}
