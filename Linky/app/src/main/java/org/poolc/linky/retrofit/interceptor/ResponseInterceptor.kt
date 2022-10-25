package org.poolc.linky.retrofit.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import org.poolc.linky.MyApplication

class ResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        when(response.code()) {
            403 -> {
                MyApplication.instance.tokenExpired()
            }
        }
        return response
    }
}