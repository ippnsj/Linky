package org.poolc.linky.retrofit.service

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface RetrofitService {
    // verify token
    @GET("/token/valid")
    fun verifyToken(): Call<Void>

    // verify email
    @GET("/email/valid")
    fun verifyEmail(
        @Query("email") email:String
    ): Call<Void>

    // register
    @POST("/member")
    fun register(
        @Body body: RequestBody
    ): Call<String>

    // login
    @POST("/login")
    fun login(
        @Body body:RequestBody
    ): Call<JsonElement>

    // get profile
    @GET("/member/me")
    fun getProfile(): Call<JsonElement>

    // get follow preview
    @GET("/follow/small")
    fun getFollowPreview(): Call<JsonElement>

    // edit profile
    @PUT("/member/me")
    fun editProfile(
        @Body body: RequestBody
    ): Call<String>

    // get my folder & link
    @GET("/folder/me")
    fun read(
        @Query("path") path:String,
        @Query("showLink") showLink:String
    ): Call<JsonElement>

    // get other's folder & link
    @GET("/folder")
    fun readOther(
        @Query("email") email:String,
        @Query("path") path:String
    ): Call<JsonElement>

    // get link info
    @GET("/link")
    fun getLinkInfo(
        @Query("path") path:String,
        @Query("id") id:String
    ): Call<JsonElement>

    // create folder
    @POST("/folder")
    fun createFolder(
        @Body body: RequestBody
    ): Call<Void>

    // edit folder
    @PUT("/folder")
    fun editFolder(
        @Body body: RequestBody
    ): Call<Void>

    // move folder
    @PUT("/folder/path")
    fun moveFolder(
        @Body body: RequestBody
    ): Call<Void>

    // delete folder
    @HTTP(method="DELETE", hasBody=true, path="/folder")
    fun deleteFolder(
        @Body body: RequestBody
    ): Call<Void>

    // create link
    @POST("/link")
    fun createLink(
        @Body body: RequestBody
    ): Call<String>

    // edit link
    @PUT("/link")
    fun editLink(
        @Body body: RequestBody
    ): Call<String>

    // move link
    @PUT("/link/path")
    fun moveLink(
        @Body body: RequestBody
    ): Call<Void>

    // delete link
    @HTTP(method="DELETE", hasBody=true, path="/link")
    fun deleteLink(
        @Body body: RequestBody
    ): Call<Void>

    // follow by nickname
    @POST("/follow/nickname")
    fun follow(
        @Body body: RequestBody
    ): Call<JsonElement>

    // follow by email
    @POST("/follow/email")
    fun followByEmail(
        @Body body: RequestBody
    ): Call<JsonElement>

    // unfollow by email
    @HTTP(method="DELETE", hasBody=true, path="/follow/email")
    fun unfollowByEmail(
        @Body body: RequestBody
    ): Call<Void>

    // following
    @GET("/following")
    fun getFollowing(): Call<JsonElement>

    // follower
    @GET("/follower")
    fun getFollower(): Call<JsonElement>

    // search folder
    @GET("/folder/elastic")
    fun searchFolder(
        @Query("keyword") keyword:String,
        @Query("searchMe") searchMe:String
    ): Call<JsonElement>

    // search link
    @GET("/link/elastic")
    fun searchLink(
        @Query("keyword") keyword:String,
        @Query("searchMe") searchMe:String
    ): Call<JsonElement>

    // search user
    @GET("/user/elastic")
    fun searchUser(
        @Query("keyword") keyword: String
    ): Call<JsonElement>

    // get other user's profile
    @GET("/member/email")
    fun getUserProfile(
        @Query("otherEmail") otherEmail:String
    ): Call<JsonElement>
}