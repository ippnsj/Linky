package org.poolc.linky

class Folder {
    private var folderName : String = ""
    private var isSelected : Boolean = false

    constructor(folderName:String, isSelected:Boolean) {
        this.folderName = folderName
        this.isSelected = isSelected
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
}