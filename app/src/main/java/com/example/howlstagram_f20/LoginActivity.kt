package com.example.howlstagram_f20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private val GOOGLE_LOGIN_CODE = 9001
    private lateinit var auth: FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        this.googleSignInClient = GoogleSignIn.getClient(this, gso)

        email_sign_in_btn.setOnClickListener() { signInAndSignUp() }
        sign_in_by_google.setOnClickListener() { googleLogin() }
    }

    // 자동 로그인
//    override fun onStart() {
//        super.onStart()
//        moveMainPage(auth?.currentUser)
//    }
    private fun googleLogin() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result!!.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account)

            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    // Login
                    Toast.makeText(this, "구글 아이디로 로그인 되었습니다", Toast.LENGTH_SHORT).show()
                    moveMainPage(task.result!!.user)
                } else {
                    // Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInAndSignUp() {
        auth.createUserWithEmailAndPassword(email_edit_text.text.toString(), password_edit_text.text.toString())
            .addOnCompleteListener(this) { task ->
                when {
                    task.isSuccessful -> {
                        // Creating a user account
                        Toast.makeText(this, "회원가입 되었습니다", Toast.LENGTH_SHORT).show()
                        moveMainPage(task.result!!.user)
                    }
                    task.exception?.message.isNullOrEmpty() -> {
                        // Show the error message
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Login if you have account
                        signInEmail()
                    }
                }
            }
    }

    private fun signInEmail() {
        auth.signInWithEmailAndPassword(email_edit_text.text.toString(), password_edit_text.text.toString())
            .addOnCompleteListener() { task ->
                if (task.isSuccessful) {
                    //Login
                    Toast.makeText(this, "로그인 되었습니다", Toast.LENGTH_SHORT).show()
                    moveMainPage(task.result!!.user)
                } else {
                    // Show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            startActivity((Intent(this, MainActivity::class.java)))
            finish() // MainActivity가 꺼짐
        }

    }


}

