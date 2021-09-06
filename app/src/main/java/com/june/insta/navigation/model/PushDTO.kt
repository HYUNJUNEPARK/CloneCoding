package com.june.insta.navigation.model

//이 양식으로 보내줘야 구글 서버가 push를 이해할 수 있음
data class PushDTO(
    //토큰을 받는 사람의 아이디
    var to : String? = null,
    var notification : Notification = Notification()
){
    data class Notification(
        var body : String? = null,
        var title : String? = null
    )
}
