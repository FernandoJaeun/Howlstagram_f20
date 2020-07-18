package com.example.howlstagram_f20.navigation.model

// 콘텐츠 관리용
data class ContentDTO(
    var explain: String? = null, // 이미지 설명을 관리
    var imageUrl: String? = null, // 이미지 경로를 관리
    var uid: String? = null, // 유저 구분용
    var userId: String? = null, // 유저의 이미지 관리
    var timeStamp: Long? = null, // 몇시 몇분의 컨텐츠를 올렸는지 관리
    var favoriteCount: Int? = 0,  // 좋아요 개수 관리
    var favorites: Map<String, Boolean> = HashMap() // 중복 좋아요 방지, 좋아요 눌른 유저 관리
)

// 댓글 관리용
data class Comment(
    var uid: String? = null,
    var userId: String? = null,
    var comment: String? = null, // 코멘트 관리
    var timeStamp: Long? = null
)

