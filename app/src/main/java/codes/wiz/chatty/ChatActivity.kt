package codes.wiz.chatty

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import codes.wiz.chatty.databinding.ActivityChatBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ChatActivity : AppCompatActivity() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private var messageList = ArrayList<MessageModel>()
    lateinit var receiverUid: String
    lateinit var receiverName: String
    lateinit var senderUid: String
    lateinit var senderRoom: String
    lateinit var receiverRoom: String
    lateinit var mStore: FirebaseFirestore
    val gsUrl = "gs://chatty-dca56.appspot.com"
    val mStor = Firebase.storage(gsUrl)
    val mStorRef = mStor.reference
    val PICK_IMAGE = 500
    private var pingStart: Long = 0
    private val longZero: Long = 0
    private var pingMessage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mStore = FirebaseFirestore.getInstance()
        receiverUid = intent.extras?.get("receiverUid") as String
        receiverName = intent.extras?.get("receiverName") as String
        senderUid = intent.extras?.get("senderUid") as String
        supportActionBar?.title = receiverName
        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid
        Timber.i("sender uid ${senderUid}")
//        viewModel = ViewModelProviders.of(this).get(ChatViewModel.class);
//        chatAdapter.data=viewModel.messages
//        subscribeOnAddMessage()

        setupBtnListener()

        messageAdapter = MessageAdapter(this, messageList)
        binding.chatRecyclerView.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecyclerView.adapter = messageAdapter

        setupRoom()

    }

    private fun setupBtnListener(){
        binding.btnSend.setOnClickListener {
            val message = binding.messageBox.text.toString()
            if(message != ""){
                pingStart = Calendar.getInstance().timeInMillis
                var indexEnd = 4
                if(message.length-1 < indexEnd) indexEnd = message.length - 1
                pingMessage = message.slice(0..indexEnd)
                val messageModel = MessageModel(
                    message,
                    senderUid,
                    MessageModel.STATUS_UNREAD,
                    MessageModel.MEDIA_TYPE_TEXT
                )
                mStore.collection("rooms").document(senderRoom)
                    .update("history", FieldValue.arrayUnion(messageModel))
                mStore.collection("rooms").document(receiverRoom)
                    .update("history", FieldValue.arrayUnion(messageModel))
                binding.messageBox.setText("")
            }
        }
        binding.btnImage.setOnClickListener{
            val intentGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(intentGallery, PICK_IMAGE)
        }
        binding.messageBox.setOnKeyListener{ v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                binding.btnSend.performClick()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            val imageUri = data?.data
            val filename = File(imageUri?.path as String).name
            Timber.i("image selected, URI: ${imageUri}, filename: ${filename}")
            val imgRef = mStorRef.child("media/images/${senderRoom}/${Calendar.getInstance().timeInMillis.toString()}/${filename}")
            val uploadTask = imgRef.putFile(imageUri)
            uploadTask.addOnFailureListener{
                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                Timber.i("Upload Failed: ${it.toString()}")
            }
            uploadTask.addOnSuccessListener { taskSnapShot ->
//                val imgModel = MessageModel(taskSnapShot.metadata?.md5Hash!!, senderUid, MessageModel.STATUS_UNREAD, MessageModel.MEDIA_TYPE_IMAGE)
                val imgModel = MessageModel(gsUrl+imgRef.path, senderUid, MessageModel.STATUS_UNREAD, MessageModel.MEDIA_TYPE_IMAGE)
                mStore.collection("rooms").document(senderRoom).update("history", FieldValue.arrayUnion(imgModel))
                mStore.collection("rooms").document(receiverRoom).update("history", FieldValue.arrayUnion(imgModel))
                Timber.i("Upload Success! path: ${imgModel.content}")
            }
        }
    }
    private fun setupRoom() {
        roomCheckAndSet(senderRoom)
        roomCheckAndSet(receiverRoom)
        mStore.collection("rooms").document(senderRoom).addSnapshotListener { snapshot, e ->
            Timber.i("sender room data changed!")
            if(pingStart!=longZero){
                val ping = Calendar.getInstance().timeInMillis - pingStart
                Toast.makeText(this, "Message: ${pingMessage}. Firestore Data Change Ping: ${ping.toString()}ms", Toast.LENGTH_SHORT).show()
                pingStart = longZero
            }

            if (e != null) {
                Timber.i("listening to chat room change failed!")
            }
            if (snapshot != null) {
                val cloudRoom = snapshot.toObject<ChatRoom>()
                if (cloudRoom != null) {
                    Timber.i("snapshot to list: ${cloudRoom.history.toString()}")
                    val startIndex = messageList.size
                    for (index in messageList.size until cloudRoom.history.size) {
                        messageList.add(cloudRoom.history[index])
                    }
                    messageAdapter.notifyItemRangeInserted(
                        startIndex,
                        messageList.size - startIndex
                    )
                    binding.chatRecyclerView.scrollToPosition(messageList.size-1)
                }
            }
        }
    }

    private fun roomCheckAndSet(room: String) {
        mStore.collection("rooms").document(room).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val doc = task.result
                if (doc != null && doc.exists()) {
                    Timber.i("room $room exists")
                } else {
                    mStore.collection("rooms").document(room).set(
                        ChatRoom(ArrayList<MessageModel>())
                    )
                }
            } else {
                Timber.i("room check failed: ${task.exception}")
            }
        }
    }

    class ChatRoom(val history: ArrayList<MessageModel> = ArrayList<MessageModel>()) {
        constructor() : this(ArrayList<MessageModel>())
    }
}

