package com.example.seoulnadeuri;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.seoulnadeuri.databinding.ItemHotplaceBinding;
import java.util.List;

public class HotPlaceAdapter extends RecyclerView.Adapter<HotPlaceAdapter.ViewHolder> {

    private List<HotPlace> hotPlaceList; // 띄워줄 데이터 목록

    // 공장장에게 리스트 데이터를 전달받음
    public HotPlaceAdapter(List<HotPlace> hotPlaceList) {
        this.hotPlaceList = hotPlaceList;
    }

    // 1. 붕어빵 틀(XML)을 가져와서 찍어내는 곳
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHotplaceBinding binding = ItemHotplaceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    // 2. 찍어낸 붕어빵에 팥(데이터)을 넣어주는 곳
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HotPlace item = hotPlaceList.get(position);
        holder.binding.tvPlaceName.setText(item.getPlaceName());
        holder.binding.tvCongestion.setText(item.getCongestion());
    }

    // 3. 총 몇 개인지 알려주는 곳
    @Override
    public int getItemCount() {
        return hotPlaceList.size();
    }

    // 화면에 보여줄 뷰(텍스트뷰 등)를 꽉 쥐고 있는 녀석
    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemHotplaceBinding binding;

        public ViewHolder(ItemHotplaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}