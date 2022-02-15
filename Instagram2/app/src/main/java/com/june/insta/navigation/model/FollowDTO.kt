package com.june.insta.navigation.model

data class FollowDTO (
    //followers, followings 는 중복 사용자를 막기 위한 변수
    var followerCount : Int = 0,
    var followers : MutableMap<String, Boolean> = HashMap(),

    var followingCount : Int = 0,
    var followings : MutableMap<String, Boolean> = HashMap()
)