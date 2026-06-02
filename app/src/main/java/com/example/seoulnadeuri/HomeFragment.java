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
        binding.btnFilterIndoor.setOnClickListener(v -> Toast.makeText(getContext(), "실내 핫플 필터 준비중입니다!", Toast.LENGTH_SHORT).show());
        binding.btnFilterOutdoor.setOnClickListener(v -> Toast.makeText(getContext(), "야외 핫플 필터 준비중입니다!", Toast.LENGTH_SHORT).show());
        binding.btnFilterRelaxed.setOnClickListener(v -> Toast.makeText(getContext(), "쾌적한 장소 필터 준비중입니다!", Toast.LENGTH_SHORT).show());
        binding.btnFilterFestival.setOnClickListener(v -> Toast.makeText(getContext(), "축제 정보 필터 준비중입니다!", Toast.LENGTH_SHORT).show());

        // 👇 찜한 장소 버튼 누르면 찜 목록 화면으로 이동!
        binding.btnFilterWishlist.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new WishlistFragment())
                    .addToBackStack(null)
                    .commit();
        });
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

                    // 유저 취향(임시 0.8)을 넣고 AI 엔진 돌리기
                    List<HotPlace> resultList = runInference(apiDataList, 0.8f);

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

            // 👇 2. AI 예측 점수를 100점 만점으로 변환해서 꽂아주기!
            int score100 = (int) (scoreData.getScore() * 100);
            hotPlace.setAiScore("✨ 추천 " + score100 + "점");

            // 3. 리스트에 담기
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