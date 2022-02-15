package com.june.insta.navigation


import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.firestore.Query
import com.june.insta.R
import com.june.insta.databinding.FragmentDetailBinding
import com.june.insta.databinding.ItemDetailBinding
import com.june.insta.navigation.model.AlarmDTO
import com.june.insta.navigation.model.ContentDTO
import com.june.insta.navigation.util.FcmPush

class DetailViewFragment : Fragment() {
    //[Variation for Fragment ViewBinding]
    private var _binding : FragmentDetailBinding? = null
    private val binding get() = _binding!!

    //[Variation for using firebase]
    var firestore: FirebaseFirestore? = null
    var uid : String? = null

//[START onCreateView]
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)


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
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        private var contentUidList: ArrayList<String> = arrayListOf()

        init {
            //게시글 'DESCENDING 속성' 부여로 가장 최근 게시글이 맨 위에 업로드
            firestore?.collection("images")?.orderBy("timestamp", Query.Direction.DESCENDING)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if (querySnapshot == null) return@addSnapshotListener
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()
                }
        }

    //[START 리사이클러뷰 오버라이딩]
        //[1. START 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolderBinding = (holder as CustomViewHolder).binding

            viewHolderBinding.detailviewitemProfileTextview.text = contentDTOs!![position].userId
            viewHolderBinding.detailviewitemExplainTextview.text = contentDTOs!![position].explain
            viewHolderBinding.detailviewitemFavoritecounterTextview.text = "좋아요 " + contentDTOs!![position].favoriteCount+"개"

            //프로필 이미지 세팅
            FirebaseFirestore.getInstance().collection("profileImages").document(contentDTOs[position].uid!!).get().addOnCompleteListener { task->
                if (task.isSuccessful){
                    val url = task.result["image"]

                    Glide.with(this@DetailViewFragment).load(url).apply(RequestOptions().circleCrop()).into(viewHolderBinding.detailviewitemProfileImage)
                }

            }

            //게시글 메인 이미지 세팅
            Glide.with(this@DetailViewFragment).load(contentDTOs!![position].imageUrl).into(viewHolderBinding.detailviewitemImageviewContent)

            //하트색(검 <-> 흰)
            if(contentDTOs!![position].favorites.containsKey(uid)){ view
                viewHolderBinding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewHolderBinding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //1-1. '좋아요' 클릭 -> 좋아요 수 세팅
            viewHolderBinding.detailviewitemFavoriteImageview.setOnClickListener {
                Log.d("checkLog","Like Button Clicked")
                favoriteEvent(position)
            }

            //1-2. 프로필 이미지가 클릭 -> UserFragment 로 이동
            viewHolderBinding.detailviewitemProfileImage.setOnClickListener {
                Log.d("checkLog","Profile Image Clicked")
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

            //1-3. 코멘트 이미지가 클릭 -> CommentActivity 로 이동
            viewHolderBinding.detailviewitemCommentImageview.setOnClickListener { v ->
                Log.d("checkLog","Comment Button Clicked")
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }
        //[1. END 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]

        //[2. START getItemCount]
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        //[2. END getItemCount]

        //[3. START inner class CustomViewHolder,  override fun onCreateViewHolder 를 만든 것은 메모리를 적게 사용하기 위한 약속]
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var binding = ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }
        inner class CustomViewHolder(val binding: ItemDetailBinding) : RecyclerView.ViewHolder(binding.root)
        //[3. END inner class CustomViewHolder,  override fun onCreateViewHolder]
    //[END 리사이클러뷰 오버라이딩]

    //[START 바인딩뷰홀더에서 사용되는 함수 : 좋아요 이벤트]
        //[1. START 좋아요 카운팅 세팅]
        private fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            firestore?.runTransaction { transaction ->
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)){
                    contentDTO.favoriteCount = contentDTO.favoriteCount -1
                    contentDTO.favorites.remove(uid)
                }else{
                    contentDTO.favoriteCount = contentDTO.favoriteCount +1
                    contentDTO.favorites[uid!!] = true

                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO)
            }
        }
        //[1. END 좋아요 카운팅 세팅]

        //[2. START 좋아요 알람]
        private fun favoriteAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "test favoriteAlarm", message)
        }
        //[2. END 좋아요 알람]
    //[END 바인딩뷰홀더에서 사용되는 함수 : 좋아요 이벤트]
    }
//END 라사이클러뷰 어댑터/홀더]
}
