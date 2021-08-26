package com.june.insta

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null
    var googleSignInClient: GoogleSignInClient?= null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager: CallbackManager?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        //이메일 로그인
        email_login_button.setOnClickListener {
            signinAndSignup()
        }//setOnClickListener

        //SNS 로그인 1STEP
        google_sign_in_button.setOnClickListener {
            googleLogin()
        }//setOnClickListener

        facebook_login_button.setOnClickListener {
            //facebook login 1step
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString((R.string.default_web_client_id))) //google api 키를 넣어서 토큰을 요청함
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()


    }//onCreate
    /*페이스북 로그인*/
    //onCreate 에서 한번 호출해 로그로 찍힌 hashkey 를 얻는다
    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey: String = String(Base64.encode(md.digest(), 0))
                Log.i("log", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("log", "Can not print HashKey : NoSuchAlgorithmException", e)
        } catch (e: Exception) {
            Log.e("log", "Can not print HashKey", e)
        }
    }//fun

    fun facebookLogin(){
        //페이스북에서 프로필과 이메일을 요청함
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        //페이스북 로그인에 성공했을 때 넘어오는 부분
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                //facebook login 2step
                //로그인 성공 시 정보를 파이어베이스에 넘김
                handleFacebookAcessToken(result?.accessToken)
            }
            override fun onCancel() {
            }
            override fun onError(error: FacebookException?) {
            }
        })
    }//fun

    fun handleFacebookAcessToken(token : AccessToken?){
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //facebook login 3step
                //Login
                moveMainPage(task.result?.user)
            } else {
                Log.d("log", "[ Login Failed at handleFacebookAcessToken ]")
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }//fun


    /*구글 로그인*/
    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }//fun

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //onCreate 의 callbackManager 결과값이 onActivityResult 로 넘어옴
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                var account = result.signInAccount
                //SNS 로그인 2STEP
                firebaseAuthWithGoogle(account)
            }
        }
    }//fun

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                //Login
                moveMainPage(task.result?.user)
            } else {
                Log.d("log", "[ Login Failed at firebaseAuthWithGoogle ]")
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }//fun

    /* 이메일 로그인 */
    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(
            email_edittext.text.toString(),
            password_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                moveMainPage(task.result?.user)
            } else if (task.exception?.message.isNullOrEmpty()) {
                Log.d("log", "[ Login Failed at signinAndSignup ]")
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
                Log.d("log", "[ Login Failed at signinEmail ]")
                Toast.makeText(this, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }
    }//fun

    fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }//fun
}//class