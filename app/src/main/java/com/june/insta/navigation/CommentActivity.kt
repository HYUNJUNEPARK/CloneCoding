package com.june.insta.navigation

import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.june.insta.R
import com.june.insta.databinding.ActivityCommentBinding
import com.june.insta.databinding.ItemCommentBinding
import com.june.insta.navigation.model.AlarmDTO
import com.june.insta.navigation.model.ContentDTO
import com.june.insta.navigation.util.FcmPush
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.item_comment.view.*
import java.sql.Date
import java.util.*
import kotlin.collections.ArrayList

class CommentActivity : AppCompatActivity() {
    //[Variation for ViewBinding]
    private lateinit var binding : ActivityCommentBinding

    private var destinationUid : String? = null
    var contentUid : String? = null

//[START onCreate]
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        binding.commentRecyclerview.adapter = CommentRecyclerviewAdapter()
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        //'댓글 달기' 버튼을 눌렀을 때
        binding.commentBtnSend?.setOnClickListener {
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, binding.commentEditMessage.text.toString())
            binding.commentEditMessage.setText("")
        }
    }
//[END onCreate]

//[START 라사이클러뷰 어댑터/홀더]
    inner class CommentRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        private var comments : ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            FirebaseFirestore.getInstance()
                .collection("images")
                .document(contentUid!!)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    comments.clear()

                    if(querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

    //[START 리사이클러뷰 오버라이딩]
        //[1. START 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolderBinding = (holder as CustomViewHolder).binding

            //Long 타입으로 저장된 시간 데이터 -> 년월일 데이터 변환
            val timestamp = comments[position].timestamp
            val data = Date(timestamp!!)
            val dateFormat = SimpleDateFormat("yy-MM-dd")
            val strDate: String = dateFormat.format(data)
            viewHolderBinding.commentviewitemTextviewDate.text = strDate

            viewHolderBinding.commentviewitemTextviewComment.text = comments[position].comment
            viewHolderBinding.commentviewitemTextviewProfile.text = comments[position].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(viewHolderBinding.commentviewitemImageviewProfile)
                    }
                }
        }
        //[1. END 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]

         //[2. START getItemCount]
        override fun getItemCount(): Int {
            return comments.size
        }
        //[2. END getItemCount]

        //[3. START inner class CustomViewHolder,  override fun onCreateViewHolder 를 만든 것은 메모리를 적게 사용하기 위한 약속]
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        private inner class CustomViewHolder(val binding : ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)
        //[3. END inner class CustomViewHolder,  override fun onCreateViewHolder]
    //[END 리사이클러뷰 오버라이딩]
    }
//[END 라사이클러뷰 어댑터/홀더]

//[START 사용함수]
    //[START 사용함수 : 코멘트 알람]
    private fun commentAlarm(destinationUid : String, message : String){
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val msg = FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment) + " of " + message
        FcmPush.instance.sendMessage(destinationUid, "test favoriteAlarm", msg)
    }
    //[END 사용함수 : 코멘트 알람]
//[END 사용함수]
}