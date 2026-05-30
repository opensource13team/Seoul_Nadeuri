package com.example.seoulnadeuri;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.seoulnadeuri.databinding.ItemHotplaceBinding;
import java.util.List;

public class HotPlaceAdapter extends RecyclerView.Adapter<HotPlaceAdapter.ViewHolder> {

    private List<HotPlace> hotPlaceList;

    public HotPlaceAdapter(List<HotPlace> hotPlaceList) {
        this.hotPlaceList = hotPlaceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHotplaceBinding binding = ItemHotplaceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HotPlace item = hotPlaceList.get(position);
        holder.binding.tvPlaceName.setText(item.getPlaceName());
        holder.binding.tvCongestion.setText(item.getCongestion());
        holder.binding.tvWeatherInfo.setText(item.getWeatherInfo());
        holder.binding.tvPlaceInfo.setText(item.getPlaceInfo());

        // 👇 요기 추가! (리스트의 한 칸을 클릭했을 때 작동하는 이벤트)
        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);

            // 택배 상자에 데이터 욱여넣기
            intent.putExtra("PLACE_NAME", item.getPlaceName());
            intent.putExtra("CONGESTION", item.getCongestion());
            intent.putExtra("WEATHER_INFO", item.getWeatherInfo());
            intent.putExtra("PLACE_INFO", item.getPlaceInfo());
            intent.putExtra("EVENT_DETAIL", item.getEventDetail());

            context.startActivity(intent); // 상세 페이지로 화면 전환!
        });
    }

    @Override
    public int getItemCount() {
        return hotPlaceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemHotplaceBinding binding;
        public ViewHolder(ItemHotplaceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}