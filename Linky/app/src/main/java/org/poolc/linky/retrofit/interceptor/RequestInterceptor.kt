package org.poolc.linky.retrofit.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import org.poolc.linky.MyApplication

class RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = MyApplication.sharedPref.getString("token", "")
        val request = chain.request().newBuilder()
            .addHeader("X-AUTH-TOKEN", token)
            .build()
        return chain.proceed(request)
    }
}