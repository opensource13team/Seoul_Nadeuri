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
            // 장소 이름과 혼잡도 텍스트를 넘겨줌 (예: "뚝섬 한강공원", "여유")
            resultList.add(new HotPlace(originData.placeName, originData.congestion));
        }

        return resultList;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}