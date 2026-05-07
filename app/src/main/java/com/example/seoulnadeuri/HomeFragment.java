package com.example.seoulnadeuri;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.seoulnadeuri.databinding.FragmentHomeBinding;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // 프래그먼트용 뷰 바인딩 객체
    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 뷰 바인딩 세팅 (프래그먼트는 액티비티랑 살짝 다릅니다)
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // 1. 임시 데이터(가짜 핫플들) 만들기
        List<HotPlace> dummyList = new ArrayList<>();
        dummyList.add(new HotPlace("뚝섬 한강공원", "🟢 여유"));
        dummyList.add(new HotPlace("성수동 카페거리", "🔴 매우 붐빔"));
        dummyList.add(new HotPlace("홍대 상상마당", "🟡 보통"));
        dummyList.add(new HotPlace("강남역 11번 출구", "🔴 붐빔"));
        dummyList.add(new HotPlace("여의도 한강공원", "🟢 여유"));
        dummyList.add(new HotPlace("북촌 한옥마을", "🟡 보통"));
        dummyList.add(new HotPlace("명동 예술극장", "🔴 붐빔"));

        // 2. 어댑터에 데이터 넘겨주기
        HotPlaceAdapter adapter = new HotPlaceAdapter(dummyList);

        // 3. 리사이클러뷰 세팅 (세로로 스크롤되게 설정하고 어댑터 장착!)
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);

        return view;
    }

    // 화면이 꺼질 때 메모리 누수를 막아주는 국룰 코드
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}