package com.june.insta.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.june.insta.R
import kotlinx.android.synthetic.main.activity_add_photo_activitiy.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivitiy : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo_activitiy)

        // 스토리지 초기화 -> fun contentUpload 에서 사용함
        storage = FirebaseStorage.getInstance()

        //1. 액티비티 실행 시 암묵적 Intent 를 request code 와 함께 보내줌 -> onActivityResult
        //type 은 안드로이드 스토리지 내 파일 형식을 입력 *동영상일 경우 "video/*"
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }//onCreate

    //2. 암묵적 Intent 에서 이미지를 처리할 수 있는 Activity 를 열어주고, 사용자가 이미지를 선택했다면 requstCode, resultCode, data 를 응답으로 받음
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {
                photoUri = data?.data
                //레이아웃에 사용자가 선택한 이미지를 세팅
                addphoto_image.setImageURI(photoUri)
            } else {
                Log.d("log", "Photo Upload Canceled")
                finish()
            }
        }
    }//fun

    //3. 업로드 버튼을 누르면 파이어베이스 Storage 에 이미지 업로드
    fun contentUpload() {
        //이름이 중복되지 않도록 날짜로 파일 이름 만들고 storageRef 에 이미지가 저장될 폴더 설정
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.jpeg"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //파일 업로드
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
        }
    }//fun
}//class