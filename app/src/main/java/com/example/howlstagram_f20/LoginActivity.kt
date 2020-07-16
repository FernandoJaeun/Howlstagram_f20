package com.example.howlstagram_f20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_sign_in_btn.setOnClickListener(){signInAndSignUp()}
    }

    private fun signInAndSignUp() {
        auth.createUserWithEmailAndPassword(email_edit_text.text.toString(), password_edit_text.text.toString())
            ?.addOnCompleteListener(this) { task ->
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
            ?.addOnCompleteListener() { task ->
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

    private fun moveMainPage(user: FirebaseUser?){
        if(user != null){
            startActivity((Intent(this, MainActivity::class.java)))
        }

    }


}

