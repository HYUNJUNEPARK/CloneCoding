package com.june.insta

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.auth.GoogleAuthProvider
import com.june.insta.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    //[Variation For ViewBinding]
    private lateinit var binding : ActivityLoginBinding

    //[Variation For FirebaseAuth]
    private var auth: FirebaseAuth? = null

    //[Variation For Google Login]
    private var googleSignInClient: GoogleSignInClient? = null
    private var googleLoginCode = 9001
    private var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString((R.string.default_web_client_id)))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()

        binding.emailLoginButton.setOnClickListener {
            Log.d("checkLog","Email Login Button Clicked")
            if(binding.emailEdittext.text.isNullOrEmpty()){
                Toast.makeText(this, "ID/PW 를 확인해주세요", Toast.LENGTH_SHORT).show()
            }else{
                val id = binding.emailEdittext.text.toString()
                val pw = binding.passwordEdittext.text.toString()

                if(id.contains("@") && id.contains(".")){
                    signIn(id, pw)
                }else{
                    Toast.makeText(this, "ID/PW 를 확인해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.emailSignUpButton.setOnClickListener {
            Log.d("checkLog","Email SignUp Button Clicked")
            if(binding.emailEdittext.text.isNullOrEmpty()){
                Toast.makeText(this.applicationContext, "ID/PW 를 확인해주세요", Toast.LENGTH_SHORT).show()
            }else{
                val id = binding.emailEdittext.text.toString()
                val pw = binding.passwordEdittext.text.toString()

                if(id.contains("@") && id.contains(".")){
                    signUp(id, pw)
                }else{
                    Toast.makeText(this, "ID/PW 를 확인해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.googleSignInButton.setOnClickListener {
            Log.d("checkLog","Google Login Button Clicked")
            googleLogin()
        }

        binding.facebookLoginButton.setOnClickListener {
            Log.d("checkLog","Facebook Login Button Clicked")
            facebookLogin()
        }
    }

//    //[START 자동 로그인 기능]
//    override fun onStart() {
//        super.onStart()
//        moveMainPage(auth?.currentUser)
//    }
//    //[END 자동 로그인 기능]




    //[START 페이스북 로그인]
    private fun facebookLogin(){
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("public_profile", "email"))

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult>{
            override fun onSuccess(result: LoginResult?) {
                handleFacebookAccessToken(result?.accessToken)
            }
            override fun onCancel() {
            }
            override fun onError(error: FacebookException?) {
            }
        })
    }
    fun handleFacebookAccessToken(token : AccessToken?){
        val credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("checkLog", "로그인 성공 MainActivity 오픈")
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }
//    fun printHashKey() {
//        try {
//            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
//            for (signature in info.signatures) {
//                val md = MessageDigest.getInstance("SHA")
//                md.update(signature.toByteArray())
//                val hashKey: String = String(Base64.encode(md.digest(), 0))
//                Log.i("checkLog", "printHashKey() Hash Key: $hashKey")
//            }
//        } catch (e: NoSuchAlgorithmException) {
//            Log.e("checkLog", "Can not print HashKey : NoSuchAlgorithmException", e)
//        } catch (e: Exception) {
//            Log.e("checkLog", "Can not print HashKey", e)
//        }
//    }
    //[END 페이스북 로그인]

    //[START 구글 로그인]
    private fun googleLogin() {
        val signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, googleLoginCode)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

        if (requestCode == googleLoginCode) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                Log.d("checkLog", "파이어베이스권한 : " + account.id)
                firebaseAuthWithGoogle(account)
            } else {
                Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("checkLog", "로그인 성공 MainActivity 오픈")
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //[START 구글 로그인]

    //[START 이메일 로그인]
    private fun signIn(id:String, pw:String) {
        auth?.signInWithEmailAndPassword(id, pw)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun signUp(id:String, pw:String) {
        auth?.createUserWithEmailAndPassword(id, pw)?.addOnCompleteListener { task ->
            when {
                task.isSuccessful -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                task.exception?.message.isNullOrEmpty() -> {
                    Log.d("checkLog", "User Account Null or Empty")
                    Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "로그인 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    //[END 이메일 로그인]
}