package org.poolc.linky

import org.json.JSONArray

class Link {
    private var id : String = ""
    private var keywords : JSONArray
    private var linkTitle : String = ""
    private var imgUrl : String = ""
    private var url : String = ""
    private var isSelected : Boolean = false

    constructor(id:String, keywords:JSONArray, linkTitle:String, imgUrl:String, url:String, isSelected:Boolean) {
        this.id = id
        this.keywords = keywords
        this.linkTitle = linkTitle
        this.imgUrl = imgUrl
        this.url = url
        this.isSelected = isSelected
    }

    fun getKeywords() : JSONArray{
        return keywords
    }

    fun getLinkTitle() : String {
        return linkTitle
    }

    fun getImgUrl() : String {
        return imgUrl
    }

    fun getUrl() : String {
        return url
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
}