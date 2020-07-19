package com.example.howlstagram_f20.navigation

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.howlstagram_f20.R
import com.example.howlstagram_f20.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail_view.view.*
import kotlinx.android.synthetic.main.item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var uid: String? = null

    private fun init() {
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail_view, container, false)

        init()

        view.detail_view_fragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detail_view_fragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timeStamp")
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    contentDTOs.clear()
                    contentUidList.clear()
                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    notifyDataSetChanged()  //값이 새로고침 되도록 하는 메소드
                }
        }

        // 리싸이클러 뷰가 생성되었을 때, item_detail 레이아웃에 있는 내용들을 view 라는 변수에 담아 넘겨준다.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        // 메모리를 적게 차지하기 위해서 만든 약속. 문법과 상관 없이 필요한 부분
        // RecyclerView.ViewHolder 에 위에서 만든 view 가 들어간 걸 볼 수 있다.
        inner class CustomViewHolder(view: View) :
            RecyclerView.ViewHolder(view)

        // 리싸이클러 뷰의 개수를 넘겨주는 메소드
        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        // 데이터를 하나하나 매핑시켜주는 메소드
        // 데이터들을 순서대로 캐스팅 해준다 (길거리 캐스팅)
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            // item_detail 에서 View 에 대한 정보를 넣을 예정, 우선 호출한다.
            var viewholder = (holder as CustomViewHolder).itemView
            // Profile Image
//            Glide
//                .with(holder.itemView.context)
//                .load(contentDTOs!![position].imageUrl)
//                .into(viewholder.detail_view_item_profile_image)

            // UserID
            viewholder.detail_view_item_profile_name.text = contentDTOs!![position].userId

            // Image , Glide 는 이미지를 쉽게 가져오도록 도와주는 아주 유용한 라이브러리라고 한다
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(viewholder.detail_view_item_image_view_content)

            // 좋아요
            viewholder.detail_view_item_favorite_number.text = "좋아요 " + contentDTOs!![position].favoriteCount.toString() + "개"

            // Explain of content
            viewholder.detail_view_item_explain_text_view.text = contentDTOs!![position].explain
            
            // 좋아요 버튼을 눌렀을 때
            viewholder.detail_view_item_favorite_image_view.setOnClickListener{ favoriteEvent(position)}
            
            // 페이지가 로드 됐을 때
            if (contentDTOs!![position].favorites.containsKey(uid)) {
                // 좋아요 누른 상태
                viewholder.detail_view_item_favorite_image_view.setImageResource(R.drawable.ic_favorite)
                
            } else {
                // 좋아요 아닌 상태
                viewholder.detail_view_item_favorite_image_view.setImageResource(R.drawable.ic_favorite_border)
            }
            // 상대방 프로필 이미지를 클릭했을 때
            viewholder.detail_view_item_profile_image.setOnClickListener{
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid) // 내 uid
                bundle.putString("userUid", contentDTOs[position].userId) // 식별자 : 아이디
                fragment.arguments = bundle //UserFragment에서 uid랑 userID 부분만 바꿨구나
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

        }

        fun favoriteEvent(position: Int) {
            // 좋아요가 눌러져 있는 상태 = uid 값이 있는 상태.
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)) { // 이미 uid 값을 가지고 있다 == 좋아요가 이미 눌러져 있다.
                    // 좋아요 눌러진 상태에서 또 눌렀으니 좋아요 취소,
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! - 1
                    contentDTO?.favorites.remove(uid)
                } else {
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount!! + 1
                    contentDTO?.favorites[uid!!] = true
                }
                transaction.set(tsDoc, contentDTO)
            }

        }
    }
}