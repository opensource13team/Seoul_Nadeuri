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
import com.google.android.material.chip.Chip;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RecordFragment extends Fragment {

    private FragmentRecordBinding binding;
    private final List<SeoulPlaceData> fullRawList = new ArrayList<>();
    private final Map<String, Float> indoorByPlace = new HashMap<>();
    private HotPlaceAdapter adapter;
    private String searchKeyword = "";

    private boolean filterFestival;
    private boolean filterLowCongestion;
    private boolean filterIndoor;
    private boolean filterOutdoor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.rvSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HotPlaceAdapter(new ArrayList<>());
        binding.rvSearch.setAdapter(adapter);

        loadPlaceMeta();

        fetchDataForSearch();
        setupSearch();
        setupFilterChips();

        return view;
    }

    private void loadPlaceMeta() {
        if (getContext() == null) return;
        try {
            InputStream is = getContext().getAssets().open("place_meta.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONArray jsonArray = new JSONArray(new String(buffer, "UTF-8"));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                indoorByPlace.put(
                        obj.getString("placeName"),
                        (float) obj.getDouble("indoorTag")
                );
            }
        } catch (Exception e) {
            // 메타 없어도 API 기반 필터는 동작
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchKeyword = s.toString();
                applyFilters();
            }
        });
    }

    private void setupFilterChips() {
        bindFilterChip(binding.chipFestival, checked -> filterFestival = checked);
        bindFilterChip(binding.chipLowCongestion, checked -> filterLowCongestion = checked);
        bindFilterChip(binding.chipIndoor, checked -> filterIndoor = checked);
        bindFilterChip(binding.chipOutdoor, checked -> filterOutdoor = checked);
    }

    private void bindFilterChip(Chip chip, FilterToggle toggle) {
        chip.setOnCheckedChangeListener((button, isChecked) -> {
            toggle.set(isChecked);
            applyFilters();
        });
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
                if (response.isSuccessful() && response.body() != null && binding != null) {
                    fullRawList.clear();
                    fullRawList.addAll(response.body());
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<SeoulPlaceData>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        if (binding == null) return;

        List<HotPlace> result = new ArrayList<>();
        for (SeoulPlaceData data : fullRawList) {
            if (!matchesKeyword(data)) continue;
            if (!matchesFilters(data)) continue;
            result.add(toHotPlace(data));
        }
        adapter.updateList(result);
    }

    private boolean matchesKeyword(SeoulPlaceData data) {
        if (searchKeyword.isEmpty()) return true;
        return data.placeName != null
                && data.placeName.toLowerCase().contains(searchKeyword.toLowerCase());
    }

    private boolean matchesFilters(SeoulPlaceData data) {
        if (filterFestival && data.localEvent < 1.0f) return false;
        if (filterLowCongestion && !"여유".equals(data.congestion)) return false;

        float indoorTag = getIndoorTag(data);
        if (filterIndoor && indoorTag < 1.0f) return false;
        if (filterOutdoor && indoorTag > 0.0f) return false;

        return true;
    }

    private float getIndoorTag(SeoulPlaceData data) {
        Float meta = indoorByPlace.get(data.placeName);
        return meta != null ? meta : data.indoorTag;
    }

    private HotPlace toHotPlace(SeoulPlaceData data) {
        String pmText = "좋음";
        if (data.pmIndex > 150) pmText = "매우 나쁨";
        else if (data.pmIndex > 80) pmText = "나쁨";
        else if (data.pmIndex > 30) pmText = "보통";

        String weatherStr = String.format(
                "🌡 %.1f℃ | 😷 미세먼지: %s | ☔ 강수량: %.1fmm",
                data.temp, pmText, data.rain);

        float indoorValue = getIndoorTag(data);
        String indoorText = "야외";
        if (indoorValue >= 1.0f) indoorText = "실내";
        else if (indoorValue == 0.5f) indoorText = "실내외 복합";

        String eventText = (data.localEvent >= 1.0f) ? "🎪 축제: 개최중" : "🎪 축제: 없음";
        String placeInfoStr = eventText + " | " + indoorText;
        String realEventName = (data.localEvent >= 1.0f && data.eventName != null && !data.eventName.isEmpty())
                ? "🎪 " + data.eventName
                : "현재 진행중인 축제/행사가 없습니다.";

        return new HotPlace(data.placeName, data.congestion, weatherStr, placeInfoStr, realEventName);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    private interface FilterToggle {
        void set(boolean checked);
    }
}
