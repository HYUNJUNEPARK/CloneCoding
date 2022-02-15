package com.june.insta.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.june.insta.databinding.ActivityAddPhotoActivitiyBinding
import com.june.insta.navigation.model.ContentDTO
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivitiy : AppCompatActivity() {
    //[Variation for ViewBinding]
    private lateinit var binding : ActivityAddPhotoActivitiyBinding

    //[Variation for using Firebase database]
    var auth : FirebaseAuth? = null
    private var firestore : FirebaseFirestore? = null

    //[Variation for image upload to Firebase database]
    private var storage: FirebaseStorage? = null
    private var photoUri: Uri? = null
    private var pickImageFromAlbum = 0

//[START onCreate]
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPhotoActivitiyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //[START 이미지를 가져오기 위한 Implicit intent]
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, pickImageFromAlbum)
        //[END 이미지를 가져오기 위한 Implicit intent]

        //[START 파이어베이스 DB를 사용하기 위한 인스턴스 초기화]
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
         //[END 파이어베이스 DB를 사용하기 위한 인스턴스 초기화]

        binding.addphotoBtnUpload.setOnClickListener {
            contentUpload()
        }
    }
//[END onCreate]

//[START onActivityResult : Implicit intent 에서 이미지 처리하는 Activity 오픈 -> 사용자 이미지 선택 -> requestCode, resultCode, data 를 응답으로 받음]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageFromAlbum) {
            if (resultCode == Activity.RESULT_OK) {
                photoUri = data?.data
                binding.addphotoImage.setImageURI(photoUri)
                Toast.makeText(this, "이미지를 성공적으로 불러왔습니다.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "이미지를 불러오지 못했습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
//[END onActivityResult]

//[START contentUpload : 파이어베이스 DB 에 데이터 업로드]
    private fun contentUpload() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_" + timestamp + "_.jpeg"
        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()
                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.explain = binding.addphotoEditExplain.text.toString()
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
//[END contentUpload]
}