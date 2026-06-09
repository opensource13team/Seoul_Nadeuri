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
import androidx.recyclerview.widget.GridLayoutManager; // 💡 패키지 변경됨!
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

    @Override
    public void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        SharedPreferences prefs = getContext().getSharedPreferences("SeoulWishlist", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        List<HotPlace> wishList = new ArrayList<>();
        Gson gson = new Gson();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String json = entry.getValue().toString();
            HotPlace place = gson.fromJson(json, HotPlace.class);
            wishList.add(place);
        }

        if (wishList.isEmpty()) {
            binding.rvWishlist.setVisibility(View.GONE);
            binding.tvEmptyWishlist.setVisibility(View.VISIBLE);
        } else {
            binding.rvWishlist.setVisibility(View.VISIBLE);
            binding.tvEmptyWishlist.setVisibility(View.GONE);

            // 💡 찜목록 전용 어댑터로 교체!
            WishlistAdapter adapter = new WishlistAdapter(wishList);

            // 💡 2열 바둑판(Grid) 형태로 변경!
            binding.rvWishlist.setLayoutManager(new GridLayoutManager(getContext(), 2));
            binding.rvWishlist.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}