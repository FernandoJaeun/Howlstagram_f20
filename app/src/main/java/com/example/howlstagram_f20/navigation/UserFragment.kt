package com.example.howlstagram_f20.navigation

import android.app.Activity
import android.app.DownloadManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.howlstagram_f20.LoginActivity
import com.example.howlstagram_f20.MainActivity
import com.example.howlstagram_f20.R
import com.example.howlstagram_f20.navigation.model.ContentDTO
import com.example.howlstagram_f20.navigation.model.FollowDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user_view.view.*

class UserFragment : Fragment() {

    var fragmentView: View? = null
    var uid: String? = null // 나 (로그인 한 그 아이디)
    var currentUserUid: String? = null //

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    companion object { // static으로 선언
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user_view, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        // 유저 프레그먼트로 들어왔을 때 나인지 상대방인지 판단하여 화면을 구성하겠다.
        if (currentUserUid == uid) {
            //내 페이지일 때
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.sign_out) // 나의 정보 보이기
            fragmentView?.account_btn_follow_signout?.setOnClickListener { // 로그아웃 띄우기
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java)) // 로그아웃하고 로그인페이지로 이동
            }
        } else {
            // 다른 사람 페이지일 때
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow) // 팔로우 버튼 활성화
            var mainActivity = (activity as MainActivity)
            mainActivity?.toolbar_userID?.text = arguments?.getString("userId")
            mainActivity?.toolbar_btn_back?.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            // 뒤로가기 버튼 보이기
            mainActivity?.toolbar_userID?.visibility = View.VISIBLE
            mainActivity?.toolbar_btn_back?.visibility = View.VISIBLE
            mainActivity?.toolbar_title_image?.visibility = View.VISIBLE

            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }

        }
        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(activity!!, 3) // 가로에 사진을 3개씩 배치

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }
        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    // 팔로워와 팔로잉 숫자 가져오기
    private fun getFollowerAndFollowing() {
        //uid. 나를 클릭하면 내 uid가 뜨고 상대방을 클릭하면 상대방 uid가 뜬다.
        //snapshot은 실시간으로 값을 날린다
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { value, error ->
            if (value == null) return@addSnapshotListener
            var followDTO = value.toObject((FollowDTO::class.java))
            if (followDTO?.followingCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()
            }
            if (followDTO?.followerCount != null) {
                fragmentView?.account_tv_follower_count?.text = followDTO?.followerCount?.toString()
                if (followDTO?.followers?.containsKey(currentUserUid!!)) {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(
                        ContextCompat.getColor(activity!!, R.color.colorLightGray),
                        PorterDuff.Mode.MULTIPLY
                    )
                } else {
                    fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                    if (uid != currentUserUid) {
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    // 팔로우 신청하기
    private fun requestFollow() {
        // 내 계정에 팔로잉 중인 상대방 표시
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            var alreadyFollowed = followDTO!!.followers.containsKey(uid)

            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            if (alreadyFollowed) { // 이미 팔로우 한 상태, 팔로우 취소하기
                followDTO?.followingCount = followDTO!!.followingCount - 1
                followDTO?.followers?.remove(uid)
            } else { // 팔로우 요청하기
                followDTO?.followerCount = followDTO!!.followingCount + 1
                followDTO?.followers[uid!!] = true // 상대방 uid 추가
            }
            transaction.set(tsDocFollowing, followDTO) // DB에 저장
            return@runTransaction
        }
        // 상대방이 팔로잉 중인 사람 수
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)// 상대방 Uid
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            var alreadyFollowed = followDTO!!.followers.containsKey(currentUserUid)

            if (followDTO == null) { // 팔로우에 대한 내역이 없을 때
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1 // 내 팔로우가 최초값이기 때문에
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }
            if (alreadyFollowed) {
                followDTO!!.followerCount = followDTO!!.followingCount - 1
                followDTO!!.followers.remove((currentUserUid!!))
            } else {
                followDTO!!.followerCount = followDTO!!.followingCount + 1
                followDTO!!.followers[currentUserUid!!] = true
            }
            transaction.set(tsDocFollower, followDTO!!) // DB에 값 저장
            return@runTransaction
        }
    }

    // 서버에 올린 이미지 실시간으로 가져오기
    private fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { value, error ->
            if (value == null) return@addSnapshotListener
            if (value?.data != null) {
                var url = value?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
                Toast.makeText(context, "프로필사진 변경", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { value, error ->
                if (value == null) return@addSnapshotListener

                for (snapshot in value.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)


        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView

            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)

        }
    }
}