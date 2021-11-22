package codes.wiz.chatty

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import codes.wiz.chatty.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import java.lang.IllegalStateException

/*
 * Defines an array that contains column names to move from
 * the Cursor to the ListView.
 */
@SuppressLint("InlinedApi")
private val FROM_COLUMNS: Array<String> = arrayOf(
    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    } else {
        ContactsContract.Contacts.DISPLAY_NAME
    }
)
/*
 * Defines an array that contains resource ids for the layout views
 * that get the Cursor column contents. The id is pre-defined in
 * the Android framework, so it is prefaced with "android.R.id"
 */
private val TO_IDS: IntArray = intArrayOf(android.R.id.text1)

@SuppressLint("InlinedApi")
private val PROJECTION: Array<out String> = arrayOf(
    ContactsContract.Contacts._ID,
    ContactsContract.Contacts.LOOKUP_KEY,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
    else
        ContactsContract.Contacts.DISPLAY_NAME
)
// The column index for the _ID column
private const val CONTACT_ID_INDEX: Int = 0
// The column index for the CONTACT_KEY column
private const val CONTACT_KEY_INDEX: Int = 1
@SuppressLint("InlinedApi")
private val SELECTION: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?"
    else
        "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
// Defines a variable for the search string
private var searchString: String = "%"
// Defines the array to hold values that replace the ?
private val selectionArgs = arrayOf<String>(searchString)



class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cursorAdapter: SimpleCursorAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private var contactList = ArrayList<UserModel>()
    private var registeredUserList = ArrayList<UserModel>()
    private var totalList = ArrayList<UserModel>()
    private lateinit var mStore: FirebaseFirestore
    var contactId: Long = 0
    var contactKey: String? = null
    var contactUri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userAdapter = UserAdapter(this, totalList)
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = userAdapter


        mAuth = FirebaseAuth.getInstance()
        mStore = FirebaseFirestore.getInstance()
        mStore.collection("user").addSnapshotListener{ snapshot, e ->
            if(e!=null){
                Timber.i("listening to user data change failed!")
            }
            if(snapshot!=null){
                registeredUserList.clear()
                totalList.clear()
                for(userHashMap in snapshot){
                    if(userHashMap.data["uid"] == mAuth.currentUser?.uid) continue
                    val userModel = UserModel(userHashMap.data["email"] as String, userHashMap.data["name"] as String, userHashMap.data["uid"] as String)
                    registeredUserList.add(userModel)
                    Timber.i("userModel: ${userModel.email}, ${userModel.name}, ${userModel.uid}")
                }
                totalList.addAll(registeredUserList)
                totalList.addAll(contactList)
                userAdapter.notifyDataSetChanged()
            }
        }
//        supportActionBar?.hide()

//        // ListView test code
//        val dummies = arrayOf("Android", "PHP", "JavaScript")
//        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(
//            this, android.R.layout.simple_list_item_1, dummies
//        )
//        binding.contactsListView.adapter = arrayAdapter
//        binding.contactsListView.setOnItemClickListener{
//            adapterView, view, i, l ->
//            Toast.makeText(this, "Item ${i} Selected: ${dummies[i]}", Toast.LENGTH_SHORT).show()
//        }

//        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 200)
        checkPermission()

        cursorAdapter = SimpleCursorAdapter(this, R.layout.contacts_list_item, null, FROM_COLUMNS, TO_IDS, 0)
//        binding.contactsListView.adapter = cursorAdapter
//        binding.contactsListView.onItemClickListener = this
        supportLoaderManager.initLoader(0, null, this)




        Timber.i("onCreate Finished")

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        selectionArgs[0] = "%$searchString%"
        // Starts the query
//        return activity?.let {
//            return CursorLoader(
//                it,
//                ContactsContract.Contacts.CONTENT_URI,
//                PROJECTION,
//                SELECTION,
//                selectionArgs,
//                null
//            )
//        } ?: throw IllegalStateException()
        Timber.i("onCreateLoader called")
        return CursorLoader(
            this,
            ContactsContract.Contacts.CONTENT_URI,
            PROJECTION,
            SELECTION,
            selectionArgs,
            null
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        Timber.i("onLoadFinished called")
        cursorAdapter.swapCursor(data)
        if(contactList.size == 0){
            contactList = contactsFromCursor(data)
            if(contactList.size > 0){
                totalList.clear()
                totalList.addAll(registeredUserList)
                totalList.addAll(contactList)
                userAdapter.notifyDataSetChanged()
            }
            Timber.i("contactList[0].name = ${contactList?.get(0)?.name}")
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        cursorAdapter.swapCursor(null)
    }

    @SuppressLint("Range")
    private fun contactsFromCursor(cursor: Cursor): ArrayList<UserModel> {
        contactList.clear()
        if (cursor.count > 0) {
            cursor.moveToFirst()
            do {
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                contactList.add(UserModel(null, name, null))
            } while (cursor.moveToNext())
        }
        return contactList
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.logout){
            mAuth.signOut()
            val intent = Intent(this, LogInActivity::class.java)
            finish()
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkPermission(){
        if (ContextCompat.checkSelfPermission(this@MainActivity,
                Manifest.permission.READ_CONTACTS) !==
            PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                    Manifest.permission.READ_CONTACTS)) {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_CONTACTS), 1)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity,
                    arrayOf(Manifest.permission.READ_CONTACTS), 1)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.READ_CONTACTS) ===
                                PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}