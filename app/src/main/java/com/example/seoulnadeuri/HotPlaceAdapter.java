package com.example.seoulnadeuri;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HotPlaceAdapter extends RecyclerView.Adapter<HotPlaceAdapter.ViewHolder> {

    private List<HotPlace> placeList;

    public HotPlaceAdapter(List<HotPlace> placeList) {
        this.placeList = placeList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hotplace, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HotPlace item = placeList.get(position);

        holder.tvPlaceName.setText(item.getPlaceName());
        holder.tvCongestion.setText(item.getCongestion());
        holder.tvWeather.setText(item.getWeatherInfo());
        holder.tvPlaceInfo.setText(item.getPlaceInfo());

        // 👇 AI 점수가 들어있으면 보여주고, 아니면 숨김 처리!
        if (item.getAiScore() != null && !item.getAiScore().isEmpty()) {
            holder.tvAiScore.setVisibility(View.VISIBLE);
            holder.tvAiScore.setText(item.getAiScore());
        } else {
            holder.tvAiScore.setVisibility(View.GONE);
        }

        // 클릭 시 상세 페이지(DetailActivity)로 이동 및 데이터 전달
        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);

            intent.putExtra("PLACE_NAME", item.getPlaceName());
            intent.putExtra("CONGESTION", item.getCongestion());
            intent.putExtra("WEATHER_INFO", item.getWeatherInfo());
            intent.putExtra("PLACE_INFO", item.getPlaceInfo());
            intent.putExtra("EVENT_DETAIL", item.getEventDetail());
            intent.putExtra("IMAGE_URL", item.getImageUrl());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return placeList.size();
    }

    public void updateList(List<HotPlace> newList) {
        this.placeList = newList;
        notifyDataSetChanged(); // "데이터 바뀌었으니 화면 다시 싹 그려라!" 하고 새로고침하는 핵심 명령어
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaceName, tvCongestion, tvWeather, tvPlaceInfo, tvAiScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tv_place_name);
            tvCongestion = itemView.findViewById(R.id.tv_congestion);
            tvWeather = itemView.findViewById(R.id.tv_weather_info);
            tvPlaceInfo = itemView.findViewById(R.id.tv_place_info);
            tvAiScore = itemView.findViewById(R.id.tv_ai_score); // 요기 추가!
        }
    }
}