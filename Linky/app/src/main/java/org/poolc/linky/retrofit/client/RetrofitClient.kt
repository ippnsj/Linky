package org.poolc.linky.retrofit.client

import okhttp3.OkHttpClient
import org.poolc.linky.MyApplication
import org.poolc.linky.retrofit.interceptor.RequestInterceptor
import org.poolc.linky.retrofit.interceptor.ResponseInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient {
    private val baseUrl = "http://${MyApplication.ip}:${MyApplication.port}"

    private val interceptorClient = OkHttpClient().newBuilder()
        .addInterceptor(RequestInterceptor())
        .addInterceptor(ResponseInterceptor())
        .build()

    val retrofit: Retrofit = Retrofit.Builder().baseUrl(baseUrl)
        .client(interceptorClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}