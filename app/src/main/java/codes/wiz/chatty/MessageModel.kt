package codes.wiz.chatty

class MessageModel(val content:String, val sender: String, val status: String, val mediaType: String) {
    constructor():this("", "", "", "")
    companion object {
        const val TYPE_USER_MESSAGE = 0
        const val TYPE_FRIEND_MESSAGE = 1
        const val TYPE_USER_IMAGE = 2
        const val TYPE_FRIEND_IMAGE =3
        const val STATUS_UNREAD = "UNREAD"
        const val STATUS_READ = "READ"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
        const val MEDIA_TYPE_TEXT = "TEXT"
    }
}