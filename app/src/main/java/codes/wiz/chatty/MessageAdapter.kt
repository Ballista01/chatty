package codes.wiz.chatty

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.File
import java.lang.IllegalArgumentException

class MessageAdapter(val context: ChatActivity, var data: ArrayList<MessageModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        Timber.i("onCreateViewHolder called! Type: $viewType")
        return when(viewType) {
            MessageModel.TYPE_USER_MESSAGE -> {
                val view = LayoutInflater.from(context).inflate(R.layout.user_message_view, parent, false)
                UserMessageViewHolder(view)
            }
            MessageModel.TYPE_FRIEND_MESSAGE -> {
                val view = LayoutInflater.from(context).inflate(R.layout.friend_message_view, parent, false)
                FriendMessageViewHolder(view)
            }
            MessageModel.TYPE_USER_IMAGE -> {
                val view = LayoutInflater.from(context).inflate(R.layout.user_image_view, parent, false)
                UserImageViewHolder(view)
            }
            MessageModel.TYPE_FRIEND_IMAGE -> {
                val view = LayoutInflater.from(context).inflate(R.layout.friend_image_view, parent, false)
                FriendImageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid Message Type!")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(data[position].sender){
            context.senderUid -> if(data[position].mediaType == MessageModel.MEDIA_TYPE_TEXT) MessageModel.TYPE_USER_MESSAGE else MessageModel.TYPE_USER_IMAGE
            context.receiverUid -> if(data[position].mediaType == MessageModel.MEDIA_TYPE_TEXT) MessageModel.TYPE_FRIEND_MESSAGE else MessageModel.TYPE_FRIEND_IMAGE
            else -> -1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = data[position]
//        Timber.i("Binding ${position.toString()}: ${item.content} ")
        when(holder) {
            is UserMessageViewHolder -> {
                holder.messageContent.setText(data[position].content)
                Timber.i("Binding holder.messageContent.text to ${holder.messageContent.text}")
            }
            is FriendMessageViewHolder -> {
                holder.messageContent.setText(data[position].content)
                holder.avatar.setText(context.receiverName[0].toString())
            }
            is UserImageViewHolder -> {
//                val imageRef = context.mStorRef.child(data[position].content)
//                val imageRef = Firebase.storage("gs://chatty-dca56.appspot.com").reference.child(data[position].content)
                Timber.i("data[position].content storageUri: ${data[position].content}")
                val imageRef = context.mStor.getReferenceFromUrl(data[position].content)
                val localFile = File.createTempFile("images", "jpg")
                imageRef.getFile(localFile).addOnSuccessListener {
                    holder.messageContent.setImageURI(Uri.fromFile(localFile))
                }
                Timber.i("binding imageRef, path: ${imageRef.path}, data[position].content: ${data[position].content}")
//                Glide.with(context).load(imageRef).into(holder.messageContent)
            }
            is FriendImageViewHolder -> {
                holder.avatar.setText(context.receiverName[0].toString())
                Timber.i("data[position].content storageUri: ${data[position].content}")
                val imageRef = context.mStor.getReferenceFromUrl(data[position].content)
                val localFile = File.createTempFile("images", "jpg")
                imageRef.getFile(localFile).addOnSuccessListener {
                    holder.messageContent.setImageURI(Uri.fromFile(localFile))
                }
                Timber.i("binding imageRef, path: ${imageRef.path}, data[position].content: ${data[position].content}")
            }
            else -> throw IllegalArgumentException()
        }

    }

    override fun getItemCount(): Int {
        return data.size
    }

}



class UserMessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val messageContent = view.findViewById<TextView>(R.id.message)
}

class FriendMessageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val messageContent = view.findViewById<TextView>(R.id.message)
    val avatar = view.findViewById<TextView>(R.id.friend_avatar)
}

class UserImageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val messageContent = view.findViewById<ImageView>(R.id.imageMassage)
}

class FriendImageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val messageContent = view.findViewById<ImageView>(R.id.imageMassage)
    val avatar  = view.findViewById<TextView>(R.id.friend_avatar)
}
