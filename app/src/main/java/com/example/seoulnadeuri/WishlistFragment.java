package com.example.seoulnadeuri;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.seoulnadeuri.databinding.FragmentWishlistBinding;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    // onResume은 탭을 눌러서 이 화면이 보여질 때마다 실행됩니다.
    // 찜을 하고 돌아왔을 때 새로고침 되게 하려고 여기에 적습니다.
    @Override
    public void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        // 스마트폰에 저장된 찜 목록 불러오기
        SharedPreferences prefs = getContext().getSharedPreferences("SeoulWishlist", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        List<HotPlace> wishList = new ArrayList<>();
        Gson gson = new Gson();

        // 저장된 데이터(JSON)를 다시 HotPlace 객체로 변환
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String json = entry.getValue().toString();
            HotPlace place = gson.fromJson(json, HotPlace.class);
            wishList.add(place);
        }

        // 리스트에 데이터 꽂아주기
        if (wishList.isEmpty()) {
            binding.rvWishlist.setVisibility(View.GONE);
            binding.tvEmptyWishlist.setVisibility(View.VISIBLE);
        } else {
            binding.rvWishlist.setVisibility(View.VISIBLE);
            binding.tvEmptyWishlist.setVisibility(View.GONE);

            HotPlaceAdapter adapter = new HotPlaceAdapter(wishList);
            binding.rvWishlist.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.rvWishlist.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}