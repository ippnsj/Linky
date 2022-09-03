package org.poolc.linky

class User {
    private var email : String = ""
    private var nickname : String = ""
    private var imageUrl : String = ""
    private var following: Boolean = false

    constructor(email:String, nickname:String, imageUrl:String, following:Boolean) {
        this.email = email
        this.nickname = nickname
        this.imageUrl = imageUrl
        this.following = following
    }

    fun getEmail() : String {
        return email
    }

    fun getNickname() : String {
        return nickname
    }

    fun getImageUrl() : String {
        return imageUrl
    }

    fun getFollowing() : Boolean {
        return following
    }
}