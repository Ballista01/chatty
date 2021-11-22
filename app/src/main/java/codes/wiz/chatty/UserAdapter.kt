package codes.wiz.chatty

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber

class UserAdapter(val context: Context, val userList: ArrayList<UserModel>?): RecyclerView.Adapter<UserAdapter.UserViewHolder>(), View.OnClickListener {

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.contacts_list_item, parent, false)
        view.setOnClickListener(this)
        return UserViewHolder(view)
    }
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList?.get(position)
        holder.textName.text = currentUser?.name
        holder.avatar.text = currentUser?.name?.get(0).toString()
        if(currentUser?.uid == null){
//            Timber.i("Unregistered User: ${currentUser?.name}, ${currentUser?.email}, ${currentUser?.uid}")
            holder.textName.setTextColor(Color.GRAY)
        }else{
//            Timber.i("Registered User: ${currentUser?.name}, ${currentUser?.email}, ${currentUser?.uid}")
            holder.textName.setTextColor(Color.BLACK)
        }
    }
    override fun getItemCount(): Int {
        return if (userList != null) {
            userList.size
        } else 0
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onClick(item: View) {
        val pos = mRecyclerView.getChildLayoutPosition(item)
        if(userList?.get(pos)?.uid == null){
            Toast.makeText(context, "User Not Registered for This App", Toast.LENGTH_SHORT).show()
        }else{
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("senderUid", FirebaseAuth.getInstance().currentUser?.uid)
            intent.putExtra("receiverUid", userList?.get(pos)?.uid)
            intent.putExtra("receiverName", userList?.get(pos)?.name)
            context.startActivity(intent)
        }
//        Toast.makeText(context, "item ${userList?.get(pos)?.name} clicked",Toast.LENGTH_SHORT).show()

    }


    class UserViewHolder(item: View): RecyclerView.ViewHolder(item) {
        val textName = item.findViewById<TextView>(R.id.contact_name)
        val avatar = item.findViewById<TextView>(R.id.friend_avatar)
    }

}