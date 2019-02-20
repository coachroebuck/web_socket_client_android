package com.coachroebuck.uploadexample

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class MyAdapter {

    private fun createFikiAPI(client: OkHttpClient): MyAppAPI? {
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://7eac23d1.ngrok.io/ws/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        return retrofit.create(MyAppAPI::class.java)
    }

    private fun getHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    fun requestAvailablePort(callback: Callback<Long>) {
        val api = createFikiAPI(getHttpClient())
        api?.requestAvailablePort()?.enqueue(callback)
    }

    fun openWebSocket(portId: Long, callback: Callback<JsonElement>): Call<JsonElement>? {
        val api = createFikiAPI(getHttpClient())
        return api?.openWebSocket(portId = portId)
    }
}
