package com.example.howlstagram_f20

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*
import org.w3c.dom.Text

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()

        email_loginbtn.setOnClickListener { createAccount(email_edittext.text.toString(), password_edittext.text.toString()) }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth?.currentUser
        updateUI(currentUser)
    }

    private fun createAccount(email: String, password: String) {
        // show Progress Bar()
        auth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Hello! My new User", Toast.LENGTH_SHORT).show()
                moveMainPage(task.result?.user)
            }else{
                Toast.makeText(this, "Ooops, something is happened",Toast.LENGTH_LONG).show()
            }
        }
        // hide progress Bar()
    }

    private fun moveMainPage(user: FirebaseUser?) {
        val intent = Intent(this, MainActivity::class.java).apply { user }
        startActivity(intent)
    }


    private fun updateUI(currentUser: FirebaseUser?) {
    }

}

