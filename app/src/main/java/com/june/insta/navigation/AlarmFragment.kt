package com.june.insta.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.june.insta.R
import com.june.insta.databinding.FragmentAlarmBinding
import com.june.insta.databinding.ItemCommentBinding
import com.june.insta.navigation.model.AlarmDTO
import java.sql.Date
import java.text.SimpleDateFormat

class AlarmFragment : Fragment() {
    //[Variation for Fragment ViewBinding]
    private var _binding : FragmentAlarmBinding? = null
    private val binding get() = _binding!!

//[START onCreateView]
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerviewAdapter()
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        return binding.root
    }
//[END onCreateView]

//[START onDestroyView]
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
//[END onDestroyView]

//[START 라사이클러뷰 어댑터/홀더]
    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        private var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid).addSnapshotListener{ querySnapshot, firebaseFirestoreException ->
                alarmDTOList.clear()

                if (querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        //[1. START 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolderBinding = (holder as CustomViewHolder).binding

            //프로필 이미지 세팅
            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result["image"]

                    Glide.with(this@AlarmFragment).load(url).apply(RequestOptions().circleCrop()).into(viewHolderBinding.commentviewitemImageviewProfile)
                }
            }

            //Long 타입으로 저장된 시간 데이터 -> 년월일 데이터 변환
            val timestamp = alarmDTOList[position].timestamp
            val date = Date(timestamp!!)
            val dateFormat = SimpleDateFormat("yy-MM-dd")
            val strDate: String = dateFormat.format(date)
            viewHolderBinding.commentviewitemTextviewDate.text = strDate

            //알람 분류
            when(alarmDTOList[position].kind){
                //'좋아요' 알람
                0 -> {
                    val like = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    viewHolderBinding.commentviewitemTextviewProfile.text = like
                }
                //'메세지' 알람
                1 -> {
                    val msg = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment)+" - "+alarmDTOList[position].message
                    viewHolderBinding.commentviewitemTextviewProfile.text = msg
                }
                //'팔로우' 알람
                2 -> {
                    val follow = alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    viewHolderBinding.commentviewitemTextviewProfile.text = follow
                }
            }
            viewHolderBinding.commentviewitemTextviewComment.visibility = View.INVISIBLE
        }
        //[1. END 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]

        //[2. START getItemCount]
        override fun getItemCount(): Int {
            return alarmDTOList.size
        }
        //[2. END getItemCount]

        //[3. START inner class CustomViewHolder,  override fun onCreateViewHolder 를 만든 것은 메모리를 적게 사용하기 위한 약속]
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }
        inner class CustomViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)
        //[3. END inner class CustomViewHolder,  override fun onCreateViewHolder]
    }
//[END 라사이클러뷰 어댑터/홀더]
}