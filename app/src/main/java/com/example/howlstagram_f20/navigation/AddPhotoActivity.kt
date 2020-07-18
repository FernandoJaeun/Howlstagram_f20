package com.example.howlstagram_f20.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.howlstagram_f20.MainActivity
import com.example.howlstagram_f20.R
import com.example.howlstagram_f20.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class AddPhotoActivity : AppCompatActivity() {

    private var PICK_IMAGE_FROM_ALBUE = 0
    private lateinit var storage: FirebaseStorage
    private lateinit var photoUri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private fun init() {
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        //  이미지 스토리지 시작
        init()

        //  앨범 열기 & 사진 선택
        var photoPickerIntent = Intent(Intent.ACTION_PICK) // 선택기능을 가진 인텐트
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUE)
        //  사진 업로드하기
        add_photo_btn_upload.setOnClickListener { contentUpload() }
    }

    //startActivityForResult 가 작동하면 실행되는 안드로이드 기본 메소드
    //사진이 선택되면 사진의 정보를 변수에 담고 화면에 보여주는 동작을 만듦
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { // 응답코드, 결과코드, 선택된 사진의 경로 데이터를 반환)
        super.onActivityResult(requestCode, resultCode, data)
        // 사진이 선택되었을 때,
        if (requestCode == PICK_IMAGE_FROM_ALBUE) {
            if (resultCode == Activity.RESULT_OK) {
                photoUri = data?.data!! // 변수에 경로를 담고
                add_photo_image.setImageURI(photoUri) // 화면에 뿌려준다.
            } else { // 사진 선택을 취소했을 때,
                finish()
            }
        }
    }

    private fun contentUpload() {
        // 업로드 할 사진 파일의 이름을 만든다. !! 중복 주의 !!
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png" // 사진이름과 시간이 섞이니 절대 중복되지 않는다 ^^

        // 사진 저장 경로 지정
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        // Promise Method , 사진 업로드
        storageRef?.putFile(photoUri!!)?.continueWith { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWith storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            // 이미지 경로 Uniform Resource Locator, (URI = Uniform Resource Identifier)
            contentDTO.imageUrl = uri.toString()

            // 이미지에 대한 설명을 저장
            contentDTO.explain = add_photo_edit_explain.text.toString()

            // 유저가 로그인할 때 사용한 그 아이디! 도메인네임이라고 보면 된다.
            contentDTO.userId = auth?.currentUser?.email

            // 유저 식별자! IP 주소라고 보면 된다. UserID나 uid 둘 다 고유한 ID 이지만, 사람이 보기 편한것과 시스템에서 구분하기 편한 것의 차이가 있는 것 같다
            contentDTO.uid = auth?.currentUser?.uid

            // 업로드 시간을 알기 위한 것
            contentDTO.timeStamp = System.currentTimeMillis()

            // 위의 모든 것을 images 폴더에 사진과 함께 저장!
            firestore?.collection("images").document()?.set(contentDTO)
            setResult(Activity.RESULT_OK)
            Toast.makeText(this, R.string.upload_success, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

//        // 사진 업로드! 업로드 완료 토스트 메시지 알림
//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener { task ->
//            if (task.task.isSuccessful) {
//                Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
//                startActivity(Intent(this, MainActivity::class.java))
//            } else {
//                Toast.makeText(this, R.string.upload_fail, Toast.LENGTH_SHORT).show()
//            }
//        }
}


