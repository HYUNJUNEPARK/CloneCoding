package com.june.insta

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.june.insta.databinding.ActivityMainBinding
import com.june.insta.navigation.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    //[Variation For ViewBinding]
    private lateinit var binding : ActivityMainBinding

//[START onCreate]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)

        //디테일 뷰 프레그먼트
        binding.bottomNavigation.selectedItemId = R.id.action_home
        //로그인을 실행하면 토큰을 저장함
        registerPushToken()
    }
//[END onCreate]

//[START 네비게이션]
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setToolbarDefault()
        when(item.itemId){
            //[1. START 홈 화면]
            R.id.action_home -> {
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            //[1. END 홈 화면]

            //[2. START DB 전체 이미지 보기]
            R.id.action_search -> {
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }
            //[2. START DB 전체 이미지 보기]

            //[3. START 사진 업로드]
            R.id.action_add_photo -> {
                //권한 승인을 하면 false 로 바뀌면서 바로 갤러리로 넘어갈 수 있도록 만든 변수
                var checker: Boolean = true
                //사진 업로드 버튼을 누르면 저장소 접근 권한을 물음
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

                while (checker){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        Log.d("checkLog","사진 업로드 activity 오픈")
                        checker = false
                        startActivity(Intent(this, AddPhotoActivitiy::class.java))
                    }
                }
                return true
            }
            //[3. END 사진 업로드]

            //[4. START 알람 확인 페이지]
            R.id.action_favorite_alarm -> {
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            //[4. END 알람 확인 페이지]

            //[5. START 유저 페이지]
            R.id.action_account -> {
                val userFragment = UserFragment()
                //유저 페이지 만들기
                var bundle = Bundle()
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
            //[5. END 유저 페이지]
        }
        return false
    }
//[END 네비게이션]

    //인스타 타이틀 보이기, 백버튼&유저네임 숨김.
    private fun setToolbarDefault(){
        toolbar_username.visibility = View.GONE
        toolbar_btn_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }//setToolbarDefault

    private fun registerPushToken(){
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
            task ->
            val token = task.result?.token
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String, Any>()
            map["pushToken"] = token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        }
    }//registerPushToken

//    override fun onStop() {
//        super.onStop()
//        FcmPush.instance.sendMessage("gyQsSR6D5RhHWpDJsyCTPx0XpJ02","hi", "bye")
//    }

    //UserFragment 에서 프로필 사진을 선택한 응답을 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            //uid 가 파일명
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }//onActivityResult
}