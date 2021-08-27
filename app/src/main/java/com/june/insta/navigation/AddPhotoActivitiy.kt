package com.june.insta.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.june.insta.R
import com.june.insta.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo_activitiy.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivitiy : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo_activitiy)

        //fun contentUpload 에서 사용
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //1. 액티비티 실행 시 암묵적 Intent 를 request code 와 함께 보내줌 -> onActivityResult
        //type 은 안드로이드 스토리지 내 파일 형식을 입력 * 동영상일 경우 "video/*"
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        addphoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }//onCreate

    //2. 암묵적 Intent 에서 이미지를 처리할 수 있는 Activity 를 열어주고, 사용자가 이미지를 선택했다면 requestCode, resultCode, data 를 응답으로 받음
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

    //3. 업로드 버튼을 누르면 Storage 에 이미지 업로드하고 Firestore Database 에 나머지 데이터 업로드
    fun contentUpload() {
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.jpeg"
        //Storage 에 이미지를 저장
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //Collback method
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()
                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.explain = addphoto_edit_explain.text.toString()
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }//fun
}//class