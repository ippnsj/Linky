package org.poolc.linky

class User {
    private var email : String = ""
    private var nickname : String = ""
    private var imageUrl : String = ""

    constructor(email:String, nickname:String, imageUrl:String) {
        this.email = email
        this.nickname = nickname
        this.imageUrl = imageUrl
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
}