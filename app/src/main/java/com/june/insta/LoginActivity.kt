package com.june.insta

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        email_login_button.setOnClickListener {
            signinAndSignup()
        }

    }//onCreate

    /* 로그인 플랫폼 사용하지 않고 로그인 */
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            email_edittext.text.toString(),
            password_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                moveMainPage(task.result?.user)
            } else if (task.exception?.message.isNullOrEmpty()) {
                Log.d("insta", "[ Login Failed_1 ]")
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            } else {
                signinEmail()
            }
        }
    }//fun

    fun signinEmail() {
        auth?.signInWithEmailAndPassword(
            email_edittext.text.toString(),
            password_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //Login
                moveMainPage(task.result?.user)
            } else {
                Log.d("insta", "[ Login Failed_2 ]")
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }//fun

    fun moveMainPage(user: FirebaseUser?) {
        //파이어베이스에서 넘어온 유저 상태가 있을 경우, MainActivity 로 이동
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }//fun

}