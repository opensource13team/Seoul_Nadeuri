package com.example.seoulnadeuri;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.seoulnadeuri.databinding.FragmentRecordBinding;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RecordFragment extends Fragment {

    private FragmentRecordBinding binding;
    private List<HotPlace> fullPlaceList = new ArrayList<>(); // 전체 121개 원본 리스트

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 뷰 바인딩
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // 리사이클러뷰 세팅
        binding.rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1. 서버에서 데이터 가져오기 (AI 추천 없이 가나다순 등으로 띄우기 위함)
        fetchDataForSearch();

        // 2. 검색창에 글자를 칠 때마다 작동하는 마법의 이벤트 (TextWatcher)
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 글자가 입력되고 나면, 필터링 함수 실행!
                filterList(s.toString());
            }
        });

        return view;
    }

    private void fetchDataForSearch() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://seoul-outing-proxy.comfy202.workers.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SeoulApiService apiService = retrofit.create(SeoulApiService.class);
        apiService.getAllPlaces().enqueue(new Callback<List<SeoulPlaceData>>() {
            @Override
            public void onResponse(Call<List<SeoulPlaceData>> call, Response<List<SeoulPlaceData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SeoulPlaceData> apiData = response.body();
                    fullPlaceList.clear(); // 기존 데이터 초기화

                    // API 데이터를 UI용 객체(HotPlace)로 싹 변환해서 보관
                    for (SeoulPlaceData data : apiData) {
                        String pmText = (data.pmIndex > 150) ? "매우 나쁨" : (data.pmIndex > 80) ? "나쁨" : (data.pmIndex > 30) ? "보통" : "좋음";
                        String weatherStr = String.format("🌡 %.1f℃ | 😷 미세먼지: %s | ☔ 강수량: %.1fmm",
                                data.temp, pmText, data.rain);
                        String eventText = (data.localEvent >= 1.0f) ? "🎪 축제: 개최중" : "🎪 축제: 없음";
                        String realEventName = (data.localEvent >= 1.0f && data.eventName != null) ? "🎪 " + data.eventName : "현재 진행중인 축제/행사가 없습니다.";
                        String placeInfoStr = eventText + " | " + (data.indoorTag >= 1.0f ? "실내" : "야외");

                        fullPlaceList.add(new HotPlace(
                                data.placeName, data.congestion, weatherStr, placeInfoStr, realEventName
                        ));
                    }

                    // 처음 화면을 켰을 때는 필터링 없이 전체 리스트 띄우기
                    binding.rvSearch.setAdapter(new HotPlaceAdapter(fullPlaceList));
                }
            }

            @Override
            public void onFailure(Call<List<SeoulPlaceData>> call, Throwable t) {
                Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 입력받은 검색어(keyword)로 리스트를 걸러내는 함수
    private void filterList(String keyword) {
        List<HotPlace> filteredList = new ArrayList<>();

        for (HotPlace place : fullPlaceList) {
            // 장소 이름에 내가 친 글자(keyword)가 포함되어 있다면? -> 합격!
            if (place.getPlaceName().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(place);
            }
        }

        // 걸러진 리스트만 어댑터에 새로 꽂아주기
        binding.rvSearch.setAdapter(new HotPlaceAdapter(filteredList));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}