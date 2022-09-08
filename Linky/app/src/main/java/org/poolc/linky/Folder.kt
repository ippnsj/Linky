package org.poolc.linky

class Folder {
    private var folderName : String = ""
    private var isSelected : Boolean = false
    private var nickName: String = ""
    private var ownerEmail: String = ""
    private var path: String = ""

    constructor(folderName:String, isSelected:Boolean) {
        this.folderName = folderName
        this.isSelected = isSelected
    }

    constructor(folderName: String, nickName: String, ownerEmail: String, path: String) {
        this.folderName = folderName
        this.nickName = nickName
        this.ownerEmail = ownerEmail
        this.path = path
    }

    fun getFolderName() : String {
       return folderName
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

    fun getOwnerEmail() : String {
        return ownerEmail
    }

    fun getPath() : String {
        return path
    }
}