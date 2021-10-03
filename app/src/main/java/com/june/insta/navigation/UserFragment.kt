package com.june.insta.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.june.insta.LoginActivity
import com.june.insta.MainActivity
import com.june.insta.R
import com.june.insta.databinding.FragmentUserBinding
import com.june.insta.navigation.model.AlarmDTO
import com.june.insta.navigation.model.ContentDTO
import com.june.insta.navigation.model.FollowDTO
import com.june.insta.navigation.util.FcmPush
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    //[Variation for Fragment ViewBinding]
    private var _binding : FragmentUserBinding? = null
    private val binding get() = _binding!!

    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    private var currentUserUid : String? = null

    //static 변수라서 MainActivity 에서도 접근 가능
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

//[START onCreateView]
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentUserBinding.inflate(inflater, container, false)

        //DetailViewFragment 에서 넘어온 uid 을 받아옴
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        //[START uid 에 따른 UserFragment 세팅(if-else)]
        //1. 내가 내 유저페이지를 보는 경우
        if (uid == currentUserUid){
            //로그아웃 활성화
            binding.accountBtnFollowSignout?.text = getString(R.string.signout)

            //로그아웃 버튼 클릭 -> LoginActivity 로 이동
            binding.accountBtnFollowSignout?.setOnClickListener {
                Log.d("checkLog","Logout Button Clicked")
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
            }

            //프로필 이미지 클릭 -> 갤러리 오픈
            binding.accountIvProfile?.setOnClickListener {
                Log.d("checkLog","Profile Image Clicked")

                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"

                activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
            }

        //2. 다른 사람의 유저페이지를 보는 경우
        }else{
            //팔로우 버튼 활성화
            binding.accountBtnFollowSignout?.text = getString(R.string.follow)

            //팔로우 버튼 클릭
            binding.accountBtnFollowSignout.setOnClickListener {
                Log.d("checkLog","Follow Button Clicked")
                requestFollow()
            }

            //UserFragment 에서 MainActivity 의 XML 요소를 잡기 위해 변수 초기화
            val mainActivity = (activity as MainActivity)
            //인스타 타이틀 숨김, 백버튼&유저이름 보이기
            mainActivity.binding.toolbarTitleImage?.visibility = View.GONE
            mainActivity.binding.toolbarUsername?.visibility = View.VISIBLE
            mainActivity.binding.toolbarBtnBack?.visibility = View.VISIBLE
            mainActivity.binding.toolbarUsername?.text = arguments?.getString("userId")

            //백버튼 클릭 -> DetailFragment 로 이동
            mainActivity.binding.toolbarBtnBack?.setOnClickListener {
                mainActivity.binding.bottomNavigation.selectedItemId = R.id.action_home
            }
        }
        //[END uid 에 따른 UserFragment 세팅(if-else)]

        binding.accountRecyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview?.layoutManager = GridLayoutManager(activity, 3)

        return binding.root
    }
//[END onCreateView]

//[START onViewCreated : 프로필 이미지 세팅, 팔로우/팔로잉 카운팅]
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProfileImage(view)
        getFollowerAndFollowing(view)
    }
//[END onViewCreated : 프로필 이미지 세팅, 팔로우/팔로잉 카운팅]

//[START onDestroyView]
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
//[END onDestroyView]

//[START 라사이클러뷰 어댑터/홀더]
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        private var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            //DB 에서 내가 올린 이미지 데이터만 읽어 옴
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }

                binding.accountTvPostCount?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }
        //[1. START 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }
        //[1. END 바인딩뷰홀더 : 아이템뷰에 들어갈 정보 배치]

        //[2. START getItemCount]
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        //[2. END getItemCount]

        //[3. START inner class CustomViewHolder,  override fun onCreateViewHolder 를 만든 것은 메모리를 적게 사용하기 위한 약속]
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //화면 넓이의 1/3 값을 가져와 이미지를 넣어줄 준비
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }
        inner class CustomViewHolder(var imageView: ImageView): RecyclerView.ViewHolder(imageView)
        //[3. END inner class CustomViewHolder,  override fun onCreateViewHolder]
    }
//[END 라사이클러뷰 어댑터/홀더]

//[START 사용 함수]
    //[1. START 팔로워 알람]
    private fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        val message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "test favoriteAlarm", message)
    }
    //[1. END 팔로워 알람]

    //[2. START 파이어베이스 DB 에서 프로필 이미지 불러옴]
    private fun getProfileImage(view: View){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener

            if(documentSnapshot.data != null){
                val url = documentSnapshot?.data!!["image"]
                Glide.with(requireContext()).load(url).apply(RequestOptions().circleCrop()).into(binding.accountIvProfile)
            }
        }
    }
    //[2. END 파이어베이스 DB 에서 프로필 이미지 불러옴]

    //[3. START 팔로우/팔로잉 카운팅 & 팔로우/팔로우 취소 전환]
    private fun getFollowerAndFollowing(view: View) {
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            //'팔로잉' 이 있다면 카운팅을 화면에 반영
            if (followDTO?.followingCount != null) {
                binding.accountTvFollowingCount?.text = followDTO?.followingCount?.toString()
            }

            //'팔로우' 가 있다면
            if (followDTO?.followerCount != null) {
                //카운팅을 화면에 반영
                binding.accountTvFollowerCount?.text = followDTO?.followerCount?.toString()

                //팔로우 취소 활성화
                if (followDTO?.followers?.containsKey(currentUserUid!!)) {
                    binding.accountBtnFollowSignout?.text = getString(R.string.follow_cancel)
                    binding.accountBtnFollowSignout?.background?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid) {
                        binding.accountBtnFollowSignout?.text = getString(R.string.follow)
                        binding.accountBtnFollowSignout?.background?.colorFilter = null
                    }
                }
            }
        }
    }
    //[3. END 팔로우/팔로잉 카운팅 & 팔로우/팔로우 취소 전환]

    //[4. START 팔로우 버튼을 눌렀을 때]
    private fun requestFollow(){
        val tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)

        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true
                transaction.set(tsDocFollowing, followDTO!!)
                return@runTransaction
            }

            if(followDTO.followings.containsKey(uid)){
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            }else{
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }

            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        val tsDocFollower = firestore?.collection("users")?.document(uid!!)

        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower,followDTO!!)
                return@runTransaction
            }
            if(followDTO!!.followers.containsKey(currentUserUid!!)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
    //[4. END 팔로우 버튼을 눌렀을 때]
//[END 사용 함수]
}