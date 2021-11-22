package codes.wiz.chatty

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import codes.wiz.chatty.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var email = intent.getStringExtra("email").toString()
        binding.emailText.setText(email)
        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()

        binding.btnSignUp.setOnClickListener {
            email = binding.emailText.text.toString()
            val password = binding.passwordText.text.toString()
            val name = binding.nameText.text.toString()
            if(email=="" || password=="") {
                Toast.makeText(this@SignUpActivity, "Please enter email and password!", Toast.LENGTH_SHORT).show()
            }else if (password.length < 6) {
                Toast.makeText(this@SignUpActivity, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
            } else {
                signUp(email, name, password)
            }
        }
    }

    private fun signUp(email: String, name: String, password: String) {
        Timber.i("Creating user: email: ${email}, name: ${name}, password: ${password}")
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Timber.i("Sign Up Successful!")
                    addUserToDatabase(name, email, mAuth.currentUser?.uid!!)
                    val intent = Intent(this, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Sign Up Unsuccessful", Toast.LENGTH_SHORT).show()
                    Timber.i("SignUp Exception: ${task.exception.toString()}")
                }
            }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String) {
        mStore = Firebase.firestore
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "uid" to uid,
        )
        mStore.collection("user").document(uid).set(userMap)
    }
}