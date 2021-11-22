package codes.wiz.chatty

import android.content.ContentProviderClient
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import codes.wiz.chatty.databinding.ActivityLogInBinding
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import com.facebook.FacebookException

import com.facebook.login.LoginResult

import com.facebook.FacebookCallback

import android.R
import android.telecom.Call
import com.facebook.AccessToken

import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider


class LogInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding
    private lateinit var mAuth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    val RC_SIGN_IN = 1234
    private lateinit var gso: GoogleSignInOptions
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    val googleClientString = "99168862354-e6of9ksp33b3mon7ekbfvhdo979giu1h.apps.googleusercontent.com"


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.plant(Timber.DebugTree())
        Timber.i("onCreate called")
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientString)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

//        val btnSignUp = findViewById<View>(R.id.btn_sign_up)
//        btnSignUp.setOnClickListener{
//            Timber.i("signup onclick listener called")
//            val intent = Intent(this, SignUpActivity::class.java)
//            startActivity(intent)
//        }

        binding.btnSignUp.setOnClickListener{
            Timber.i("signup onclick listener called")
            val intent = Intent(this, SignUpActivity::class.java)
            intent.putExtra("email", binding.emailText.text.toString())
            startActivity(intent)
        }
        binding.btnLogIn.setOnClickListener{
            val email = binding.emailText.text.toString()
            val password = binding.passwordText.text.toString()
            if(email!="" && password!="") login(email, password)
            else Toast.makeText(this@LogInActivity, "Please enter email and password!", Toast.LENGTH_SHORT).show()
        }
        binding.btnGoogleLogin.setOnClickListener{
            googleSignIn()
        }
        setupFacebookSignIn()
    }

    private fun login(email: String, password: String){
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful){
                    Timber.i("Sign Up Successful!")
                    val intent = Intent(this, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                }else{
                    Toast.makeText(this, "Log In Unsuccessful", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create();
        val buttonFacebookLogin = binding.loginButton as LoginButton

        buttonFacebookLogin.setReadPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Timber.i("facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Timber.i("facebook:onCancel")
                Toast.makeText(applicationContext, "Facebook SignIn button canceled", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: FacebookException) {
                Timber.i("facebook:onError ${error.toString()}")
                Toast.makeText(applicationContext, "Facebook SignIn button error: ${error.toString()}", Toast.LENGTH_LONG).show()
            }
        })


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Timber.i("firebaseAuthWithGoogle: ${account.id}")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Timber.e("Google sign in failed: ${e.toString()}")
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.i("signInWithCredential:success")
                    val currentUser = mAuth.currentUser
                    val intent = Intent(this, MainActivity::class.java)
                    addUserToDatabase(currentUser!!.displayName!!, currentUser!!.email!!, currentUser.uid)
                    finish()
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Timber.i("signInWithCredential:failure ${task.exception}")
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken){
        Timber.i("handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.i("signInWithCredential:success")
                    currentUser = mAuth.currentUser
                    val intent = Intent(this, MainActivity::class.java)
                    addUserToDatabase(currentUser!!.displayName!!, currentUser!!.email!!, currentUser!!.uid)
                    finish()
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Timber.i("signInWithCredential:failure${task.exception}")
                    Toast.makeText(baseContext, "Authentication failed. ${task.exception}",
                        Toast.LENGTH_LONG).show()
                }
            }

    }
    private fun addUserToDatabase(name: String, email: String, uid: String) {
        val mStore = Firebase.firestore
        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "uid" to uid,
        )
        mStore.collection("user").document(uid).set(userMap)
    }
}