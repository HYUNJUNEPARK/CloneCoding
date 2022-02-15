package com.june.insta.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.june.insta.databinding.FragmentGridBinding
import com.june.insta.navigation.model.ContentDTO

class GridFragment : Fragment() {
    //[Variation for Fragment ViewBinding]
    private var _binding : FragmentGridBinding? = null
    private val binding get() = _binding!!

    var firestore : FirebaseFirestore? = null
    private var fragmentView : View? = null

//[START onCreateViw]
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        firestore = FirebaseFirestore.getInstance()

        _binding = FragmentGridBinding.inflate(inflater, container, false)
        binding.gridfragmentRecyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        binding.gridfragmentRecyclerview?.layoutManager = GridLayoutManager(activity, 3)

        return binding.root
    }
//[END onCreateViw]

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
            firestore?.collection("images")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        //[START onBindViewHolder]
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(
                RequestOptions().centerCrop()).into(imageView)
        }
        //[END onBindViewHolder]

        //[START getItemCount]
        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        //[END getItemCount]

        //[START START inner class CustomViewHolder,  override fun onCreateViewHolder]
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            //화면 넓이의 1/3 값을 가져와 이미지를 넣어줄 준비
            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView): RecyclerView.ViewHolder(imageView)
        //[END inner class CustomViewHolder,  override fun onCreateViewHolder]
    }
//[END 라사이클러뷰 어댑터/홀더]
}