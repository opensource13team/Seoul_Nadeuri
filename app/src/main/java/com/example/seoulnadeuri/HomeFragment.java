package com.example.seoulnadeuri;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.seoulnadeuri.databinding.FragmentHomeBinding;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    // 오토 롤링 배너 타이머 장치
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;

    // AI 추천 엔진
    private RecommendationEngine aiEngine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // AI 엔진 초기화
        aiEngine = new RecommendationEngine(getContext());

        // 1. 헤더: 돋보기 버튼 누르면 '검색' 탭으로 이동 (XML에서 btn_top_search로 수정했으므로 정상 작동!)
        binding.btnTopSearch.setOnClickListener(v -> {
            // 하단 탭을 조작하는 대신, 프래그먼트를 직접 검색 화면(RecordFragment)으로 교체
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RecordFragment()) // MainActivity의 프레임레이아웃 ID
                    .addToBackStack(null) // 휴대폰 뒤로가기 버튼 누르면 다시 홈으로 돌아오게 설정
                    .commit();
        });

        // 2. 오토 롤링 배너 세팅
        setupHeroCarousel();

        // 3. 퀵 필터 버튼 이벤트 세팅
        setupQuickFilters();

        // 4. 가로 스크롤 AI 맞춤 추천 리스트 세팅
        setupAiRecommend();

        return binding.getRoot();
    }

    private void setupHeroCarousel() {
        List<BannerAdapter.BannerItem> bannerItems = new ArrayList<>();

        // 👇 인터넷 주소 대신 유저님이 만드신 assets 폴더의 로컬 경로를 사용합니다!
        bannerItems.add(new BannerAdapter.BannerItem("이번 주말, 한강 피크닉 어때요?", "file:///android_asset/place_images/반포한강공원.jpg"));
        bannerItems.add(new BannerAdapter.BannerItem("도심 속 야경 명소 Top 5", "file:///android_asset/place_images/잠실롯데타워·석촌호수.jpg"));
        bannerItems.add(new BannerAdapter.BannerItem("비 오는 날엔 실내 데이트 ☔", "file:///android_asset/place_images/DDP(동대문디자인플라자).jpg"));

        BannerAdapter adapter = new BannerAdapter(bannerItems);
        binding.vpHeroBanner.setAdapter(adapter);

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                int nextItem = binding.vpHeroBanner.getCurrentItem() + 1;
                if (nextItem >= adapter.getItemCount()) nextItem = 0;
                binding.vpHeroBanner.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 3000);
            }
        };
    }

    private void setupQuickFilters() {
        binding.btnFilterIndoor.setOnClickListener(v -> openFilteredSearch(RecordFragment.FILTER_INDOOR));
        binding.btnFilterOutdoor.setOnClickListener(v -> openFilteredSearch(RecordFragment.FILTER_OUTDOOR));
        binding.btnFilterRelaxed.setOnClickListener(v -> openFilteredSearch(RecordFragment.FILTER_LOW_CONGESTION));
        binding.btnFilterFestival.setOnClickListener(v -> openFilteredSearch(RecordFragment.FILTER_FESTIVAL));

        // 👇 찜한 장소 버튼 누르면 찜 목록 화면으로 이동!
        binding.btnFilterWishlist.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new WishlistFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void openFilteredSearch(String filter) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, RecordFragment.newInstance(filter))
                .addToBackStack(null)
                .commit();
    }

    private void setupAiRecommend() {
        binding.rvAiRecommend.setLayoutManager(new LinearLayoutManager(getContext()));

        // 서버에서 실시간 데이터 가져오기
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://seoul-outing-proxy.comfy202.workers.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SeoulApiService apiService = retrofit.create(SeoulApiService.class);
        apiService.getAllPlaces().enqueue(new Callback<List<SeoulPlaceData>>() {
            @Override
            public void onResponse(Call<List<SeoulPlaceData>> call, Response<List<SeoulPlaceData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SeoulPlaceData> apiDataList = response.body();

                    // 👇 [알고리즘 연동] 스마트폰에 몰래 저장된 내 취향 점수 불러오기!
                    android.content.SharedPreferences prefs = getContext().getSharedPreferences("SeoulUserPref", android.content.Context.MODE_PRIVATE);
                    float mySecretPref = prefs.getFloat("USER_PREF", 0.5f); // 기본값은 중간인 0.5

                    // 불러온 진짜 취향 점수를 AI 엔진에 넣고 리스트 뽑기
                    List<HotPlace> resultList = runInference(apiDataList, mySecretPref);

                    // 완성된 리스트를 어댑터에 꽂아서 화면에 띄우기
                    binding.rvAiRecommend.setAdapter(new HotPlaceAdapter(resultList));
                }
            }

            @Override
            public void onFailure(Call<List<SeoulPlaceData>> call, Throwable t) {
                Log.e("API_ERROR", "데이터 로드 실패: " + t.getMessage());
                Toast.makeText(getContext(), "실시간 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // AI 엔진 점수대로 10개를 뽑고, UI에 띄울 텍스트로 가공하는 함수
    private List<HotPlace> runInference(List<SeoulPlaceData> apiDataList, float userPref) {
        List<HotPlace> resultList = new ArrayList<>();
        List<PlaceScore> top10Scores = aiEngine.getTop10Places(apiDataList, userPref);

        for (PlaceScore scoreData : top10Scores) {
            SeoulPlaceData originData = scoreData.getOriginData();

            String pmText = (originData.pmIndex > 150) ? "매우 나쁨" : (originData.pmIndex > 80) ? "나쁨" : (originData.pmIndex > 30) ? "보통" : "좋음";
            String weatherStr = String.format("🌡 %.1f℃ | 😷 미세먼지: %s | ☔ 강수량: %.1fmm", originData.temp, pmText, originData.rain);
            String indoorText = (originData.indoorTag >= 1.0f) ? "실내" : (originData.indoorTag == 0.5f) ? "실내외 복합" : "야외";

            String eventText = (originData.localEvent >= 1.0f) ? "🎪 축제: 개최중" : "🎪 축제: 없음";
            String realEventName = (originData.localEvent >= 1.0f && originData.eventName != null && !originData.eventName.isEmpty())
                    ? "🎪 " + originData.eventName
                    : "현재 진행중인 축제/행사가 없습니다.";

            String placeInfoStr = eventText + " | " + indoorText;
            String localImageUrl = "file:///android_asset/place_images/" + originData.placeName + ".jpg";

            // 1. 객체 생성
            HotPlace hotPlace = new HotPlace(
                    originData.placeName,
                    originData.congestion,
                    weatherStr,
                    placeInfoStr,
                    realEventName,
                    localImageUrl
            );

            // 2. AI 예측 점수 세팅
            int score100 = (int) (scoreData.getScore() * 100);
            hotPlace.setAiScore("✨ 추천 " + score100 + "점");

            // 3. 보따리 챗봇의 한 줄 추천 이유
            String reason = "💬 봇따리: ";
            String eventName = "특별한 축제";
            if (originData.eventName != null && !originData.eventName.isEmpty()) {
                String[] eventArray = originData.eventName.split(",");
                int randomIdx = (int) (Math.random() * eventArray.length);
                eventName = "[" + eventArray[randomIdx].trim() + "]";
            }

            if (originData.localEvent >= 1.0f && originData.rain > 0 && originData.indoorTag >= 1.0f) {
                reason += "비가 오지만 실내라서 쾌적해요! 게다가 " + eventName + "도 열리고 있어서 꿀잼 보장!";
            } else if (originData.localEvent >= 1.0f && originData.rain == 0 && originData.pmIndex <= 30 && originData.indoorTag == 0.0f) {
                reason += "날씨도 완벽한데 " + eventName + "까지 열리고 있어요! 당장 야외로 뛰어나가요!";
            } else if (originData.localEvent >= 1.0f) {
                reason += "지금 " + eventName + " 진행 중이에요! 핫플 분위기 제대로 느껴보세요!";
            } else if (originData.rain > 0 && originData.indoorTag >= 1.0f) {
                reason += "비 오는 날씨를 피해 쾌적하게 놀기 좋은 실내 핫플이에요!";
            } else if (originData.rain == 0 && originData.pmIndex <= 30 && originData.indoorTag == 0.0f) {
                reason += "미세먼지 없이 맑은 날씨! 탁 트인 야외에서 힐링하기 최고예요!";
            } else if ("여유".equals(originData.congestion)) {
                reason += "지금 사람이 적어서 복잡하지 않고 여유롭게 둘러볼 수 있어요.";
            } else {
                reason += "실시간 데이터를 종합해 본 결과, 지금 당장 떠나기 가장 좋은 곳이에요!";
            }

            hotPlace.setRecommendReason(reason);

            // 4. 리스트에 최종 담기
            resultList.add(hotPlace);
        }

        return resultList;
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
