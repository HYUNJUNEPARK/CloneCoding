package com.june.insta.navigation


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.june.insta.R
import com.june.insta.navigation.model.AlarmDTO
import com.june.insta.navigation.model.ContentDTO
import com.june.insta.navigation.util.FcmPush
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }//onCreateView

    //어댑터(Adapter) : 사용자 데이터 리스트로부터 아이템 뷰를 만들어 리사이클러뷰에 표시
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()
        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    if (querySnapshot == null) return@addSnapshotListener

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    //리사이클러뷰 새로고침
                    notifyDataSetChanged()
                }
        }//init

        //뷰홀더(ViewHolder) : 어댑터를 통해 만들어진 아이템 뷰를 저장. 어댑터에서 생성, 관리되고 재활용되어 자원을 효율적으로 사용함
        //inner class CustomViewHolder,  override fun onCreateViewHolder 를 만든 것은 메모리를 적게 사용하기 위한 약속
        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }//onCreateViewHolder

        //아이템 뷰에 들어갈 정보 배치
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain
            viewholder.detailviewitem_favoritecounter_textview.text = "Likes " + contentDTOs!![position].favoriteCount
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }
            //좋아요가 눌렸을 때 하트색을 바꿔줌
            if(contentDTOs!![position].favorites.containsKey(uid)){
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else{
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //프로필 이미지가 클릭됐을 때
            viewholder.detailviewitem_profile_image.setOnClickListener {
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

            //코멘트 이미지가 클릭됐을 때
            viewholder.detailviewitem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }//onBindViewHolder

        override fun getItemCount(): Int {
            return contentDTOs.size
        }//getItemCount

        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                //containkey 가 되어있다는 것은 좋아요 버튼이 눌려있다는 것으로 버튼을 한번 더 누르면 좋아요가 취소됨
                if (contentDTO!!.favorites.containsKey(uid)){
                    contentDTO.favoriteCount = contentDTO.favoriteCount -1
                    contentDTO.favorites.remove(uid)
                }else{
                    contentDTO.favoriteCount = contentDTO.favoriteCount +1
                    contentDTO.favorites[uid!!] = true

                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                //트랜잭션을 다시 서버로 돌려줌
                transaction.set(tsDoc, contentDTO)
            }
        }//favoriteEvent

        fun favoriteAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "test favoriteAlarm", message)
        }//favoriteAlarm
    }//inner class
}//class