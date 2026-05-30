package com.example.seoulnadeuri;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.seoulnadeuri.databinding.ActivityDetailBinding;
import com.google.gson.Gson;

public class DetailActivity extends AppCompatActivity {

    private ActivityDetailBinding binding;
    private SharedPreferences prefs;
    private boolean isWished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 뷰 바인딩 켜기
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. 앞 화면(어댑터)에서 보낸 택배 상자 까보기
        Intent intent = getIntent();
        String placeName = intent.getStringExtra("PLACE_NAME");
        String congestion = intent.getStringExtra("CONGESTION");
        String weatherInfo = intent.getStringExtra("WEATHER_INFO");
        String placeInfo = intent.getStringExtra("PLACE_INFO");
        String eventDetail = intent.getStringExtra("EVENT_DETAIL"); // 진짜 축제 이름

        // 2. 화면에 글자 박아넣기
        binding.tvDetailTitle.setText(placeName);
        binding.tvDetailCongestion.setText(congestion);
        binding.tvDetailWeather.setText(weatherInfo);

        // 👇 쉼표+띄어쓰기(", ") 기준으로만 깔끔하게 줄바꿈 처리!
        String formattedEvent = eventDetail.replace(", ", "\n");

        // 노란 박스에 띄우기
        binding.tvDetailEventList.setText(formattedEvent.trim());

        // 3. 찜(북마크) 기능 세팅
        prefs = getSharedPreferences("SeoulWishlist", MODE_PRIVATE);

        // 이미 찜해둔 장소인지 확인하고 별 아이콘 상태 맞추기
        isWished = prefs.contains(placeName);
        updateWishlistIcon();

        // 별(찜) 버튼을 눌렀을 때의 동작
        binding.btnWishlist.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            if (isWished) {
                // 찜 취소: 스마트폰 메모장에서 삭제
                editor.remove(placeName);
                isWished = false;
                Toast.makeText(this, "찜 목록에서 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 찜 추가: 5개의 데이터를 모두 담아서 JSON으로 저장
                HotPlace savedItem = new HotPlace(placeName, congestion, weatherInfo, placeInfo, eventDetail);
                String json = new Gson().toJson(savedItem);
                editor.putString(placeName, json);
                isWished = true;
                Toast.makeText(this, "찜 목록에 추가되었습니다!", Toast.LENGTH_SHORT).show();
            }
            editor.apply(); // 저장 완료
            updateWishlistIcon(); // 별 아이콘 색깔 바꾸기
        });

        // 4. 네이버 지도 연동 버튼 클릭 이벤트
        binding.btnNaverMap.setOnClickListener(v -> {
            // URL 스킴을 이용해 네이버 지도 앱으로 바로 목적지 쏘기
            String url = "nmap://search?query=" + placeName + "&appname=com.example.seoulnadeuri";
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mapIntent.addCategory(Intent.CATEGORY_BROWSABLE);

            try {
                // 휴대폰에 네이버 지도 앱이 깔려있을 때
                startActivity(mapIntent);
            } catch (Exception e) {
                // 휴대폰에 네이버 지도 앱이 없을 때 -> 인터넷 브라우저로 띄워줌
                Toast.makeText(this, "네이버 지도 앱이 없어 웹으로 연결합니다.", Toast.LENGTH_SHORT).show();
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://map.naver.com/v5/search/" + placeName));
                startActivity(webIntent);
            }
        });
    }

    // 별 모양 아이콘 껐다 켰다 하는 전용 함수
    private void updateWishlistIcon() {
        if (isWished) {
            binding.btnWishlist.setImageResource(android.R.drawable.btn_star_big_on); // 노란색(켜짐) 별
        } else {
            binding.btnWishlist.setImageResource(android.R.drawable.btn_star_big_off); // 회색(꺼짐) 별
        }
    }
}