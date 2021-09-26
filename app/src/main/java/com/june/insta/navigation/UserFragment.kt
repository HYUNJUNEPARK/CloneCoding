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
import com.june.insta.databinding.FragmentDetailBinding
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

    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null

    //static 변수라서 MainActivity 에서도 접근 가능
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }

//[START onCreateView]
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentUserBinding.inflate(inflater, container, false)

        //이전 화면에서 넘어온 uid 을 받아옴
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

                var photoPickerIntent = Intent(Intent.ACTION_PICK)
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
            var mainActivity = (activity as MainActivity)
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
//[START onCreateView]

//[START onViewCreated]
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProfileImage(view)
        getFollowerAndFollowing(view)
    }
//[END onViewCreated]

//[START onDestroyView]
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
//[END onDestroyView]

//[START 사용 함수]
    fun followerAlarm(destinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        var message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "test favoriteAlarm", message)
    }//followerAlarm

    fun getProfileImage(view: View){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var url = documentSnapshot?.data!!["image"]
                Glide.with(requireContext()).load(url).apply(RequestOptions().circleCrop()).into(view?.account_iv_profile!!)
            }
        }
    }//getProfileImage

    //화면에 Follow/Following 카운팅 & Follow/Follow Cancel 전환
    fun getFollowerAndFollowing(view: View) {
            firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount != null) {
                view?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if (followDTO?.followerCount != null) {
                view?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                //팔로우를 하고 있으면 버튼이 바뀌는 코드
                if (followDTO?.followers?.containsKey(currentUserUid!!)) {
                    view?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    view?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                } else {
                    if (uid != currentUserUid) {
                        view?.account_btn_follow_signout?.text = getString(R.string.follow)
                        view?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }//getFollowerAndFollowing

    //Follow 버튼 눌렀을 때
    fun requestFollow(){
        //Save data to my account
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if(followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing,followDTO!!)
                return@runTransaction
            }
            if(followDTO.followings.containsKey(uid)){
                //It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings.remove(uid)
            }else{
                //It add following third person when a third person do not follow me
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        //Save data to third account
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
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
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else{
                //It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower,followDTO!!)
            return@runTransaction
        }
    }
//[END 사용 함수]

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()

        init {
            //DB 에서 내가 올린 이미지 데이터만 읽어 옴
            firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                //사용자가 올린 전체 POST 수
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                //리사이클러뷰 새로고침
                notifyDataSetChanged()
            }
        }//init

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //화면 넓이의 1/3 값을 가져와 이미지를 넣어줄 준비
            var width = resources.displayMetrics.widthPixels / 3
            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageview)
        }//onCreateViewHolder

        inner class CustomViewHolder(var imageview: ImageView): RecyclerView.ViewHolder(imageview)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }

        //
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        //
    }
}