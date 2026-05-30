package com.example.seoulnadeuri;

import android.os.Bundle;
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
    private RecommendationEngine aiEngine; // TFLite 모델 엔진

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // 리사이클러뷰 기본 세팅
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. AI 엔진 초기화 (seoul_outing.tflite 파일 로드)
        aiEngine = new RecommendationEngine(getContext());

        // 2. 클라우드플레어 워커 API 호출 시작
        fetchSeoulDataAndRunAI();

        return binding.getRoot();
    }

    private void fetchSeoulDataAndRunAI() {
        // Retrofit 통신 준비
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://seoul-outing-proxy.comfy202.workers.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SeoulApiService apiService = retrofit.create(SeoulApiService.class);

        // API 비동기 호출
        apiService.getAllPlaces().enqueue(new Callback<List<SeoulPlaceData>>() {
            @Override
            public void onResponse(Call<List<SeoulPlaceData>> call, Response<List<SeoulPlaceData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SeoulPlaceData> all121Places = response.body();

                    // TODO: 현재 유저의 취향 점수를 가져옵니다. (0.0 ~ 1.0)
                    // 일단 테스트용으로 0.8(상당히 핫플을 좋아함)로 고정
                    float userPref = 0.8f;

                    // 3. AI 모델에 121개 데이터를 넣고 Top 10 뽑아내기
                    List<HotPlace> top10List = runInference(all121Places, userPref);

                    // 4. 어댑터에 Top 10 데이터 전달 후 화면 새로고침
                    HotPlaceAdapter adapter = new HotPlaceAdapter(top10List);
                    binding.recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<SeoulPlaceData>> call, Throwable t) {
                Toast.makeText(getContext(), "데이터를 불러오지 못했습니다: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // AI 엔진을 돌려서 어댑터에 들어갈 형태로 변환하는 함수
    private List<HotPlace> runInference(List<SeoulPlaceData> apiDataList, float userPref) {
        List<HotPlace> resultList = new ArrayList<>();

        // 엔진에서 점수가 높은 순으로 10개를 받아옴
        List<PlaceScore> top10Scores = aiEngine.getTop10Places(apiDataList, userPref);

        // UI 띄우기용 객체(HotPlace)로 변환
        for (PlaceScore scoreData : top10Scores) {
            SeoulPlaceData originData = scoreData.getOriginData();

            // 1. 미세먼지(pmIndex) 수치를 한국 기준 글자로 변환
            String pmText = "좋음";
            if (originData.pmIndex > 150) pmText = "매우 나쁨";
            else if (originData.pmIndex > 80) pmText = "나쁨";
            else if (originData.pmIndex > 30) pmText = "보통";

            // 2. 날씨 정보 한 줄로 묶기
            String weatherStr = String.format("🌡 %.1f℃ | 😷 미세먼지: %s | ☔ 강수량: %.1fmm",
                    originData.temp, pmText, originData.rain);

            // 3. 실내외 태그를 글자로 변환 (JSON 메타데이터 기준)
            float indoorValue = originData.indoorTag;
            String indoorText = "야외";
            if (indoorValue >= 1.0f) indoorText = "실내";
            else if (indoorValue == 0.5f) indoorText = "실내외 복합";

            // 4. 축제 여부 확인
            String eventText = (originData.localEvent >= 1.0f)
                    ? "🎪 축제: 개최중"
                    : "🎪 축제: 없음";

            // 5. 장소 정보 한 줄로 묶기 (메인 화면용: 짧게)
            String placeInfoStr = eventText + " | " + indoorText;

            // 6. 진짜 축제 이름 뽑아두기 (상세 화면용: 길게)
            String realEventName = (originData.localEvent >= 1.0f && originData.eventName != null && !originData.eventName.isEmpty())
                    ? "🎪 " + originData.eventName
                    : "현재 진행중인 축제/행사가 없습니다.";

            // 최종 리스트에 담기 (realEventName 추가!)
            resultList.add(new HotPlace(
                    originData.placeName,
                    originData.congestion,
                    weatherStr,
                    placeInfoStr,
                    realEventName
            ));
        }

        return resultList;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}