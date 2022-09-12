package org.poolc.linky

import org.json.JSONArray

class Link {
    private var id : String = ""
    private var keywords : JSONArray = JSONArray()
    private var linkTitle : String = ""
    private var imgUrl : String = ""
    private var url : String = ""
    private var isPublic : String = "false"
    private var isSelected : Boolean = false

    private var nickName : String = ""
    private var ownerId : String = ""
    private var path : String = ""
    private var following: Boolean = false

    constructor()

    constructor(id:String, keywords:JSONArray, linkTitle:String, imgUrl:String, url:String, isPublic:String, isSelected:Boolean) {
        this.id = id
        this.keywords = keywords
        this.linkTitle = linkTitle
        this.imgUrl = imgUrl
        this.url = url
        this.isPublic = isPublic
        this.isSelected = isSelected
    }

    constructor(id: String, keywords: JSONArray, linkTitle: String, imgUrl: String, url: String, nickName: String, ownerId: String, path: String) {
        this.id = id
        this.keywords = keywords
        this.linkTitle = linkTitle
        this.imgUrl = imgUrl
        this.url = url
        this.nickName = nickName
        this.ownerId = ownerId
        this.path = path
    }

    constructor(id: String, keywords: JSONArray, linkTitle: String, imgUrl: String, url: String, nickName: String, ownerId: String, path: String, following: Boolean) {
        this.id = id
        this.keywords = keywords
        this.linkTitle = linkTitle
        this.imgUrl = imgUrl
        this.url = url
        this.nickName = nickName
        this.ownerId = ownerId
        this.path = path
        this.following = following
    }

    fun getId() : String {
        return id
    }

    fun setId(id:String) {
        this.id = id
    }

    fun getKeywords() : JSONArray{
        return keywords
    }

    fun setKeywords(keywords:ArrayList<String>) {
        val keywordsJsonArr = JSONArray(keywords)
        this.keywords = keywordsJsonArr
    }

    fun getLinkTitle() : String {
        return linkTitle
    }

    fun setLinkTitle(title:String) {
        this.linkTitle = title
    }

    fun getImgUrl() : String {
        return imgUrl
    }

    fun setImgUrl(imgUrl:String) {
        this.imgUrl = imgUrl
    }

    fun getUrl() : String {
        return url
    }

    fun setUrl(url:String) {
        this.url = url
    }

    fun getIsPublic() : String {
        return isPublic
    }

    fun setIsPublic(isPublic:String) {
        this.isPublic = isPublic
    }

    fun getIsSelected() : Boolean {
        return isSelected
    }

    fun setIsSelected(isSelected:Boolean) {
        this.isSelected = isSelected
    }

    fun switchIsSelected() {
        isSelected = !isSelected
    }

    fun getNickname() : String {
        return nickName
    }

    fun getFollowing() : Boolean {
        return following
    }
}