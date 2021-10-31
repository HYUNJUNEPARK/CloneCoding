# Instagram2

'인스타그램 클론코딩' 의 버그 수정과 일부 기능을 추가한 안드로이드 앱
google-services.json를 삭제 했기 때문에 정상 작동 안될 수 있음

** 개선 및 수정 사항 **<br/>
1 UI 구성 중 '이메일 로그인' 과 '이메일 회원가입' 을 분할해 재구성(activity_login.xml)<br/>
2 EditText 의 text 값에 아무것도 입력하지 않고 '이메일 로그인' 이나 '이메일 회원가입' 을 눌렀을 때 앱이 종료되던 문제를 해결(LoginActivity.kt)<br/>
3 EditText 에 이메일 형식이 아닌 ID 를 넣으면 앱이 종료되던 문제를 해결(LoginActivity.kt)<br/>
4 변수와 함수에 private 를 선언해 보안성을 높힘(LoginActivity.kt)<br/>
6 'kotlin-android-extensions' 뷰바인딩으로 교체<br/>
8 앱을 실행하면 바로 외부 저장소 권한을 묻는 기능 -> 사진 업로드 할 때 묻도록 수정(MainActivity.kt)<br/>
9 앱 아이콘 등록<br/>
10 게시글 작성날짜 기준으로 오름차순 정렬 -> 내림차순 정렬 되도록 수정. 최신 게시글을 제일 먼저 볼 수 있음(DetailViewFragment.kt)<br/>
11 UserFragment 에서 타인이 내 프로필을 클릭했을 때 수정가능했던 오류를 수정(UserFragment.kt)<br/>
12 게시글에 달린 댓글 볼 때 댓글을 단 날짜 표기<br/>
13 알람에 알람이 도착한 날짜 표기<br/>


** 참고 링크  **<br/>
파이어베이스 공식 문서 : https://firebase.google.com/docs/auth/android/google-signin?hl=ko<br/>
뷰바인딩 참고 페이지(Activity/Fragment) : https://duckssi.tistory.com/42<br/>
리사이클러뷰 바인딩 참고 페이지 : https://ichi.pro/ko/recyclerview-eodaebteoeseo-byu-bainding-eul-sayonghaneun-bangbeob-272785073792906<br/>
